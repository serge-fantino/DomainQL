package org.kmsf.domainql.model;

import org.kmsf.domainql.expression.Expression;
import org.kmsf.domainql.expression.MappingReference;
import org.kmsf.domainql.expression.type.ExpressionType;
import com.google.gson.JsonObject;

/**
 * A Atribute defines in the scope of a Domain the relation between a name and a computational definition.
 * The definition can just be a simple mapping to some underlying system, like a database column.
 * It can also be a link to a Relation, thus defining a way to navigate the model.
 * It can also be a generic expression involving some computation / relation / etc...
 */
public class Attribute {
    private final String name;
    private final Domain domain;
    private final ExpressionType type;
    private final Expression expression;
    /**
     * This is the legacy constructor - in that case we will define a simple mapping reference with the same name and type.
     * @param name
     * @param domain
     * @param type
     */
    public Attribute(String name, Domain domain, ExpressionType type) {
        this.name = name;
        this.domain = domain;
        this.type = type;
        this.expression = new MappingReference(domain, name, type);
    }

    /**
     * This is the constructor for a new Attribute, based on an actual expression
     * @return
     */
    public Attribute(String name, Domain domain, Expression expression) {
        this.name = name;
        this.domain = domain;
        this.type = expression.getType();
        this.expression = expression;
    }

    public String getName() {
        return name;
    }

    public Domain getDomain() {
        return domain;
    }

    public Expression getExpression() {
        return expression;
    }

    public ExpressionType getType() {
        return type;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("name", getName());
        json.addProperty("type", getType().toString());
        json.addProperty("expression", expression.toString());  
        return json;
    }
} 