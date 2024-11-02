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

    private int aliasCounter = 0;
    private int subqueryCounter = 0;
    
        public String generateSQL(Query query) {
            StringBuilder sql = new StringBuilder("SELECT ");
            Set<JoinInfo> joins = new LinkedHashSet<>();
            
            // Generate projections
            generateProjections(query, sql, joins);
            
            // Generate FROM clause with necessary JOINs
            sql.append(" FROM ");
            generateFromClause(query, sql, joins);
            
            // Add JOINs
            for (JoinInfo join : joins) {
                sql.append(" JOIN ")
                   .append(join.targetDomain.getName())
                   .append(" ON ");
                generateExpression(join.condition, sql, joins);
                sql.append(" AS ")
                   .append(join.alias);
            }
    
            // Generate WHERE clause if filter exists
            if (query.getFilter() != null) {
                sql.append(" WHERE ");
                generateExpression(query.getFilter(), sql, joins);
            }
    
            // Generate GROUP BY if needed
            if (needsGroupBy(query)) {
                sql.append(" GROUP BY ");
                generateGroupByClause(query, sql);
            }
    
            return sql.toString();
        }
    
        private void generateProjections(Query query, StringBuilder sql, Set<JoinInfo> joins) {
            boolean first = true;
            for (Map.Entry<String, Expression> projection : query.getProjections().entrySet()) {
                if (!first) sql.append(", ");
                generateExpression(projection.getValue(), sql, joins);
                sql.append(" AS ").append(projection.getKey());
                first = false;
            }
        }
    
        private void generateExpression(Expression expr, StringBuilder sql, Set<JoinInfo> joins) {
            if (expr instanceof ComposeExpression) {
                ComposeExpression compose = (ComposeExpression) expr;
                Expression reference = compose.getReference();
                
                // Handle the reference part (which should be an AttributeExpression with ReferenceAttribute)
                if (reference instanceof AttributeExpression) {
                    AttributeExpression attrExpr = (AttributeExpression) reference;
                    if (attrExpr.getAttribute() instanceof ReferenceAttribute) {
                        ReferenceAttribute refAttr = (ReferenceAttribute) attrExpr.getAttribute();
                        String joinAlias = "j" + (++aliasCounter);
                        
                        // Add join info for reference attribute
                        joins.add(new JoinInfo(
                            refAttr.getReferenceDomain(),
                            refAttr.getJoinCondition(),
                            joinAlias
                        ));
                        
                        // Generate the composition part using the join alias
                        Expression composition = compose.getComposition();
                        if (composition instanceof AttributeExpression) {
                            AttributeExpression compAttr = (AttributeExpression) composition;
                            sql.append(joinAlias)
                               .append(".")
                               .append(compAttr.getAttribute().getName());
                        } else {
                            // Handle other types of composition expressions
                            generateExpression(composition, sql, joins);
                        }
                    }
                }
            } else if (expr instanceof AttributeExpression) {
                AttributeExpression attrExpr = (AttributeExpression) expr;
                Attribute attr = attrExpr.getAttribute();
                
                if (attr instanceof ReferenceAttribute) {
                    ReferenceAttribute refAttr = (ReferenceAttribute) attr;
                    // Add join info for reference attribute
                    joins.add(new JoinInfo(
                        refAttr.getReferenceDomain(),
                        refAttr.getJoinCondition(),
                        "j" + (++aliasCounter)
                    ));
                }
                
                sql.append(attr.getDomain().getName())
                   .append(".")
                   .append(attr.getName());
            } else if (expr instanceof BinaryExpression) {
                generateBinaryExpression((BinaryExpression) expr, sql, joins);
            } else if (expr instanceof AggregateExpression) {
                generateAggregateExpression((AggregateExpression) expr, sql, joins);
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
        sql.append("(")
           .append(generateSQL(expr.getQuery()))
           .append(") AS ")
           .append(subqueryAlias);
    }
    
    private void generateBinaryExpression(BinaryExpression expr, StringBuilder sql, Set<JoinInfo> joins) {
        sql.append("(");
        generateExpression(expr.getLeft(), sql, joins);
        sql.append(" ")
           .append(getBinaryOperator(expr.getOperator()))
           .append(" ");
        generateExpression(expr.getRight(), sql, joins);
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
        if (query.getSourceDomain() instanceof Query) {
            // Handle case where source is another query
            String subqueryAlias = "sq" + (++subqueryCounter);
            sql.append("(")
               .append(generateSQL((Query) query.getSourceDomain()))
               .append(") AS ")
               .append(subqueryAlias);
        } else {
            sql.append(query.getSourceDomain().getName());
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

    private void generateGroupByClause(Query query, StringBuilder sql) {
        boolean first = true;
        for (Expression expr : query.getProjections().values()) {
            if (!expr.getType().isAggregate()) {
                if (!first) sql.append(", ");
                generateExpression(expr, sql, new HashSet<>());
                first = false;
            }
        }
    }

    private void generateAggregateExpression(AggregateExpression expr, StringBuilder sql, Set<JoinInfo> joins) {
        sql.append(expr.getFunction().name())
           .append("(");
        generateExpression(expr.getOperand(), sql, joins);
        sql.append(")");
    }

    private static class JoinInfo {
        final Domain targetDomain;
        final Expression condition;
        final String alias;

        JoinInfo(Domain targetDomain, Expression condition, String alias) {
            this.targetDomain = targetDomain;
            this.condition = condition;
            this.alias = alias;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof JoinInfo)) return false;
            JoinInfo joinInfo = (JoinInfo) o;
            return targetDomain.equals(joinInfo.targetDomain) &&
                   condition.equals(joinInfo.condition);
        }

        @Override
        public int hashCode() {
            return Objects.hash(targetDomain, condition);
        }
    }
} 