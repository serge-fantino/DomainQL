# DomainQL / Spring module

## Objective

The Spring module provides integration with Spring environment.
Specifically it supports:
- connection to a JDBC data source (using Spring Data JDBC) in order to:
    - generate DomainQL metadata directly from database schema
    - allow to run DomainQL queries against the database
- reverse engineering of existing Spring Entities in order to generate DomainQL metadata
- provide Controllers for DomainQL metadata exploration and Queries execution

## Usage

### As a dependency

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>org.kmsf</groupId>
    <artifactId>domainql-spring</artifactId>
</dependency>

```

Enable DomainQL in your Spring Boot application:

```java
@SpringBootApplication
@Import(DomainQLSpringApplication.class)
public class YourApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourApplication.class, args);
    }
}
```

### Configuration

In your `application.properties` or `application.yml`:
```properties
# Database configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/yourdb
spring.datasource.username=your_username
spring.datasource.password=your_password

# DomainQL configuration
domainql.scan-package=com.yourcompany.domain
domainql.enable-controllers=true
```

### Available Features

1. **Automatic Schema Discovery**
   - DomainQL will automatically scan your database schema
   - Creates corresponding domain models

2. **Entity Scanning**
   - Scans your JPA entities
   - Generates DomainQL metadata

3. **REST Controllers**
   - `/domainql/metadata` - Browse available domains and their structure
   - `/domainql/query` - Execute DomainQL queries

4. **Spring Data Integration**
   - Automatic repository detection
   - Query translation
```

This structure:
1. Provides a main Spring Boot application class
2. Enables usage as a dependency in other projects
3. Facilitates configuration through properties
4. Clarifies usage in documentation
