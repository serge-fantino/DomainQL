package org.kmsf.domainql.expression.sql;

import java.util.ArrayList;
import java.util.List;

import org.kmsf.domainql.expression.Expression;
import org.kmsf.domainql.expression.type.ExpressionType;
import org.kmsf.domainql.expression.type.SourceType;

public class TableReference implements Expression, ExpressionType, SourceType {

    private final String schemaName;
    private final String tableName;

    private final List<ColumnReference> columnReferences = new ArrayList<>();

    public TableReference(String schemaName, String tableName) {
        this.schemaName = schemaName;
        this.tableName = tableName;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public String getTableName() {
        return tableName;
    }

    public void addColumnReference(ColumnReference columnReference) {
        columnReferences.add(columnReference);
    }

    @Override
    public SourceType getSource() {
        return null;
    }

    @Override
    public ExpressionType getType() {
        return this;
    }

    @Override
    public boolean isScalar() {
        return false;
    }

    @Override
    public boolean isDomain() {
        return false;
    }

    @Override
    public boolean isAggregate() {
        return false;
    }

}