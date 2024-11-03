package org.kmsf.domainql.sql;

import org.kmsf.domainql.expression.Attribute;
import org.kmsf.domainql.expression.AttributeExpression;
import org.kmsf.domainql.expression.BinaryExpression;
import org.kmsf.domainql.expression.Domain;
import org.kmsf.domainql.expression.LiteralExpression;
import org.kmsf.domainql.expression.ComposeExpression;
import org.kmsf.domainql.expression.Query;
import org.kmsf.domainql.expression.QueryBuilder;
import org.kmsf.domainql.expression.ReferenceAttribute;
import org.kmsf.domainql.expression.type.Operator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kmsf.domainql.expression.type.ScalarType;
import static org.junit.jupiter.api.Assertions.*;

public class SQLGeneratorTest {

    private Domain personDomain;
    private Domain companyDomain;
    private Domain departmentDomain;

    @BeforeEach
    void setUp() throws Exception {
        // Set up domains
        personDomain = new Domain("person");
        companyDomain = new Domain("company"); 
        departmentDomain = new Domain("department");

        // Set up scalar attributes
        personDomain.addAttribute("id", ScalarType.INTEGER);
        personDomain.addAttribute("first_name", ScalarType.STRING);
        personDomain.addAttribute("salary", ScalarType.DECIMAL);
        personDomain.addAttribute("company_id", ScalarType.DECIMAL);
        personDomain.addAttribute("department_id", ScalarType.INTEGER);

        companyDomain.addAttribute("id", ScalarType.INTEGER);
        companyDomain.addAttribute("name", ScalarType.STRING);

        departmentDomain.addAttribute("id", ScalarType.INTEGER);
        departmentDomain.addAttribute("name", ScalarType.STRING);
        departmentDomain.addAttribute("company_id", ScalarType.INTEGER);

        // Set up references with join conditions
        personDomain.addReference("works_for", "company_id", companyDomain, "id");
        departmentDomain.addReference("company", "company_id", companyDomain, "id");
        personDomain.addReference("department", "department_id", departmentDomain, "id");
    }

    @Test
    void testJoinContext() {
        SQLGenerator.JoinContext joinContext = new SQLGenerator.JoinContext();
        joinContext.getOrCreateAlias(new SQLGenerator.DomainPath(companyDomain, null, null));
        assertEquals(1, joinContext.getAliasCount());
        joinContext.forEachJoin((path, alias) -> {
            assertEquals(companyDomain, path.domain);
            assertEquals(companyDomain.getName(), alias);
        });
    }

    @Test 
    void testSimpleQuery() {
        Query query = new Query("employee_companies", personDomain);
        query.addProjection("employee_first_name", new AttributeExpression(personDomain.getAttribute("first_name")));
        query.addProjection("employee_salary", new AttributeExpression(personDomain.getAttribute("salary")));

        String sql = SQLGenerator.generateSQL(query);
        assertEquals("SELECT person.first_name AS employee_first_name, person.salary AS employee_salary FROM person", sql.trim());
    }

    @Test
    void testSimpleComposeExpression() {
        // Setup attributes
        Attribute personFirstName = personDomain.getAttribute("first_name");
        Attribute companyName = companyDomain.getAttribute("name");
        
        // Create query with compose expression
        Query query = new Query("employee_companies", personDomain);
        
        // Add person's first name directly
        query.addProjection("employee", new AttributeExpression(personFirstName));
        
        // Add company name through composition
        ComposeExpression employee_company_name = new ComposeExpression(
            new AttributeExpression(personDomain.getAttribute("works_for")),
            new AttributeExpression(companyName)
        );
        query.addProjection("employee_company_name", employee_company_name);
        
        String sql = SQLGenerator.generateSQL(query);
        assertEquals(
            "SELECT person.first_name AS employee, works_for.name AS employee_company_name " +
            "FROM person " +
            "JOIN company AS works_for ON (person.company_id = works_for.id)",
            sql.trim()
        );
    }

    @Test
    void testSelfJoinWithAutomaticContextResolution() {
        // Setup employee domain with self-reference
        Domain employeeDomain = new Domain("employee");
        employeeDomain.addAttribute("id", ScalarType.INTEGER);
        employeeDomain.addAttribute("name", ScalarType.STRING);
        employeeDomain.addAttribute("manager_id", ScalarType.INTEGER);

        // Add self-reference - the method will automatically handle the context resolution
        employeeDomain.addReference("manager", "manager_id", employeeDomain, "id");

        // Create query using QueryBuilder
        Query query = QueryBuilder.from("employee_managers", employeeDomain)
            .select("employee_name", "name")
            .select("manager_name", "manager.name")
            .build();

        String sql = SQLGenerator.generateSQL(query);
        assertEquals(
            "SELECT employee.name AS employee_name, manager.name AS manager_name " +
            "FROM employee " +
            "JOIN employee AS manager ON (employee.manager_id = manager.id)",
            sql.trim()
        );
    }

    @Test
    void testNestedComposeExpression() {
        // Setup address domain
        Domain addressDomain = new Domain("address");
        addressDomain.addAttribute("id", ScalarType.INTEGER);
        addressDomain.addAttribute("city", ScalarType.STRING);
        
        // Add address reference to company domain
        companyDomain.addAttribute("address_id", ScalarType.INTEGER);
        companyDomain.addReference("address", "address_id", addressDomain, "id");

        // Create query using QueryBuilder
        Query query = QueryBuilder.from("employee_locations", personDomain)
            .select("employee", "first_name")
            .select("company", "works_for.name")
            .select("city", "works_for.address.city")
            .build();

        String sql = SQLGenerator.generateSQL(query);
        assertEquals(
            "SELECT person.first_name AS employee, works_for.name AS company, address.city AS city " +
            "FROM person " +
            "JOIN company AS works_for ON (person.company_id = works_for.id)" +
            "JOIN address AS address ON (works_for.address_id = address.id)",
            sql.trim()
        );
    }

    @Test
    void testComposeExpressionWithFilter() {
        // Create query with compose expression and filter
        Query query = new Query("high_salary_companies", personDomain);
        
        // Add projections
        ComposeExpression companyNameExpr = new ComposeExpression(
            new AttributeExpression(personDomain.getAttribute("works_for")),
            new AttributeExpression(companyDomain.getAttribute("name"))
        );
        query.addProjection("company", companyNameExpr);
        
        // Add filter on salary
        query.setFilter(new BinaryExpression(
            new AttributeExpression(personDomain.getAttribute("salary")),
            Operator.GREATER_THAN,
            new LiteralExpression(100000.0)
        ));
        
        String sql = SQLGenerator.generateSQL(query);
        assertEquals(
            "SELECT j1.name AS company " +
            "FROM person " +
            "JOIN company ON (person.company_id = company.id) AS j1 " +
            "WHERE (person.salary > 100000.0)",
            sql.trim()
        );
    }

    @Test
    void testComposeExpressionWithComposeFilter() {
        // Create query with compose expression and filter
        Query query = new Query("kmsf_employees", personDomain);
        query.addProjection("employee", new AttributeExpression(personDomain.getAttribute("first_name")));
        ComposeExpression companyDepartmentExpr = new ComposeExpression(
            new AttributeExpression(personDomain.getAttribute("department")),
                new AttributeExpression(departmentDomain.getAttribute("name"))
        );
        ComposeExpression companyNameExpr = new ComposeExpression(
            new AttributeExpression(personDomain.getAttribute("department")),
            new ComposeExpression(
                new AttributeExpression(departmentDomain.getAttribute("company")),
                new AttributeExpression(companyDomain.getAttribute("name"))
            )
        );
        query.addProjection("department", companyDepartmentExpr);
        query.addProjection("company", companyNameExpr);
        query.setFilter(new BinaryExpression(
            companyNameExpr,
            Operator.EQUALS,
            new LiteralExpression("KMSF")
        ));
        
        String sql = SQLGenerator.generateSQL(query);
        assertEquals(
            "SELECT person.first_name AS employee, j1.name AS department, j2.name AS company " +
            "FROM person " +
            "JOIN department ON (person.department_id = department.id) AS j1 " +
            "JOIN company ON (person.company_id = company.id) AS j2 " +
            "WHERE (j2.name = 'KMSF')",
            sql.trim()
        );
    }

} 