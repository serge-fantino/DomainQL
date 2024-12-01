package org.kmsf.domainql.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A DomainChangeSet is a collection of DomainChanges, where a DomainChange records a modification of the overall domains.
 * 
 * A DomainChange identifies a Domain, and records the changes to that Domain:
 * - addDomain(Domain)  
 * - modifyDomain(Domain)
 * - removeDomain(Domain)
 * - addAttribute(Attribute)    
 * - modifyAttribute(Attribute)
 * - removeAttribute(Attribute)
 * - addRelation(Relation)
 * - modifyRelation(Relation)
 * - removeRelation(Relation)
 */
public class DomainChangeSet {
    private List<DomainChange> changes = new ArrayList<>();
    
    public void addChange(DomainChange change) {
        changes.add(change);
    }

    public void addChange(Domain domain, ChangeType type, Object changedElement) {
        changes.add(new DomainChange(domain, type, changedElement));
    }
    
    public List<DomainChange> getChanges() {
        return changes;
    }

    public enum ChangeType {
        ADD_DOMAIN,
        MODIFY_DOMAIN,
        REMOVE_DOMAIN,
        ADD_ATTRIBUTE,
        MODIFY_ATTRIBUTE,
        REMOVE_ATTRIBUTE,
        ADD_RELATION,
        MODIFY_RELATION,
        REMOVE_RELATION
    }

    public static class DomainChange {
        private Domain domain;
        private ChangeType type;
        private Object changedElement; // Can be Domain, Attribute or Relation
        
        public DomainChange(Domain domain, ChangeType type, Object changedElement) {
            this.domain = domain;
            this.type = type;
            this.changedElement = changedElement;
        }
        
        public Domain getDomain() {
            return domain;
        }
        
        public ChangeType getType() {
            return type;
        }
        
        public Object getChangedElement() {
            return changedElement;
        }
    }
}

