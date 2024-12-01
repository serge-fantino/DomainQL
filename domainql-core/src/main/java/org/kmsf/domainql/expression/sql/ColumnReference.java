package org.kmsf.domainql.expression.sql;

import org.kmsf.domainql.expression.Expression;
import org.kmsf.domainql.expression.type.ExpressionType;
import org.kmsf.domainql.expression.type.SourceType;

public class ColumnReference implements Expression {

    private final TableReference tableReference;
    private final String columnName;
    private final ExpressionType type;

    public ColumnReference(TableReference tableReference, String columnName, ExpressionType type) {
        this.tableReference = tableReference;
        this.columnName = columnName;
        this.type = type;
        tableReference.addColumnReference(this);
    }

    public String getColumnName() {
        return columnName;
    }

    @Override
    public SourceType getSource() {
        return tableReference;
    }

    @Override
    public ExpressionType getType() {
        return type;
    }

}
