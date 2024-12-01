package org.kmsf.domainql.expression;

import org.kmsf.domainql.expression.type.*;

public class BinaryExpression implements Expression {

    private final Expression left;
    private final Expression right;
    private final Operator operator;
    private final SourceType source;

    public BinaryExpression(Expression left, Operator operator, Expression right) {
        this.left = left;
        this.right = right;
        this.operator = operator;
        
        // Determine source type based on operands
        this.source = determineSourceType(left.getSource(), right.getSource());
    }

    public Expression getLeft() {
        return left;
    }

    public Expression getRight() {
        return right;
    }   

    public Operator getOperator() {
        return operator;
    }   

    private SourceType determineSourceType(SourceType leftSource, SourceType rightSource) {
        if (leftSource == null && rightSource == null) {
            return null;
        }
        if (leftSource == null) {
            return rightSource;
        }
        if (rightSource == null) {
            return leftSource;
        }
        if (leftSource.equals(rightSource)) {
            return leftSource;
        }
        if (leftSource instanceof DomainType && rightSource instanceof DomainType) {
            DomainType leftDomain = (DomainType) leftSource;
            DomainType rightDomain = (DomainType) rightSource;
            return new CrossDomainType(leftDomain, rightDomain);
        }
        // Handle other cases or throw exception
        throw new IllegalArgumentException("Unsupported source types combination");
    }

    @Override
    public SourceType getSource() {
        return source;
    }

    @Override
    public ExpressionType getType() {
        if (operator.getReturnType() != null) {
            return operator.getReturnType();
        }
        
        // For arithmetic operators, determine type based on operands
        ExpressionType leftType = left.getType();
        ExpressionType rightType = right.getType();
        
        if (!(leftType instanceof ScalarType) || !(rightType instanceof ScalarType)) {
            throw new IllegalArgumentException("Arithmetic operations require scalar types");
        }
        
        // If either operand is DECIMAL, result is DECIMAL
        if (leftType == ScalarType.DECIMAL || rightType == ScalarType.DECIMAL) {
            return ScalarType.DECIMAL;
        }
        
        // Otherwise, result is INTEGER
        return ScalarType.INTEGER;
    }
} 