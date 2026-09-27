package com.rbdip.bookstore.order;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coordinates order creation while keeping validation, pricing and storage separate. */
@Service
public class OrderService {

    private static final String REGULAR_CUSTOMER = "regular";

    private final OrderRequestValidator requestValidator;
    private final OrderItemsResolver itemsResolver;
    private final PricingCalculator pricingCalculator;
    private final OrderWriter orderWriter;
    private final OrderConfirmationSender confirmationSender;

    public OrderService(
            OrderRequestValidator requestValidator,
            OrderItemsResolver itemsResolver,
            PricingCalculator pricingCalculator,
            OrderWriter orderWriter,
            OrderConfirmationSender confirmationSender) {
        this.requestValidator = requestValidator;
        this.itemsResolver = itemsResolver;
        this.pricingCalculator = pricingCalculator;
        this.orderWriter = orderWriter;
        this.confirmationSender = confirmationSender;
    }

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        requestValidator.validate(request);
        List<OrderItemsResolver.ResolvedItem> items = itemsResolver.resolve(request.items());
        List<PricingCalculator.LineItem> lineItems = items.stream()
                .map(item -> new PricingCalculator.LineItem(item.product().getPrice(), item.quantity()))
                .toList();
        String customerType = request.customerType() == null ? REGULAR_CUSTOMER : request.customerType();
        BigDecimal total = pricingCalculator.calculateOrderTotal(lineItems, customerType, request.couponCode());

        Order order = orderWriter.save(request, items);
        confirmationSender.send(request.customerFullName(), order.getId(), total);
        return order;
    }
}
