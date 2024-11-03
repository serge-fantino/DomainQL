package org.kmsf.domainql.sql;

import org.kmsf.domainql.expression.AggregateExpression;
import org.kmsf.domainql.expression.Attribute;
import org.kmsf.domainql.expression.AttributeExpression;
import org.kmsf.domainql.expression.BinaryExpression;
import org.kmsf.domainql.expression.ComposeExpression;
import org.kmsf.domainql.expression.Domain;
import org.kmsf.domainql.expression.Expression;
import org.kmsf.domainql.expression.LiteralExpression;
import org.kmsf.domainql.expression.Query;
import org.kmsf.domainql.expression.QueryExpression;
import org.kmsf.domainql.expression.ReferenceAttribute;
import org.kmsf.domainql.expression.type.*;
import java.util.*;

public class SQLGenerator {
    private final Query query;
    private final Map<JoinInfo, String> joinAliases = new HashMap<>();
    private int aliasCounter = 0;
    private int subqueryCounter = 0;

    public SQLGenerator(Query query) {
        this.query = query;
    }

    public static String generateSQL(Query query) {
        SQLGenerator generator = new SQLGenerator(query);
        return generator.generateSQL();
    }

    public String generateSQL() {
        StringBuilder sql = new StringBuilder("SELECT ");
        Set<JoinInfo> joins = new LinkedHashSet<>();
        
        // Generate projections
        generateProjections(sql, joins);
        
        // Generate FROM clause with necessary JOINs
        generateFromClause(query, sql, joins);
        // Add JOINs
        for (JoinInfo join : joins) {
            sql.append(" JOIN ")
               .append(join.referenceAttribute.getReferenceDomain().getName())
               .append(" AS ")
               .append(join.alias)
               .append(" ON ");
            generateExpression(join.referenceAttribute.getJoinCondition(), join.referenceAttribute, sql, joins);
        }

        // Generate WHERE clause if filter exists
        if (query.getFilter() != null) {
            sql.append(" WHERE ");
            generateExpression(query.getFilter(), null, sql, joins);
        }

        // Generate GROUP BY if needed
        if (needsGroupBy(query)) {
            sql.append(" GROUP BY ");
            generateGroupByClause(query, sql, joins);
        }

        return sql.toString();
    }

    private void generateProjections(StringBuilder sql, Set<JoinInfo> joins) {
        boolean first = true;
        for (Map.Entry<String, Expression> projection : query.getProjections().entrySet()) {
            if (!first) sql.append(", ");
            generateExpression(projection.getValue(), null, sql, joins);
            sql.append(" AS ").append(projection.getKey());
            first = false;
        }
    }

    private void generateExpression(Expression expr, ReferenceAttribute context, StringBuilder sql, Set<JoinInfo> joins) {
        if (expr instanceof ComposeExpression) {
            ComposeExpression compose = (ComposeExpression) expr;
            Expression reference = compose.getReference();
            
            if (reference instanceof AttributeExpression) {
                AttributeExpression attrExpr = (AttributeExpression) reference;
                if (attrExpr.getAttribute() instanceof ReferenceAttribute) {
                    ReferenceAttribute refAttr = (ReferenceAttribute) attrExpr.getAttribute();
                    String joinAlias = getOrCreateJoinAlias(refAttr);
                    
                    joins.add(new JoinInfo(refAttr, joinAlias));
                    
                    // Pass the reference attribute as context for the composition
                    Expression composition = compose.getComposition();
                    generateExpression(composition, refAttr, sql, joins);
                }
            }
        } else if (expr instanceof AttributeExpression) {
            AttributeExpression attrExpr = (AttributeExpression) expr;
            Attribute attr = attrExpr.getAttribute();
            
            if (context != null) {
                // We're in a composed context, use the join alias
                String joinAlias = getOrCreateJoinAlias(context);
                sql.append(joinAlias);
            } else {
                // We're at root level, use base alias
                sql.append("base");
            }
            sql.append(".")
               .append(attr.getName());
        } else if (expr instanceof BinaryExpression) {
            generateBinaryExpression((BinaryExpression) expr, context, sql, joins);
        } else if (expr instanceof AggregateExpression) {
            generateAggregateExpression((AggregateExpression) expr, context, sql, joins);
        } else if (expr instanceof QueryExpression) {
            generateQueryExpression((QueryExpression) expr, sql);
        } else if (expr instanceof LiteralExpression) {
            generateLiteralExpression((LiteralExpression) expr, sql);
        }
    }

    private void generateLiteralExpression(LiteralExpression expr, StringBuilder sql) {
        ExpressionType type = expr.getType();
        if (type instanceof ScalarType) {
            ScalarType scalarType = (ScalarType) type;
            if (scalarType.equals(ScalarType.STRING)) {
                sql.append("'").append(expr.getValue()).append("'");
            } else if (scalarType.equals(ScalarType.DATE)) {
                sql.append("to_date('").append(expr.getValue().toString()).append("', 'YYYY-MM-DD')");
            } else {
                sql.append(expr.getValue());
            }
        } else {
            throw new IllegalArgumentException("Unsupported literal type: " + type);
        }
    }
    
    private void generateQueryExpression(QueryExpression expr, StringBuilder sql) {
        String subqueryAlias = "sq" + (++subqueryCounter);
        SQLGenerator subqueryGenerator = new SQLGenerator(expr.getQuery());
        sql.append("(")
           .append(subqueryGenerator.generateSQL())
           .append(") AS ")
           .append(subqueryAlias);
    }
    
    private void generateBinaryExpression(BinaryExpression expr, ReferenceAttribute context, StringBuilder sql, Set<JoinInfo> joins) {
        sql.append("(");
        generateExpression(expr.getLeft(), context, sql, joins);
        sql.append(" ")
           .append(getBinaryOperator(expr.getOperator()))
           .append(" ");
        generateExpression(expr.getRight(), context, sql, joins);
        sql.append(")");
    }

    private String getBinaryOperator(Operator operator) {
        switch (operator) {
            case EQUALS: return "=";
            case NOT_EQUALS: return "<>";
            case GREATER_THAN: return ">";
            case GREATER_THAN_OR_EQUALS: return ">=";
            case LESS_THAN: return "<";
            case LESS_THAN_OR_EQUALS: return "<=";
            case AND: return "AND";
            case OR: return "OR";
            case PLUS: return "+";
            case MINUS: return "-";
            case MULTIPLY: return "*";
            case DIVIDE: return "/";
            case LIKE: return "LIKE";
            case IN: return "IN";
            default:
                throw new IllegalArgumentException("Unsupported operator: " + operator);
        }
    }

    private void generateFromClause(Query query, StringBuilder sql, Set<JoinInfo> joins) {
        sql.append(" FROM ");
        if (query.getSourceDomain() instanceof Query) {
            // Handle case where source is another query
            SQLGenerator subqueryGenerator = new SQLGenerator((Query) query.getSourceDomain());
            sql.append("(")
               .append(subqueryGenerator.generateSQL())
               .append(") AS base");
        } else {
            sql.append(query.getSourceDomain().getName())
            .append(" AS base");
        }
    }

    private boolean needsGroupBy(Query query) {
        boolean hasAggregate = false;
        boolean hasNonAggregate = false;
        
        for (Expression expr : query.getProjections().values()) {
            ExpressionType type = expr.getType();
            if (type instanceof AggregateType) {
                hasAggregate = true;
            } else {
                hasNonAggregate = true;
            }
        }
        
        return hasAggregate && hasNonAggregate;
    }

    private void generateGroupByClause(Query query, StringBuilder sql, Set<JoinInfo> joins) {
        boolean first = true;
        for (Expression expr : query.getProjections().values()) {
            if (!expr.getType().isAggregate()) {
                if (!first) sql.append(", ");
                generateExpression(expr, null, sql, joins);
                first = false;
            }
        }
    }

    private void generateAggregateExpression(AggregateExpression expr, ReferenceAttribute context, StringBuilder sql, Set<JoinInfo> joins) {
        sql.append(expr.getFunction().name())
           .append("(");
        generateExpression(expr.getOperand(), context, sql, joins);
        sql.append(")");
    }

    private String getOrCreateJoinAlias(ReferenceAttribute referenceAttribute) {
        JoinInfo joinInfo = new JoinInfo(referenceAttribute, null);
        return joinAliases.computeIfAbsent(joinInfo, ji -> "j" + (++aliasCounter));
    }

    private static class JoinInfo {
        final ReferenceAttribute referenceAttribute;
        final String alias;

        JoinInfo(ReferenceAttribute referenceAttribute, String alias) {
            this.referenceAttribute = referenceAttribute;
            this.alias = alias;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof JoinInfo)) return false;
            JoinInfo joinInfo = (JoinInfo) o;
            // Alias is not part of equality
            return referenceAttribute.equals(joinInfo.referenceAttribute);
        }

        @Override
        public int hashCode() {
            // Alias is not part of hash
            return Objects.hash(referenceAttribute);
        }
    }
} 