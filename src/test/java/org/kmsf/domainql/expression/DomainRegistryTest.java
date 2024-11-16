package org.kmsf.domainql.expression;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import org.kmsf.domainql.expression.type.ScalarType;

class DomainRegistryTest {
    
    private DomainRegistry registry;
    private Domain employeeDomain;
    private Domain departmentDomain;
    
    @BeforeEach
    void setUp() {
        registry = new DomainRegistry();
        
        // Création du domaine Employee
        employeeDomain = new Domain("employee");
        employeeDomain.addAttribute("id", ScalarType.INTEGER);
        employeeDomain.addAttribute("name", ScalarType.STRING);
        employeeDomain.addAttribute("dept_id", ScalarType.INTEGER);
        
        // Création du domaine Department
        departmentDomain = new Domain("department");
        departmentDomain.addAttribute("id", ScalarType.INTEGER);
        departmentDomain.addAttribute("name", ScalarType.STRING);

        employeeDomain.addReference("department", "dept_id", departmentDomain, "id");
    }
    
    @Test
    void testRegisterAndRetrieve() {
        registry.register(employeeDomain);
        registry.register(departmentDomain);
        
        assertSame(employeeDomain, registry.getDomain("employee"));
        assertSame(departmentDomain, registry.getDomain("department"));
    }
    
    @Test
    void testDomainNotFound() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> registry.getDomain("nonexistent")
        );
        assertEquals(
            "Domain 'nonexistent' not found in registry",
            exception.getMessage()
        );
    }
    
    @Test
    void testHasDomain() {
        registry.register(employeeDomain);
        
        assertTrue(registry.hasDomain("employee"));
        assertFalse(registry.hasDomain("nonexistent"));
    }
    
    @Test
    void testGetAllDomains() {
        registry.register(employeeDomain);
        registry.register(departmentDomain);
        
        var allDomains = registry.getAllDomains();
        assertEquals(2, allDomains.size());
        assertTrue(allDomains.contains(employeeDomain));
        assertTrue(allDomains.contains(departmentDomain));
        
        // Vérifier que la collection est immuable
        assertThrows(
            UnsupportedOperationException.class,
            () -> allDomains.add(new Domain("test"))
        );
    }
    
    @Test
    void testToJson() {
        registry.register(employeeDomain);
        registry.register(departmentDomain);
        
        JsonObject json = registry.toJson();

        System.out.println(json.toString());
        
        // Vérification du domaine employee
        assertTrue(json.has("employee"));
        JsonObject empJson = json.getAsJsonObject("employee");
        assertEquals("employee", empJson.get("name").getAsString());
        JsonArray empAttrs = empJson.getAsJsonArray("attributes");
        assertEquals(4, empAttrs.size());
        
        // Vérification du domaine department
        assertTrue(json.has("department"));
        JsonObject deptJson = json.getAsJsonObject("department");
        assertEquals("department", deptJson.get("name").getAsString());
        JsonArray deptAttrs = deptJson.getAsJsonArray("attributes");
        assertEquals(2, deptAttrs.size());
    }
} 