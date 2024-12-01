package org.kmsf.domainql.spring.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kmsf.domainql.sql.SQLModel;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("h2")
class SQLModelGeneratorIntegrationTest {

    private SQLModelGenerator generator;
    private DataSource dataSource;

    private String schema = "DEMO";

    @BeforeEach
    void setUp() {
        dataSource = new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            //.addScript("schema.sql")
            //.addScript("data.sql")
            .build();
        generator = new SQLModelGenerator(dataSource, Arrays.asList(schema));
    }

    @Test
    void shouldGenerateCompleteModelFromDatabase() throws Exception {
        // When
        SQLModel model = generator.generateModel();

        // Then
        assertNotNull(model);

        // Verify departments table
        SQLModel.Table departments = model.getTable(schema + ".DEPARTMENTS");
        assertNotNull(departments);
        assertThat(departments.getSqlName()).isEqualTo("DEPARTMENTS");
        assertThat(departments.getColumns())
            .extracting(SQLModel.Column::getSqlName)
            .containsExactlyInAnyOrder("ID", "NAME");

        // Verify users table
        SQLModel.Table users = model.getTable(schema + ".USERS");
        assertNotNull(users);
        assertThat(users.getSqlName()).isEqualTo("USERS");
        assertThat(users.getColumns())
            .extracting(SQLModel.Column::getSqlName)
            .containsExactlyInAnyOrder("ID", "NAME", "EMAIL", "DEPARTMENT_ID");

        // Verify foreign key relationship
        assertThat(model.getForeignKeysForTable(users)).hasSize(1);
        SQLModel.ForeignKey fk = model.getForeignKeysForTable(users).get(0);
        assertThat(fk.getSourceTable()).isEqualTo(users);
        assertThat(fk.getTargetTable()).isEqualTo(departments);
        assertThat(fk.getColumnPairs())
            .extracting(pair -> pair.getSourceColumn().getSqlName())
            .containsExactly("DEPARTMENT_ID");
    }

    @Test
    void shouldHandleColumnTypes() throws Exception {
        // When
        SQLModel model = generator.generateModel();
        SQLModel.Table users = model.getTable(schema + ".USERS");

        // Then
        assertThat(users.getColumns())
            .filteredOn(col -> col.getSqlName().equals("ID"))
            .extracting(SQLModel.Column::getSqlType)
            .containsExactly(java.sql.Types.BIGINT);

        assertThat(users.getColumns())
            .filteredOn(col -> col.getSqlName().equals("NAME"))
            .extracting(SQLModel.Column::getSqlType)
            .containsExactly(java.sql.Types.VARCHAR);
    }
} 