package refdiff.evaluation;

import refdiff.core.api.RefactoringType;
import refdiff.evaluation.benchmark.AbstractDataset;
import refdiff.evaluation.utils.RefactoringSet;

public class BenchmarkDataset extends AbstractDataset {

    public BenchmarkDataset() {
        at("https://github.com/linkedin/rest.li.git", "54fa890a6af4ccf564fb481d3e1b6ad4d084de9e")
            .add(RefactoringType.RENAME_METHOD, "com.linkedin.r2.transport.http.client.HttpClientFactory.buildAcceptEncodingSchemaNames()", "com.linkedin.r2.transport.http.client.HttpClientFactory.buildAcceptEncodingSchemas()")
            .add(RefactoringType.MOVE_OPERATION, "com.linkedin.restli.examples.TestCompressionServer.testEncodingGeneration(EncodingType[],String)", "com.linkedin.r2.filter.compression.TestClientCompressionFilter.testEncodingGeneration(EncodingType[],String)")
            .add(RefactoringType.RENAME_METHOD, "com.linkedin.r2.transport.http.client.HttpClientFactory.getCompressionConfig(String,String)", "com.linkedin.r2.transport.http.client.HttpClientFactory.getRequestCompressionConfig(String,EncodingType)")
            .add(RefactoringType.EXTRACT_OPERATION, "com.linkedin.r2.filter.compression.ClientCompressionFilter.onRestRequest(RestRequest,RequestContext,Map,NextFilter)", "com.linkedin.r2.filter.compression.ClientCompressionFilter.addResponseCompressionHeaders(CompressionOption,RestRequest)")
            
            .add(RefactoringType.EXTRACT_OPERATION, "com.linkedin.restli.examples.TestCompressionServer.test406Error(String)", "com.linkedin.restli.examples.TestCompressionServer.addCompressionHeaders(HttpGet,String)")
            .add(RefactoringType.EXTRACT_OPERATION, "com.linkedin.restli.examples.TestCompressionServer.testAcceptEncoding(String,String)", "com.linkedin.restli.examples.TestCompressionServer.addCompressionHeaders(HttpGet,String)")
            .add(RefactoringType.EXTRACT_OPERATION, "com.linkedin.restli.examples.TestCompressionServer.testCompatibleDefault(String,String)", "com.linkedin.restli.examples.TestCompressionServer.addCompressionHeaders(HttpGet,String)")
            .add(RefactoringType.EXTRACT_OPERATION, "com.linkedin.restli.examples.TestCompressionServer.testCompressionBetter(Compressor)", "com.linkedin.restli.examples.TestCompressionServer.addCompressionHeaders(HttpGet,String)")
            .add(RefactoringType.EXTRACT_OPERATION, "com.linkedin.restli.examples.TestCompressionServer.testCompressionWorse(Compressor)", "com.linkedin.restli.examples.TestCompressionServer.addCompressionHeaders(HttpGet,String)")
            
            .add(RefactoringType.MOVE_OPERATION, "com.linkedin.r2.filter.CompressionConfig.shouldCompressRequest(int,CompressionOption)", "com.linkedin.r2.filter.compression.ClientCompressionFilter.shouldCompressRequest(int,CompressionOption)")
            .add(RefactoringType.MOVE_OPERATION, "com.linkedin.restli.examples.TestCompressionServer.contentEncodingGeneratorDataProvider()", "com.linkedin.r2.filter.compression.TestClientCompressionFilter.contentEncodingGeneratorDataProvider()")
            .add(RefactoringType.INLINE_OPERATION, "com.linkedin.restli.examples.RestLiIntTestServer.createServer(Engine,int,String,boolean,int,List,List)", "com.linkedin.restli.examples.RestLiIntTestServer.createServer(Engine,int,String,boolean,int)")
            .add(RefactoringType.INLINE_OPERATION, "com.linkedin.restli.examples.RestLiIntTestServer.createServer(Engine,int,String,boolean,int,List,List)", "com.linkedin.restli.examples.RestLiIntegrationTest.init(List,List)")
            .add(RefactoringType.RENAME_METHOD, "com.linkedin.r2.filter.compression.TestClientCompressionFilter.provideRequestData()", "com.linkedin.r2.filter.compression.TestClientCompressionFilter.provideRequestCompressionData()")
            .add(RefactoringType.RENAME_METHOD, "com.linkedin.r2.filter.compression.TestClientCompressionFilter.testCompressionOperations(String,String[],boolean)", "com.linkedin.r2.filter.compression.TestClientCompressionFilter.testResponseCompressionRules(CompressionConfig,CompressionOption,String,String)")
            .add(RefactoringType.RENAME_METHOD, "com.linkedin.r2.filter.compression.ClientCompressionFilter.shouldCompressResponse(String)", "com.linkedin.r2.filter.compression.ClientCompressionFilter.shouldCompressResponseForOperation(String)")
            .add(RefactoringType.RENAME_METHOD, "com.linkedin.r2.transport.http.client.TestHttpClientFactory.testGetCompressionConfig(String,int,CompressionConfig)", "com.linkedin.r2.transport.http.client.TestHttpClientFactory.testGetRequestCompressionConfig(String,int,CompressionConfig)")
            .add(RefactoringType.RENAME_METHOD, "com.linkedin.r2.transport.http.client.HttpClientFactory.getRequestContentEncodingName(List)", "com.linkedin.r2.transport.http.client.HttpClientFactory.getRequestContentEncoding(List)");
        at("https://github.com/droolsjbpm/jbpm.git", "3815f293ba9338f423315d93a373608c95002b15")
            .add(RefactoringType.EXTRACT_SUPERCLASS, "org.jbpm.process.audit.JPAAuditLogService", "org.jbpm.process.audit.JPAService")
            .add(RefactoringType.MOVE_OPERATION, "org.jbpm.process.audit.JPAAuditLogService.convertListToInterfaceList(List,Class)", "org.jbpm.query.jpa.impl.QueryCriteriaUtil.convertListToInterfaceList(List,Class)")
            .add(RefactoringType.RENAME_METHOD, "org.jbpm.query.jpa.data.QueryWhere.startGroup()", "org.jbpm.query.jpa.data.QueryWhere.newGroup()")
            .add(RefactoringType.RENAME_CLASS, "org.jbpm.services.task.commands.TaskQueryDataCommand", "org.jbpm.services.task.commands.TaskQueryWhereCommand")
            .add(RefactoringType.RENAME_METHOD, "org.jbpm.services.task.impl.TaskQueryBuilderImpl.initiator(String[])", "org.jbpm.services.task.impl.TaskQueryBuilderImpl.createdBy(String[])")
            .add(RefactoringType.RENAME_METHOD, "org.jbpm.query.jpa.data.QueryWhere.addAppropriateParam(String,T[])", "org.jbpm.query.jpa.data.QueryWhere.addParameter(String,T[])")
            //.add(RefactoringType.MOVE_OPERATION, "org.jbpm.services.task.commands.TaskQueryDataCommand.execute(Context)", "org.jbpm.services.task.commands.TaskQueryWhereCommand.execute(Context)")
            .add(RefactoringType.RENAME_METHOD, "org.jbpm.services.task.impl.TaskQueryBuilderImpl.taskOwner(String[])", "org.jbpm.services.task.impl.TaskQueryBuilderImpl.actualOwner(String[])")
            .add(RefactoringType.RENAME_METHOD, "org.jbpm.services.task.impl.TaskQueryBuilderImpl.orderBy(OrderBy)", "org.jbpm.services.task.impl.TaskQueryBuilderImpl.getOrderByListId(OrderBy)")
            .add(RefactoringType.RENAME_METHOD, "org.jbpm.services.task.impl.TaskQueryBuilderImpl.buildQuery()", "org.jbpm.services.task.impl.TaskQueryBuilderImpl.build()")
            
            .add(RefactoringType.EXTRACT_OPERATION, "org.jbpm.services.task.impl.TaskQueryBuilderImpl.TaskQueryBuilderImpl(String,CommandService)", "org.jbpm.query.jpa.data.QueryWhere.setAscending(String)")
            .add(RefactoringType.EXTRACT_OPERATION, "org.jbpm.services.task.impl.TaskQueryBuilderImpl.clear()", "org.jbpm.query.jpa.data.QueryWhere.setAscending(String)")
            .add(RefactoringType.EXTRACT_OPERATION, "org.jbpm.services.task.impl.TaskQueryServiceImpl.getTasksByVariousFields(String,Map,boolean)", "org.jbpm.query.jpa.data.QueryWhere.setAscending(String)")
            
            .add(RefactoringType.INLINE_OPERATION, "org.jbpm.query.jpa.data.QueryWhere.getAppropriateQueryCriteria(String,int)", "org.jbpm.query.jpa.data.QueryWhere.addParameter(String,T[])")
            .add(RefactoringType.INLINE_OPERATION, "org.jbpm.query.jpa.data.QueryWhere.getAppropriateQueryCriteria(String,int)", "org.jbpm.query.jpa.data.QueryWhere.addRangeParameter(String,T,boolean)")
            .add(RefactoringType.INLINE_OPERATION, "org.jbpm.query.jpa.data.QueryWhere.resetGroup()", "org.jbpm.query.jpa.data.QueryWhere.clear()")
            
            .add(RefactoringType.RENAME_CLASS, "org.jbpm.query.jpa.data.QueryWhere.ParameterType", "org.jbpm.query.jpa.data.QueryWhere.QueryCriteriaType");
            
        at("https://github.com/gradle/gradle.git", "44aab6242f8c93059612c953af950eb1870e0774")
            .add(RefactoringType.EXTRACT_INTERFACE, "org.gradle.api.internal.file.FileResolver", "org.gradle.internal.file.RelativeFilePathResolver");
        at("https://github.com/jenkinsci/workflow-plugin.git", "d0e374ce8ecb687b4dc046d1edea9e52da17706f")
            .add(RefactoringType.MOVE_ATTRIBUTE, "org.jenkinsci.plugins.workflow.multibranch.WorkflowBranchProjectFactory.SCRIPT", "org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject.SCRIPT")
            .add(RefactoringType.INLINE_OPERATION, "org.jenkinsci.plugins.workflow.multibranch.WorkflowBranchProjectFactory.setBranch(BranchJobProperty,Branch,WorkflowJob)", "org.jenkinsci.plugins.workflow.multibranch.WorkflowBranchProjectFactory.setBranch(WorkflowJob,Branch)");
        at("https://github.com/spring-projects/spring-roo.git", "0bb4cca1105fc6eb86e7c4b75bfff3dbbd55f0c8")
            .add(RefactoringType.PULL_UP_ATTRIBUTE, "org.springframework.roo.classpath.details.MethodMetadataBuilder.genericDefinition", "org.springframework.roo.classpath.details.AbstractInvocableMemberMetadataBuilder.genericDefinition")
            .add(RefactoringType.PULL_UP_OPERATION, "org.springframework.roo.classpath.details.MethodMetadataBuilder.getGenericDefinition()", "org.springframework.roo.classpath.details.AbstractInvocableMemberMetadataBuilder.getGenericDefinition()")
            .add(RefactoringType.PULL_UP_OPERATION, "org.springframework.roo.classpath.details.MethodMetadataBuilder.setGenericDefinition(String)", "org.springframework.roo.classpath.details.AbstractInvocableMemberMetadataBuilder.setGenericDefinition(String)");
        at("https://github.com/BuildCraft/BuildCraft.git", "a5cdd8c4b10a738cb44819d7cc2fee5f5965d4a0")
            .add(RefactoringType.PUSH_DOWN_OPERATION, "buildcraft.api.robots.ResourceId.equals(Object)", "buildcraft.api.robots.ResourceIdRequest.equals(Object)")
            .add(RefactoringType.PUSH_DOWN_ATTRIBUTE, "buildcraft.api.robots.ResourceId.side", "buildcraft.api.robots.ResourceIdRequest.side")
            .add(RefactoringType.PUSH_DOWN_ATTRIBUTE, "buildcraft.api.robots.ResourceId.side", "buildcraft.api.robots.ResourceIdBlock.side")
            .add(RefactoringType.RENAME_METHOD, "buildcraft.robotics.TileRequester.provideItemsForRequest(int,ItemStack)", "buildcraft.robotics.TileRequester.offerItem(int,ItemStack)")
            .add(RefactoringType.PUSH_DOWN_ATTRIBUTE, "buildcraft.api.robots.ResourceId.index", "buildcraft.api.robots.ResourceIdRequest.index")
            .add(RefactoringType.PUSH_DOWN_ATTRIBUTE, "buildcraft.api.robots.ResourceId.index", "buildcraft.api.robots.ResourceIdBlock.index")
            .add(RefactoringType.EXTRACT_OPERATION, "buildcraft.robotics.ai.AIRobotSearchStackRequest.getOrderFromRequestingStation(DockingStation,boolean)", "buildcraft.robotics.ai.AIRobotSearchStackRequest.getAvailableRequests(DockingStation)")
            .add(RefactoringType.RENAME_METHOD, "buildcraft.robotics.TileRequester.getNumberOfRequests()", "buildcraft.robotics.TileRequester.getRequestsCount()")
            .add(RefactoringType.PUSH_DOWN_OPERATION, "buildcraft.api.robots.ResourceId.equals(Object)", "buildcraft.api.robots.ResourceIdBlock.equals(Object)")
            .add(RefactoringType.RENAME_METHOD, "buildcraft.builders.TileBuilder.getAvailableRequest(int)", "buildcraft.builders.TileBuilder.getRequest(int)")
            .add(RefactoringType.RENAME_METHOD, "buildcraft.builders.TileBuilder.provideItemsForRequest(int,ItemStack)", "buildcraft.builders.TileBuilder.offerItem(int,ItemStack)")
            .add(RefactoringType.RENAME_METHOD, "buildcraft.builders.TileBuilder.getNumberOfRequests()", "buildcraft.builders.TileBuilder.getRequestsCount()");
        at("https://github.com/droolsjbpm/drools.git", "1bf2875e9d73e2d1cd3b58200d5300485f890ff5")
            .add(RefactoringType.RENAME_METHOD, "org.drools.core.phreak.PhreakTimerNode.TimerAction.requiresImmediateFlushingIfNotFiring()", "org.drools.core.phreak.PhreakTimerNode.TimerAction.requiresImmediateFlushing()")
            .add(RefactoringType.MOVE_OPERATION, "org.drools.core.common.InternalAgenda.notifyHalt()", "org.drools.core.common.InternalWorkingMemory.notifyHalt()")
            .add(RefactoringType.MOVE_OPERATION, "org.drools.reteoo.common.ReteAgenda.notifyHalt()", "org.drools.core.phreak.SynchronizedBypassPropagationList.notifyHalt()")
            //.add(RefactoringType.MOVE_OPERATION, "org.drools.core.common.DefaultAgenda.notifyHalt()", "org.drools.core.phreak.SynchronizedBypassPropagationList.notifyHalt()")
            .add(RefactoringType.EXTRACT_OPERATION, "org.drools.core.phreak.SynchronizedPropagationList.addEntry(PropagationEntry)", "org.drools.core.phreak.SynchronizedPropagationList.internalAddEntry(PropagationEntry)")
            
            .add(RefactoringType.EXTRACT_OPERATION, "org.drools.core.common.DefaultAgenda.fireUntilHalt(AgendaFilter)", "org.drools.core.common.DefaultAgenda.waitAndEnterExecutionState(ExecutionState)")
            .add(RefactoringType.EXTRACT_OPERATION, "org.drools.core.rule.SlidingTimeWindow.assertFact(Object,InternalFactHandle,PropagationContext,InternalWorkingMemory)", "org.drools.core.rule.SlidingTimeWindow.SlidingTimeWindowContext.add(EventFactHandle)")
            .add(RefactoringType.EXTRACT_OPERATION, "org.drools.core.rule.SlidingTimeWindow.assertFact(Object,InternalFactHandle,PropagationContext,InternalWorkingMemory)", "org.drools.core.rule.SlidingTimeWindow.SlidingTimeWindowContext.peek()")
            .add(RefactoringType.EXTRACT_OPERATION, "org.drools.core.rule.SlidingTimeWindow.expireFacts(Object,PropagationContext,InternalWorkingMemory)", "org.drools.core.rule.SlidingTimeWindow.SlidingTimeWindowContext.peek()")
            .add(RefactoringType.EXTRACT_OPERATION, "org.drools.core.rule.SlidingTimeWindow.retractFact(Object,InternalFactHandle,PropagationContext,InternalWorkingMemory)", "org.drools.core.rule.SlidingTimeWindow.SlidingTimeWindowContext.peek()")
            .add(RefactoringType.EXTRACT_OPERATION, "org.drools.core.rule.SlidingTimeWindow.retractFact(Object,InternalFactHandle,PropagationContext,InternalWorkingMemory)", "org.drools.core.rule.SlidingTimeWindow.SlidingTimeWindowContext.poll()")
            .add(RefactoringType.EXTRACT_OPERATION, "org.drools.core.rule.SlidingTimeWindow.expireFacts(Object,PropagationContext,InternalWorkingMemory)", "org.drools.core.rule.SlidingTimeWindow.SlidingTimeWindowContext.remove()")
            .add(RefactoringType.EXTRACT_OPERATION, "org.drools.core.rule.SlidingTimeWindow.retractFact(Object,InternalFactHandle,PropagationContext,InternalWorkingMemory)", "org.drools.core.rule.SlidingTimeWindow.SlidingTimeWindowContext.remove(EventFactHandle)")
            
            
            .add(RefactoringType.RENAME_METHOD, "org.drools.core.phreak.PropagationEntry.AbstractPropagationEntry.requiresImmediateFlushingIfNotFiring()", "org.drools.core.phreak.PropagationEntry.AbstractPropagationEntry.requiresImmediateFlushing()")
            .add(RefactoringType.RENAME_METHOD, "org.drools.core.phreak.RuleExecutor.isHighestSalience(RuleAgendaItem)", "org.drools.core.phreak.RuleExecutor.isHigherSalience(RuleAgendaItem)")
            // Rename of abstract method
            .add(RefactoringType.RENAME_METHOD, "org.drools.core.common.InternalAgenda.executeIfNotFiring(Runnable)", "org.drools.core.common.InternalAgenda.executeTask(ExecutableEntry)")
            // Rename of two concrete implementations
            .add(RefactoringType.RENAME_METHOD, "org.drools.reteoo.common.ReteAgenda.executeIfNotFiring(Runnable)", "org.drools.reteoo.common.ReteAgenda.executeTask(ExecutableEntry)")
            .add(RefactoringType.RENAME_METHOD, "org.drools.core.common.DefaultAgenda.executeIfNotFiring(Runnable)", "org.drools.core.common.DefaultAgenda.executeTask(ExecutableEntry)")
            
            .add(RefactoringType.EXTRACT_OPERATION, "org.drools.core.common.AgendaGroupQueueImpl.AgendaGroupQueueImpl(String,InternalKnowledgeBase)", "org.drools.core.common.AgendaGroupQueueImpl.initPriorityQueue(InternalKnowledgeBase)")
            .add(RefactoringType.PUSH_DOWN_ATTRIBUTE, "org.drools.core.impl.StatefulKnowledgeSessionImpl.evaluatingActionQueue", "org.drools.reteoo.common.ReteWorkingMemory.evaluatingActionQueue");
        at("https://github.com/jersey/jersey.git", "d94ca2b27c9e8a5fa9fe19483d58d2f2ef024606")
            .add(RefactoringType.MOVE_CLASS, "org.glassfish.jersey.client.HttpUrlConnector", "org.glassfish.jersey.client.internal.HttpUrlConnector")
            .add(RefactoringType.EXTRACT_OPERATION, "org.glassfish.jersey.client.HttpUrlConnector._apply(ClientRequest)", "org.glassfish.jersey.client.internal.HttpUrlConnector.secureConnection(Client,HttpURLConnection)")
            .add(RefactoringType.EXTRACT_OPERATION, "org.glassfish.jersey.client.HttpUrlConnectorProvider.getConnector(Client,Configuration)", "org.glassfish.jersey.client.HttpUrlConnectorProvider.createHttpUrlConnector(Client,ConnectionFactory,int,boolean,boolean)");
        at("https://github.com/undertow-io/undertow.git", "d5b2bb8cd1393f1c5a5bb623e3d8906cd57e53c4")
            .add(RefactoringType.MOVE_OPERATION, "io.undertow.server.handlers.builder.HandlerParser.coerceToType(String,Token,Class,ExchangeAttributeParser)", "io.undertow.server.handlers.builder.PredicatedHandlersParser.coerceToType(String,Token,Class,ExchangeAttributeParser)")
            .add(RefactoringType.EXTRACT_OPERATION, "io.undertow.predicate.PredicatesHandler.addPredicatedHandler(Predicate,HandlerWrapper)", "io.undertow.predicate.PredicatesHandler.addPredicatedHandler(Predicate,HandlerWrapper,HandlerWrapper)")
            .add(RefactoringType.MOVE_OPERATION, "io.undertow.predicate.PredicateParser.collapseOutput(Object,Deque)", "io.undertow.server.handlers.builder.PredicatedHandlersParser.collapseOutput(Node,Deque)")
            .add(RefactoringType.MOVE_CLASS, "io.undertow.util.PredicateTokeniser.Token", "io.undertow.server.handlers.builder.PredicatedHandlersParser.Token")
            .add(RefactoringType.MOVE_OPERATION, "io.undertow.server.handlers.builder.HandlerParser.isSpecialChar(String)", "io.undertow.server.handlers.builder.PredicatedHandlersParser.isSpecialChar(String)")
            .add(RefactoringType.MOVE_OPERATION, "io.undertow.util.PredicateTokeniser.tokenize(String)", "io.undertow.server.handlers.builder.PredicatedHandlersParser.tokenize(String)")
            .add(RefactoringType.EXTRACT_OPERATION, "io.undertow.predicate.PredicateParser.parse(String,ClassLoader)", "io.undertow.server.handlers.builder.PredicatedHandlersParser.parsePredicate(String,ClassLoader)")
            .add(RefactoringType.MOVE_OPERATION, "io.undertow.predicate.PredicateParser.readArrayType(String,Deque,Token,PredicateBuilder,ExchangeAttributeParser,String)", "io.undertow.server.handlers.builder.PredicatedHandlersParser.readArrayType(String,Deque,String)")
            .add(RefactoringType.MOVE_OPERATION, "io.undertow.predicate.PredicateParser.coerceToType(String,Token,Class,ExchangeAttributeParser)", "io.undertow.server.handlers.builder.PredicatedHandlersParser.coerceToType(String,Token,Class,ExchangeAttributeParser)")
            .add(RefactoringType.MOVE_OPERATION, "io.undertow.predicate.PredicateParser.parse(String,Deque,Map,ExchangeAttributeParser)", "io.undertow.server.handlers.builder.PredicatedHandlersParser.parse(String,Deque,boolean)")
            .add(RefactoringType.MOVE_OPERATION, "io.undertow.predicate.PredicateParser.isSpecialChar(String)", "io.undertow.server.handlers.builder.PredicatedHandlersParser.isSpecialChar(String)")
            .add(RefactoringType.MOVE_OPERATION, "io.undertow.predicate.PredicateParser.isOperator(String)", "io.undertow.server.handlers.builder.PredicatedHandlersParser.isOperator(String)")
            .add(RefactoringType.MOVE_OPERATION, "io.undertow.predicate.PredicateParser.handleSingleArrayValue(String,PredicateBuilder,Deque,Token,ExchangeAttributeParser,String)", "io.undertow.server.handlers.builder.PredicatedHandlersParser.handleSingleArrayValue(String,Token,Deque,String)")
            .add(RefactoringType.MOVE_OPERATION, "io.undertow.server.handlers.builder.HandlerParser.precedence(String)", "io.undertow.server.handlers.builder.PredicatedHandlersParser.precedence(String)")
            .add(RefactoringType.MOVE_OPERATION, "io.undertow.server.handlers.builder.HandlerParser.handleSingleArrayValue(String,HandlerBuilder,Deque,Token,ExchangeAttributeParser,String,Token)", "io.undertow.server.handlers.builder.PredicatedHandlersParser.handleSingleArrayValue(String,Token,Deque,String)")
            .add(RefactoringType.MOVE_OPERATION, "io.undertow.predicate.PredicateParser.readArrayType(String,Deque,Token,PredicateBuilder,ExchangeAttributeParser,String)", "io.undertow.server.handlers.builder.PredicatedHandlersParser.readArrayType(String,String,ArrayNode,ExchangeAttributeParser,Class)")
            .add(RefactoringType.MOVE_OPERATION, "io.undertow.server.handlers.builder.HandlerParser.isOperator(String)", "io.undertow.server.handlers.builder.PredicatedHandlersParser.isOperator(String)")
            .add(RefactoringType.MOVE_OPERATION, "io.undertow.util.PredicateTokeniser.error(String,int,String)", "io.undertow.server.handlers.builder.PredicatedHandlersParser.error(String,int,String)")
            .add(RefactoringType.EXTRACT_OPERATION, "io.undertow.server.handlers.builder.HandlerParser.parse(String,ClassLoader)", "io.undertow.server.handlers.builder.PredicatedHandlersParser.parseHandler(String,ClassLoader)")
            .add(RefactoringType.MOVE_OPERATION, "io.undertow.server.handlers.builder.HandlerParser.tokenize(String)", "io.undertow.server.handlers.builder.PredicatedHandlersParser.tokenize(String)")
            .add(RefactoringType.MOVE_OPERATION, "io.undertow.predicate.PredicateParser.precedence(String)", "io.undertow.server.handlers.builder.PredicatedHandlersParser.precedence(String)")
            .add(RefactoringType.MOVE_OPERATION, "io.undertow.server.handlers.builder.HandlerParser.readArrayType(String,Deque,Token,HandlerBuilder,ExchangeAttributeParser,String,Token)", "io.undertow.server.handlers.builder.PredicatedHandlersParser.readArrayType(String,String,ArrayNode,ExchangeAttributeParser,Class)");
        at("https://github.com/kuujo/copycat.git", "19a49f8f36b2f6d82534dc13504d672e41a3a8d1")
            .add(RefactoringType.PULL_UP_OPERATION, "net.kuujo.copycat.raft.state.ActiveState.applyCommits(long)", "net.kuujo.copycat.raft.state.PassiveState.applyCommits(long)")
            .add(RefactoringType.PULL_UP_OPERATION, "net.kuujo.copycat.raft.state.ActiveState.doCheckPreviousEntry(AppendRequest)", "net.kuujo.copycat.raft.state.PassiveState.doCheckPreviousEntry(AppendRequest)")
            .add(RefactoringType.PULL_UP_OPERATION, "net.kuujo.copycat.raft.state.ActiveState.doAppendEntries(AppendRequest)", "net.kuujo.copycat.raft.state.PassiveState.doAppendEntries(AppendRequest)")
            .add(RefactoringType.PULL_UP_ATTRIBUTE, "net.kuujo.copycat.raft.state.ActiveState.transition", "net.kuujo.copycat.raft.state.PassiveState.transition")
            .add(RefactoringType.PULL_UP_OPERATION, "net.kuujo.copycat.raft.state.ActiveState.handleAppend(AppendRequest)", "net.kuujo.copycat.raft.state.PassiveState.handleAppend(AppendRequest)")
            .add(RefactoringType.PULL_UP_OPERATION, "net.kuujo.copycat.raft.state.ActiveState.applyIndex(long)", "net.kuujo.copycat.raft.state.PassiveState.applyIndex(long)");
    }

    private RefactoringSet at(String cloneUrl, String commit) {
        RefactoringSet rs = new RefactoringSet(cloneUrl, commit);
        add(rs);
        return rs;
    }

    public void printSourceCode() {
        for (RefactoringSet set : getExpected()) {
            set.printSourceCode(System.out);
        }
    }

}
