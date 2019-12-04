/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.cassandra.db.view;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.google.common.collect.Iterables;

import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.ColumnDefinition;
import org.apache.cassandra.cql3.ColumnIdentifier;
import org.apache.cassandra.db.CBuilder;
import org.apache.cassandra.db.Clustering;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.Conflicts;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.DeletionInfo;
import org.apache.cassandra.db.DeletionTime;
import org.apache.cassandra.db.LivenessInfo;
import org.apache.cassandra.db.RangeTombstone;
import org.apache.cassandra.db.Slice;
import org.apache.cassandra.db.marshal.CompositeType;
import org.apache.cassandra.db.partitions.AbstractThreadUnsafePartition;
import org.apache.cassandra.db.rows.BufferCell;
import org.apache.cassandra.db.rows.Cell;
import org.apache.cassandra.db.rows.CellPath;
import org.apache.cassandra.db.rows.Row;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.cassandra.utils.FBUtilities;

/**
 * Represents a single CQL Row in a base table, with both the currently persisted value and the update's value. The
 * values are stored in timestamp order, but also indicate whether they are from the currently persisted, allowing a
 * {@link TemporalRow.Resolver} to resolve if the value is an old value that has been updated; if it sorts after the
 * update's value, then it does not qualify.
 */
public class TemporalRow
{
    private static final int NO_TTL = LivenessInfo.NO_TTL;
    private static final long NO_TIMESTAMP = LivenessInfo.NO_TIMESTAMP;
    private static final int NO_DELETION_TIME = DeletionTime.LIVE.localDeletionTime();

    public interface Resolver
    {
        /**
         * @param cells Iterable of all cells for a certain TemporalRow's Cell, in timestamp sorted order
         * @return      A single TemporalCell from the iterable which satisfies the resolution criteria, or null if
         *              there is no cell which qualifies
         */
        TemporalCell resolve(Iterable<TemporalCell> cells);
    }

    /**
     * Returns the first value in the iterable if it is from the set of persisted cells, and the cell which results from
     * reconciliation of the remaining cells does not have the same value.
     */
    public static final Resolver oldValueIfUpdated = cells -> {
        Iterator<TemporalCell> iterator = cells.iterator();
        if (!iterator.hasNext())
            return null;

        TemporalCell initial = iterator.next();
        if (initial.isNew || !iterator.hasNext())
            return null;

        TemporalCell value = initial;
        while (iterator.hasNext())
            value = value.reconcile(iterator.next());

        return ByteBufferUtil.compareUnsigned(initial.value, value.value) != 0 ? initial : null;
    };

    public static final Resolver earliest = cells -> {
        Iterator<TemporalCell> iterator = cells.iterator();
        if (!iterator.hasNext())
            return null;
        return iterator.next();
    };

    public static final Resolver latest = cells -> {
        Iterator<TemporalCell> iterator = cells.iterator();
        if (!iterator.hasNext())
            return null;

        TemporalCell value = iterator.next();
        while (iterator.hasNext())
            value = value.reconcile(iterator.next());

        return value;
    };

    private static class TemporalCell
    {
        public final ByteBuffer value;
        public final long timestamp;
        public final int ttl;
        public final int localDeletionTime;
        public final boolean isNew;

        private TemporalCell(ByteBuffer value, long timestamp, int ttl, int localDeletionTime, boolean isNew)
        {
            this.value = value;
            this.timestamp = timestamp;
            this.ttl = ttl;
            this.localDeletionTime = localDeletionTime;
            this.isNew = isNew;
        }

        public TemporalCell reconcile(TemporalCell that)
        {
            int now = FBUtilities.nowInSeconds();
            Conflicts.Resolution resolution = Conflicts.resolveRegular(that.timestamp,
                                                                       that.isLive(now),
                                                                       that.localDeletionTime,
                                                                       that.value,
                                                                       this.timestamp,
                                                                       this.isLive(now),
                                                                       this.localDeletionTime,
                                                                       this.value);
            assert resolution != Conflicts.Resolution.MERGE;
            if (resolution == Conflicts.Resolution.LEFT_WINS)
                return that;
            return this;
        }

        private boolean isLive(int now)
        {
            return localDeletionTime == NO_DELETION_TIME || (ttl != NO_TTL && now < localDeletionTime);
        }

        public Cell cell(ColumnDefinition definition, CellPath cellPath)
        {
            return new BufferCell(definition, timestamp, ttl, localDeletionTime, value, cellPath);
        }
    }

    private final ColumnFamilyStore baseCfs;
    private final java.util.Set<ColumnIdentifier> viewPrimaryKey;
    private final ByteBuffer basePartitionKey;
    public final Map<ColumnIdentifier, ByteBuffer> clusteringColumns;
    public final int nowInSec;
    private final Map<ColumnIdentifier, Map<CellPath, SortedMap<Long, TemporalCell>>> columnValues = new HashMap<>();
    private int viewClusteringTtl = NO_TTL;
    private long viewClusteringTimestamp = NO_TIMESTAMP;
    private int viewClusteringLocalDeletionTime = NO_DELETION_TIME;

    TemporalRow(ColumnFamilyStore baseCfs, java.util.Set<ColumnIdentifier> viewPrimaryKey, ByteBuffer key, Row row, int nowInSec, boolean isNew)
    {
        this.baseCfs = baseCfs;
        this.viewPrimaryKey = viewPrimaryKey;
        this.basePartitionKey = key;
        this.nowInSec = nowInSec;
        clusteringColumns = new HashMap<>();
        LivenessInfo liveness = row.primaryKeyLivenessInfo();
        this.viewClusteringLocalDeletionTime = minValueIfSet(viewClusteringLocalDeletionTime, row.deletion().localDeletionTime(), NO_DELETION_TIME);
        this.viewClusteringTimestamp = minValueIfSet(viewClusteringTimestamp, liveness.timestamp(), NO_TIMESTAMP);
        this.viewClusteringTtl = minValueIfSet(viewClusteringTtl, liveness.ttl(), NO_TTL);

        List<ColumnDefinition> clusteringDefs = baseCfs.metadata.clusteringColumns();
        for (int i = 0; i < clusteringDefs.size(); i++)
        {
            ColumnDefinition cdef = clusteringDefs.get(i);
            clusteringColumns.put(cdef.name, row.clustering().get(i));

            addColumnValue(cdef.name, null, NO_TIMESTAMP, NO_TTL, NO_DELETION_TIME, row.clustering().get(i), isNew);
        }
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TemporalRow that = (TemporalRow) o;

        if (!clusteringColumns.equals(that.clusteringColumns)) return false;
        if (!basePartitionKey.equals(that.basePartitionKey)) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = basePartitionKey.hashCode();
        result = 31 * result + clusteringColumns.hashCode();
        return result;
    }

    public void addColumnValue(ColumnIdentifier identifier,
                               CellPath cellPath,
                               long timestamp,
                               int ttl,
                               int localDeletionTime,
                               ByteBuffer value,  boolean isNew)
    {
        if (!columnValues.containsKey(identifier))
            columnValues.put(identifier, new HashMap<>());

        Map<CellPath, SortedMap<Long, TemporalCell>> innerMap = columnValues.get(identifier);

        if (!innerMap.containsKey(cellPath))
            innerMap.put(cellPath, new TreeMap<>());

        // If this column is part of the view's primary keys
        if (viewPrimaryKey.contains(identifier))
        {
            this.viewClusteringTtl = minValueIfSet(this.viewClusteringTtl, ttl, NO_TTL);
            this.viewClusteringTimestamp = minValueIfSet(this.viewClusteringTimestamp, timestamp, NO_TIMESTAMP);
            this.viewClusteringLocalDeletionTime = minValueIfSet(this.viewClusteringLocalDeletionTime, localDeletionTime, NO_DELETION_TIME);
        }

        innerMap.get(cellPath).put(timestamp, new TemporalCell(value, timestamp, ttl, localDeletionTime, isNew));
    }

    private static int minValueIfSet(int existing, int update, int defaultValue)
    {
        if (existing == defaultValue)
            return update;
        if (update == defaultValue)
            return existing;
        return Math.min(existing, update);
    }

    private static long minValueIfSet(long existing, long update, long defaultValue)
    {
        if (existing == defaultValue)
            return update;
        if (update == defaultValue)
            return existing;
        return Math.min(existing, update);
    }

    public int viewClusteringTtl()
    {
        return viewClusteringTtl;
    }

    public long viewClusteringTimestamp()
    {
        return viewClusteringTimestamp;
    }

    public int viewClusteringLocalDeletionTime()
    {
        return viewClusteringLocalDeletionTime;
    }

    public void addCell(Cell cell, boolean isNew)
    {
        addColumnValue(cell.column().name, cell.path(), cell.timestamp(), cell.ttl(), cell.localDeletionTime(), cell.value(), isNew);
    }

    // The Definition here is actually the *base table* definition
    public ByteBuffer clusteringValue(ColumnDefinition definition, Resolver resolver)
    {
        ColumnDefinition baseDefinition = definition.cfName.equals(baseCfs.name)
                                          ? definition
                                          : baseCfs.metadata.getColumnDefinition(definition.name);

        if (baseDefinition.isPartitionKey())
        {
            if (baseDefinition.isOnAllComponents())
                return basePartitionKey;
            else
            {
                CompositeType keyComparator = (CompositeType) baseCfs.metadata.getKeyValidator();
                ByteBuffer[] components = keyComparator.split(basePartitionKey);
                return components[baseDefinition.position()];
            }
        }
        else
        {
            ColumnIdentifier columnIdentifier = baseDefinition.name;

            if (clusteringColumns.containsKey(columnIdentifier))
                return clusteringColumns.get(columnIdentifier);

            Collection<org.apache.cassandra.db.rows.Cell> val = values(definition, resolver);
            if (val != null && val.size() == 1)
                return Iterables.getOnlyElement(val).value();
        }
        return null;
    }

    public DeletionTime deletionTime(AbstractThreadUnsafePartition partition)
    {
        DeletionInfo deletionInfo = partition.deletionInfo();
        if (!deletionInfo.getPartitionDeletion().isLive())
            return deletionInfo.getPartitionDeletion();

        Clustering baseClustering = baseClusteringBuilder().build();
        RangeTombstone clusterTombstone = deletionInfo.rangeCovering(baseClustering);
        if (clusterTombstone != null)
            return clusterTombstone.deletionTime();

        Row row = partition.getRow(baseClustering);
        return row == null || row.deletion().isLive() ? DeletionTime.LIVE : row.deletion();
    }

    public Collection<org.apache.cassandra.db.rows.Cell> values(ColumnDefinition definition, Resolver resolver)
    {
        Map<CellPath, SortedMap<Long, TemporalCell>> innerMap = columnValues.get(definition.name);
        if (innerMap == null)
        {
            return Collections.emptyList();
        }

        Collection<org.apache.cassandra.db.rows.Cell> value = new ArrayList<>();
        for (Map.Entry<CellPath, SortedMap<Long, TemporalCell>> pathAndCells : innerMap.entrySet())
        {
            TemporalCell cell = resolver.resolve(pathAndCells.getValue().values());

            if (cell != null)
                value.add(cell.cell(definition, pathAndCells.getKey()));
        }
        return value;
    }

    public Slice baseSlice()
    {
        return baseClusteringBuilder().buildSlice();
    }

    private CBuilder baseClusteringBuilder()
    {
        CFMetaData metadata = baseCfs.metadata;
        CBuilder builder = CBuilder.create(metadata.comparator);

        ByteBuffer[] buffers = new ByteBuffer[clusteringColumns.size()];
        for (Map.Entry<ColumnIdentifier, ByteBuffer> buffer : clusteringColumns.entrySet())
            buffers[metadata.getColumnDefinition(buffer.getKey()).position()] = buffer.getValue();

        for (ByteBuffer byteBuffer : buffers)
            builder = builder.add(byteBuffer);

        return builder;
    }

    static class Set implements Iterable<TemporalRow>
    {
        private final ColumnFamilyStore baseCfs;
        private final java.util.Set<ColumnIdentifier> viewPrimaryKey;
        private final ByteBuffer key;
        public final DecoratedKey dk;
        private final Map<Clustering, TemporalRow> clusteringToRow;
        final int nowInSec = FBUtilities.nowInSeconds();

        Set(ColumnFamilyStore baseCfs, java.util.Set<ColumnIdentifier> viewPrimaryKey, ByteBuffer key)
        {
            this.baseCfs = baseCfs;
            this.viewPrimaryKey = viewPrimaryKey;
            this.key = key;
            this.dk = baseCfs.partitioner.decorateKey(key);
            this.clusteringToRow = new HashMap<>();
        }

        public Iterator<TemporalRow> iterator()
        {
            return clusteringToRow.values().iterator();
        }

        public TemporalRow getClustering(Clustering clustering)
        {
            return clusteringToRow.get(clustering);
        }

        public void addRow(Row row, boolean isNew)
        {
            TemporalRow temporalRow = clusteringToRow.get(row.clustering());
            if (temporalRow == null)
            {
                temporalRow = new TemporalRow(baseCfs, viewPrimaryKey, key, row, nowInSec, isNew);
                clusteringToRow.put(row.clustering(), temporalRow);
            }

            for (Cell cell: row.cells())
            {
                temporalRow.addCell(cell, isNew);
            }
        }

        public int size()
        {
            return clusteringToRow.size();
        }
    }
}
