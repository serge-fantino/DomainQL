package org.kmsf.domainql.expression;

import org.junit.Test;
import org.kmsf.domainql.expression.type.*;

import static org.junit.Assert.*;

public class ComposeExpressionTest {

    @Test
    public void testValidComposeExpression() {
        // Setup domains
        Domain employeeDomain = new Domain("employee");
        Domain companyDomain = new Domain("company");
        
        // Setup attributes
        Attribute companyNameAttr = new Attribute("name", companyDomain, ScalarType.STRING);
        
        // Setup join condition
        BinaryExpression joinCondition = new BinaryExpression(
            new AttributeExpression(new Attribute("company_id", employeeDomain, ScalarType.INTEGER)),
            Operator.EQUALS,
            new AttributeExpression(new Attribute("id", companyDomain, ScalarType.INTEGER))
        );
        
        // Setup reference attribute
        ReferenceAttribute workForAttr = new ReferenceAttribute(
            "work_for",
            employeeDomain,
            companyDomain,
            joinCondition
        );
        
        // Create expressions
        Expression reference = new AttributeExpression(workForAttr);
        Expression composition = new AttributeExpression(companyNameAttr);
        
        // Create and verify compose expression
        ComposeExpression compose = new ComposeExpression(reference, composition);
        
        assertEquals(ScalarType.STRING, compose.getType());
        assertEquals(reference, compose.getReference());
        assertEquals(composition, compose.getComposition());
        assertEquals(employeeDomain.asDomainType(), compose.getSource());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidReferenceType() {
        // Setup domain
        Domain employeeDomain = new Domain("employee");
        
        // Setup attributes with non-domain type
        Attribute nameAttr = new Attribute("name", employeeDomain, ScalarType.STRING);
        Attribute ageAttr = new Attribute("age", employeeDomain, ScalarType.INTEGER);
        
        // This should throw IllegalArgumentException because reference doesn't return DomainType
        new ComposeExpression(
            new AttributeExpression(nameAttr),
            new AttributeExpression(ageAttr)
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMismatchedDomains() {
        // Setup domains
        Domain employeeDomain = new Domain("employee");
        Domain companyDomain = new Domain("company");
        Domain departmentDomain = new Domain("department");  // Different domain
        
        // Setup attributes
        Attribute deptNameAttr = new Attribute("name", departmentDomain, ScalarType.STRING);
        
        // Setup join condition
        BinaryExpression joinCondition = new BinaryExpression(
            new AttributeExpression(new Attribute("company_id", employeeDomain, ScalarType.INTEGER)),
            Operator.EQUALS,
            new AttributeExpression(new Attribute("id", companyDomain, ScalarType.INTEGER))
        );
        
        // Setup reference attribute
        ReferenceAttribute workForAttr = new ReferenceAttribute(
            "work_for",
            employeeDomain,
            companyDomain,
            joinCondition
        );
        
        // This should throw IllegalArgumentException because composition references wrong domain
        new ComposeExpression(
            new AttributeExpression(workForAttr),
            new AttributeExpression(deptNameAttr)  // Wrong domain
        );
    }

    @Test
    public void testNestedComposeExpression() {
        // Setup domains
        Domain employeeDomain = new Domain("employee");
        Domain companyDomain = new Domain("company");
        Domain addressDomain = new Domain("address");
        
        // Setup join conditions
        BinaryExpression employeeCompanyJoin = new BinaryExpression(
            new AttributeExpression(new Attribute("company_id", employeeDomain, ScalarType.INTEGER)),
            Operator.EQUALS,
            new AttributeExpression(new Attribute("id", companyDomain, ScalarType.INTEGER))
        );
        
        BinaryExpression companyAddressJoin = new BinaryExpression(
            new AttributeExpression(new Attribute("address_id", companyDomain, ScalarType.INTEGER)),
            Operator.EQUALS,
            new AttributeExpression(new Attribute("id", addressDomain, ScalarType.INTEGER))
        );
        
        // Setup reference attributes
        ReferenceAttribute workForAttr = new ReferenceAttribute(
            "work_for",
            employeeDomain,
            companyDomain,
            employeeCompanyJoin
        );
        
        ReferenceAttribute locationAttr = new ReferenceAttribute(
            "location",
            companyDomain,
            addressDomain,
            companyAddressJoin
        );
        
        // Setup final attribute
        Attribute cityAttr = new Attribute("city", addressDomain, ScalarType.STRING);
        
        // Create nested compose expression
        ComposeExpression innerCompose = new ComposeExpression(
            new AttributeExpression(locationAttr),
            new AttributeExpression(cityAttr)
        );
        
        ComposeExpression outerCompose = new ComposeExpression(
            new AttributeExpression(workForAttr),
            innerCompose
        );
        
        assertEquals(ScalarType.STRING, outerCompose.getType());
        assertTrue(outerCompose.getComposition() instanceof ComposeExpression);
    }
} 