package org.kmsf.domainql.expression;

import java.util.LinkedHashMap;
import java.util.Map;

public class Query extends Domain {
    private Domain sourceDomain;
    private Map<String, Expression> projections;
    private Expression filter;
    
    public Query(String name, Domain sourceDomain) {
        super(name);
        this.sourceDomain = sourceDomain;
        this.projections = new LinkedHashMap<>();
    }

    public void addProjection(String alias, Expression expression) {
        // if source is not null, it means the expression is a constant expression
        if (expression.getSource()!=null && !expression.getSource().equals(sourceDomain.asDomainType())) {
            throw new IllegalArgumentException(
                "Expression source must match query source domain. " +
                "Expected: " + sourceDomain.getName() + 
                ", Got: " + expression.getSource()
            );
        }
        
        projections.put(alias, expression);
        // Create corresponding attribute for the query when used as domain
        addAttribute(alias, new Attribute(alias, this, expression.getType()));
    }

    public Domain getSourceDomain() {
        return sourceDomain;
    }

    public Map<String, Expression> getProjections() {
        return projections;
    }

    public Expression getFilter() {
        return filter;
    }

    public void setFilter(Expression filter) {
        this.filter = filter;
    }

    @Override
    public String toString() {
        return "Query{" +
            "name='" + getName() + '\'' +
            ", sourceDomain=" + sourceDomain +
            '}';
    }
} 
