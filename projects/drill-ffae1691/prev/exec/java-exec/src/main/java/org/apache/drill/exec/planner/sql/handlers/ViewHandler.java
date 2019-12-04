/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.planner.sql.handlers;

import java.io.IOException;

import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.Schema.TableType;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Table;
import org.apache.calcite.tools.Planner;
import org.apache.calcite.tools.RelConversionException;
import org.apache.calcite.tools.ValidationException;

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.dotdrill.View;
import org.apache.drill.exec.ops.QueryContext;
import org.apache.drill.exec.physical.PhysicalPlan;
import org.apache.drill.exec.planner.sql.DirectPlan;
import org.apache.drill.exec.planner.sql.SchemaUtilites;
import org.apache.drill.exec.planner.sql.parser.SqlCreateView;
import org.apache.drill.exec.planner.sql.parser.SqlDropView;
import org.apache.drill.exec.store.AbstractSchema;
import org.apache.drill.exec.work.foreman.ForemanSetupException;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.sql.SqlNode;

public abstract class ViewHandler extends AbstractSqlHandler {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ViewHandler.class);

  protected Planner planner;
  protected QueryContext context;

  public ViewHandler(Planner planner, QueryContext context) {
    this.planner = planner;
    this.context = context;
  }

  /** Handler for Create View DDL command */
  public static class CreateView extends ViewHandler {

    public CreateView(Planner planner, QueryContext context) {
      super(planner, context);
    }

    @Override
    public PhysicalPlan getPlan(SqlNode sqlNode) throws ValidationException, RelConversionException, IOException, ForemanSetupException {
      SqlCreateView createView = unwrap(sqlNode, SqlCreateView.class);

      final String newViewName = createView.getName();

      // Store the viewSql as view def SqlNode is modified as part of the resolving the new table definition below.
      final String viewSql = createView.getQuery().toString();

      final RelNode newViewRelNode =
          SqlHandlerUtil.resolveNewTableRel(true, planner, createView.getFieldNames(), createView.getQuery());

      final SchemaPlus defaultSchema = context.getNewDefaultSchema();
      final AbstractSchema drillSchema = SchemaUtilites.resolveToMutableDrillSchema(defaultSchema, createView.getSchemaPath());

      final String schemaPath = drillSchema.getFullSchemaName();
      final View view = new View(newViewName, viewSql, newViewRelNode.getRowType(),
          SchemaUtilites.getSchemaPathAsList(defaultSchema));

      final Table existingTable = SqlHandlerUtil.getTableFromSchema(drillSchema, newViewName);

      if (existingTable != null) {
        if (existingTable.getJdbcTableType() != Schema.TableType.VIEW) {
          // existing table is not a view
          throw UserException.validationError()
              .message("A non-view table with given name [%s] already exists in schema [%s]",
                  newViewName, schemaPath)
              .build();
        }

        if (existingTable.getJdbcTableType() == Schema.TableType.VIEW && !createView.getReplace()) {
          // existing table is a view and create view has no "REPLACE" clause
          throw UserException.validationError()
              .message("A view with given name [%s] already exists in schema [%s]",
                  newViewName, schemaPath)
              .build();
        }
      }

      final boolean replaced = drillSchema.createView(view);
      final String summary = String.format("View '%s' %s successfully in '%s' schema",
          createView.getName(), replaced ? "replaced" : "created", schemaPath);

      return DirectPlan.createDirectPlan(context, true, summary);
    }
  }

  /** Handler for Drop View DDL command. */
  public static class DropView extends ViewHandler {
    public DropView(QueryContext context) {
      super(null, context);
    }

    @Override
    public PhysicalPlan getPlan(SqlNode sqlNode) throws ValidationException, RelConversionException, IOException, ForemanSetupException {
      SqlDropView dropView = unwrap(sqlNode, SqlDropView.class);
      final String viewToDrop = dropView.getName();
      final AbstractSchema drillSchema =
          SchemaUtilites.resolveToMutableDrillSchema(context.getNewDefaultSchema(), dropView.getSchemaPath());

      final String schemaPath = drillSchema.getFullSchemaName();

      final Table existingTable = SqlHandlerUtil.getTableFromSchema(drillSchema, viewToDrop);
      if (existingTable != null && existingTable.getJdbcTableType() != Schema.TableType.VIEW) {
        throw UserException.validationError()
            .message("[%s] is not a VIEW in schema [%s]", viewToDrop, schemaPath)
            .build();
      } else if (existingTable == null) {
        throw UserException.validationError()
            .message("Unknown view [%s] in schema [%s].", viewToDrop, schemaPath)
            .build();
      }

      drillSchema.dropView(viewToDrop);

      return DirectPlan.createDirectPlan(context, true,
          String.format("View [%s] deleted successfully from schema [%s].", viewToDrop, schemaPath));
    }
  }
}
