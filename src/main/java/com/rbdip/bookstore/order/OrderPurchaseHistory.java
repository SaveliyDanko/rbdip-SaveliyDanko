package com.rbdip.bookstore.order;

import com.rbdip.bookstore.review.api.PurchaseHistory;
import org.springframework.stereotype.Component;

/** Adapts order persistence to the review module's purchase contract. */
@Component
public class OrderPurchaseHistory implements PurchaseHistory {

    private final OrderItemRepository orderItemRepository;

    public OrderPurchaseHistory(OrderItemRepository orderItemRepository) {
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    public boolean hasPurchased(Long productId, String customerName) {
        return orderItemRepository.existsPurchase(productId, customerName);
    }
}
