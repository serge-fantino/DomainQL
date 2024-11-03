package org.kmsf.domainql.expression;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import org.kmsf.domainql.expression.AttributeExpression.ContextResolution;
import org.kmsf.domainql.expression.type.ScalarType;

class DomainTest {

    @Test
    void testAddAttribute() {
        Domain domain = new Domain("test");
        Attribute attr = domain.addAttribute("name", ScalarType.STRING);
        
        assertEquals("name", attr.getName());
        assertEquals(ScalarType.STRING, attr.getType());
        assertEquals(domain, attr.getDomain());
        assertSame(attr, domain.getAttribute("name"));
    }

    @Test
    void testAddAttributeWithExistingAttribute() {
        Domain domain = new Domain("test");
        Attribute attr1 = domain.addAttribute("name", ScalarType.STRING);
        Attribute attr2 = new Attribute("name", domain, ScalarType.INTEGER);
        
        domain.addAttribute(attr2);
        assertSame(attr2, domain.getAttribute("name"));
        assertNotSame(attr1, domain.getAttribute("name"));
    }

    @Test
    void testGetNonExistentAttribute() {
        Domain domain = new Domain("test");
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> domain.getAttribute("nonexistent")
        );
        assertEquals(
            "Attribute 'nonexistent' not found in domain 'test'",
            exception.getMessage()
        );
    }

    @Test
    void testAddRegularReference() {
        Domain sourceDomain = new Domain("employee");
        Domain targetDomain = new Domain("department");
        
        sourceDomain.addAttribute("dept_id", ScalarType.INTEGER);
        targetDomain.addAttribute("id", ScalarType.INTEGER);

        ReferenceAttribute ref = sourceDomain.addReference(
            "department",
            "dept_id",
            targetDomain,
            "id"
        );

        assertEquals("department", ref.getName());
        assertEquals(sourceDomain, ref.getDomain());
        assertEquals(targetDomain, ref.getReferenceDomain());

        // Check join condition
        BinaryExpression joinCondition = (BinaryExpression) ref.getJoinCondition();
        AttributeExpression leftExpr = (AttributeExpression) joinCondition.getLeft();
        AttributeExpression rightExpr = (AttributeExpression) joinCondition.getRight();

        assertEquals(ContextResolution.DEFAULT, leftExpr.getContextResolution());
        assertEquals(ContextResolution.DEFAULT, rightExpr.getContextResolution());
        assertEquals("dept_id", leftExpr.getAttribute().getName());
        assertEquals("id", rightExpr.getAttribute().getName());
    }

    @Test
    void testAddSelfReference() {
        Domain employeeDomain = new Domain("employee");
        employeeDomain.addAttribute("id", ScalarType.INTEGER);
        employeeDomain.addAttribute("manager_id", ScalarType.INTEGER);

        ReferenceAttribute ref = employeeDomain.addReference(
            "manager",
            "manager_id",
            employeeDomain,  // same domain for self-reference
            "id"
        );

        assertEquals("manager", ref.getName());
        assertEquals(employeeDomain, ref.getDomain());
        assertEquals(employeeDomain, ref.getReferenceDomain());

        // Check join condition with context resolution
        BinaryExpression joinCondition = (BinaryExpression) ref.getJoinCondition();
        AttributeExpression leftExpr = (AttributeExpression) joinCondition.getLeft();
        AttributeExpression rightExpr = (AttributeExpression) joinCondition.getRight();

        assertEquals(ContextResolution.LEFT, leftExpr.getContextResolution());
        assertEquals(ContextResolution.RIGHT, rightExpr.getContextResolution());
        assertEquals("manager_id", leftExpr.getAttribute().getName());
        assertEquals("id", rightExpr.getAttribute().getName());
    }

    @Test
    void testAddReferenceWithMissingSourceAttribute() {
        Domain sourceDomain = new Domain("source");
        Domain targetDomain = new Domain("target");
        targetDomain.addAttribute("id", ScalarType.INTEGER);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> sourceDomain.addReference("ref", "missing_attr", targetDomain, "id")
        );
        assertTrue(exception.getMessage().contains("missing_attr"));
    }

    @Test
    void testAddReferenceWithMissingTargetAttribute() {
        Domain sourceDomain = new Domain("source");
        Domain targetDomain = new Domain("target");
        sourceDomain.addAttribute("ref_id", ScalarType.INTEGER);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> sourceDomain.addReference("ref", "ref_id", targetDomain, "missing_attr")
        );
        assertTrue(exception.getMessage().contains("missing_attr"));
    }
} 