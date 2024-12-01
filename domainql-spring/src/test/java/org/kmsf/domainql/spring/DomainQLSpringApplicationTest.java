package org.kmsf.domainql.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kmsf.domainql.model.DomainRegistry;
import org.kmsf.domainql.spring.jdbc.JdbcMetadataReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest
@ActiveProfiles("h2")
class DomainQLSpringApplicationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private JdbcMetadataReader metadataReader;

    @BeforeAll
    @Sql({"/schema.sql", "/data.sql"})
    static void setUp() {
        System.out.println("Setting up test");
    }

    @Test
    void contextLoads() {
        assertNotNull(applicationContext, "Application context should be loaded");
        assertNotNull(metadataReader, "JdbcMetadataReader should be autowired");
    }

    @Test
    void shouldLoadDomainMetadata() throws Exception {
        // When
        DomainRegistry registry = metadataReader.readMetadata();

        // Then
        assertNotNull(registry, "Domain registry should not be null");
        assertThat(registry.getDomains())
            .isNotEmpty()
            .containsKeys("users", "departments");
    }

    @Test
    void shouldHaveRequiredBeans() {
        assertThat(applicationContext.containsBean("jdbcMetadataReader"))
            .as("JdbcMetadataReader bean should be present")
            .isTrue();
        
        assertThat(applicationContext.containsBean("dataSource"))
            .as("DataSource bean should be present")
            .isTrue();
    }
} 