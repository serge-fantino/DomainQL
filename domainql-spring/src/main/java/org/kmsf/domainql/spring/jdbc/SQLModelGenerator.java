package org.kmsf.domainql.spring.jdbc;

import org.kmsf.domainql.sql.SQLModel;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * This class is responsible for generating a SQLModel by reverse engineering a database schema using JDBC metadata informations.
 * As an input it uses a DataSource object, and produce an complete SQLModel object as an output.
 * It is possible to select only some schemas to generate the model.
 */
public class SQLModelGenerator {

    private final DataSource dataSource;
    private final List<String> schemas;

    public SQLModelGenerator(DataSource dataSource, List<String> schemas) {
        this.dataSource = dataSource;
        this.schemas = schemas;
    }

    public SQLModel generateModel() throws SQLException {
        SQLModel model = new SQLModel();

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            // First pass: Create tables and columns
            try (ResultSet tables = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (tables.next()) {
                    String schema = tables.getString("TABLE_SCHEM");
                    String tableName = tables.getString("TABLE_NAME");

                    if (schemas != null && !schemas.isEmpty() && schemas.contains(schema)) {
                        SQLModel.Table table = new SQLModel.Table(schema, tableName);
                        
                        // Add columns
                        try (ResultSet columns = metaData.getColumns(null, schema, tableName, "%")) {
                            while (columns.next()) {
                                String columnName = columns.getString("COLUMN_NAME");
                                int sqlType = columns.getInt("DATA_TYPE");
                                table.addColumn(columnName, sqlType);
                            }
                        }
                    
                        model.addTable(table);
                    }
                }
            }
            
            // Second pass: Add foreign key relationships
            try (ResultSet tables = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (tables.next()) {
                    String schema = tables.getString("TABLE_SCHEM");
                    String tableName = tables.getString("TABLE_NAME");
                    String qualifiedName = schema != null ? schema + "." + tableName : tableName;
                    
                    SQLModel.Table sourceTable = model.getTable(qualifiedName);

                    if (model.getTable(qualifiedName)!=null) {
                        try (ResultSet foreignKeys = metaData.getImportedKeys(null, schema, tableName)) {
                            while (foreignKeys.next()) {
                                String pkSchema = foreignKeys.getString("PKTABLE_SCHEM");
                                String pkTable = foreignKeys.getString("PKTABLE_NAME");
                                String pkColumn = foreignKeys.getString("PKCOLUMN_NAME");
                                String fkColumn = foreignKeys.getString("FKCOLUMN_NAME");
                                
                                String targetQualifiedName = pkSchema != null ? pkSchema + "." + pkTable : pkTable;
                                SQLModel.Table targetTable = model.getTable(targetQualifiedName);
                                
                                SQLModel.ForeignKey foreignKey = new SQLModel.ForeignKey(sourceTable, targetTable);
                                
                                // Find the corresponding columns
                                SQLModel.Column sourceColumn = sourceTable.getColumns().stream()
                                    .filter(col -> col.getSqlName().equals(fkColumn))
                                    .findFirst()
                                    .orElseThrow();
                                    
                                SQLModel.Column targetColumn = targetTable.getColumns().stream()
                                    .filter(col -> col.getSqlName().equals(pkColumn))
                                    .findFirst()
                                    .orElseThrow();
                                    
                                foreignKey.addColumnPair(sourceColumn, targetColumn);
                                model.addForeignKey(foreignKey);
                            }
                        }
                    }
                }
            }
        }
        
        return model;
    }
}
