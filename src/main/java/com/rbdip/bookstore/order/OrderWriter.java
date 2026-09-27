package com.rbdip.bookstore.order;

import java.util.List;
import org.springframework.stereotype.Component;

/** Persists an order and its items within the caller's transaction. */
@Component
public class OrderWriter {

    private static final String NEW_STATUS = "new";

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public OrderWriter(OrderRepository orderRepository, OrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    public Order save(CreateOrderRequest request, List<OrderItemsResolver.ResolvedItem> items) {
        Order order = new Order(
                request.customerFullName(), request.customerAddress(), request.customerPhone(), NEW_STATUS);
        order = orderRepository.save(order);

        for (OrderItemsResolver.ResolvedItem item : items) {
            orderItemRepository.save(new OrderItem(
                    order.getId(), item.product().getName(), item.product().getPrice(), item.quantity()));
        }
        return order;
    }
}
