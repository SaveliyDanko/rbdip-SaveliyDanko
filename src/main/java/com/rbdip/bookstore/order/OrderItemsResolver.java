package com.rbdip.bookstore.order;

import com.rbdip.bookstore.product.Product;
import com.rbdip.bookstore.product.ProductRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/** Looks up requested products and resolves their quantities. */
@Component
public class OrderItemsResolver {

    private static final int DEFAULT_QUANTITY = 1;

    private final ProductRepository productRepository;

    public OrderItemsResolver(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ResolvedItem> resolve(List<CreateOrderRequest.Item> requestedItems) {
        List<ResolvedItem> resolved = new ArrayList<>();
        for (CreateOrderRequest.Item item : requestedItems) {
            Product product = productRepository.findById(item.productId())
                    .orElseThrow(() -> new IllegalArgumentException("product " + item.productId() + " not found"));
            int quantity = item.quantity() == null ? DEFAULT_QUANTITY : item.quantity();
            if (quantity <= 0) {
                throw new IllegalArgumentException("quantity must be positive");
            }
            resolved.add(new ResolvedItem(product, quantity));
        }
        return resolved;
    }

    public record ResolvedItem(Product product, int quantity) {
    }
}
