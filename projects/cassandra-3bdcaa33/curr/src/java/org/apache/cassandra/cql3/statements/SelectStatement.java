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
package org.apache.cassandra.cql3.statements;

import java.nio.ByteBuffer;
import java.util.*;

import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.auth.Permission;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.ColumnDefinition;
import org.apache.cassandra.config.Schema;
import org.apache.cassandra.cql3.*;
import org.apache.cassandra.cql3.functions.Function;
import org.apache.cassandra.cql3.restrictions.StatementRestrictions;
import org.apache.cassandra.cql3.selection.RawSelector;
import org.apache.cassandra.cql3.selection.Selection;
import org.apache.cassandra.db.*;
import org.apache.cassandra.db.rows.*;
import org.apache.cassandra.db.partitions.*;
import org.apache.cassandra.db.filter.*;
import org.apache.cassandra.db.index.SecondaryIndexManager;
import org.apache.cassandra.db.marshal.CollectionType;
import org.apache.cassandra.db.marshal.CompositeType;
import org.apache.cassandra.db.marshal.Int32Type;
import org.apache.cassandra.dht.AbstractBounds;
import org.apache.cassandra.exceptions.*;
import org.apache.cassandra.serializers.MarshalException;
import org.apache.cassandra.service.*;
import org.apache.cassandra.service.pager.QueryPager;
import org.apache.cassandra.service.pager.PagingState;
import org.apache.cassandra.thrift.ThriftValidation;
import org.apache.cassandra.transport.messages.ResultMessage;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.cassandra.utils.FBUtilities;

import static org.apache.cassandra.cql3.statements.RequestValidations.checkFalse;
import static org.apache.cassandra.cql3.statements.RequestValidations.checkNotNull;
import static org.apache.cassandra.cql3.statements.RequestValidations.checkTrue;
import static org.apache.cassandra.cql3.statements.RequestValidations.invalidRequest;
import static org.apache.cassandra.utils.ByteBufferUtil.UNSET_BYTE_BUFFER;

/**
 * Encapsulates a completely parsed SELECT query, including the target
 * column family, expression, result count, and ordering clause.
 *
 * A number of public methods here are only used internally. However,
 * many of these are made accessible for the benefit of custom
 * QueryHandler implementations, so before reducing their accessibility
 * due consideration should be given.
 */
public class SelectStatement implements CQLStatement
{
    private static final Logger logger = LoggerFactory.getLogger(SelectStatement.class);

    private static final int DEFAULT_COUNT_PAGE_SIZE = 10000;

    private final int boundTerms;
    public final CFMetaData cfm;
    public final Parameters parameters;
    private final Selection selection;
    private final Term limit;

    private final StatementRestrictions restrictions;

    private final boolean isReversed;

    /**
     * The comparator used to orders results when multiple keys are selected (using IN).
     */
    private final Comparator<List<ByteBuffer>> orderingComparator;

    private final ColumnFilter queriedColumns;

    // Used by forSelection below
    private static final Parameters defaultParameters = new Parameters(Collections.<ColumnIdentifier.Raw, Boolean>emptyMap(), false, false, false);

    public SelectStatement(CFMetaData cfm,
                           int boundTerms,
                           Parameters parameters,
                           Selection selection,
                           StatementRestrictions restrictions,
                           boolean isReversed,
                           Comparator<List<ByteBuffer>> orderingComparator,
                           Term limit)
    {
        this.cfm = cfm;
        this.boundTerms = boundTerms;
        this.selection = selection;
        this.restrictions = restrictions;
        this.isReversed = isReversed;
        this.orderingComparator = orderingComparator;
        this.parameters = parameters;
        this.limit = limit;
        this.queriedColumns = gatherQueriedColumns();
    }

    public Iterable<Function> getFunctions()
    {
        return Iterables.concat(selection.getFunctions(),
                                restrictions.getFunctions(),
                                limit != null ? limit.getFunctions() : Collections.<Function>emptySet());
    }

    // Note that the queried columns internally is different from the one selected by the
    // user as it also include any column for which we have a restriction on.
    private ColumnFilter gatherQueriedColumns()
    {
        if (selection.isWildcard())
            return ColumnFilter.all(cfm);

        ColumnFilter.Builder builder = ColumnFilter.allColumnsBuilder(cfm);
        // Adds all selected columns
        for (ColumnDefinition def : selection.getColumns())
            if (!def.isPrimaryKeyColumn())
                builder.add(def);
        // as well as any restricted column (so we can actually apply the restriction)
        builder.addAll(restrictions.nonPKRestrictedColumns());
        return builder.build();
    }

    // Creates a simple select based on the given selection.
    // Note that the results select statement should not be used for actual queries, but only for processing already
    // queried data through processColumnFamily.
    static SelectStatement forSelection(CFMetaData cfm, Selection selection)
    {
        return new SelectStatement(cfm,
                                   0,
                                   defaultParameters,
                                   selection,
                                   StatementRestrictions.empty(cfm),
                                   false,
                                   null,
                                   null);
    }

    public ResultSet.ResultMetadata getResultMetadata()
    {
        return selection.getResultMetadata(parameters.isJson);
    }

    public int getBoundTerms()
    {
        return boundTerms;
    }

    public void checkAccess(ClientState state) throws InvalidRequestException, UnauthorizedException
    {
        state.hasColumnFamilyAccess(keyspace(), columnFamily(), Permission.SELECT);
        for (Function function : getFunctions())
            state.ensureHasPermission(Permission.EXECUTE, function);
    }

    public void validate(ClientState state) throws InvalidRequestException
    {
        // Nothing to do, all validation has been done by RawStatement.prepare()
    }

    public ResultMessage.Rows execute(QueryState state, QueryOptions options) throws RequestExecutionException, RequestValidationException
    {
        ConsistencyLevel cl = options.getConsistency();
        checkNotNull(cl, "Invalid empty consistency level");

        cl.validateForRead(keyspace());

        int nowInSec = FBUtilities.nowInSeconds();
        ReadQuery query = getQuery(options, nowInSec);

        int pageSize = getPageSize(options);

        if (pageSize <= 0 || query.limits().count() <= pageSize)
            return execute(query, options, state, nowInSec);

        QueryPager pager = query.getPager(options.getPagingState());
        return execute(Pager.forDistributedQuery(pager, cl, state.getClientState()), options, pageSize, nowInSec);
    }

    private int getPageSize(QueryOptions options)
    {
        int pageSize = options.getPageSize();

        // An aggregation query will never be paged for the user, but we always page it internally to avoid OOM.
        // If we user provided a pageSize we'll use that to page internally (because why not), otherwise we use our default
        // Note that if there are some nodes in the cluster with a version less than 2.0, we can't use paging (CASSANDRA-6707).
        if (selection.isAggregate() && pageSize <= 0)
            pageSize = DEFAULT_COUNT_PAGE_SIZE;

        return  pageSize;
    }

    public ReadQuery getQuery(QueryOptions options, int nowInSec) throws RequestValidationException
    {
        DataLimits limit = getLimit(options);
        if (restrictions.isKeyRange() || restrictions.usesSecondaryIndexing())
            return getRangeCommand(options, limit, nowInSec);

        return getSliceCommands(options, limit, nowInSec);
    }

    private ResultMessage.Rows execute(ReadQuery query, QueryOptions options, QueryState state, int nowInSec) throws RequestValidationException, RequestExecutionException
    {
        try (PartitionIterator data = query.execute(options.getConsistency(), state.getClientState()))
        {
            return processResults(data, options, nowInSec);
        }
    }

    // Simple wrapper class to avoid some code duplication
    private static abstract class Pager
    {
        protected QueryPager pager;

        protected Pager(QueryPager pager)
        {
            this.pager = pager;
        }

        public static Pager forInternalQuery(QueryPager pager, ReadOrderGroup orderGroup)
        {
            return new InternalPager(pager, orderGroup);
        }

        public static Pager forDistributedQuery(QueryPager pager, ConsistencyLevel consistency, ClientState clientState)
        {
            return new NormalPager(pager, consistency, clientState);
        }

        public boolean isExhausted()
        {
            return pager.isExhausted();
        }

        public PagingState state()
        {
            return pager.state();
        }

        public abstract PartitionIterator fetchPage(int pageSize);

        public static class NormalPager extends Pager
        {
            private final ConsistencyLevel consistency;
            private final ClientState clientState;

            private NormalPager(QueryPager pager, ConsistencyLevel consistency, ClientState clientState)
            {
                super(pager);
                this.consistency = consistency;
                this.clientState = clientState;
            }

            public PartitionIterator fetchPage(int pageSize)
            {
                return pager.fetchPage(pageSize, consistency, clientState);
            }
        }

        public static class InternalPager extends Pager
        {
            private final ReadOrderGroup orderGroup;

            private InternalPager(QueryPager pager, ReadOrderGroup orderGroup)
            {
                super(pager);
                this.orderGroup = orderGroup;
            }

            public PartitionIterator fetchPage(int pageSize)
            {
                return pager.fetchPageInternal(pageSize, orderGroup);
            }
        }
    }

    private ResultMessage.Rows execute(Pager pager, QueryOptions options, int pageSize, int nowInSec)
    throws RequestValidationException, RequestExecutionException
    {
        if (selection.isAggregate())
            return pageAggregateQuery(pager, options, pageSize, nowInSec);

        // We can't properly do post-query ordering if we page (see #6722)
        checkFalse(needsPostQueryOrdering(),
                  "Cannot page queries with both ORDER BY and a IN restriction on the partition key;"
                  + " you must either remove the ORDER BY or the IN and sort client side, or disable paging for this query");

        ResultMessage.Rows msg;
        try (PartitionIterator page = pager.fetchPage(pageSize))
        {
            msg = processResults(page, options, nowInSec);
        }

        // Please note that the isExhausted state of the pager only gets updated when we've closed the page, so this
        // shouldn't be moved inside the 'try' above.
        if (!pager.isExhausted())
            msg.result.metadata.setHasMorePages(pager.state());

        return msg;
    }

    private ResultMessage.Rows pageAggregateQuery(Pager pager, QueryOptions options, int pageSize, int nowInSec)
    throws RequestValidationException, RequestExecutionException
    {
        Selection.ResultSetBuilder result = selection.resultSetBuilder(parameters.isJson);
        while (!pager.isExhausted())
        {
            try (PartitionIterator iter = pager.fetchPage(pageSize))
            {
                while (iter.hasNext())
                    processPartition(iter.next(), options, result, nowInSec);
            }
        }
        return new ResultMessage.Rows(result.build(options.getProtocolVersion()));
    }

    private ResultMessage.Rows processResults(PartitionIterator partitions, QueryOptions options, int nowInSec) throws RequestValidationException
    {
        ResultSet rset = process(partitions, options, nowInSec);
        return new ResultMessage.Rows(rset);
    }

    public ResultMessage.Rows executeInternal(QueryState state, QueryOptions options) throws RequestExecutionException, RequestValidationException
    {
        int nowInSec = FBUtilities.nowInSeconds();
        ReadQuery query = getQuery(options, nowInSec);
        int pageSize = getPageSize(options);

        try (ReadOrderGroup orderGroup = query.startOrderGroup())
        {
            if (pageSize <= 0 || query.limits().count() <= pageSize)
            {
                try (PartitionIterator data = query.executeInternal(orderGroup))
                {
                    return processResults(data, options, nowInSec);
                }
            }
            else
            {
                QueryPager pager = query.getPager(options.getPagingState());
                return execute(Pager.forInternalQuery(pager, orderGroup), options, pageSize, nowInSec);
            }
        }
    }

    public ResultSet process(PartitionIterator partitions, int nowInSec) throws InvalidRequestException
    {
        return process(partitions, QueryOptions.DEFAULT, nowInSec);
    }

    public String keyspace()
    {
        return cfm.ksName;
    }

    public String columnFamily()
    {
        return cfm.cfName;
    }

    /**
     * May be used by custom QueryHandler implementations
     */
    public Selection getSelection()
    {
        return selection;
    }

    /**
     * May be used by custom QueryHandler implementations
     */
    public StatementRestrictions getRestrictions()
    {
        return restrictions;
    }

    private ReadQuery getSliceCommands(QueryOptions options, DataLimits limit, int nowInSec) throws RequestValidationException
    {
        Collection<ByteBuffer> keys = restrictions.getPartitionKeys(options);
        if (keys.isEmpty())
            return ReadQuery.EMPTY;

        ClusteringIndexFilter filter = makeClusteringIndexFilter(options);
        if (filter == null)
            return ReadQuery.EMPTY;

        RowFilter rowFilter = getRowFilter(options);

        // Note that we use the total limit for every key, which is potentially inefficient.
        // However, IN + LIMIT is not a very sensible choice.
        List<SinglePartitionReadCommand<?>> commands = new ArrayList<>(keys.size());
        for (ByteBuffer key : keys)
        {
            QueryProcessor.validateKey(key);
            DecoratedKey dk = StorageService.getPartitioner().decorateKey(ByteBufferUtil.clone(key));
            commands.add(SinglePartitionReadCommand.create(cfm, nowInSec, queriedColumns, rowFilter, limit, dk, filter));
        }

        return new SinglePartitionReadCommand.Group(commands, limit);
    }

    private ReadQuery getRangeCommand(QueryOptions options, DataLimits limit, int nowInSec) throws RequestValidationException
    {
        ClusteringIndexFilter clusteringIndexFilter = makeClusteringIndexFilter(options);
        if (clusteringIndexFilter == null)
            return ReadQuery.EMPTY;

        RowFilter rowFilter = getRowFilter(options);
        // The LIMIT provided by the user is the number of CQL row he wants returned.
        // We want to have getRangeSlice to count the number of columns, not the number of keys.
        AbstractBounds<PartitionPosition> keyBounds = restrictions.getPartitionKeyBounds(options);
        return keyBounds == null
             ? ReadQuery.EMPTY
             : new PartitionRangeReadCommand(cfm, nowInSec, queriedColumns, rowFilter, limit, new DataRange(keyBounds, clusteringIndexFilter));
    }

    private ClusteringIndexFilter makeClusteringIndexFilter(QueryOptions options)
    throws InvalidRequestException
    {
        if (parameters.isDistinct)
        {
            // We need to be able to distinguish between partition having live rows and those that don't. But
            // doing so is not trivial since "having a live row" depends potentially on
            //   1) when the query is performed, due to TTLs
            //   2) how thing reconcile together between different nodes
            // so that it's hard to really optimize properly internally. So to keep it simple, we simply query
            // for the first row of the partition and hence uses Slices.ALL. We'll limit it to the first live
            // row however in getLimit().
            return new ClusteringIndexSliceFilter(Slices.ALL, false);
        }

        if (restrictions.isColumnRange())
        {
            Slices slices = makeSlices(options);
            if (slices == Slices.NONE && !selection.containsStaticColumns())
                return null;

            return new ClusteringIndexSliceFilter(slices, isReversed);
        }
        else
        {
            NavigableSet<Clustering> clusterings = getRequestedRows(options);
            if (clusterings.isEmpty() && !selection.containsStaticColumns()) // in case of IN () for the last column of the key
                return null;

            return new ClusteringIndexNamesFilter(clusterings, isReversed);
        }
    }

    private Slices makeSlices(QueryOptions options)
    throws InvalidRequestException
    {
        SortedSet<Slice.Bound> startBounds = restrictions.getClusteringColumnsBounds(Bound.START, options);
        SortedSet<Slice.Bound> endBounds = restrictions.getClusteringColumnsBounds(Bound.END, options);
        assert startBounds.size() == endBounds.size();

        // The case where startBounds == 1 is common enough that it's worth optimizing
        if (startBounds.size() == 1)
        {
            Slice.Bound start = startBounds.first();
            Slice.Bound end = endBounds.first();
            return cfm.comparator.compare(start, end) > 0
                 ? Slices.NONE
                 : Slices.with(cfm.comparator, Slice.make(start, end));
        }

        Slices.Builder builder = new Slices.Builder(cfm.comparator, startBounds.size());
        Iterator<Slice.Bound> startIter = startBounds.iterator();
        Iterator<Slice.Bound> endIter = endBounds.iterator();
        while (startIter.hasNext() && endIter.hasNext())
        {
            Slice.Bound start = startIter.next();
            Slice.Bound end = endIter.next();

            // Ignore slices that are nonsensical
            if (cfm.comparator.compare(start, end) > 0)
                continue;

            builder.add(start, end);
        }

        return builder.build();
    }

    /**
     * May be used by custom QueryHandler implementations
     */
    public DataLimits getLimit(QueryOptions options) throws InvalidRequestException
    {
        int userLimit = -1;
        // If we aggregate, the limit really apply to the number of rows returned to the user, not to what is queried, and
        // since in practice we currently only aggregate at top level (we have no GROUP BY support yet), we'll only ever
        // return 1 result and can therefore basically ignore the user LIMIT in this case.
        // Whenever we support GROUP BY, we'll have to add a new DataLimits kind that knows how things are grouped and is thus
        // able to apply the user limit properly.
        if (limit != null && !selection.isAggregate())
        {
            ByteBuffer b = checkNotNull(limit.bindAndGet(options), "Invalid null value of limit");
            // treat UNSET limit value as 'unlimited'
            if (b != UNSET_BYTE_BUFFER)
            {
                try
                {
                    Int32Type.instance.validate(b);
                    userLimit = Int32Type.instance.compose(b);
                    checkTrue(userLimit > 0, "LIMIT must be strictly positive");
                }
                catch (MarshalException e)
                {
                    throw new InvalidRequestException("Invalid limit value");
                }
            }
        }

        if (parameters.isDistinct)
            return userLimit < 0 ? DataLimits.DISTINCT_NONE : DataLimits.distinctLimits(userLimit);

        return userLimit < 0 ? DataLimits.NONE : DataLimits.cqlLimits(userLimit);
    }

    private NavigableSet<Clustering> getRequestedRows(QueryOptions options) throws InvalidRequestException
    {
        // Note: getRequestedColumns don't handle static columns, but due to CASSANDRA-5762
        // we always do a slice for CQL3 tables, so it's ok to ignore them here
        assert !restrictions.isColumnRange();
        return restrictions.getClusteringColumns(options);
    }

    /**
     * May be used by custom QueryHandler implementations
     */
    public RowFilter getRowFilter(QueryOptions options) throws InvalidRequestException
    {
        ColumnFamilyStore cfs = Keyspace.open(keyspace()).getColumnFamilyStore(columnFamily());
        SecondaryIndexManager secondaryIndexManager = cfs.indexManager;
        RowFilter filter = restrictions.getRowFilter(secondaryIndexManager, options);
        secondaryIndexManager.validateFilter(filter);
        return filter;
    }

    private ResultSet process(PartitionIterator partitions, QueryOptions options, int nowInSec) throws InvalidRequestException
    {
        Selection.ResultSetBuilder result = selection.resultSetBuilder(parameters.isJson);
        while (partitions.hasNext())
        {
            try (RowIterator partition = partitions.next())
            {
                processPartition(partition, options, result, nowInSec);
            }
        }

        ResultSet cqlRows = result.build(options.getProtocolVersion());

        orderResults(cqlRows);

        return cqlRows;
    }

    public static ByteBuffer[] getComponents(CFMetaData cfm, DecoratedKey dk)
    {
        ByteBuffer key = dk.getKey();
        if (cfm.getKeyValidator() instanceof CompositeType)
        {
            return ((CompositeType)cfm.getKeyValidator()).split(key);
        }
        else
        {
            return new ByteBuffer[]{ key };
        }
    }

    // Used by ModificationStatement for CAS operations
    void processPartition(RowIterator partition, QueryOptions options, Selection.ResultSetBuilder result, int nowInSec)
    throws InvalidRequestException
    {
        int protocolVersion = options.getProtocolVersion();

        ByteBuffer[] keyComponents = getComponents(cfm, partition.partitionKey());

        Row staticRow = partition.staticRow();
        // If there is no rows, then provided the select was a full partition selection
        // (i.e. not a 2ndary index search and there was no condition on clustering columns),
        // we want to include static columns and we're done.
        if (!partition.hasNext())
        {
            if (!staticRow.isEmpty() && (!restrictions.usesSecondaryIndexing() || cfm.isStaticCompactTable()) && !restrictions.hasClusteringColumnsRestriction())
            {
                result.newRow(protocolVersion);
                for (ColumnDefinition def : selection.getColumns())
                {
                    switch (def.kind)
                    {
                        case PARTITION_KEY:
                            result.add(keyComponents[def.position()]);
                            break;
                        case STATIC:
                            addValue(result, def, staticRow, nowInSec, protocolVersion);
                            break;
                        default:
                            result.add((ByteBuffer)null);
                    }
                }
            }
            return;
        }

        while (partition.hasNext())
        {
            Row row = partition.next();
            result.newRow(protocolVersion);
            // Respect selection order
            for (ColumnDefinition def : selection.getColumns())
            {
                switch (def.kind)
                {
                    case PARTITION_KEY:
                        result.add(keyComponents[def.position()]);
                        break;
                    case CLUSTERING:
                        result.add(row.clustering().get(def.position()));
                        break;
                    case REGULAR:
                        addValue(result, def, row, nowInSec, protocolVersion);
                        break;
                    case STATIC:
                        addValue(result, def, staticRow, nowInSec, protocolVersion);
                        break;
                }
            }
        }
    }

    private static void addValue(Selection.ResultSetBuilder result, ColumnDefinition def, Row row, int nowInSec, int protocolVersion)
    {
        if (def.isComplex())
        {
            // Collections are the only complex types we have so far
            assert def.type.isCollection() && def.type.isMultiCell();
            ComplexColumnData complexData = row.getComplexColumnData(def);
            if (complexData == null)
                result.add((ByteBuffer)null);
            else
                result.add(((CollectionType)def.type).serializeForNativeProtocol(def, complexData.iterator(), protocolVersion));
        }
        else
        {
            result.add(row.getCell(def), nowInSec);
        }
    }

    private boolean needsPostQueryOrdering()
    {
        // We need post-query ordering only for queries with IN on the partition key and an ORDER BY.
        return restrictions.keyIsInRelation() && !parameters.orderings.isEmpty();
    }

    /**
     * Orders results when multiple keys are selected (using IN)
     */
    private void orderResults(ResultSet cqlRows)
    {
        if (cqlRows.size() == 0 || !needsPostQueryOrdering())
            return;

        Collections.sort(cqlRows.rows, orderingComparator);
    }

    public static class RawStatement extends CFStatement
    {
        public final Parameters parameters;
        public final List<RawSelector> selectClause;
        public final List<Relation> whereClause;
        public final Term.Raw limit;

        public RawStatement(CFName cfName, Parameters parameters, List<RawSelector> selectClause, List<Relation> whereClause, Term.Raw limit)
        {
            super(cfName);
            this.parameters = parameters;
            this.selectClause = selectClause;
            this.whereClause = whereClause == null ? Collections.<Relation>emptyList() : whereClause;
            this.limit = limit;
        }

        public ParsedStatement.Prepared prepare() throws InvalidRequestException
        {
            CFMetaData cfm = ThriftValidation.validateColumnFamily(keyspace(), columnFamily());
            VariableSpecifications boundNames = getBoundVariables();

            Selection selection = selectClause.isEmpty()
                                  ? Selection.wildcard(cfm)
                                  : Selection.fromSelectors(cfm, selectClause);

            StatementRestrictions restrictions = prepareRestrictions(cfm, boundNames, selection);

            if (parameters.isDistinct)
                validateDistinctSelection(cfm, selection, restrictions);

            Comparator<List<ByteBuffer>> orderingComparator = null;
            boolean isReversed = false;

            if (!parameters.orderings.isEmpty())
            {
                verifyOrderingIsAllowed(restrictions);
                orderingComparator = getOrderingComparator(cfm, selection, restrictions);
                isReversed = isReversed(cfm);
            }

            checkNeedsFiltering(restrictions);

            SelectStatement stmt = new SelectStatement(cfm,
                                                        boundNames.size(),
                                                        parameters,
                                                        selection,
                                                        restrictions,
                                                        isReversed,
                                                        orderingComparator,
                                                        prepareLimit(boundNames));

            return new ParsedStatement.Prepared(stmt, boundNames, boundNames.getPartitionKeyBindIndexes(cfm));
        }

        /**
         * Prepares the restrictions.
         *
         * @param cfm the column family meta data
         * @param boundNames the variable specifications
         * @param selection the selection
         * @return the restrictions
         * @throws InvalidRequestException if a problem occurs while building the restrictions
         */
        private StatementRestrictions prepareRestrictions(CFMetaData cfm,
                                                          VariableSpecifications boundNames,
                                                          Selection selection) throws InvalidRequestException
        {
            try
            {
                return new StatementRestrictions(cfm,
                                                 whereClause,
                                                 boundNames,
                                                 selection.containsOnlyStaticColumns(),
                                                 selection.containsACollection(),
                                                 parameters.allowFiltering);
            }
            catch (UnrecognizedEntityException e)
            {
                if (containsAlias(e.entity))
                    throw invalidRequest("Aliases aren't allowed in the where clause ('%s')", e.relation);
                throw e;
            }
        }

        /** Returns a Term for the limit or null if no limit is set */
        private Term prepareLimit(VariableSpecifications boundNames) throws InvalidRequestException
        {
            if (limit == null)
                return null;

            Term prepLimit = limit.prepare(keyspace(), limitReceiver());
            prepLimit.collectMarkerSpecification(boundNames);
            return prepLimit;
        }

        private static void verifyOrderingIsAllowed(StatementRestrictions restrictions) throws InvalidRequestException
        {
            checkFalse(restrictions.usesSecondaryIndexing(), "ORDER BY with 2ndary indexes is not supported.");
            checkFalse(restrictions.isKeyRange(), "ORDER BY is only supported when the partition key is restricted by an EQ or an IN.");
        }

        private static void validateDistinctSelection(CFMetaData cfm,
                                                      Selection selection,
                                                      StatementRestrictions restrictions)
                                                      throws InvalidRequestException
        {
            Collection<ColumnDefinition> requestedColumns = selection.getColumns();
            for (ColumnDefinition def : requestedColumns)
                checkFalse(!def.isPartitionKey() && !def.isStatic(),
                           "SELECT DISTINCT queries must only request partition key columns and/or static columns (not %s)",
                           def.name);

            // If it's a key range, we require that all partition key columns are selected so we don't have to bother
            // with post-query grouping.
            if (!restrictions.isKeyRange())
                return;

            for (ColumnDefinition def : cfm.partitionKeyColumns())
                checkTrue(requestedColumns.contains(def),
                          "SELECT DISTINCT queries must request all the partition key columns (missing %s)", def.name);
        }

        private void handleUnrecognizedOrderingColumn(ColumnIdentifier column) throws InvalidRequestException
        {
            checkFalse(containsAlias(column), "Aliases are not allowed in order by clause ('%s')", column);
            checkFalse(true, "Order by on unknown column %s", column);
        }

        private Comparator<List<ByteBuffer>> getOrderingComparator(CFMetaData cfm,
                                                                   Selection selection,
                                                                   StatementRestrictions restrictions)
                                                                   throws InvalidRequestException
        {
            if (!restrictions.keyIsInRelation())
                return null;

            Map<ColumnIdentifier, Integer> orderingIndexes = getOrderingIndex(cfm, selection);

            List<Integer> idToSort = new ArrayList<Integer>();
            List<Comparator<ByteBuffer>> sorters = new ArrayList<Comparator<ByteBuffer>>();

            for (ColumnIdentifier.Raw raw : parameters.orderings.keySet())
            {
                ColumnIdentifier identifier = raw.prepare(cfm);
                ColumnDefinition orderingColumn = cfm.getColumnDefinition(identifier);
                idToSort.add(orderingIndexes.get(orderingColumn.name));
                sorters.add(orderingColumn.type);
            }
            return idToSort.size() == 1 ? new SingleColumnComparator(idToSort.get(0), sorters.get(0))
                    : new CompositeComparator(sorters, idToSort);
        }

        private Map<ColumnIdentifier, Integer> getOrderingIndex(CFMetaData cfm, Selection selection)
                throws InvalidRequestException
        {
            // If we order post-query (see orderResults), the sorted column needs to be in the ResultSet for sorting,
            // even if we don't
            // ultimately ship them to the client (CASSANDRA-4911).
            Map<ColumnIdentifier, Integer> orderingIndexes = new HashMap<>();
            for (ColumnIdentifier.Raw raw : parameters.orderings.keySet())
            {
                ColumnIdentifier column = raw.prepare(cfm);
                final ColumnDefinition def = cfm.getColumnDefinition(column);
                if (def == null)
                    handleUnrecognizedOrderingColumn(column);
                int index = selection.indexOf(def);
                if (index < 0)
                    index = selection.addColumnForOrdering(def);
                orderingIndexes.put(def.name, index);
            }
            return orderingIndexes;
        }

        private boolean isReversed(CFMetaData cfm) throws InvalidRequestException
        {
            Boolean[] reversedMap = new Boolean[cfm.clusteringColumns().size()];
            int i = 0;
            for (Map.Entry<ColumnIdentifier.Raw, Boolean> entry : parameters.orderings.entrySet())
            {
                ColumnIdentifier column = entry.getKey().prepare(cfm);
                boolean reversed = entry.getValue();

                ColumnDefinition def = cfm.getColumnDefinition(column);
                if (def == null)
                    handleUnrecognizedOrderingColumn(column);

                checkTrue(def.isClusteringColumn(),
                          "Order by is currently only supported on the clustered columns of the PRIMARY KEY, got %s", column);

                checkTrue(i++ == def.position(),
                          "Order by currently only support the ordering of columns following their declared order in the PRIMARY KEY");

                reversedMap[def.position()] = (reversed != def.isReversedType());
            }

            // Check that all boolean in reversedMap, if set, agrees
            Boolean isReversed = null;
            for (Boolean b : reversedMap)
            {
                // Column on which order is specified can be in any order
                if (b == null)
                    continue;

                if (isReversed == null)
                {
                    isReversed = b;
                    continue;
                }
                checkTrue(isReversed.equals(b), "Unsupported order by relation");
            }
            assert isReversed != null;
            return isReversed;
        }

        /** If ALLOW FILTERING was not specified, this verifies that it is not needed */
        private void checkNeedsFiltering(StatementRestrictions restrictions) throws InvalidRequestException
        {
            // non-key-range non-indexed queries cannot involve filtering underneath
            if (!parameters.allowFiltering && (restrictions.isKeyRange() || restrictions.usesSecondaryIndexing()))
            {
                // We will potentially filter data if either:
                //  - Have more than one IndexExpression
                //  - Have no index expression and the row filter is not the identity
                checkFalse(restrictions.needFiltering(),
                           "Cannot execute this query as it might involve data filtering and " +
                           "thus may have unpredictable performance. If you want to execute " +
                           "this query despite the performance unpredictability, use ALLOW FILTERING");
            }
        }

        private boolean containsAlias(final ColumnIdentifier name)
        {
            return Iterables.any(selectClause, new Predicate<RawSelector>()
                                               {
                                                   public boolean apply(RawSelector raw)
                                                   {
                                                       return name.equals(raw.alias);
                                                   }
                                               });
        }

        private ColumnSpecification limitReceiver()
        {
            return new ColumnSpecification(keyspace(), columnFamily(), new ColumnIdentifier("[limit]", true), Int32Type.instance);
        }

        @Override
        public String toString()
        {
            return Objects.toStringHelper(this)
                          .add("name", cfName)
                          .add("selectClause", selectClause)
                          .add("whereClause", whereClause)
                          .add("isDistinct", parameters.isDistinct)
                          .toString();
        }
    }

    public static class Parameters
    {
        // Public because CASSANDRA-9858
        public final Map<ColumnIdentifier.Raw, Boolean> orderings;
        public final boolean isDistinct;
        public final boolean allowFiltering;
        public final boolean isJson;

        public Parameters(Map<ColumnIdentifier.Raw, Boolean> orderings,
                          boolean isDistinct,
                          boolean allowFiltering,
                          boolean isJson)
        {
            this.orderings = orderings;
            this.isDistinct = isDistinct;
            this.allowFiltering = allowFiltering;
            this.isJson = isJson;
        }
    }

    /**
     * Used in orderResults(...) method when single 'ORDER BY' condition where given
     */
    private static class SingleColumnComparator implements Comparator<List<ByteBuffer>>
    {
        private final int index;
        private final Comparator<ByteBuffer> comparator;

        public SingleColumnComparator(int columnIndex, Comparator<ByteBuffer> orderer)
        {
            index = columnIndex;
            comparator = orderer;
        }

        public int compare(List<ByteBuffer> a, List<ByteBuffer> b)
        {
            return comparator.compare(a.get(index), b.get(index));
        }
    }

    /**
     * Used in orderResults(...) method when multiple 'ORDER BY' conditions where given
     */
    private static class CompositeComparator implements Comparator<List<ByteBuffer>>
    {
        private final List<Comparator<ByteBuffer>> orderTypes;
        private final List<Integer> positions;

        private CompositeComparator(List<Comparator<ByteBuffer>> orderTypes, List<Integer> positions)
        {
            this.orderTypes = orderTypes;
            this.positions = positions;
        }

        public int compare(List<ByteBuffer> a, List<ByteBuffer> b)
        {
            for (int i = 0; i < positions.size(); i++)
            {
                Comparator<ByteBuffer> type = orderTypes.get(i);
                int columnPos = positions.get(i);

                ByteBuffer aValue = a.get(columnPos);
                ByteBuffer bValue = b.get(columnPos);

                int comparison = type.compare(aValue, bValue);

                if (comparison != 0)
                    return comparison;
            }

            return 0;
        }
    }
}
