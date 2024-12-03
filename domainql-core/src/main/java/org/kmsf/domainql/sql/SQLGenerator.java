package org.kmsf.domainql.sql;

import org.kmsf.domainql.expression.AggregateExpression;
import org.kmsf.domainql.expression.AttributeExpression;
import org.kmsf.domainql.expression.BinaryExpression;
import org.kmsf.domainql.expression.ComposeExpression;
import org.kmsf.domainql.expression.Expression;
import org.kmsf.domainql.expression.LiteralExpression;
import org.kmsf.domainql.expression.MappingReference;
import org.kmsf.domainql.expression.QueryExpression;
import org.kmsf.domainql.expression.type.*;
import org.kmsf.domainql.model.Attribute;
import org.kmsf.domainql.model.Domain;
import org.kmsf.domainql.model.Query;
import org.kmsf.domainql.model.ReferenceAttribute;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * This class is used to generate a SQL query from a domainQL query and a SQLMapping.
 * Domains and Attributes must be resolved against the SQLMapping's tables and columns.
 */
public class SQLGenerator {
    private final Query query;
    private final SQLMapping sqlMapping;
    private final JoinContext joinContext;
    private int subqueryCounter = 0;

    public SQLGenerator(Query query, SQLMapping sqlMapping) {
        this.query = query;
        this.sqlMapping = sqlMapping;
        this.joinContext = new JoinContext();
    }

    public static String generateSQL(Query query) {
        SQLGenerator generator = new SQLGenerator(query, null);
        return generator.generateSQL();
    }

    public String generateSQL() {

        // initialize the root path
        DomainPath rootPath = new DomainPath(query.getSourceDomain());
        joinContext.getOrCreateAlias(rootPath);

        // Generate projections
        StringBuilder selectPart = new StringBuilder();
        generateProjections(selectPart, rootPath);

        // Generate WHERE clause if filter exists
        StringBuilder wherePart = new StringBuilder();
        if (query.getFilter() != null) {
            wherePart.append(" WHERE ");
            generateExpression(query.getFilter(), new SimplePathResolver(rootPath), wherePart);
        }

        // Generate GROUP BY if needed
        StringBuilder groupByPart = new StringBuilder();
        if (needsGroupBy(query)) {
            groupByPart.append(" GROUP BY ");
            generateGroupByClause(query, rootPath, groupByPart);
        }
        
        // Generate FROM clause with necessary JOINs
        StringBuilder sql = new StringBuilder("SELECT ");
        sql.append(selectPart);
        generateFromClause(query, sql);
        sql.append(wherePart);
        sql.append(groupByPart);

        return sql.toString();
    }

    private void generateProjections(StringBuilder sql, DomainPath rootPath) {
        boolean first = true;
        for (Map.Entry<String, Expression> projection : query.getProjections().entrySet()) {
            if (!first) sql.append(", ");
            generateExpression(projection.getValue(), new SimplePathResolver(rootPath), sql);
            sql.append(" AS ").append(projection.getKey());
            first = false;
        }
    }

    private void generateExpression(Expression expr, PathResolver pathResolver, StringBuilder sql) {
        if (expr instanceof ComposeExpression) {
            ComposeExpression compose = (ComposeExpression) expr;
            Expression reference = compose.getReference();
            
            if (reference instanceof AttributeExpression) {
                AttributeExpression attrExpr = (AttributeExpression) reference;
                if (attrExpr.getAttribute() instanceof ReferenceAttribute) {
                    ReferenceAttribute refAttr = (ReferenceAttribute) attrExpr.getAttribute();
                    DomainPath currentPath = pathResolver.resolve(attrExpr);
                    DomainPath newPath = new DomainPath(refAttr.getReferenceDomain(), currentPath, refAttr);
                    joinContext.getOrCreateAlias(newPath);
                    generateExpression(compose.getComposition(), new SimplePathResolver(newPath), sql);
                    return;
                }
            }
        } else if (expr instanceof AttributeExpression) {
            AttributeExpression attrExpr = (AttributeExpression) expr;
            Attribute attribute = attrExpr.getAttribute();
            Expression definition = attribute.getExpression();
            if (definition instanceof MappingReference) {
                MappingReference mappingReference = (MappingReference) definition;
                String columReference = mappingReference.getName();
                SQLModel.Column column = sqlMapping.findDomainMappingTable(attribute.getDomain()).findColumn(columReference);
                DomainPath path = pathResolver.resolve(attrExpr);
                String alias = joinContext.getOrCreateAlias(path);
                sql.append(alias)
                   .append(".")
                   .append(column.getSqlName());
                return;
            } esle {

            }
        } else if (expr instanceof MappingReference) {
            MappingReference mappingReference = (MappingReference) expr;
            String columReference = mappingReference.getName();
            SQLModel.Column column = sqlMapping.findDomainMappingTable(attribute.getDomain()).findColumn(columReference);
            DomainPath path = pathResolver.resolve(attrExpr);
            String alias = joinContext.getOrCreateAlias(path);
            sql.append(alias)
               .append(".")
               .append(column.getSqlName());
            return;
        } else if (expr instanceof BinaryExpression) {
            generateBinaryExpression((BinaryExpression) expr, pathResolver, sql);
        } else if (expr instanceof AggregateExpression) {
            generateAggregateExpression((AggregateExpression) expr, pathResolver, sql);
        } else if (expr instanceof QueryExpression) {
            generateQueryExpression((QueryExpression) expr, sql);
        } else if (expr instanceof LiteralExpression) {
            generateLiteralExpression((LiteralExpression) expr, sql);
        }
    }

    private void generateLiteralExpression(LiteralExpression expr, StringBuilder sql) {
        ExpressionType type = expr.getType();
        if (type instanceof ScalarType) {
            ScalarType scalarType = (ScalarType) type;
            if (scalarType.equals(ScalarType.STRING)) {
                sql.append("'").append(expr.getValue()).append("'");
            } else if (scalarType.equals(ScalarType.DATE)) {
                sql.append("to_date('").append(expr.getValue().toString()).append("', 'YYYY-MM-DD')");
            } else {
                sql.append(expr.getValue());
            }
        } else {
            throw new IllegalArgumentException("Unsupported literal type: " + type);
        }
    }
    
    private void generateQueryExpression(QueryExpression expr, StringBuilder sql) {
        String subqueryAlias = "sq" + (++subqueryCounter);
        SQLGenerator subqueryGenerator = new SQLGenerator(expr.getQuery());
        sql.append("(")
           .append(subqueryGenerator.generateSQL())
           .append(") AS ")
           .append(subqueryAlias);
    }
    
    private void generateBinaryExpression(BinaryExpression expr, PathResolver pathResolver, StringBuilder sql) {
        sql.append("(");
        generateExpression(expr.getLeft(), pathResolver, sql);
        sql.append(" ")
           .append(getBinaryOperator(expr.getOperator()))
           .append(" ");
        generateExpression(expr.getRight(), pathResolver, sql);
        sql.append(")");
    }

    private String getBinaryOperator(Operator operator) {
        switch (operator) {
            case EQUALS: return "=";
            case NOT_EQUALS: return "<>";
            case GREATER_THAN: return ">";
            case GREATER_THAN_OR_EQUALS: return ">=";
            case LESS_THAN: return "<";
            case LESS_THAN_OR_EQUALS: return "<=";
            case AND: return "AND";
            case OR: return "OR";
            case PLUS: return "+";
            case MINUS: return "-";
            case MULTIPLY: return "*";
            case DIVIDE: return "/";
            case LIKE: return "LIKE";
            case IN: return "IN";
            default:
                throw new IllegalArgumentException("Unsupported operator: " + operator);
        }
    }

    private void generateFromClause(Query query, StringBuilder sql) {
        joinContext.forEachJoin((path, alias) -> {
            if (path.parent == null) {
                sql.append(" FROM ");
                generateTableNameAndAlias(path, alias, sql);
            } else {
                sql.append(" JOIN ");
                generateTableNameAndAlias(path, alias, sql);
                sql.append(" ON ");
                generateExpression(path.reference.getJoinCondition(), 
                                   new JoinPathResolver(path.parent, path), 
                                   sql);
            }
        });
    }

    private void generateTableNameAndAlias(DomainPath path, String alias, StringBuilder sql) {
        sql.append(path.domain.getName());
        if (alias != null && !alias.isEmpty() && !alias.equals(path.domain.getName())) {
            sql.append(" AS ").append(alias);
        }
    }

    private boolean needsGroupBy(Query query) {
        boolean hasAggregate = false;
        boolean hasNonAggregate = false;
        
        for (Expression expr : query.getProjections().values()) {
            ExpressionType type = expr.getType();
            if (type instanceof AggregateType) {
                hasAggregate = true;
            } else {
                hasNonAggregate = true;
            }
        }
        
        return hasAggregate && hasNonAggregate;
    }

    private void generateGroupByClause(Query query, DomainPath rootPath, StringBuilder sql) {
        boolean first = true;
        for (Expression expr : query.getProjections().values()) {
            if (!expr.getType().isAggregate()) {
                if (!first) sql.append(", ");
                generateExpression(expr, new SimplePathResolver(rootPath), sql);
                first = false;
            }
        }
    }

    private void generateAggregateExpression(AggregateExpression expr, PathResolver pathResolver, StringBuilder sql) {
        sql.append(expr.getFunction().name())
           .append("(");
        generateExpression(expr.getOperand(), pathResolver, sql);
        sql.append(")");
    }

    public static class DomainPath {
        final Domain domain;
        final DomainPath parent;
        final ReferenceAttribute reference;

        DomainPath(Domain domain) {
            this(domain, null, null);
        }

        DomainPath(Domain domain, DomainPath parent, ReferenceAttribute reference) {
            this.domain = domain;
            this.parent = parent;
            this.reference = reference;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DomainPath)) return false;
            DomainPath that = (DomainPath) o;
            return Objects.equals(domain, that.domain) &&
                   Objects.equals(parent, that.parent) &&
                   Objects.equals(reference, that.reference);
        }

        @Override
        public int hashCode() {
            return Objects.hash(domain, parent, reference);
        }
    }

    public static class JoinContext {
        private final Map<DomainPath, String> domainAliases = new LinkedHashMap<>();
        private int aliasCounter = 0;

        public String getOrCreateAlias(DomainPath path) {
            String alias = domainAliases.computeIfAbsent(path, p -> {
                return generateUniqueAlias(generateMeaningfullAlias(p));
            });
            addJoin(path, alias);
            return alias;
        }

        private String generateUniqueAlias(String someAlias) {
            if (domainAliases.containsValue(someAlias)) {
                return someAlias + "_" + (++aliasCounter);
            }
            return someAlias;
        }

        private String generateMeaningfullAlias(DomainPath path) {
            if (path.parent == null) {
                return path.domain.getName();
            } else {
                return path.reference.getName();
            }
        }

        private void addJoin(DomainPath path, String alias) {
            domainAliases.putIfAbsent(path, alias);
        }

        public void forEachJoin(BiConsumer<DomainPath, String> consumer) {
            domainAliases.entrySet().stream()
                .forEach(e -> consumer.accept(e.getKey(), e.getValue()));
        }

        public Integer getAliasCount() {
            return domainAliases.size();
        }
    }

    public interface PathResolver {
        DomainPath resolve(AttributeExpression expr);
    }
    
    public static class SimplePathResolver implements PathResolver {
        private final DomainPath currentPath;
    
        public SimplePathResolver(DomainPath currentPath) {
            this.currentPath = currentPath;
        }
    
        @Override
        public DomainPath resolve(AttributeExpression expr) {
            return currentPath; // We know the expression's source type is compatible
        }
    }
    
    public static class JoinPathResolver implements PathResolver {
        private final DomainPath leftPath;
        private final DomainPath rightPath;
    
        public JoinPathResolver(DomainPath leftPath, DomainPath rightPath) {
            this.leftPath = leftPath;
            this.rightPath = rightPath;
        }
    
        @Override
        public DomainPath resolve(AttributeExpression expr) {
            switch (expr.getContextResolution()) {
                case LEFT: return leftPath;
                case RIGHT: return rightPath;
                case DEFAULT: 
                    Domain attrDomain = expr.getAttribute().getDomain();
                    if (attrDomain.equals(leftPath.domain)) return leftPath;
                    if (attrDomain.equals(rightPath.domain)) return rightPath;
                    throw new IllegalStateException(
                        "Attribute domain " + attrDomain + " doesn't match either side of the join"
                    );
            }
            throw new IllegalStateException("Unknown context resolution: " + expr.getContextResolution());
        }
    } 
} 