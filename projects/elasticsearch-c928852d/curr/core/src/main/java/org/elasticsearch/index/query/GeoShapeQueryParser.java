/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.index.query;

import org.apache.lucene.search.*;
import org.apache.lucene.spatial.prefix.PrefixTreeStrategy;
import org.apache.lucene.spatial.prefix.RecursivePrefixTreeStrategy;
import org.apache.lucene.spatial.query.SpatialArgs;
import org.apache.lucene.spatial.query.SpatialOperation;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.geo.ShapeRelation;
import org.elasticsearch.common.geo.builders.ShapeBuilder;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.mapper.FieldMapper;
import org.elasticsearch.index.mapper.MappedFieldType;
import org.elasticsearch.index.mapper.geo.GeoShapeFieldMapper;
import org.elasticsearch.index.search.shape.ShapeFetchService;
import org.elasticsearch.search.internal.SearchContext;

import java.io.IOException;

public class GeoShapeQueryParser extends BaseQueryParserTemp {

    private ShapeFetchService fetchService;

    public static class DEFAULTS {
        public static final String INDEX_NAME = "shapes";
        public static final String SHAPE_FIELD_NAME = "shape";
    }

    @Override
    public String[] names() {
        return new String[]{GeoShapeQueryBuilder.NAME, Strings.toCamelCase(GeoShapeQueryBuilder.NAME)};
    }

    @Override
    public Query parse(QueryParseContext parseContext) throws IOException, QueryParsingException {
        XContentParser parser = parseContext.parser();

        String fieldName = null;
        ShapeRelation shapeRelation = ShapeRelation.INTERSECTS;
        String strategyName = null;
        ShapeBuilder shape = null;

        String id = null;
        String type = null;
        String index = DEFAULTS.INDEX_NAME;
        String shapePath = DEFAULTS.SHAPE_FIELD_NAME;

        XContentParser.Token token;
        String currentFieldName = null;
        float boost = 1f;
        String queryName = null;

        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentFieldName = parser.currentName();
            } else if (token == XContentParser.Token.START_OBJECT) {
                fieldName = currentFieldName;

                while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                    if (token == XContentParser.Token.FIELD_NAME) {
                        currentFieldName = parser.currentName();
                        token = parser.nextToken();
                        if ("shape".equals(currentFieldName)) {
                            shape = ShapeBuilder.parse(parser);
                        } else if ("strategy".equals(currentFieldName)) {
                            strategyName = parser.text();
                        } else if ("relation".equals(currentFieldName)) {
                            shapeRelation = ShapeRelation.getRelationByName(parser.text());
                            if (shapeRelation == null) {
                                throw new QueryParsingException(parseContext, "Unknown shape operation [" + parser.text() + " ]");
                            }
                        } else if ("indexed_shape".equals(currentFieldName) || "indexedShape".equals(currentFieldName)) {
                            while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                                if (token == XContentParser.Token.FIELD_NAME) {
                                    currentFieldName = parser.currentName();
                                } else if (token.isValue()) {
                                    if ("id".equals(currentFieldName)) {
                                        id = parser.text();
                                    } else if ("type".equals(currentFieldName)) {
                                        type = parser.text();
                                    } else if ("index".equals(currentFieldName)) {
                                        index = parser.text();
                                    } else if ("path".equals(currentFieldName)) {
                                        shapePath = parser.text();
                                    }
                                }
                            }
                            if (id == null) {
                                throw new QueryParsingException(parseContext, "ID for indexed shape not provided");
                            } else if (type == null) {
                                throw new QueryParsingException(parseContext, "Type for indexed shape not provided");
                            }
                            GetRequest getRequest = new GetRequest(index, type, id);
                            getRequest.copyContextAndHeadersFrom(SearchContext.current());
                            shape = fetchService.fetch(getRequest, shapePath);
                        } else {
                            throw new QueryParsingException(parseContext, "[geo_shape] query does not support [" + currentFieldName + "]");
                        }
                    }
                }
            } else if (token.isValue()) {
                if ("boost".equals(currentFieldName)) {
                    boost = parser.floatValue();
                } else if ("_name".equals(currentFieldName)) {
                    queryName = parser.text();
                } else {
                    throw new QueryParsingException(parseContext, "[geo_shape] query does not support [" + currentFieldName + "]");
                }
            }
        }

        if (shape == null) {
            throw new QueryParsingException(parseContext, "No Shape defined");
        } else if (shapeRelation == null) {
            throw new QueryParsingException(parseContext, "No Shape Relation defined");
        }

        MappedFieldType fieldType = parseContext.fieldMapper(fieldName);
        if (fieldType == null) {
            throw new QueryParsingException(parseContext, "Failed to find geo_shape field [" + fieldName + "]");
        }

        // TODO: This isn't the nicest way to check this
        if (!(fieldType instanceof GeoShapeFieldMapper.GeoShapeFieldType)) {
            throw new QueryParsingException(parseContext, "Field [" + fieldName + "] is not a geo_shape");
        }

        GeoShapeFieldMapper.GeoShapeFieldType shapeFieldType = (GeoShapeFieldMapper.GeoShapeFieldType) fieldType;

        PrefixTreeStrategy strategy = shapeFieldType.defaultStrategy();
        if (strategyName != null) {
            strategy = shapeFieldType.resolveStrategy(strategyName);
        }
        Query query;
        if (strategy instanceof RecursivePrefixTreeStrategy && shapeRelation == ShapeRelation.DISJOINT) {
            // this strategy doesn't support disjoint anymore: but it did before, including creating lucene fieldcache (!)
            // in this case, execute disjoint as exists && !intersects
            BooleanQuery bool = new BooleanQuery();
            Query exists = ExistsQueryBuilder.newFilter(parseContext, fieldName, null);
            Filter intersects = strategy.makeFilter(getArgs(shape, ShapeRelation.INTERSECTS));
            bool.add(exists, BooleanClause.Occur.MUST);
            bool.add(intersects, BooleanClause.Occur.MUST_NOT);
            query = new ConstantScoreQuery(bool);
        } else {
            query = strategy.makeQuery(getArgs(shape, shapeRelation));
        }
        query.setBoost(boost);
        if (queryName != null) {
            parseContext.addNamedQuery(queryName, query);
        }
        return query;
    }

    @Inject(optional = true)
    public void setFetchService(@Nullable ShapeFetchService fetchService) {
        this.fetchService = fetchService;
    }

    public static SpatialArgs getArgs(ShapeBuilder shape, ShapeRelation relation) {
        switch(relation) {
        case DISJOINT:
            return new SpatialArgs(SpatialOperation.IsDisjointTo, shape.build());
        case INTERSECTS:
            return new SpatialArgs(SpatialOperation.Intersects, shape.build());
        case WITHIN:
            return new SpatialArgs(SpatialOperation.IsWithin, shape.build());
        default:
            throw new IllegalArgumentException("");
        }
    }

    @Override
    public GeoShapeQueryBuilder getBuilderPrototype() {
        return GeoShapeQueryBuilder.PROTOTYPE;
    }
}
