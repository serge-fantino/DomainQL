package org.kmsf.domainql.expression;

import java.util.HashMap;
import java.util.Map;

import org.kmsf.domainql.expression.type.DomainType;

public class Domain {
    private String name;
    private Map<String, Attribute> attributes;
    private final DomainType domainType = new DomainType(this);

    public Domain(String name) {
        this.name = name;
        this.attributes = new HashMap<>();
    }

    public void addAttribute(String name, Attribute attribute) {
        attributes.put(name, attribute);
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