package org.kmsf.domainql.parser;

import org.kmsf.domainql.expression.ExpressionBuilder;
import org.kmsf.domainql.model.Domain;
import org.kmsf.domainql.model.DomainRegistry;
import org.kmsf.domainql.model.Query;
import org.kmsf.domainql.model.QueryBuilder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class QueryParser {
    private final DomainRegistry domainRegistry;

    public QueryParser(DomainRegistry domainRegistry) {
        this.domainRegistry = domainRegistry;
    }

    public Query parseQuery(String json) {
        JsonObject queryObj = JsonParser.parseString(json).getAsJsonObject();
        
        // Parse required fields
        String name = queryObj.get("name").getAsString();
        String sourceDomainName = queryObj.get("from").getAsString();
        Domain sourceDomain = getDomain(sourceDomainName);

        // Start building the query
        QueryBuilder builder = QueryBuilder.from(name, sourceDomain);

        // Parse projections
        if (queryObj.has("select")) {
            JsonArray projections = queryObj.getAsJsonArray("select");
            for (JsonElement proj : projections) {
                parseProjection(builder, proj.getAsJsonObject());
            }
        }

        // Parse where clause
        if (queryObj.has("where")) {
            builder.where(parseExpression(queryObj.get("where").getAsJsonObject()));
        }

        // Parse order by
        /*
        if (queryObj.has("orderBy")) {
            JsonArray orderClauses = queryObj.getAsJsonArray("orderBy");
            for (JsonElement order : orderClauses) {
                JsonObject orderObj = order.getAsJsonObject();
                builder.orderBy(
                    parseExpression(orderObj.get("expression").getAsJsonObject()),
                    orderObj.get("ascending").getAsBoolean()
                );
            }
        }
        */

        return builder.build();
    }

    private ExpressionBuilder parseExpression(JsonObject exprObj) {
        String type = exprObj.get("type").getAsString();
        
        switch (type) {
            case "attribute":
                return ExpressionBuilder.attr(exprObj.get("path").getAsString());
                
            case "literal":
                return parseLiteral(exprObj.get("value"));
                
            case "binary":
                return parseBinaryExpression(exprObj);
                
            case "aggregate":
                return parseAggregateExpression(exprObj);
                
           // case "reverse":
           //     return ExpressionBuilder.reverse(exprObj.get("reference").getAsString());
                
            default:
                throw new IllegalArgumentException("Unknown expression type: " + type);
        }
    }

    private void parseProjection(QueryBuilder builder, JsonObject proj) {
        String alias = proj.get("alias").getAsString();
        ExpressionBuilder expr = parseExpression(proj.get("expression").getAsJsonObject());
        builder.select(alias, expr);
    }

    private ExpressionBuilder parseBinaryExpression(JsonObject exprObj) {
        ExpressionBuilder left = parseExpression(exprObj.get("left").getAsJsonObject());
        ExpressionBuilder right = parseExpression(exprObj.get("right").getAsJsonObject());
        String operator = exprObj.get("operator").getAsString();
        
        switch (operator) {
            case "EQUALS": return ExpressionBuilder.EQUALS(left, right);
            case "GREATER_THAN": return ExpressionBuilder.GREATER_THAN(left, right);
            // ... autres opérateurs
            default:
                throw new IllegalArgumentException("Unknown operator: " + operator);
        }
    }

    private ExpressionBuilder parseAggregateExpression(JsonObject exprObj) {
        String function = exprObj.get("function").getAsString();
        if (function.equals("COUNT") && !exprObj.has("operand")) {
            return ExpressionBuilder.COUNT_ALL();
        }
        ExpressionBuilder operand = parseExpression(exprObj.get("operand").getAsJsonObject());
        
        switch (function) {
            case "COUNT": return ExpressionBuilder.COUNT(operand);
            case "SUM": return ExpressionBuilder.SUM(operand);
            case "AVG": return ExpressionBuilder.AVG(operand);
            // ... autres fonctions
            default:
                throw new IllegalArgumentException("Unknown aggregate function: " + function);
        }
    }

    private Domain getDomain(String name) {
        Domain domain = domainRegistry.getDomain(name);
        if (domain == null) {
            throw new IllegalArgumentException("Unknown domain: " + name);
        }
        return domain;
    }

    private ExpressionBuilder parseLiteral(JsonElement value) {
        if (value.isJsonNull()) {
            return ExpressionBuilder.literal(null);
        } else if (value.isJsonPrimitive()) {
            var primitive = value.getAsJsonPrimitive();
            if (primitive.isString()) {
                return ExpressionBuilder.literal(primitive.getAsString());
            } else if (primitive.isNumber()) {
                return ExpressionBuilder.literal(primitive.getAsNumber());
            } else if (primitive.isBoolean()) {
                return ExpressionBuilder.literal(primitive.getAsBoolean());
            }
        }
        throw new IllegalArgumentException("Unsupported literal type: " + value);
    }
} 