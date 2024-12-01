package org.kmsf.domainql.expression;

import org.kmsf.domainql.expression.type.DomainType;
import org.kmsf.domainql.expression.type.ExpressionType;
import org.kmsf.domainql.expression.type.SourceType;

public class ComposeExpression implements Expression {
    private final Expression reference;
    private final Expression composition;

    public ComposeExpression(Expression reference, Expression composition) {
        // Validate that reference returns a DomainType
        if (!(reference.getType() instanceof DomainType)) {
            throw new IllegalArgumentException("Reference expression must return a DomainType");
        }
        
        // Validate that composition's source matches reference's target domain
        DomainType refType = (DomainType) reference.getType();
        if (!composition.getSource().equals(refType)) {
            throw new IllegalArgumentException("Composition expression must reference the domain returned by reference");
        }

        this.reference = reference;
        this.composition = composition;
    }

    @Override
    public SourceType getSource() {
        return reference.getSource();
    }

    @Override
    public ExpressionType getType() {
        return composition.getType();
    }

    public Expression getReference() {
        return reference;
    }

    public Expression getComposition() {
        return composition;
    }
} 