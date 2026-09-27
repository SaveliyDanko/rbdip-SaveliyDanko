package com.rbdip.bookstore.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Component;

/** Calculates the order total using the existing discount sequence. */
@Component
public class PricingCalculator {

    private static final int BULK_QUANTITY_THRESHOLD = 10;
    private static final BigDecimal BULK_MULTIPLIER = BigDecimal.valueOf(0.95);
    private static final BigDecimal VIP_MULTIPLIER = BigDecimal.valueOf(0.9);
    private static final BigDecimal WHOLESALE_MULTIPLIER = BigDecimal.valueOf(0.85);
    private static final BigDecimal SAVE10_DISCOUNT = BigDecimal.TEN;
    private static final BigDecimal SAVE20_MULTIPLIER = BigDecimal.valueOf(0.8);
    private static final BigDecimal LARGE_ORDER_THRESHOLD = BigDecimal.valueOf(1000);
    private static final BigDecimal LARGE_ORDER_MULTIPLIER = BigDecimal.valueOf(0.98);
    private static final int MONEY_SCALE = 2;

    private static final String VIP_CUSTOMER = "vip";
    private static final String WHOLESALE_CUSTOMER = "wholesale";
    private static final String SAVE10_COUPON = "SAVE10";
    private static final String SAVE20_COUPON = "SAVE20PERCENT";

    public record LineItem(BigDecimal price, int quantity) {
    }

    public BigDecimal calculateOrderTotal(List<LineItem> items, String customerType, String couponCode) {
        BigDecimal total = calculateLinesTotal(items);
        total = applyCustomerDiscount(total, customerType);
        total = applyCoupon(total, couponCode);
        total = total.max(BigDecimal.ZERO);
        if (total.compareTo(LARGE_ORDER_THRESHOLD) > 0) {
            total = total.multiply(LARGE_ORDER_MULTIPLIER);
        }
        return total.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateLinesTotal(List<LineItem> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (LineItem item : items) {
            BigDecimal linePrice = item.price().multiply(BigDecimal.valueOf(item.quantity()));
            if (item.quantity() > BULK_QUANTITY_THRESHOLD) {
                linePrice = linePrice.multiply(BULK_MULTIPLIER);
            }
            total = total.add(linePrice);
        }
        return total;
    }

    private BigDecimal applyCustomerDiscount(BigDecimal total, String customerType) {
        if (VIP_CUSTOMER.equals(customerType)) {
            return total.multiply(VIP_MULTIPLIER);
        }
        if (WHOLESALE_CUSTOMER.equals(customerType)) {
            return total.multiply(WHOLESALE_MULTIPLIER);
        }
        return total;
    }

    private BigDecimal applyCoupon(BigDecimal total, String couponCode) {
        if (SAVE10_COUPON.equals(couponCode)) {
            return total.subtract(SAVE10_DISCOUNT);
        }
        if (SAVE20_COUPON.equals(couponCode)) {
            return total.multiply(SAVE20_MULTIPLIER);
        }
        return total;
    }
}
