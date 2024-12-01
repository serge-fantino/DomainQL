# DomainQL Core

DomainQL Core is a Java library that provides a domain-driven query language abstraction over SQL databases. It allows you to define your domain model and write queries against it, which are then automatically translated to SQL.

## Key Features

- Domain model definition with attributes and relationships
- Type-safe query building
- Automatic SQL generation
- Database schema reverse engineering
- Support for complex joins and aggregations

## Core Concepts

### 1. SQL Model

The SQL model represents the database structure:

```java
SQLModel model = new SQLModel();
SQLModel.Table users = new SQLModel.Table("public", "users");
users.addColumn("id", Types.BIGINT);
users.addColumn("name", Types.VARCHAR);
model.addTable(users);
```

### 2. Domain Model

The domain model consists of:

- **DomainRegistry**: A registry of all domains and their attributes and relationships
- **Domains**: Business entities (e.g., User, Department)
- **Attributes**: Properties of domains (e.g., id, name)

```java
Domain userDomain = new Domain("User");
userDomain.addAttribute("id", ScalarType.LONG);
userDomain.addAttribute("name", ScalarType.STRING);
```

### 3. Type System

DomainQL provides a rich type system:

#### Scalar Types
- `STRING`
- `INTEGER`
- `LONG`
- `DECIMAL`
- `BOOLEAN`
- `DATE`
- `TIMESTAMP`

#### Complex Types
- `DomainType`: References to other domains
- `CrossDomainType`: References to two domains (e.g. for Relation conditions between two domains)

### 4. Expression Model

DomainQL uses an expression tree to represent queries:

#### Basic Expressions
- `AttributeExpression`: Access domain attributes
- `LiteralExpression`: Constant values
- `ReferenceExpression`: Navigate relationships using relation composition

#### Operators
- Comparison: `=`, `<>`, `<`, `>`, `<=`, `>=`
- Logical: `AND`, `OR`, `NOT`
- Arithmetic: `+`, `-`, `*`, `/`

Example:
```java
Expression query = new ComparisonExpression(
    new AttributeExpression("name"),
    ComparisonOperator.EQUALS,
    new LiteralExpression("John")
);
```

## Usage Examples

### 1. Define Your Domain Model

```java
// Create a domain registry
DomainRegistry registry = new DomainRegistry();

// Define domains
Domain userDomain = new Domain("User");
userDomain.addAttribute("id", ScalarType.LONG);
userDomain.addAttribute("name", ScalarType.STRING);
userDomain.addAttribute("department", new DomainType("Department"));

registry.register(userDomain);
```

### 2. Build Queries

```java
// Build a simple query
Expression query = DomainQL.select("User")
    .where(attr("name").eq("John"))
    .and(attr("department.name").eq("IT"))
    .build();
```

### 3. Generate SQL

```java
// Generate SQL
String sql = SQLMapping.generateSQL(query);
```

## Database Integration

DomainQL can automatically generate the domain model from an existing database schema (note that automatic SQL model generation from JDBC is provided in a separate module domain-spring):

```java
SQLModelGenerator generator = new SQLModelGenerator(dataSource);
SQLModel sqlModel = generator.generateModel();
SQLMapping mapping = new SQLMapping(sqlModel);
DomainRegistry registry = mapping.generateSimpleDomainQL();
```

## Type Safety

DomainQL ensures type safety at multiple levels:

1. **Compile-time**: Domain and attribute references
2. **Runtime**: Type checking during query building
3. **Execution**: Value type validation

## Advanced Features

- Complex joins and relationships
- Aggregation functions
- Subqueries
- Custom type mappings
- Expression optimization

## Best Practices

1. Define clear domain boundaries
2. Use meaningful domain and attribute names
3. Leverage the type system for data integrity
4. Keep domains focused and cohesive
5. Document domain relationships

## Contributing

Contributions are welcome! Please read our contributing guidelines and submit pull requests.
