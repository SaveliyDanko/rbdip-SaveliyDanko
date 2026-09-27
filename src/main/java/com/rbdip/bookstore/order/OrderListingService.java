package com.rbdip.bookstore.order;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Loads orders, customers, items and products in two queries. */
@Service
public class OrderListingService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public OrderListingService(OrderRepository orderRepository, OrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listOrders() {
        List<Order> orders = orderRepository.findAllWithCustomer();
        if (orders.isEmpty()) {
            return List.of();
        }

        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        Map<Long, List<OrderItem>> itemsByOrder = orderItemRepository.findWithProductByOrderIdIn(orderIds)
                .stream().collect(Collectors.groupingBy(OrderItem::getOrderId));

        return orders.stream()
                .map(order -> Map.<String, Object>of(
                        "id", order.getId(),
                        "customerFullName", order.getCustomerFullName(),
                        "status", order.getStatus(),
                        "items", itemsByOrder.getOrDefault(order.getId(), List.of()).stream()
                                .map(item -> Map.of(
                                        "productName", item.getProductName(),
                                        "quantity", item.getQuantity()))
                                .toList()))
                .toList();
    }
}
