package org.kmsf.domainql.expression;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;

public class DomainRegistry {
    private final Map<String, Domain> domains = new HashMap<>();
    
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
    
    public Collection<Domain> getAllDomains() {
        return Collections.unmodifiableCollection(domains.values());
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        domains.forEach((name, domain) -> json.add(name, domain.toJson()));
        return json;
    }
} 
