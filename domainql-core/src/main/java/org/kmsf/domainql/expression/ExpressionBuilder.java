package org.kmsf.domainql.expression;

import org.kmsf.domainql.expression.AggregateExpression.AggregateFunction;
import org.kmsf.domainql.expression.type.*;
import org.kmsf.domainql.model.Attribute;
import org.kmsf.domainql.model.Domain;
import org.kmsf.domainql.model.ReferenceAttribute;

public class ExpressionBuilder {

    protected ExpressionBuilder() {
    }

    // Static constructors for attributes and literals
    public static ExpressionBuilder attr(String attributePath) {
        return new AttributeBuilder(attributePath);
    }

    public static ExpressionBuilder literal(Object value) {
        return new LiteralBuilder(value);
    }

    // Comparison operators
    public static ExpressionBuilder EQUALS(ExpressionBuilder left, ExpressionBuilder right) {
        return new BinaryExpressionBuilder(left, Operator.EQUALS, right);
    }

    public static ExpressionBuilder NOT_EQUALS(ExpressionBuilder left, ExpressionBuilder right) {
        return new BinaryExpressionBuilder(left, Operator.NOT_EQUALS, right);
    }

    public static ExpressionBuilder GREATER_THAN(ExpressionBuilder left, ExpressionBuilder right) {
        return new BinaryExpressionBuilder(left, Operator.GREATER_THAN, right);
    }

    public static ExpressionBuilder GREATER_THAN_OR_EQUALS(ExpressionBuilder left, ExpressionBuilder right) {
        return new BinaryExpressionBuilder(left, Operator.GREATER_THAN_OR_EQUALS, right);
    }

    public static ExpressionBuilder LESS_THAN(ExpressionBuilder left, ExpressionBuilder right) {
        return new BinaryExpressionBuilder(left, Operator.LESS_THAN, right);
    }

    public static ExpressionBuilder LESS_THAN_OR_EQUALS(ExpressionBuilder left, ExpressionBuilder right) {
        return new BinaryExpressionBuilder(left, Operator.LESS_THAN_OR_EQUALS, right);
    }

    public static ExpressionBuilder LIKE(ExpressionBuilder left, ExpressionBuilder right) {
        return new BinaryExpressionBuilder(left, Operator.LIKE, right);
    }

    public static ExpressionBuilder IN(ExpressionBuilder left, ExpressionBuilder right) {
        return new BinaryExpressionBuilder(left, Operator.IN, right);
    }

    // Logical operators
    public static ExpressionBuilder AND(ExpressionBuilder left, ExpressionBuilder right) {
        return new BinaryExpressionBuilder(left, Operator.AND, right);
    }

    public static ExpressionBuilder OR(ExpressionBuilder left, ExpressionBuilder right) {
        return new BinaryExpressionBuilder(left, Operator.OR, right);
    }

    // Arithmetic operators
    public static ExpressionBuilder PLUS(ExpressionBuilder left, ExpressionBuilder right) {
        return new BinaryExpressionBuilder(left, Operator.PLUS, right);
    }

    public static ExpressionBuilder MINUS(ExpressionBuilder left, ExpressionBuilder right) {
        return new BinaryExpressionBuilder(left, Operator.MINUS, right);
    }

    public static ExpressionBuilder MULTIPLY(ExpressionBuilder left, ExpressionBuilder right) {
        return new BinaryExpressionBuilder(left, Operator.MULTIPLY, right);
    }

    public static ExpressionBuilder DIVIDE(ExpressionBuilder left, ExpressionBuilder right) {
        return new BinaryExpressionBuilder(left, Operator.DIVIDE, right);
    }

    // Convenience methods for common patterns
    public static ExpressionBuilder eq(String attributePath, Object value) {
        return EQUALS(attr(attributePath), literal(value));
    }

    public static ExpressionBuilder gt(String attributePath, Object value) {
        return GREATER_THAN(attr(attributePath), literal(value));
    }

    // Aggregate functions
    public static ExpressionBuilder COUNT(ExpressionBuilder operand) {
        return new AggregateExpressionBuilder(operand, AggregateFunction.COUNT);
    }

    public static ExpressionBuilder SUM(ExpressionBuilder operand) {
        return new AggregateExpressionBuilder(operand, AggregateFunction.SUM);
    }

    public static ExpressionBuilder AVG(ExpressionBuilder operand) {
        return new AggregateExpressionBuilder(operand, AggregateFunction.AVG);
    }

    public static ExpressionBuilder MIN(ExpressionBuilder operand) {
        return new AggregateExpressionBuilder(operand, AggregateFunction.MIN);
    }

    public static ExpressionBuilder MAX(ExpressionBuilder operand) {
        return new AggregateExpressionBuilder(operand, AggregateFunction.MAX);
    }

    // Special case for COUNT(*)
    public static ExpressionBuilder COUNT_ALL() {
        return new AggregateExpressionBuilder(null, AggregateFunction.COUNT);
    }

    // Build the actual Expression
    public Expression build(Domain rootDomain) {
        throw new UnsupportedOperationException("This method should be implemented by subclasses");
    }
}

class AttributeBuilder extends ExpressionBuilder {

    private final String attributePath;

    protected AttributeBuilder(String attributePath) {
        this.attributePath = attributePath;
    }

    @Override
    public Expression build(Domain rootDomain) {
        return buildAttributePath(rootDomain, attributePath);
    }


    private Expression buildAttributePath(Domain rootDomain, String attributePath) {
        String[] attributes = attributePath.split("\\.");
        return buildAttributePathRecursive(rootDomain, attributes, 0);
    }

    private Expression buildAttributePathRecursive(Domain currentDomain, String[] attributes, int index) {
        // Base case - no more attributes to process
        if (index >= attributes.length) {
            return null;
        }

        String attrName = attributes[index];
        Attribute attr = currentDomain.getAttribute(attrName);
        
        if (attr == null) {
            throw new IllegalArgumentException(
                "Attribute '" + attrName + "' not found in domain " + currentDomain.getName()
            );
        }

        Expression attrExpr = new AttributeExpression(attr);

        // If this is the last attribute, return it directly
        if (index == attributes.length - 1) {
            return attrExpr;
        }

        // For intermediate attributes, verify it's a reference and recurse
        if (!(attr instanceof ReferenceAttribute)) {
            throw new IllegalArgumentException(
                "Attribute '" + attrName + "' in path must be a reference attribute"
            );
        }

        Domain nextDomain = ((ReferenceAttribute) attr).getReferenceDomain();
        Expression remainingPath = buildAttributePathRecursive(nextDomain, attributes, index + 1);

        return new ComposeExpression(attrExpr, remainingPath);
    }
}

class LiteralBuilder extends ExpressionBuilder {

    private final Object value;

    protected LiteralBuilder(Object value) {
        this.value = value;
    }

    @Override
    public Expression build(Domain rootDomain) {
        return new LiteralExpression(value);
    }
}

class BinaryExpressionBuilder extends ExpressionBuilder {
    private final ExpressionBuilder left;
    private final Operator operator;
    private final ExpressionBuilder right;

    BinaryExpressionBuilder(ExpressionBuilder left, Operator operator, ExpressionBuilder right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    @Override
    public Expression build(Domain rootDomain) {
        return new BinaryExpression(
            left.build(rootDomain),
            operator,
            right.build(rootDomain)
        );
    }
} 

class AggregateExpressionBuilder extends ExpressionBuilder {

    private final ExpressionBuilder operand;
    private final AggregateExpression.AggregateFunction function;

    protected AggregateExpressionBuilder(ExpressionBuilder operand, AggregateExpression.AggregateFunction function) {
        this.operand = operand;
        this.function = function;
    }

    @Override
    public Expression build(Domain rootDomain) {
        Expression operandExpr = operand == null ? null : operand.build(rootDomain);
        return new AggregateExpression(operandExpr, function);
    }
}