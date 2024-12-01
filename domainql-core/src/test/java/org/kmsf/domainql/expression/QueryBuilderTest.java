package org.kmsf.domainql.expression;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kmsf.domainql.expression.type.Operator;
import org.kmsf.domainql.expression.type.ScalarType;
import org.kmsf.domainql.model.Domain;
import org.kmsf.domainql.model.Query;
import org.kmsf.domainql.model.QueryBuilder;
import org.kmsf.domainql.model.ReferenceAttribute;

import static org.kmsf.domainql.expression.ExpressionBuilder.*;
import static org.junit.jupiter.api.Assertions.*;

public class QueryBuilderTest {
    private Domain employeeDomain;
    private Domain departmentDomain;
    private Domain companyDomain;

    @BeforeEach
    void setUp() {
        // Set up Company domain
        companyDomain = new Domain("Company");
        companyDomain.addAttribute("id", ScalarType.INTEGER);
        companyDomain.addAttribute("name", ScalarType.STRING);
        companyDomain.addAttribute("foundedYear", ScalarType.INTEGER);

        // Set up Department domain
        departmentDomain = new Domain("Department");
        departmentDomain.addAttribute("id", ScalarType.INTEGER);
        departmentDomain.addAttribute("name", ScalarType.STRING);
        departmentDomain.addAttribute("code", ScalarType.STRING);
        departmentDomain.addAttribute("company_id", ScalarType.INTEGER);
        departmentDomain.addReference("company", "company_id", companyDomain, "id");

        // Set up Employee domain
        employeeDomain = new Domain("Employee");
        employeeDomain.addAttribute("id", ScalarType.INTEGER);
        employeeDomain.addAttribute("firstName", ScalarType.STRING);
        employeeDomain.addAttribute("lastName", ScalarType.STRING);
        employeeDomain.addAttribute("email", ScalarType.STRING);
        employeeDomain.addAttribute("salary", ScalarType.DECIMAL);
        employeeDomain.addAttribute("active", ScalarType.BOOLEAN);
        employeeDomain.addAttribute("department_id", ScalarType.INTEGER);
        employeeDomain.addReference("department", "department_id", departmentDomain, "id");

        // create the manager reference
        departmentDomain.addAttribute("manager_id", ScalarType.INTEGER);
        departmentDomain.addReference("manager", "manager_id", employeeDomain, "id");
    }

    @Test
    void testSimpleSelect() {
        Query query = QueryBuilder.from("employeeQuery", employeeDomain)
            .select("id")
            .select("firstName")
            .select("lastName")
            .build();

        assertNotNull(query);
        assertEquals("employeeQuery", query.getName());
        assertEquals(employeeDomain, query.getSourceDomain());
        assertEquals(3, query.getProjections().size());
        assertTrue(query.getProjections().containsKey("id"));
        assertTrue(query.getProjections().containsKey("firstName"));
        assertTrue(query.getProjections().containsKey("lastName"));
    }

    @Test
    void testSelectWithCustomAlias() {
        Query query = QueryBuilder.from("employeeQuery", employeeDomain)
            .select("name", "firstName")
            .build();

        assertNotNull(query);
        assertTrue(query.getProjections().containsKey("name"));
        assertFalse(query.getProjections().containsKey("firstName"));
    }

    @Test
    void testSelectWithComposedPath() {
        Query query = QueryBuilder.from("employeeQuery", employeeDomain)
            .select("deptName", "department.name")
            .select("companyName", "department.company.name")
            .build();

        assertNotNull(query);
        assertTrue(query.getProjections().containsKey("deptName"));
        assertTrue(query.getProjections().containsKey("companyName"));
    }

    @Test
    void testSelectWithVeryLongPathToCheckCompositionIsInCorrectOrder() {
        Query query = QueryBuilder.from("employeeQuery", employeeDomain)
            .select("companyName", "department.company.name")
            .build();

        assertNotNull(query);
        assertTrue(query.getProjections().containsKey("companyName"));
        assertTrue(query.getProjections().get("companyName") instanceof ComposeExpression);
        assertTrue(((ComposeExpression) query.getProjections().get("companyName")).getReference() instanceof AttributeExpression);
        assertTrue(((AttributeExpression)((ComposeExpression) query.getProjections().get("companyName")).getReference()).getAttribute() instanceof ReferenceAttribute);
    }

    @Test
    void testSelectWithManagerPath() {
        Query query = QueryBuilder.from("employeeQuery", employeeDomain)
            .select("mgrName", "department.manager.firstName")
            .select("mgrDept", "department.manager.department.name")
            .build();

        assertNotNull(query);
        assertTrue(query.getProjections().containsKey("mgrName"));
        assertTrue(query.getProjections().containsKey("mgrDept"));
    }

    @Test
    void testWhereEquals() {
        Query query = QueryBuilder.from("employeeQuery", employeeDomain)
            .select("firstName")
            .where(EQUALS(attr("active"), literal(true)))
            .build();

        assertNotNull(query);
        assertNotNull(query.getFilter());
        assertTrue(query.getFilter() instanceof BinaryExpression);
    }

    @Test
    void testWhereEqualsWithComposedPath() {
        Query query = QueryBuilder.from("employeeQuery", employeeDomain)
            .select("firstName")
            .where(EQUALS(attr("department.code"), literal("HR")))
            .build();

        assertNotNull(query);
        assertNotNull(query.getFilter());
        assertTrue(query.getFilter() instanceof BinaryExpression);
    }

    @Test
    void testComplexQuery() {
        Query query = QueryBuilder.from("employeeQuery", employeeDomain)
            .select("id")
            .select("fullName", "firstName")
            .select("deptName", "department.name")
            .select("companyName", "department.company.name")
            .select("managerName", "department.manager.firstName")
            .where(EQUALS(attr("active"), literal(true)))
            .where(EQUALS(attr("department.company.name"), literal("Acme Corp")))
            .build();

        assertNotNull(query);
        assertEquals(5, query.getProjections().size());
        assertNotNull(query.getFilter());
        assertTrue(query.getFilter() instanceof BinaryExpression);
        assertTrue(((BinaryExpression)query.getFilter()).getOperator() == Operator.AND);
    }

    @Test
    void testInvalidAttributePath() {
        assertThrows(IllegalArgumentException.class, () -> {
            QueryBuilder.from("employeeQuery", employeeDomain)
                .select("invalid.path")
                .build();
        });
    }

    @Test
    void testNonReferenceAttributeInPath() {
        assertThrows(IllegalArgumentException.class, () -> {
            QueryBuilder.from("employeeQuery", employeeDomain)
                .select("firstName.invalid")
                .build();
        });
    }

    @Test
    void testEmptyAttributePath() {
        assertThrows(IllegalArgumentException.class, () -> {
            QueryBuilder.from("employeeQuery", employeeDomain)
                .select("")
                .build();
        });
    }
} 