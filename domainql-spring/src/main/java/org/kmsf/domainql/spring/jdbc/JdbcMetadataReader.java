package org.kmsf.domainql.spring.jdbc;

import org.kmsf.domainql.expression.type.ScalarType;
import org.kmsf.domainql.model.Attribute;
import org.kmsf.domainql.model.Domain;
import org.kmsf.domainql.model.DomainRegistry;
import org.kmsf.domainql.model.ReferenceAttribute;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

@Component
public class JdbcMetadataReader {
    private final DataSource dataSource;

    public JdbcMetadataReader(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public DomainRegistry readMetadata() throws SQLException {
        DomainRegistry domainRegistry = new DomainRegistry();

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            // Lire les tables
            try (ResultSet tables = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    domainRegistry.register(readTableMetadata(metaData, tableName));
                }
            }
            
            return domainRegistry;
        }
    }

    private Domain readTableMetadata(DatabaseMetaData metaData, String tableName) throws SQLException {

        String domainName = convertToCamelCase(tableName);
        Domain domain = new Domain(domainName);
        
        // Lire les colonnes
        try (ResultSet columns = metaData.getColumns(null, null, tableName, "%")) {
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                int sqlType = columns.getInt("DATA_TYPE");
                domain.addAttribute(convertToCamelCase(columnName), mapSqlTypeToAttributeType(sqlType));
            }
        }

        // Lire les clés étrangères pour les relations
        try (ResultSet foreignKeys = metaData.getImportedKeys(null, null, tableName)) {
            while (foreignKeys.next()) {
                String fkColumnName = foreignKeys.getString("FKCOLUMN_NAME");
                String pkTableName = foreignKeys.getString("PKTABLE_NAME");
                //domain.addAttribute(fkColumnName, new ReferenceAttribute(fkColumnName, "Domain{" + pkTableName + "}"));
            }
        }
        
        return domain;
    }
        
    private String convertToCamelCase(String tableName) {
        return tableName.replaceAll("([A-Z])", " $1").trim().toLowerCase().replaceAll("\\s+", "");
    }
        
    private ScalarType mapSqlTypeToAttributeType(int sqlType) {
        return switch (sqlType) {
            case Types.VARCHAR, Types.CHAR, Types.LONGVARCHAR -> ScalarType.STRING;
            case Types.INTEGER, Types.SMALLINT -> ScalarType.INTEGER;
            case Types.BIGINT -> ScalarType.LONG;
            case Types.DECIMAL, Types.NUMERIC -> ScalarType.DECIMAL;
            case Types.BOOLEAN -> ScalarType.BOOLEAN;
            case Types.DATE -> ScalarType.DATE;
            case Types.TIMESTAMP -> ScalarType.TIMESTAMP;
            default -> ScalarType.STRING;
        };
    }
} 