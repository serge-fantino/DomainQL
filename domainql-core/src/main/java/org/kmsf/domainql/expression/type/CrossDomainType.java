package org.kmsf.domainql.expression.type;

public class CrossDomainType implements SourceType {
    private final DomainType leftDomain;
    private final DomainType rightDomain;

    public CrossDomainType(DomainType leftDomain, DomainType rightDomain) {
        this.leftDomain = leftDomain;
        this.rightDomain = rightDomain;
    }

    public DomainType getLeftDomain() {
        return leftDomain;
    }

    public DomainType getRightDomain() {
        return rightDomain;
    }

    @Override
    public String toString() {
        return leftDomain.toString() + " x " + rightDomain.toString();
    }
} 