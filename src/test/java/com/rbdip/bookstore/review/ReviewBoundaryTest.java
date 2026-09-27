package com.rbdip.bookstore.review;

import static org.assertj.core.api.Assertions.assertThat;

import com.rbdip.bookstore.order.CreateOrderRequest;
import com.rbdip.bookstore.reference.AbstractIntegrationTest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ReviewBoundaryTest extends AbstractIntegrationTest {

    @Test
    void purchaseVerificationUsesTheAuthorAndReviewedProduct() {
        Long boughtProductId = createProduct("Bought book");
        Long otherProductId = createProduct("Other book");
        String customerName = "Reader " + UUID.randomUUID();
        CreateOrderRequest order = new CreateOrderRequest(customerName, "Home", null, "regular", null,
                List.of(new CreateOrderRequest.Item(boughtProductId, 1)));
        assertThat(restTemplate.postForEntity("/orders", order, Map.class).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);

        assertThat(createReview(boughtProductId, customerName).get("verifiedPurchase")).isEqualTo(true);
        assertThat(createReview(otherProductId, customerName).get("verifiedPurchase")).isEqualTo(false);
        assertThat(createReview(boughtProductId, "Other reader").get("verifiedPurchase")).isEqualTo(false);

        ResponseEntity<List> listed = restTemplate.getForEntity(
                "/products/" + boughtProductId + "/reviews", List.class);
        assertThat(listed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listed.getBody()).hasSize(2);
    }

    private Long createProduct(String name) {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/products", Map.of("name", name, "price", new BigDecimal("12.00")), Map.class);
        return Long.valueOf(response.getBody().get("id").toString());
    }

    private Map<?, ?> createReview(Long productId, String authorName) {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/products/" + productId + "/reviews",
                Map.of("authorName", authorName, "rating", 5, "comment", "Useful"), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().get("id")).isNotNull();
        return response.getBody();
    }
}
