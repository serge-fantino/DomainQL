package org.kmsf.domainql.expression;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kmsf.domainql.expression.type.Operator;
import org.kmsf.domainql.expression.type.ScalarType;

import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
import static org.kmsf.domainql.expression.ExpressionBuilder.*;

class ExpressionBuilderTest {
    private Domain employeeDomain;
    private Domain departmentDomain;
    private Domain companyDomain;

    @BeforeEach
    void setup() {
        // Create domains with simple initialization
        employeeDomain = new Domain("employee")
            .withAttribute("id", ScalarType.INTEGER)
            .withAttribute("name", ScalarType.STRING)
            .withAttribute("salary", ScalarType.INTEGER)
            .withAttribute("hire_date", ScalarType.DATE)
            .withAttribute("dept_id", ScalarType.INTEGER);

        departmentDomain = new Domain("department")
            .withAttribute("id", ScalarType.INTEGER)
            .withAttribute("name", ScalarType.STRING)
            .withAttribute("budget", ScalarType.INTEGER)
            .withAttribute("company_id", ScalarType.INTEGER);

        companyDomain = new Domain("company")
            .withAttribute("id", ScalarType.INTEGER)
            .withAttribute("name", ScalarType.STRING)
            .withAttribute("founded_date", ScalarType.DATE);

        // Add references
        employeeDomain.addReference("department", "dept_id", departmentDomain, "id");
        departmentDomain.addReference("company", "company_id", companyDomain, "id");
    }

    @Test
    void testSimpleAttributePath() {
        Expression expr = attr("name").build(employeeDomain);
        
        assertTrue(expr instanceof AttributeExpression);
        AttributeExpression attrExpr = (AttributeExpression) expr;
        assertEquals("name", attrExpr.getAttribute().getName());
    }

    @Test
    void testComplexAttributePath() {
        Expression expr = attr("department.company.name").build(employeeDomain);
        
        assertTrue(expr instanceof ComposeExpression);
        ComposeExpression compExpr = (ComposeExpression) expr;
        
        // Verify the chain: employee -> department -> company -> name
        assertTrue(compExpr.getReference() instanceof AttributeExpression);
        assertTrue(compExpr.getComposition() instanceof ComposeExpression);
        
        ComposeExpression companyExpr = (ComposeExpression) compExpr.getComposition();
        assertTrue(companyExpr.getComposition() instanceof AttributeExpression);
        
        AttributeExpression nameExpr = (AttributeExpression) companyExpr.getComposition();
        assertEquals("name", nameExpr.getAttribute().getName());
    }

    @Test
    void testNumericComparison() {
        // salary > 50000
        Expression expr = GREATER_THAN(attr("salary"), literal(50000))
            .build(employeeDomain);
        
        assertTrue(expr instanceof BinaryExpression);
        BinaryExpression binExpr = (BinaryExpression) expr;
        assertEquals(Operator.GREATER_THAN, binExpr.getOperator());
        assertTrue(binExpr.getLeft() instanceof AttributeExpression);
        assertTrue(binExpr.getRight() instanceof LiteralExpression);
    }

    @Test
    void testDateComparison() {
        // hire_date >= '2023-01-01'
        Expression expr = GREATER_THAN_OR_EQUALS(
            attr("hire_date"), 
            literal(LocalDate.of(2023, 1, 1))
        ).build(employeeDomain);
        
        assertTrue(expr instanceof BinaryExpression);
        BinaryExpression binExpr = (BinaryExpression) expr;
        assertEquals(Operator.GREATER_THAN_OR_EQUALS, binExpr.getOperator());
    }

    @Test
    void testStringComparison() {
        // name LIKE '%John%'
        Expression expr = LIKE(attr("name"), literal("%John%"))
            .build(employeeDomain);
        
        assertTrue(expr instanceof BinaryExpression);
        BinaryExpression binExpr = (BinaryExpression) expr;
        assertEquals(Operator.LIKE, binExpr.getOperator());
    }

    @Test
    void testAttributeToAttributeComparison() {
        // department.budget > employee.salary
        Expression expr = GREATER_THAN(
            attr("department.budget"),
            attr("salary")
        ).build(employeeDomain);
        
        assertTrue(expr instanceof BinaryExpression);
        BinaryExpression binExpr = (BinaryExpression) expr;
        assertTrue(binExpr.getLeft() instanceof ComposeExpression);
        assertTrue(binExpr.getRight() instanceof AttributeExpression);
    }

    @Test
    void testComplexLogicalExpression() {
        // salary > 50000 AND department.company.name = 'ACME' AND hire_date > '2023-01-01'
        Expression expr = AND(
            AND(
                GREATER_THAN(attr("salary"), literal(50000)),
                EQUALS(attr("department.company.name"), literal("ACME"))
            ),
            GREATER_THAN(
                attr("hire_date"), 
                literal(LocalDate.of(2023, 1, 1))
            )
        ).build(employeeDomain);
        
        assertTrue(expr instanceof BinaryExpression);
        BinaryExpression binExpr = (BinaryExpression) expr;
        assertEquals(Operator.AND, binExpr.getOperator());
        assertTrue(binExpr.getLeft() instanceof BinaryExpression);
        assertTrue(binExpr.getRight() instanceof BinaryExpression);
    }

    @Test
    void testInvalidAttributePath() {
        assertThrows(IllegalArgumentException.class, 
            () -> attr("invalid.path").build(employeeDomain));
    }

    @Test
    void testArithmeticOperations() {
        // (salary * 2) > department.budget
        Expression expr = GREATER_THAN(
            MULTIPLY(attr("salary"), literal(2)),
            attr("department.budget")
        ).build(employeeDomain);
        
        assertTrue(expr instanceof BinaryExpression);
        BinaryExpression compareExpr = (BinaryExpression) expr;
        assertTrue(compareExpr.getLeft() instanceof BinaryExpression);
        BinaryExpression multiplyExpr = (BinaryExpression) compareExpr.getLeft();
        assertEquals(Operator.MULTIPLY, multiplyExpr.getOperator());
    }
} 