package org.kmsf.domainql.model;

import org.kmsf.domainql.expression.AttributeExpression;
import org.kmsf.domainql.expression.BinaryExpression;
import org.kmsf.domainql.expression.ComposeExpression;
import org.kmsf.domainql.expression.Expression;
import org.kmsf.domainql.expression.ExpressionBuilder;
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

    public QueryBuilder select(String alias, ExpressionBuilder exprBuilder) {
        query.addProjection(alias, exprBuilder.build(sourceDomain));
        return this;
    }

    private Expression buildAttributePath(String[] attributes) {
        return buildAttributePathRecursive(sourceDomain, attributes, 0);
    }

    private Expression buildAttributePathRecursive(Domain currentDomain, String[] attributes, int index) {
        // Base case - no more attributes to process
        if (index >= attributes.length) {
            return null;
        }

        String attrName = attributes[index];
        Attribute attr = currentDomain.getAttribute(attrName);
        
        if (attr == null) {
            throw new IllegalArgumentException(
                "Attribute '" + attrName + "' not found in domain " + currentDomain.getName()
            );
        }

        Expression attrExpr = new AttributeExpression(attr);

        // If this is the last attribute, return it directly
        if (index == attributes.length - 1) {
            return attrExpr;
        }

        // For intermediate attributes, verify it's a reference and recurse
        if (!(attr instanceof ReferenceAttribute)) {
            throw new IllegalArgumentException(
                "Attribute '" + attrName + "' in path must be a reference attribute"
            );
        }

        Domain nextDomain = ((ReferenceAttribute) attr).getReferenceDomain();
        Expression remainingPath = buildAttributePathRecursive(nextDomain, attributes, index + 1);

        return new ComposeExpression(attrExpr, remainingPath);
    }

    public QueryBuilder where(ExpressionBuilder exprBuilder) {
        return where(exprBuilder.build(sourceDomain));
    }

    public QueryBuilder where(Expression filter) {
        if (query.getFilter() == null) {
            query.setFilter(filter);
        } else {
            query.setFilter(new BinaryExpression(query.getFilter(), Operator.AND, filter));
        }
        return this;
    }

    public Query build() {
        return query;
    }
} 