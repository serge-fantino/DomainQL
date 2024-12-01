package org.kmsf.domainql.spring.jdbc;

import org.junit.jupiter.api.Test;
import org.kmsf.domainql.model.Attribute;
import org.kmsf.domainql.model.Domain;
import org.kmsf.domainql.model.DomainRegistry;
import org.kmsf.domainql.spring.jdbc.JdbcMetadataReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class JdbcMetadataReaderTest {
    
    @Autowired
    private JdbcMetadataReader metadataReader;
    
    @Test
    void shouldReadDatabaseMetadata() throws SQLException {
        DomainRegistry registry = metadataReader.readMetadata();
        assertNotNull(registry);
        
        // Vérifier la présence des domaines attendus
        Domain employeeDomain = registry.getDomain("employee");
        assertNotNull(employeeDomain);
        
        // Vérifier les attributs
        assertTrue(employeeDomain.hasAttribute("id"));
        assertTrue(employeeDomain.hasAttribute("name"));
        
        // Vérifier les relations
        Attribute deptAttribute = employeeDomain.getAttribute("department");
        assertEquals("Domain{department}", deptAttribute.getType());
    }
} 