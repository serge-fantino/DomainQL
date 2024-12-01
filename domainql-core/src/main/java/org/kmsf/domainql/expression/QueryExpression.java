package org.kmsf.domainql.expression;

import org.kmsf.domainql.expression.type.DomainType;
import org.kmsf.domainql.expression.type.ExpressionType;
import org.kmsf.domainql.expression.type.SourceType;
import org.kmsf.domainql.model.Query;

public class QueryExpression implements Expression {
    private Query query;

    public QueryExpression(Query query) {
        this.query = query;
    }

    @Override
    public SourceType getSource() {
        return new DomainType(query);
    }

    @Override
    public ExpressionType getType() {
        return new DomainType(query);
    }

    public Query getQuery() {
        return query;
    }
} 