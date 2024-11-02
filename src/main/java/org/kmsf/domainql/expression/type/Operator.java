package org.kmsf.domainql.expression.type;

public enum Operator {
    // Comparison operators (return boolean)
    EQUALS(ScalarType.BOOLEAN),
    NOT_EQUALS(ScalarType.BOOLEAN),
    GREATER_THAN(ScalarType.BOOLEAN),
    GREATER_THAN_OR_EQUALS(ScalarType.BOOLEAN),
    LESS_THAN(ScalarType.BOOLEAN),
    LESS_THAN_OR_EQUALS(ScalarType.BOOLEAN),
    LIKE(ScalarType.BOOLEAN),
    IN(ScalarType.BOOLEAN),
    
    // Logical operators (return boolean)
    AND(ScalarType.BOOLEAN),
    OR(ScalarType.BOOLEAN),
    
    // Arithmetic operators (preserve numeric type)
    PLUS(null),      // type determined at runtime
    MINUS(null),     // type determined at runtime
    MULTIPLY(null),  // type determined at runtime
    DIVIDE(null);    // type determined at runtime

    private final ExpressionType returnType;

    Operator(ExpressionType returnType) {
        this.returnType = returnType;
    }

    public ExpressionType getReturnType() {
        return returnType;
    }
}