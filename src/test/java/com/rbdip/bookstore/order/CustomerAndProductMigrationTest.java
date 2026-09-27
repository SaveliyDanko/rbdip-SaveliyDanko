package com.rbdip.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class CustomerAndProductMigrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("bookstore_migration_test")
            .withUsername("bookstore")
            .withPassword("bookstore");

    @Test
    void preservesExistingOrdersAndReusesCustomersAndProducts() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(postgres.getJdbcUrl());
        dataSource.setUser(postgres.getUsername());
        dataSource.setPassword(postgres.getPassword());
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        Flyway.configure().dataSource(dataSource).target("1").load().migrate();
        jdbc.update("INSERT INTO products (name, price) VALUES ('Existing', 12.00)");
        jdbc.update("""
                INSERT INTO orders (customer_full_name, customer_address, customer_phone)
                VALUES ('Alice', 'Home', NULL), ('Alice', 'Home', NULL), ('Bob', NULL, '123')
                """);
        jdbc.update("""
                INSERT INTO order_items (order_id, product_name, product_price, quantity)
                VALUES (1, 'Existing', 12.00, 2), (2, 'Lost', 7.50, 1), (3, 'Lost', 7.50, 3)
                """);

        Flyway.configure().dataSource(dataSource).load().migrate();

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM customers", Long.class)).isEqualTo(2L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM products", Long.class)).isEqualTo(2L);
        List<Long> aliceCustomers = jdbc.queryForList(
                "SELECT customer_id FROM orders WHERE id IN (1, 2) ORDER BY id", Long.class);
        assertThat(aliceCustomers).hasSize(2).containsOnly(aliceCustomers.getFirst());
        assertThat(jdbc.queryForList("SELECT product_id FROM order_items ORDER BY id", Long.class))
                .containsExactly(1L, 2L, 2L);
        assertThat(jdbc.queryForObject("SELECT SUM(quantity) FROM order_items", Long.class)).isEqualTo(6L);
    }
}
