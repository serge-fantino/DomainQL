package org.kmsf.domainql.expression.type;

public interface ExpressionType {
    boolean isScalar();
    boolean isAggregate();
    boolean isDomain();
} 