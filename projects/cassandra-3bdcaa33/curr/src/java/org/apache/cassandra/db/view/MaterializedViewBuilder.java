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

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.concurrent.ScheduledExecutors;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.Mutation;
import org.apache.cassandra.db.ReadOrderGroup;
import org.apache.cassandra.db.SinglePartitionReadCommand;
import org.apache.cassandra.db.SystemKeyspace;
import org.apache.cassandra.db.compaction.CompactionInfo;
import org.apache.cassandra.db.compaction.CompactionManager;
import org.apache.cassandra.db.compaction.OperationType;
import org.apache.cassandra.db.lifecycle.SSTableSet;
import org.apache.cassandra.db.lifecycle.View;
import org.apache.cassandra.db.partitions.FilteredPartition;
import org.apache.cassandra.db.partitions.PartitionIterator;
import org.apache.cassandra.db.rows.RowIterator;
import org.apache.cassandra.dht.Range;
import org.apache.cassandra.dht.Token;
import org.apache.cassandra.exceptions.WriteTimeoutException;
import org.apache.cassandra.io.sstable.ReducingKeyIterator;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.service.StorageProxy;
import org.apache.cassandra.service.StorageService;
import org.apache.cassandra.service.pager.QueryPager;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.NoSpamLogger;
import org.apache.cassandra.utils.Pair;
import org.apache.cassandra.utils.UUIDGen;
import org.apache.cassandra.utils.concurrent.Refs;

public class MaterializedViewBuilder extends CompactionInfo.Holder
{
    private final ColumnFamilyStore baseCfs;
    private final MaterializedView view;
    private final UUID compactionId;
    private volatile Token prevToken = null;

    private static final Logger logger = LoggerFactory.getLogger(MaterializedViewBuilder.class);

    private volatile boolean isStopped = false;

    public MaterializedViewBuilder(ColumnFamilyStore baseCfs, MaterializedView view)
    {
        this.baseCfs = baseCfs;
        this.view = view;
        compactionId = UUIDGen.getTimeUUID();
    }

    private void buildKey(DecoratedKey key)
    {
        QueryPager pager = SinglePartitionReadCommand.fullPartitionRead(baseCfs.metadata, FBUtilities.nowInSeconds(), key).getPager(null);

        while (!pager.isExhausted())
        {
           try (ReadOrderGroup orderGroup = pager.startOrderGroup();
                PartitionIterator partitionIterator = pager.fetchPageInternal(128, orderGroup))
           {
               if (!partitionIterator.hasNext())
                   return;

               try (RowIterator rowIterator = partitionIterator.next())
               {
                   Collection<Mutation> mutations = view.createMutations(key.getKey(), FilteredPartition.create(rowIterator), true);

                   if (mutations != null)
                   {
                       try
                       {
                           StorageProxy.mutateMV(key.getKey(), mutations);
                           break;
                       }
                       catch (WriteTimeoutException ex)
                       {
                           NoSpamLogger.getLogger(logger, 1, TimeUnit.MINUTES)
                                       .warn("Encountered write timeout when building materialized view {}, the entries were stored in the batchlog and will be replayed at another time", view.name);
                       }
                   }
               }
           }
        }
    }

    public void run()
    {
        String ksname = baseCfs.metadata.ksName, viewName = view.name;

        if (SystemKeyspace.isViewBuilt(ksname, viewName))
            return;

        Iterable<Range<Token>> ranges = StorageService.instance.getLocalRanges(baseCfs.metadata.ksName);
        final Pair<Integer, Token> buildStatus = SystemKeyspace.getMaterializedViewBuildStatus(ksname, viewName);
        Token lastToken;
        Function<View, Iterable<SSTableReader>> function;
        if (buildStatus == null)
        {
            baseCfs.forceBlockingFlush();
            function = View.select(SSTableSet.CANONICAL);
            int generation = Integer.MIN_VALUE;

            try (Refs<SSTableReader> temp = baseCfs.selectAndReference(function).refs)
            {
                for (SSTableReader reader : temp)
                {
                    generation = Math.max(reader.descriptor.generation, generation);
                }
            }

            SystemKeyspace.beginMaterializedViewBuild(ksname, viewName, generation);
            lastToken = null;
        }
        else
        {
            function = new Function<View, Iterable<SSTableReader>>()
            {
                @Nullable
                public Iterable<SSTableReader> apply(View view)
                {
                    Iterable<SSTableReader> readers = View.select(SSTableSet.CANONICAL).apply(view);
                    if (readers != null)
                        return Iterables.filter(readers, ssTableReader -> ssTableReader.descriptor.generation <= buildStatus.left);
                    return null;
                }
            };
            lastToken = buildStatus.right;
        }

        prevToken = lastToken;
        try (Refs<SSTableReader> sstables = baseCfs.selectAndReference(function).refs;
             ReducingKeyIterator iter = new ReducingKeyIterator(sstables))
        {
            while (!isStopped && iter.hasNext())
            {
                DecoratedKey key = iter.next();
                Token token = key.getToken();
                if (lastToken == null || lastToken.compareTo(token) < 0)
                {
                    for (Range<Token> range : ranges)
                    {
                        if (range.contains(token))
                        {
                            buildKey(key);

                            if (prevToken == null || prevToken.compareTo(token) != 0)
                            {
                                SystemKeyspace.updateMaterializedViewBuildStatus(ksname, viewName, key.getToken());
                                prevToken = token;
                            }
                        }
                    }
                    lastToken = null;
                }
            }

            SystemKeyspace.finishMaterializedViewBuildStatus(ksname, viewName);

        }
        catch (Exception e)
        {
            final MaterializedViewBuilder builder = new MaterializedViewBuilder(baseCfs, view);
            ScheduledExecutors.nonPeriodicTasks.schedule(() -> CompactionManager.instance.submitMaterializedViewBuilder(builder),
                                                         5,
                                                         TimeUnit.MINUTES);
            logger.warn("Materialized View failed to complete, sleeping 5 minutes before restarting", e);
        }
    }

    public CompactionInfo getCompactionInfo()
    {
        long rangesLeft = 0, rangesTotal = 0;
        Token lastToken = prevToken;

        // This approximation is not very accurate, but since we do not have a method which allows us to calculate the
        // percentage of a range covered by a second range, this is the best approximation that we can calculate.
        // Instead, we just count the total number of ranges that haven't been seen by the node (we use the order of
        // the tokens to determine whether they have been seen yet or not), and the total number of ranges that a node
        // has.
        for (Range<Token> range : StorageService.instance.getLocalRanges(baseCfs.keyspace.getName()))
        {
            rangesLeft++;
            rangesTotal++;
            // This will reset rangesLeft, so that the number of ranges left will be less than the total ranges at the
            // end of the method.
            if (lastToken == null || range.contains(lastToken))
                rangesLeft = 0;
        }
        return new CompactionInfo(baseCfs.metadata, OperationType.VIEW_BUILD, rangesLeft, rangesTotal, "ranges", compactionId);
    }

    public void stop()
    {
        isStopped = true;
    }
}
