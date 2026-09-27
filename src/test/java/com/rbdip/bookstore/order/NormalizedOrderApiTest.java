package com.rbdip.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.rbdip.bookstore.reference.AbstractIntegrationTest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class NormalizedOrderApiTest extends AbstractIntegrationTest {

    @Test
    void repeatedContactDetailsReuseCustomerWithoutChangingOrderApi() {
        String customerName = "Lab2 Customer " + UUID.randomUUID();
        ResponseEntity<Map> product = restTemplate.postForEntity(
                "/products", Map.of("name", "Patterns", "price", new BigDecimal("24.00")), Map.class);
        Long productId = Long.valueOf(product.getBody().get("id").toString());

        createOrder(customerName, "First address", productId);
        createOrder(customerName, "First address", productId);
        createOrder(customerName, "Second address", productId);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM customers WHERE customer_full_name = ?", Long.class, customerName))
                .isEqualTo(2L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT customer_id) FROM orders", Long.class)).isEqualTo(2L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM order_items WHERE product_id = ?", Long.class, productId))
                .isEqualTo(3L);

        ResponseEntity<List> response = restTemplate.getForEntity("/orders", List.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(3);
        assertThat(response.getBody()).allSatisfy(order -> {
            Map<?, ?> body = (Map<?, ?>) order;
            assertThat(body.get("customerFullName")).isEqualTo(customerName);
            assertThat((List<?>) body.get("items")).hasSize(1);
        });
    }

    private void createOrder(String customerName, String address, Long productId) {
        CreateOrderRequest request = new CreateOrderRequest(customerName, address, null, "regular", null,
                List.of(new CreateOrderRequest.Item(productId, 1)));
        assertThat(restTemplate.postForEntity("/orders", request, Map.class).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
    }
}
