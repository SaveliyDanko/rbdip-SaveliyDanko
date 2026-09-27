package com.rbdip.bookstore.order;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Emulates the existing order confirmation side effect. */
@Component
public class OrderConfirmationSender {

    public void send(String customerName, Long orderId, BigDecimal total) {
        System.out.printf(
                "[email] Dear %s, your order #%d for %s has been placed.%n", customerName, orderId, total);
    }
}
