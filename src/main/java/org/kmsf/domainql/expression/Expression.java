package org.kmsf.domainql.expression;

import org.kmsf.domainql.expression.type.ExpressionType;
import org.kmsf.domainql.expression.type.SourceType;

public interface Expression {
    SourceType getSource();
    ExpressionType getType();
} 