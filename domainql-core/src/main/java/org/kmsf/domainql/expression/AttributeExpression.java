package org.kmsf.domainql.expression;

import org.kmsf.domainql.expression.type.ExpressionType;
import org.kmsf.domainql.expression.type.SourceType;
import org.kmsf.domainql.model.Attribute;
import org.kmsf.domainql.model.ReferenceAttribute;

public class AttributeExpression implements Expression {

    public enum ContextResolution {
        DEFAULT,  // use attribute's domain
        LEFT,     // use left side of join
        RIGHT     // use right side of join
    }

    private final Attribute attribute;
    private final ContextResolution contextResolution;

    public AttributeExpression(Attribute attribute) {
        this(attribute, ContextResolution.DEFAULT);
    }

    public AttributeExpression(Attribute attribute, ContextResolution contextResolution) {
        this.attribute = attribute;
        this.contextResolution = contextResolution;
    }

    public Attribute getAttribute() {
        return attribute;
    }

    public ContextResolution getContextResolution() {
        return contextResolution;
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