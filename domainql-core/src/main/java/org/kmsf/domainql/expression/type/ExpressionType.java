package org.kmsf.domainql.expression.type;

public interface ExpressionType {
    boolean isScalar();
    boolean isAggregate();
    boolean isDomain();

    default DomainType hasDomainType() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("This type does not have a domain type");
    }
} 