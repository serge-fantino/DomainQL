package org.kmsf.domainql.sql;

import org.kmsf.domainql.expression.Attribute;
import org.kmsf.domainql.expression.AttributeExpression;
import org.kmsf.domainql.expression.BinaryExpression;
import org.kmsf.domainql.expression.Domain;
import org.kmsf.domainql.expression.LiteralExpression;
import org.kmsf.domainql.expression.ComposeExpression;
import org.kmsf.domainql.expression.Query;
import org.kmsf.domainql.expression.ReferenceAttribute;
import org.kmsf.domainql.expression.type.Operator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kmsf.domainql.expression.type.ScalarType;
import static org.junit.jupiter.api.Assertions.*;

public class SQLGeneratorTest {
    private SQLGenerator sqlGenerator;
    private Domain personDomain;
    private Domain companyDomain;
    private Domain departmentDomain;

    @BeforeEach
    void setUp() throws Exception {
        sqlGenerator = new SQLGenerator();
        
        // Set up domains
        personDomain = new Domain("person");
        companyDomain = new Domain("company");
        departmentDomain = new Domain("department");

        // Set up scalar attributes
        personDomain.addAttribute("id", new Attribute("id", personDomain, ScalarType.INTEGER));
        personDomain.addAttribute("first_name", new Attribute("first_name", personDomain, 
            ScalarType.STRING));
        personDomain.addAttribute("salary", new Attribute("salary", personDomain, 
            ScalarType.DECIMAL));
        personDomain.addAttribute("company_id", new Attribute("company_id", personDomain, 
                ScalarType.DECIMAL));
        personDomain.addAttribute("department_id", new Attribute("department_id", personDomain,
                ScalarType.INTEGER));

        companyDomain.addAttribute("id", new Attribute("id", companyDomain, 
            ScalarType.INTEGER));
        companyDomain.addAttribute("name", new Attribute("name", companyDomain, 
            ScalarType.STRING));

        departmentDomain.addAttribute("id", new Attribute("id", departmentDomain,
            ScalarType.INTEGER));
        departmentDomain.addAttribute("name", new Attribute("name", departmentDomain,
            ScalarType.STRING));
        departmentDomain.addAttribute("company_id", new Attribute("company_id", departmentDomain,
            ScalarType.INTEGER));

        // Set up reference attributes with custom join conditions
        BinaryExpression worksForJoin = new BinaryExpression(
            new AttributeExpression(personDomain.getAttribute("company_id")),
            Operator.EQUALS,
            new AttributeExpression(companyDomain.getAttribute("id"))
        );
        ReferenceAttribute worksFor = new ReferenceAttribute(
            "works_for", 
            personDomain, 
            companyDomain, 
            worksForJoin
        );
        personDomain.addAttribute("works_for", worksFor);

        // Add department to company reference
        BinaryExpression departmentCompanyJoin = new BinaryExpression(
            new AttributeExpression(departmentDomain.getAttribute("company_id")),
            Operator.EQUALS, 
            new AttributeExpression(companyDomain.getAttribute("id"))
        );
        ReferenceAttribute belongsToCompany = new ReferenceAttribute(
            "company",
            departmentDomain,
            companyDomain,
            departmentCompanyJoin
        );
        departmentDomain.addAttribute("company", belongsToCompany);

        // Add person to department reference
        BinaryExpression personDepartmentJoin = new BinaryExpression(
            new AttributeExpression(personDomain.getAttribute("department_id")),
            Operator.EQUALS,
            new AttributeExpression(departmentDomain.getAttribute("id"))
        );
        ReferenceAttribute worksInDepartment = new ReferenceAttribute(
            "department",
            personDomain,
            departmentDomain,
            personDepartmentJoin
        );
        personDomain.addAttribute("department", worksInDepartment);
    }

    @Test 
    void testSimpleQuery() {
        Query query = new Query("employee_companies", personDomain);
        query.addProjection("employee_first_name", new AttributeExpression(personDomain.getAttribute("first_name")));
        query.addProjection("employee_salary", new AttributeExpression(personDomain.getAttribute("salary")));

        String sql = sqlGenerator.generateSQL(query);
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
        
        String sql = sqlGenerator.generateSQL(query);
        assertEquals(
            "SELECT person.first_name AS employee, j1.name AS employee_company_name " +
            "FROM person " +
            "JOIN company ON (person.company_id = company.id) AS j1",
            sql.trim()
        );
    }

    @Test
    void testNestedComposeExpression() {
        // Setup additional domains and attributes
        Domain addressDomain = new Domain("address");
        addressDomain.addAttribute("id", new Attribute("id", addressDomain, ScalarType.INTEGER));
        addressDomain.addAttribute("city", new Attribute("city", addressDomain, ScalarType.STRING));
        
        // Add address reference to company
        BinaryExpression addressJoin = new BinaryExpression(
            new AttributeExpression(new Attribute("address_id", companyDomain, ScalarType.INTEGER)),
            Operator.EQUALS,
            new AttributeExpression(addressDomain.getAttribute("id"))
        );
        
        ReferenceAttribute hasAddress = new ReferenceAttribute(
            "address",
            companyDomain,
            addressDomain,
            addressJoin
        );
        companyDomain.addAttribute("address", hasAddress);
        
        // Create query with nested compose expressions
        Query query = new Query("employee_locations", personDomain);
        
        // Add person's name
        query.addProjection("employee", new AttributeExpression(
            personDomain.getAttribute("first_name")
        ));
        
        // Add company name through composition
        ComposeExpression companyNameExpr = new ComposeExpression(
            new AttributeExpression(personDomain.getAttribute("works_for")),
            new AttributeExpression(companyDomain.getAttribute("name"))
        );
        query.addProjection("company", companyNameExpr);
        
        // Add company's city through nested composition
        ComposeExpression companyCityExpr = new ComposeExpression(
            new AttributeExpression(personDomain.getAttribute("works_for")),
            new ComposeExpression(
                new AttributeExpression(companyDomain.getAttribute("address")),
                new AttributeExpression(addressDomain.getAttribute("city"))
            )
        );
        query.addProjection("city", companyCityExpr);
        
        String sql = sqlGenerator.generateSQL(query);
        assertEquals(
            "SELECT person.first_name AS employee, j1.name AS company, j2.city AS city " +
            "FROM person " +
            "JOIN company ON (person.company_id = company.id) AS j1 " +
            "JOIN address ON (j1.address_id = address.id) AS j2",
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
        
        String sql = sqlGenerator.generateSQL(query);
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
        
        String sql = sqlGenerator.generateSQL(query);
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