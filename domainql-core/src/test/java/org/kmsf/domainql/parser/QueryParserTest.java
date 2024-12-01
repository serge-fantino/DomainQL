package org.kmsf.domainql.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.kmsf.domainql.expression.type.ScalarType;
import org.kmsf.domainql.model.Domain;
import org.kmsf.domainql.model.DomainRegistry;
import org.kmsf.domainql.model.Query;

public class QueryParserTest {

    private Domain departmentDomain;
    private Domain employeeDomain;
    private DomainRegistry registry;
    
    @org.junit.jupiter.api.BeforeEach
    void setup() {
        // Setup domains
        departmentDomain = new Domain("department")
            .withAttribute("id", ScalarType.INTEGER)
            .withAttribute("name", ScalarType.STRING);

        employeeDomain = new Domain("employee")
            .withAttribute("id", ScalarType.INTEGER)
            .withAttribute("name", ScalarType.STRING)
            .withAttribute("salary", ScalarType.INTEGER)
            .withAttribute("dept_id", ScalarType.INTEGER);
        
        employeeDomain.addReference("department", "dept_id", departmentDomain, "id");

        // Create domain registry
        registry = new DomainRegistry();
        registry.register(employeeDomain);
        registry.register(departmentDomain);
    }

    @Test
    public void testParseQuery() {

        QueryParser parser = new QueryParser(registry);
        
        String json = """
            {
            "name": "high_salary_employees",
            "from": "employee",
            "select": [
                {
                "alias": "name",
                "expression": {
                    "type": "attribute",
                    "path": "name"
                }
                }
            ],
            "where": {
                "type": "binary",
                "operator": "GREATER_THAN",
                "left": {
                "type": "attribute",
                "path": "salary"
                },
                "right": {
                "type": "literal",
                "value": 50000
                }
            }
            }
            """;

        Query query = parser.parseQuery(json);
        
        assertEquals("high_salary_employees", query.getName());
        assertEquals(employeeDomain, query.getSourceDomain());
            // ... autres assertions
    }

    @Test
    public void testAggregateExpressions() {

        QueryParser parser = new QueryParser(registry);

        String json = """
            {
            "name": "employee_report",
            "from": "employee",
            "select": [
                {
                    "alias": "name",
                    "expression": {
                        "type": "attribute",
                        "path": "name"
                    }
                },
                {
                    "alias": "department_name",
                    "expression": {
                        "type": "attribute",
                        "path": "department.name"
                    }
                },
                {
                    "alias": "avg_team_salary",
                    "expression": {
                        "type": "aggregate",
                        "function": "AVG",
                        "operand": {
                            "type": "attribute",
                    "path": "salary"
                    }
                }
                }
            ],
            "where": {
                "type": "binary",
                "operator": "GREATER_THAN",
                "left": {
                "type": "attribute",
                "path": "salary"
                },
                "right": {
                "type": "literal",
                "value": 50000
                }
            },
            "orderBy": [
                {
                "expression": {
                    "type": "attribute",
                    "path": "name"
                },
                "ascending": true
                }
            ]
            }
            """;

        Query query = parser.parseQuery(json);
        
        assertEquals("employee_report", query.getName());
        assertEquals(employeeDomain, query.getSourceDomain());

    }

} 