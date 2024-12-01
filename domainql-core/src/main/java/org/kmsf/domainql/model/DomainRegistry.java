package org.kmsf.domainql.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.kmsf.domainql.expression.Expression;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * 
 * A DomainRegistry is a collection of Domains, that can be registered and retrieved by name.
 * It also allows to register relationships between Domains using Relation objects.
 * A Relation can then by used in a DomainQL expression to navigate between domains.
 * 
 */
public class DomainRegistry {
    private final Map<String, Domain> domains = new HashMap<>();
    private final List<Relation> relations = new ArrayList<>();
    
    public void register(Domain domain) {
        domains.put(domain.getName(), domain);
    }
    
    public Domain getDomain(String name) {
        Domain domain = domains.get(name);
        if (domain == null) {
            throw new IllegalArgumentException("Domain '" + name + "' not found in registry");
        }
        return domain;
    }
    
    public boolean hasDomain(String name) {
        return domains.containsKey(name);
    }

    public Map<String, Domain> getDomains() {
        return Collections.unmodifiableMap(domains);
    }
    
    public Collection<Domain> getAllDomains() {
        return Collections.unmodifiableCollection(domains.values());
    }

    public void registerRelationship(Domain leftDomain, Domain rightDomain, Expression joinCondition) {
        Relation relation = new Relation(leftDomain, rightDomain, joinCondition);
        relations.add(relation);
    }

    public List<Relation> getRelations() {
        return Collections.unmodifiableList(relations);
    }

    public List<Relation> getRelationsForDomain(String domainName) {
        List<Relation> domainRelations = new ArrayList<>();
        for (Relation relation : relations) {
            if (relation.getLeftDomain().getName().equals(domainName) || 
                relation.getRightDomain().getName().equals(domainName)) {
                domainRelations.add(relation);
            }
        }
        return Collections.unmodifiableList(domainRelations);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        JsonArray jsonDomains = new JsonArray();
        domains.values().forEach((domain) -> jsonDomains.add(domain.toJson()));
        json.add("domains", jsonDomains);
        
        JsonArray jsonRelations = new JsonArray();
        relations.forEach((relation) -> jsonRelations.add(relation.toJson()));
        json.add("relations", jsonRelations);
        
        return json;
    }
} 
