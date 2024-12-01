package org.kmsf.domainql.model;

import java.util.Objects;

import org.kmsf.domainql.expression.Expression;
import org.kmsf.domainql.expression.type.CrossDomainType;
import org.kmsf.domainql.expression.type.DomainType;
import org.kmsf.domainql.expression.type.ScalarType;
import org.kmsf.domainql.expression.type.SourceType;

import com.google.gson.JsonObject;

/** 
 * Represents a relationship between two domains.
 * It is a generalization of a foreign key constraint, that can use any expression to connect two domains.
 * 
 */
public class Relation {
    private final Domain leftDomain;
    private final Domain rightDomain;

    private final Expression joinCondition;

    public Relation(Domain leftDomain, Domain rightDomain, Expression joinCondition) {
        this.leftDomain = leftDomain;
        this.rightDomain = rightDomain;
        this.joinCondition = joinCondition;
        if (!isValidJoinCondition(joinCondition)) {
            throw new IllegalArgumentException("Invalid join condition");
        }
    }

    public Domain getLeftDomain() {
        return leftDomain;
    }

    public Domain getRightDomain() {
        return rightDomain;
    }

    public Expression getJoinCondition() {
        return joinCondition;
    }

    private boolean isValidJoinCondition(Expression joinCondition) {
        if (!joinCondition.getType().equals(ScalarType.BOOLEAN)) {
            throw new IllegalArgumentException("Join condition must be boolean");
        }
        return isValidJoinConditionDomains(joinCondition.getSource(), leftDomain, rightDomain);
    }

    private boolean isValidJoinConditionDomains(SourceType source, Domain leftDomain, Domain rightDomain) {
        if (source instanceof CrossDomainType) {
            CrossDomainType crossDomain = (CrossDomainType) source;
            DomainType left = crossDomain.getLeftDomain();
            DomainType right = crossDomain.getRightDomain();
            // symetric is ok
            return (left.getDomain().equals(leftDomain) && right.getDomain().equals(rightDomain)) ||
                   (left.getDomain().equals(rightDomain) && right.getDomain().equals(leftDomain));
        } else if (source instanceof DomainType && leftDomain.equals(rightDomain)) {
            // self reference
            DomainType domain = (DomainType) source;
            return domain.getDomain().equals(leftDomain);
        }
        return false;
    }

    @Override
    public String toString() {
        return "Relation{" +
            "leftDomain=" + leftDomain.getName() +
            ", rightDomain=" + rightDomain.getName() +
            ", joinCondition=" + joinCondition +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Relation relation = (Relation) o;
        return Objects.equals(leftDomain, relation.leftDomain) && Objects.equals(rightDomain, relation.rightDomain) && Objects.equals(joinCondition, relation.joinCondition);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("leftDomain", leftDomain.getName());
        json.addProperty("rightDomain", rightDomain.getName());
        json.addProperty("joinCondition", joinCondition.toString());
        return json;
    }
}
