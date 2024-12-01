package org.kmsf.domainql.expression;

import org.kmsf.domainql.expression.type.ExpressionType;
import org.kmsf.domainql.expression.type.DomainType;
import org.kmsf.domainql.model.Domain;

/**
 * a MappingReference is an simple expression that will connect DomainQL with some underlying mapping system, usually a SQL database.
 * MappingReference can then be translated by a MappingEvaluator, to resolve DomainQL references.
 * 
 * The sourceType of a MappingReference is always a DomainType, which references a Domain.
 */
public class MappingReference implements Expression {
    private final String name;
    private final Domain domain;
    private final ExpressionType type;

    public MappingReference(Domain domain, String name, ExpressionType type) {
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

    @Override
    public ExpressionType getType() {
        return type;
    }

    @Override
    public DomainType getSource() {
        return domain.asDomainType();
    }
}
