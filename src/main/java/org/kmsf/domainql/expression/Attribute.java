package org.kmsf.domainql.expression;

import org.kmsf.domainql.expression.type.ExpressionType;

public class Attribute {
    private final String name;
    private final Domain domain;
    private final ExpressionType type;

    public Attribute(String name, Domain domain, ExpressionType type) {
        this.name = name;
        this.domain = domain;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public Domain getDomain() {
        return domain;
    }

    public ExpressionType getType() {
        return type;
    }
} 