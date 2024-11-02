package org.kmsf.domainql.expression.type;

import org.kmsf.domainql.expression.Domain;

public class DomainType implements ExpressionType, SourceType {
    private final Domain domain;

    public DomainType(Domain domain) {
        this.domain = domain;
    }

    public Domain getDomain() {
        return domain;
    }

    @Override
    public boolean isScalar() { return false; }

    @Override
    public boolean isAggregate() { return false; }

    @Override
    public boolean isDomain() { return true; }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DomainType && ((DomainType) obj).domain.equals(domain);
    }
} 