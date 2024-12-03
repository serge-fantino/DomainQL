package org.kmsf.domainql.sql;

import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import org.kmsf.domainql.expression.AttributeExpression;
import org.kmsf.domainql.expression.BinaryExpression;
import org.kmsf.domainql.expression.Expression;
import org.kmsf.domainql.expression.MappingReference;
import org.kmsf.domainql.expression.type.Operator;
import org.kmsf.domainql.expression.type.ScalarType;
import org.kmsf.domainql.model.Attribute;
import org.kmsf.domainql.model.Domain;
import org.kmsf.domainql.model.DomainChangeSet;
import org.kmsf.domainql.model.DomainRegistry;
import org.kmsf.domainql.sql.SQLModel.ForeignKey;

/**
 * This class is used to map domainQL expressions to a SQL model.
 * It takes a valid SQLModel as constructor input and use it to resolve Domains and Attributes references.
 * It also serves as the external mapping between a DomainRegistry and the SQLModel, to associate Domains with SQL Tables.
 * It is also possible to generate a straightforward DomainQL mapping out of the SQLModel, that can then be further customized to match business needs.
 */
public class SQLMapping {

    private final SQLModel sqlModel;
    private final DomainRegistry domainRegistry;
    private final Map<Domain, SQLModel.Table> domainMapping = new HashMap<>();

    public SQLMapping(SQLModel sqlModel, DomainRegistry domainRegistry) {
        this.sqlModel = sqlModel;
        this.domainRegistry = domainRegistry;
    }

    public SQLModel.Table findDomainMappingTable(Domain domain) {
        SQLModel.Table table = domainMapping.get(domain);
        if (table == null) {
            throw new IllegalArgumentException("No mapping found for domain " + domain.getName());
        }
        return table;
    }
    
    public String generateSQL(Expression expression) {
        return "not yet implemented";
    }

    public DomainChangeSet generateSimpleDomainQL() {
        DomainChangeSet domainChanges = new DomainChangeSet();

        Map<SQLModel.Column, Attribute> attributeMapping = new HashMap<>();

        for (SQLModel.Table table : sqlModel.getTables()) {
            String domainName = convertToCamelCase(table.getSqlName());
            Domain domain = new Domain(domainName);
            domainRegistry.register(domain);
            domainMapping.put(domain, table);
            domainChanges.addChange(domain, DomainChangeSet.ChangeType.ADD_DOMAIN, null);
            for (SQLModel.Column column : table.getColumns()) {
                String attributeName = convertToCamelCase(column.getSqlName());
                ScalarType attributeType = mapSqlTypeToAttributeType(column.getSqlType());
                Expression attributeExpression = new MappingReference(domain, column.getSqlName(), attributeType);
                Attribute attribute = new Attribute(attributeName, domain, attributeExpression);
                domain.addAttribute(attribute);
                domainChanges.addChange(domain, DomainChangeSet.ChangeType.ADD_ATTRIBUTE, attributeName);
                attributeMapping.put(column, attribute);
            }
        }
        for (SQLModel.ForeignKey foreignKey : sqlModel.getForeignKeys()) {
            Domain leftDomain = domainRegistry.getDomain(foreignKey.getSourceTable().getQualifiedName());
            Domain rightDomain = domainRegistry.getDomain(foreignKey.getTargetTable().getQualifiedName());
            Expression joinCondition = generateSelfJoinCondition(foreignKey, leftDomain, rightDomain, attributeMapping);
            domainRegistry.registerRelationship(leftDomain, rightDomain, joinCondition);
        }
        return domainChanges;
    }

    /**
     * Generate a self join condition for a foreign key.
     * The condition is a boolean expression that can be used to join the left and right domains.
     * @param foreignKey
     * @return
     */
    private Expression generateSelfJoinCondition(ForeignKey foreignKey, Domain leftDomain, Domain rightDomain, Map<SQLModel.Column, Attribute> attributeMapping) {
        // we need to build a EQUAL BinaryExpression for each ForeignKey column pair
        Expression joinCondition = null;
        for (SQLModel.ColumnPair columnPair : foreignKey.getColumnPairs()) {
            Attribute leftAttribute = attributeMapping.get(columnPair.getSourceColumn());
            Attribute rightAttribute = attributeMapping.get(columnPair.getTargetColumn());
            Expression leftExpression = new AttributeExpression(leftAttribute);
            Expression rightExpression = new AttributeExpression(rightAttribute);
            Expression equality = new BinaryExpression(leftExpression, Operator.EQUALS, rightExpression);
            if (joinCondition == null) {
                joinCondition = equality;
            } else {
                joinCondition = new BinaryExpression(joinCondition, Operator.AND, equality);
            }
        }
        return joinCondition;
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
