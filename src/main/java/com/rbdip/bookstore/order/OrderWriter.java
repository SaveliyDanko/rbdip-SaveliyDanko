package com.rbdip.bookstore.order;

import java.util.List;
import org.springframework.stereotype.Component;

/** Persists an order and its items within the caller's transaction. */
@Component
public class OrderWriter {

    private static final String NEW_STATUS = "new";

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CustomerRepository customerRepository;

    public OrderWriter(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
            CustomerRepository customerRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.customerRepository = customerRepository;
    }

    public Order save(CreateOrderRequest request, List<OrderItemsResolver.ResolvedItem> items) {
        Customer customer = customerRepository.findByFullNameAndAddressAndPhone(
                request.customerFullName(), request.customerAddress(), request.customerPhone())
                .orElseGet(() -> customerRepository.save(new Customer(
                        request.customerFullName(), request.customerAddress(), request.customerPhone())));
        Order order = new Order(customer, NEW_STATUS);
        order = orderRepository.save(order);

        for (OrderItemsResolver.ResolvedItem item : items) {
            orderItemRepository.save(new OrderItem(order.getId(), item.product(), item.quantity()));
        }
        return order;
    }
}
