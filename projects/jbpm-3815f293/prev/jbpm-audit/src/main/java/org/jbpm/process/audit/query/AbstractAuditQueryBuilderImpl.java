/*
 * Copyright 2015 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.jbpm.process.audit.query;

import static org.kie.internal.query.QueryParameterIdentifiers.DATE_LIST;
import static org.kie.internal.query.QueryParameterIdentifiers.PROCESS_ID_LIST;
import static org.kie.internal.query.QueryParameterIdentifiers.PROCESS_INSTANCE_ID_LIST;

import java.util.Date;

import org.jbpm.process.audit.JPAAuditLogService;
import org.jbpm.process.audit.command.AuditCommand;
import org.jbpm.query.jpa.builder.impl.AbstractQueryBuilderImpl;
import org.kie.api.runtime.CommandExecutor;
import org.kie.internal.command.Context;
import org.kie.internal.runtime.manager.audit.query.AuditQueryBuilder;

public class AbstractAuditQueryBuilderImpl<T> extends AbstractQueryBuilderImpl<T> implements AuditQueryBuilder<T> {

    protected final CommandExecutor executor; 
    protected final JPAAuditLogService jpaAuditService; 
    
    protected AbstractAuditQueryBuilderImpl(JPAAuditLogService jpaService) { 
        this.executor = null;
        this.jpaAuditService = jpaService;
    }
    
    protected AbstractAuditQueryBuilderImpl(CommandExecutor cmdExecutor) { 
        this.executor = cmdExecutor;
        this.jpaAuditService = null;
    }
   
    // service methods
    
    protected JPAAuditLogService getJpaAuditLogService() { 
        JPAAuditLogService jpaAuditLogService = this.jpaAuditService;
        if( jpaAuditLogService == null ) { 
           jpaAuditLogService = this.executor.execute(getJpaAuditLogServiceCommand);
        }
        return jpaAuditLogService;
    }
    
    private AuditCommand<JPAAuditLogService> getJpaAuditLogServiceCommand = new AuditCommand<JPAAuditLogService>() {
        private static final long serialVersionUID = 101L;
        @Override
        public JPAAuditLogService execute( Context context ) {
            setLogEnvironment(context);
            return (JPAAuditLogService) this.auditLogService;
        }
    };

    // query builder methods
    
    @Override
    @SuppressWarnings("unchecked")
    public T processInstanceId( long... processInstanceId ) {
        addLongParameter(PROCESS_INSTANCE_ID_LIST, "process instance id", processInstanceId);
        return (T) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T processId( String... processId ) {
        addObjectParameter(PROCESS_ID_LIST, "process id", processId);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T date( Date... date ) {
        addObjectParameter(DATE_LIST, "date", date);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T dateRangeStart( Date rangeStart ) {
        addRangeParameter(DATE_LIST, "date range start", rangeStart, true);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T dateRangeEnd( Date rangeStart ) {
        addRangeParameter(DATE_LIST, "date range end", rangeStart, false);
        return (T) this;
    }

    // query builder result methods
  
}
