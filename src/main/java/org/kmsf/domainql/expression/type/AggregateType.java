package org.kmsf.domainql.expression.type;

public class AggregateType implements ExpressionType {
    private final ScalarType resultType;

    public AggregateType(ScalarType resultType) {
        this.resultType = resultType;
    }

    public ScalarType getResultType() {
        return resultType;
    }

    @Override
    public boolean isScalar() { return false; }

    @Override
    public boolean isAggregate() { return true; }

    @Override
    public boolean isDomain() { return false; }

    @Override
    public String toString() {
        return resultType.toString();
    }
} 