package org.kmsf.domainql.spring.jdbc;


import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kmsf.domainql.model.Domain;
import org.kmsf.domainql.model.DomainRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;

//@SpringBootTest(classes = TestConfiguration.class)
@ActiveProfiles("h2")
@SpringBootTest
class JdbcMetadataReaderIntegrationTest {

    @Autowired
    private JdbcMetadataReader metadataReader;

    @BeforeAll
    @Sql({"/schema.sql", "/data.sql"})
    static void setUp() {
        System.out.println("Setting up test");
    }

    @Test
    void shouldReadDatabaseMetadata() throws Exception {
        // When
        DomainRegistry registry = metadataReader.readMetadata();
        assertNotNull(registry);

        // Then
        Domain userDomain = registry.getDomain("Users");
        assertThat(userDomain).isNotNull();
        assertThat(userDomain.getAttributes().keySet())
            .containsExactlyInAnyOrder("id", "name", "email", "department_id");
        
        // Verify foreign key relationship
        assertThat(userDomain.getAttribute("department_id").getType().hasDomainType().getDomain().getName())
            .isEqualTo("Departments");
    }
} 