package org.kmsf.domainql.expression;

import org.kmsf.domainql.expression.type.ExpressionType;
import org.kmsf.domainql.expression.type.ScalarType;
import org.kmsf.domainql.expression.type.SourceType;

public class LiteralExpression implements Expression {
    private Object value;

    public LiteralExpression(Object value) {
        this.value = value;
    }

    @Override
    public SourceType getSource() {
        return null; // Literals don't have a source domain
    }

    @Override
    public ExpressionType getType() {
        return ScalarType.fromClass(value.getClass());
    }

    public Object getValue() {
        return value;
    }
} 