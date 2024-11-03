package org.kmsf.domainql.expression;

import org.kmsf.domainql.expression.type.Operator;

public class QueryBuilder {
    private final Query query;
    private final Domain sourceDomain;

    private QueryBuilder(String name, Domain sourceDomain) {
        this.sourceDomain = sourceDomain;
        this.query = new Query(name, sourceDomain);
    }

    public static QueryBuilder from(String name, Domain sourceDomain) {
        return new QueryBuilder(name, sourceDomain);
    }

    public QueryBuilder select(String alias, String attributePath) {
        String[] attributes = attributePath.split("\\.");
        Expression expression = buildAttributePath(attributes);
        query.addProjection(alias, expression);
        return this;
    }

    public QueryBuilder select(String attributePath) {
        String[] attributes = attributePath.split("\\.");
        // Use the last attribute name as alias
        return select(attributes[attributes.length - 1], attributePath);
    }

    private Expression buildAttributePath(String[] attributes) {
        Domain currentDomain = sourceDomain;
        Expression expression = null;

        for (int i = 0; i < attributes.length; i++) {
            String attrName = attributes[i];
            Attribute attr = currentDomain.getAttribute(attrName);
            
            if (attr == null) {
                throw new IllegalArgumentException(
                    "Attribute '" + attrName + "' not found in domain " + currentDomain.getName()
                );
            }

            Expression attrExpr = new AttributeExpression(attr);
            
            if (expression == null) {
                expression = attrExpr;
            } else {
                expression = new ComposeExpression(expression, attrExpr);
            }

            // If not the last attribute, it must be a reference
            if (i < attributes.length - 1) {
                if (!(attr instanceof ReferenceAttribute)) {
                    throw new IllegalArgumentException(
                        "Attribute '" + attrName + "' in path must be a reference attribute"
                    );
                }
                currentDomain = ((ReferenceAttribute) attr).getReferenceDomain();
            }
        }

        return expression;
    }

    public QueryBuilder whereEquals(String attributePath, Object value) {
        String[] attributes = attributePath.split("\\.");
        Expression pathExpr = buildAttributePath(attributes);
        
        // Get the type from the last attribute in the path
        Domain currentDomain = sourceDomain;
        for (int i = 0; i < attributes.length - 1; i++) {
            Attribute attr = currentDomain.getAttribute(attributes[i]);
            currentDomain = ((ReferenceAttribute) attr).getReferenceDomain();
        }
        Expression filter = new BinaryExpression(
            pathExpr,
            Operator.EQUALS,
            new LiteralExpression(value)
        );
        return where(filter);
    }

    public QueryBuilder where(Expression filter) {
        query.setFilter(filter);
        return this;
    }

    public Query build() {
        return query;
    }
} 