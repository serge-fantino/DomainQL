package org.kmsf.domainql.expression;

import java.util.HashMap;
import java.util.Map;

import org.kmsf.domainql.expression.type.DomainType;
import org.kmsf.domainql.expression.type.Operator;
import org.kmsf.domainql.expression.type.ScalarType;

public class Domain {
    private String name;
    private Map<String, Attribute> attributes;
    private final DomainType domainType = new DomainType(this);

    public Domain(String name) {
        this.name = name;
        this.attributes = new HashMap<>();
    }

    public void addAttribute(Attribute attribute) {
        attributes.put(attribute.getName(), attribute);
    }

    public void addAttribute(String name, Attribute attribute) {
        attributes.put(name, attribute);
    }

    public Attribute addAttribute(String name, ScalarType type) {
        Attribute attribute = new Attribute(name, this, type);
        attributes.put(name, attribute);
        return attribute;
    }

    public ReferenceAttribute addReference(String name, String sourceReference, Domain targetDomain, String targetReference) {
        Expression joinCondition;
        if (this.equals(targetDomain)) {
            // Self-join case: use explicit LEFT/RIGHT context resolution
            joinCondition = new BinaryExpression(
                new AttributeExpression(getAttribute(sourceReference), AttributeExpression.ContextResolution.LEFT),
                Operator.EQUALS,
                new AttributeExpression(targetDomain.getAttribute(targetReference), AttributeExpression.ContextResolution.RIGHT)
            );
        } else {
            // Regular join case: use default context resolution
            joinCondition = new BinaryExpression(
                    new AttributeExpression(getAttribute(sourceReference)),
                Operator.EQUALS,
                new AttributeExpression(targetDomain.getAttribute(targetReference))
            );
        }
        
        ReferenceAttribute referenceAttribute = new ReferenceAttribute(name, this, targetDomain, joinCondition);
        attributes.put(name, referenceAttribute);
        return referenceAttribute;
    }

    public Attribute getAttribute(String name) {
        Attribute attribute = attributes.get(name);
        if (attribute == null) {
            throw new IllegalArgumentException("Attribute '" + name + "' not found in domain '" + this.name + "'");
        }
        return attribute;
    }

    public String getName() {
        return name;
    }
    
    public DomainType asDomainType() {
        return domainType;
    }
} 