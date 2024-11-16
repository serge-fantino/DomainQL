package org.kmsf.domainql.expression;

import org.kmsf.domainql.expression.type.CrossDomainType;
import org.kmsf.domainql.expression.type.DomainType;
import org.kmsf.domainql.expression.type.ScalarType;
import org.kmsf.domainql.expression.type.SourceType;

public class ReferenceAttribute extends Attribute {
    private final Domain referenceDomain;
    private final Expression joinCondition;

    public ReferenceAttribute(String name, Domain domain, Domain referenceDomain, Expression joinCondition) {
        super(name, domain, new DomainType(referenceDomain));
        
        // Validate join condition
        if (!joinCondition.getType().equals(ScalarType.BOOLEAN)) {
            throw new IllegalArgumentException("Join condition must be boolean");
        }
        
        // Validate that join condition source matches the domains
        if (!isValidJoinConditionSource(joinCondition.getSource(), domain, referenceDomain)) {
            throw new IllegalArgumentException("Join condition must reference only the source and reference domains");
        }

        this.referenceDomain = referenceDomain;
        this.joinCondition = joinCondition;
    }

    private boolean isValidJoinConditionSource(SourceType source, Domain sourceDomain, Domain targetDomain) {
        if (source instanceof CrossDomainType) {
            CrossDomainType crossDomain = (CrossDomainType) source;
            DomainType left = crossDomain.getLeftDomain();
            DomainType right = crossDomain.getRightDomain();
            return (left.getDomain().equals(sourceDomain) && right.getDomain().equals(targetDomain)) ||
                   (left.getDomain().equals(targetDomain) && right.getDomain().equals(sourceDomain));
        } else if (source instanceof DomainType && sourceDomain.equals(targetDomain)) {
            DomainType domain = (DomainType) source;
            return domain.getDomain().equals(sourceDomain);
        }
        return false;
    }

    public Domain getReferenceDomain() {
        return referenceDomain;
    }

    public Expression getJoinCondition() {
        return joinCondition;
    }
} 
