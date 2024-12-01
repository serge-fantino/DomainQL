package org.kmsf.domainql.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "org.kmsf.domainql")
public class DomainQLSpringApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(DomainQLSpringApplication.class, args);
    }
} 