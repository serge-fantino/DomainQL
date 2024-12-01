package org.kmsf.domainql.spring.jdbc;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.kmsf.domainql.spring.DomainQLSpringApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class SpinrgBootDummyTest {


    @Autowired
    private DomainQLSpringApplication application;

    @Test
    public void contextLoads() {
        // Vérifie que le contexte se charge correctement
        assertTrue(true);
    }
    
}
