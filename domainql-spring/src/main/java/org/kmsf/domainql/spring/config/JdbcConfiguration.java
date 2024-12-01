package org.kmsf.domainql.spring.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;

import javax.sql.DataSource;
import java.sql.SQLException;

import org.kmsf.domainql.model.DomainRegistry;
import org.kmsf.domainql.spring.jdbc.JdbcMetadataReader;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Profile;

@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages = "org.kmsf")
public class JdbcConfiguration {
    private final Environment environment;

    public JdbcConfiguration(Environment environment) {
        this.environment = environment;
    }
    
    @Bean
    public DomainRegistry domainRegistry(JdbcMetadataReader metadataReader) throws SQLException {
        return metadataReader.readMetadata();
    }

    @Bean
    @ConditionalOnMissingBean
    @Profile("h2")
    public DataSource h2DataSource() {
        return new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .build();
    }

    @Bean
    @ConditionalOnMissingBean
    @Profile("!h2")
    public DataSource standardDataSource() {
        return DataSourceBuilder.create()
            .url(environment.getProperty("spring.datasource.url"))
            .username(environment.getProperty("spring.datasource.username"))
            .password(environment.getProperty("spring.datasource.password"))
            .build();
    }

    @Bean
    public JdbcMetadataReader jdbcMetadataReader(DataSource dataSource) {
        return new JdbcMetadataReader(dataSource);
    }
} 
