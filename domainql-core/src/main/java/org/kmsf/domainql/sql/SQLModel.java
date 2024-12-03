package org.kmsf.domainql.sql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * SQLModel represents the SQL database schema structure, containing tables and their relationships.
 * It maintains a mapping of qualified table names to Table objects, where each Table contains
 * information about its columns and foreign key relationships. This model serves as a bridge
 * between the domain model and the underlying SQL database structure.
 */
public class SQLModel {
    private final Map<String, Table> tables;
    private final List<ForeignKey> foreignKeys;

    public SQLModel() {
        this.tables = new HashMap<>();
        this.foreignKeys = new ArrayList<>();
    }

    public void addTable(Table table) {
        tables.put(table.getQualifiedName(), table);
    }

    public Table getTable(String qualifiedName) {
        return tables.get(qualifiedName);
    }

    // return an immutable list of tables
    public List<Table> getTables() {
        return Collections.unmodifiableList(new ArrayList<>(tables.values()));
    }

    public void addForeignKey(ForeignKey foreignKey) {
        foreignKeys.add(foreignKey);
    }

    public List<ForeignKey> getForeignKeys() {
        return Collections.unmodifiableList(foreignKeys);
    }

    public List<ForeignKey> getForeignKeysForTable(Table table) {
        return foreignKeys.stream()
            .filter(fk -> fk.getSourceTable().equals(table))
            .toList();
    }
    
    @Override
    public String toString() {
        return "SQLModel{" +
            "tables=" + tables +
            ", foreignKeys=" + foreignKeys +
            '}';
    }

    public static class Table {
        private final String schema;
        private final String sqlName;
        private final List<Column> columns;

        public Table(String schema, String sqlName) {
            this.schema = schema;
            this.sqlName = sqlName;
            this.columns = new ArrayList<>();
        }

        public String getSchema() {
            return schema;
        }

        public String getSqlName() {
            return sqlName;
        }   

        public String getQualifiedName() {
            return schema != null ? schema + "." + sqlName : sqlName;
        }

        public void addColumn(String name, int sqlType) {
            columns.add(new Column(this, name, sqlType));
        }

        public List<Column> getColumns() {
            return columns;
        }

        public Column findColumn(String sqlName) {
            return columns.stream()
                .filter(column -> column.getSqlName().equals(sqlName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Column " + sqlName + " not found in table " + getQualifiedName()));
        }

        public String toString() {
            return "Table{" +
                "schema='" + schema + '\'' +
                ", sqlName='" + sqlName + '\'' +
                ", columns=" + columns +
                '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Table table = (Table) o;
            return Objects.equals(schema, table.schema) && Objects.equals(sqlName, table.sqlName);
        }
    }

    public static class Column {
        private final Table table;
        private final String sqlName;
        private final int sqlType;

        protected Column(Table table, String sqlName, int sqlType) {
            this.table = table;
            this.sqlName = sqlName;
            this.sqlType = sqlType;
        }

        public String getSqlName() {
            return sqlName;
        }

        public String getQualifiedName() {
            return table.getQualifiedName() + "." + sqlName;
        }

        public int getSqlType() {
            return sqlType;
        }

        public String toString() {
            return "Column{" +
                "sqlName='" + sqlName + '\'' +
                ", sqlType=" + sqlType +
                '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Column column = (Column) o;
            return Objects.equals(table, column.table) && Objects.equals(sqlName, column.sqlName);
        }
    }

    public static class ForeignKey {
        private final Table sourceTable;
        private final Table targetTable;
        private final List<ColumnPair> columnPairs;

        public ForeignKey(Table sourceTable, Table targetTable) {
            this.sourceTable = sourceTable;
            this.targetTable = targetTable;
            this.columnPairs = new ArrayList<>();
        }

        public void addColumnPair(Column sourceColumn, Column targetColumn) {
            columnPairs.add(new ColumnPair(sourceColumn, targetColumn));
        }

        public Table getSourceTable() {
            return sourceTable;
        }

        public Table getTargetTable() {
            return targetTable;
        }

        public List<ColumnPair> getColumnPairs() {
            return columnPairs;
        }

        public String toString() {
            return "ForeignKey{" +
                ", targetTable=" + targetTable.getQualifiedName() +
                ", columnPairs=" + columnPairs +
                '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ForeignKey that = (ForeignKey) o;
            return Objects.equals(sourceTable, that.sourceTable) && Objects.equals(targetTable, that.targetTable);
        }
    }

    public static class ColumnPair {
        private final Column sourceColumn;
        private final Column targetColumn;

        public ColumnPair(Column sourceColumn, Column targetColumn) {
            this.sourceColumn = sourceColumn;
            this.targetColumn = targetColumn;
        }

        public Column getSourceColumn() {
            return sourceColumn;
        }

        public Column getTargetColumn() {
            return targetColumn;
        }

        public String toString() {
            return "ColumnPair{" +
                "sourceColumn=" + sourceColumn.getSqlName() +
                ", targetColumn=" + targetColumn.getSqlName() +
                '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ColumnPair that = (ColumnPair) o;
            return Objects.equals(sourceColumn, that.sourceColumn) && Objects.equals(targetColumn, that.targetColumn);
        }
    }
}
