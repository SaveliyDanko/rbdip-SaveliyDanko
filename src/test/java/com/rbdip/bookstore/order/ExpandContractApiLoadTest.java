package com.rbdip.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.rbdip.bookstore.BookstoreApplication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class ExpandContractApiLoadTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("bookstore_contract_test")
            .withUsername("bookstore")
            .withPassword("bookstore");

    @Test
    void apiRemainsAvailableWhileContractMigrationRuns() throws Exception {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(postgres.getJdbcUrl());
        dataSource.setUser(postgres.getUsername());
        dataSource.setPassword(postgres.getPassword());
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        Flyway.configure().dataSource(dataSource).target("3").load().migrate();
        Long productId = jdbc.queryForObject(
                "INSERT INTO products (name, price) VALUES ('Migration Book', 15.00) RETURNING id", Long.class);
        assertLegacyWritesPopulateNewColumns(jdbc);

        try (ConfigurableApplicationContext app = new SpringApplicationBuilder(BookstoreApplication.class).run(
                "--server.port=0",
                "--spring.datasource.url=" + postgres.getJdbcUrl(),
                "--spring.datasource.username=" + postgres.getUsername(),
                "--spring.datasource.password=" + postgres.getPassword(),
                "--spring.flyway.enabled=false",
                "--spring.jpa.properties.hibernate.generate_statistics=false")) {
            int port = ((WebServerApplicationContext) app).getWebServer().getPort();
            URI ordersUrl = URI.create("http://localhost:" + port + "/orders");
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            HttpRequest read = HttpRequest.newBuilder(ordersUrl).timeout(Duration.ofSeconds(10)).GET().build();
            HttpRequest write = HttpRequest.newBuilder(ordersUrl).timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(orderJson(productId)))
                    .build();

            assertThat(client.send(write, HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(201);
            assertThat(jdbc.queryForObject(
                    "SELECT COUNT(*) FROM customers WHERE first_name = 'New' AND last_name = 'User'",
                    Long.class)).isEqualTo(1L);
            assertThat(jdbc.queryForObject(
                    "SELECT COUNT(*) FROM orders WHERE customer_full_name = 'New User'",
                    Long.class)).isEqualTo(1L);

            runRequestsDuringMigration(dataSource, client, read, write, "5");
            assertOldOrderNameRemainsReadableAfterCustomerContract(jdbc);
            runRequestsDuringMigration(dataSource, client, read, write, "6");

            assertThat(client.send(read, HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(200);
            assertThat(client.send(write, HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(201);
        }
    }

    private void assertLegacyWritesPopulateNewColumns(JdbcTemplate jdbc) {
        Long orderId = jdbc.queryForObject("""
                INSERT INTO orders (customer_full_name, customer_address, status)
                VALUES ('Legacy User', 'Old address', 'new') RETURNING id
                """, Long.class);
        jdbc.update("""
                INSERT INTO order_items (order_id, product_name, product_price, quantity)
                VALUES (?, 'Legacy Book', 9.00, 1)
                """, orderId);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM orders o JOIN customers c ON c.id = o.customer_id
                WHERE o.id = ? AND c.first_name = 'Legacy' AND c.last_name = 'User'
                """, Long.class, orderId)).isEqualTo(1L);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM order_items oi JOIN products p ON p.id = oi.product_id
                WHERE oi.order_id = ? AND p.name = 'Legacy Book'
                """, Long.class, orderId)).isEqualTo(1L);
    }

    private void assertOldOrderNameRemainsReadableAfterCustomerContract(JdbcTemplate jdbc) {
        Long orderId = jdbc.queryForObject("""
                INSERT INTO orders (customer_full_name, customer_address, status)
                VALUES ('Interim User', 'Interim address', 'new') RETURNING id
                """, Long.class);
        assertThat(jdbc.queryForObject(
                "SELECT customer_full_name FROM orders WHERE id = ?", String.class, orderId))
                .isEqualTo("Interim User");
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM orders o JOIN customers c ON c.id = o.customer_id
                WHERE o.id = ? AND c.first_name = 'Interim' AND c.last_name = 'User'
                """, Long.class, orderId)).isEqualTo(1L);
    }

    private void runRequestsDuringMigration(PGSimpleDataSource dataSource, HttpClient client,
            HttpRequest read, HttpRequest write, String targetVersion) throws Exception {
        List<String> failures = new CopyOnWriteArrayList<>();
        AtomicBoolean running = new AtomicBoolean(true);
        AtomicInteger completed = new AtomicInteger();
        CountDownLatch started = new CountDownLatch(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        pool.submit(() -> sendRepeatedly(client, read, 200, started, running, completed, failures));
        pool.submit(() -> sendRepeatedly(client, write, 201, started, running, completed, failures));
        try {
            assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (completed.get() < 10 && failures.isEmpty() && System.nanoTime() < deadline) {
                Thread.sleep(10);
            }
            assertThat(failures).isEmpty();
            assertThat(completed.get()).isGreaterThanOrEqualTo(10);

            Flyway.configure().dataSource(dataSource).target(targetVersion).load().migrate();
            Thread.sleep(200);
        } finally {
            running.set(false);
            pool.shutdown();
            assertThat(pool.awaitTermination(15, TimeUnit.SECONDS)).isTrue();
        }
        assertThat(failures).isEmpty();
    }

    private void sendRepeatedly(HttpClient client, HttpRequest request, int expectedStatus,
            CountDownLatch started, AtomicBoolean running, AtomicInteger completed, List<String> failures) {
        started.countDown();
        while (running.get()) {
            try {
                int status = client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
                if (status != expectedStatus) {
                    failures.add("Expected HTTP " + expectedStatus + ", received " + status);
                }
                completed.incrementAndGet();
            } catch (Exception e) {
                failures.add(e.toString());
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private String orderJson(Long productId) {
        return """
                {"customerFullName":"New User","customerAddress":"New address",
                 "customerType":"regular","items":[{"productId":%d,"quantity":1}]}
                """.formatted(productId);
    }
}
