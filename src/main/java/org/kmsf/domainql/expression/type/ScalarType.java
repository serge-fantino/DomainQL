package org.kmsf.domainql.expression.type;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ScalarType implements ExpressionType {
    public enum BaseType {
        STRING,
        INTEGER,
        DECIMAL,
        BOOLEAN,
        DATE,
        TIMESTAMP
    }

    // Static constants for each type
    public static final ScalarType STRING = new ScalarType(BaseType.STRING);
    public static final ScalarType INTEGER = new ScalarType(BaseType.INTEGER);
    public static final ScalarType DECIMAL = new ScalarType(BaseType.DECIMAL);
    public static final ScalarType BOOLEAN = new ScalarType(BaseType.BOOLEAN);
    public static final ScalarType DATE = new ScalarType(BaseType.DATE);
    public static final ScalarType TIMESTAMP = new ScalarType(BaseType.TIMESTAMP);

    private final BaseType baseType;

    private ScalarType(BaseType baseType) {  // Made constructor private
        this.baseType = baseType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScalarType)) return false;
        ScalarType that = (ScalarType) o;
        return baseType == that.baseType;
    }

    @Override
    public int hashCode() {
        return baseType.hashCode();
    }

    @Override
    public boolean isScalar() { return true; }

    @Override
    public boolean isAggregate() { return false; }

    @Override
    public boolean isDomain() { return false; }

    public static ExpressionType fromClass(Class<?> class1) {
        if (String.class.equals(class1)) return STRING;
        if (Integer.class.equals(class1) || int.class.equals(class1)) return INTEGER;
        if (Double.class.equals(class1) || double.class.equals(class1) || 
            BigDecimal.class.equals(class1)) return DECIMAL;
        if (Boolean.class.equals(class1) || boolean.class.equals(class1)) return BOOLEAN;
        if (LocalDate.class.equals(class1)) return DATE;
        if (LocalDateTime.class.equals(class1) || Instant.class.equals(class1)) return TIMESTAMP;
        
        throw new IllegalArgumentException("Unsupported class type: " + class1.getName());
    }

    @Override
    public String toString() {
        return baseType.toString();
    }
} 