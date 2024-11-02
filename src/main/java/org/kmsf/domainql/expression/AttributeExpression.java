package org.kmsf.domainql.expression;

import org.kmsf.domainql.expression.type.ExpressionType;
import org.kmsf.domainql.expression.type.SourceType;

public class AttributeExpression implements Expression {
    
    private Attribute attribute;

    public AttributeExpression(Attribute attribute) {
        this.attribute = attribute;
    }

    public Attribute getAttribute() {
        return attribute;
    }

    @Override
    public ExpressionType getType() {
        if (attribute instanceof ReferenceAttribute) {
            ReferenceAttribute refAttr = (ReferenceAttribute) attribute;
            return refAttr.getReferenceDomain().asDomainType();
        }
        return attribute.getType();
    }

    @Override
    public SourceType getSource() {
        return attribute.getDomain().asDomainType();
    }
} 