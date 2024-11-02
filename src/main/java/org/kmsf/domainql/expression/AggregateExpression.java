package org.kmsf.domainql.expression;

import org.kmsf.domainql.expression.type.ExpressionType;
import org.kmsf.domainql.expression.type.ScalarType;
import org.kmsf.domainql.expression.type.SourceType;

public class AggregateExpression implements Expression {
    public enum AggregateFunction {
        SUM(ScalarType.DECIMAL),
        AVG(ScalarType.DECIMAL),
        COUNT(ScalarType.INTEGER),
        MIN(null),  // Takes type from inner expression
        MAX(null);  // Takes type from inner expression

        private final ScalarType resultType;

        AggregateFunction(ScalarType resultType) {
            this.resultType = resultType;
        }
    }

    private Expression operand;
    private AggregateFunction function;

    public AggregateExpression(Expression operand, AggregateFunction function) {
        if (!operand.getType().isScalar()) {
            throw new IllegalArgumentException("Aggregate functions can only be applied to scalar expressions");
        }
        this.operand = operand;
        this.function = function;
    }

    public Expression getOperand() {
        return operand;
    }   

    public AggregateFunction getFunction() {
        return function;
    }

    @Override
    public SourceType getSource() {
        return operand.getSource();
    }

    @Override
    public ExpressionType getType() {
        if (function.resultType != null) {
            return function.resultType;
        } else {
            // For MIN/MAX, use the type of the inner expression
            return (ScalarType) operand.getType();
        }
    }
} 