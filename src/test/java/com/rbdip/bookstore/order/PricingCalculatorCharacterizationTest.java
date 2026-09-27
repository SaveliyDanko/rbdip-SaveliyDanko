package com.rbdip.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class PricingCalculatorCharacterizationTest {

    private final PricingCalculator calculator = new PricingCalculator();

    @Test
    void bulkDiscountAppliesOnlyAboveTenUnitsOnTheSameLine() {
        assertTotal("100.00", List.of(item("10.00", 10)), "regular", null);
        assertTotal("104.50", List.of(item("10.00", 11)), "regular", null);
        assertTotal("110.00", List.of(item("10.00", 9), item("10.00", 2)), "regular", null);
    }

    @Test
    void customerDiscountDependsOnExactCustomerType() {
        assertTotal("90.00", List.of(item("100.00", 1)), "vip", null);
        assertTotal("85.00", List.of(item("100.00", 1)), "wholesale", null);
        assertTotal("100.00", List.of(item("100.00", 1)), "VIP", null);
        assertTotal("100.00", List.of(item("100.00", 1)), null, null);
    }

    @Test
    void couponsApplyAfterCustomerDiscount() {
        assertTotal("80.00", List.of(item("100.00", 1)), "vip", "SAVE10");
        assertTotal("72.00", List.of(item("100.00", 1)), "vip", "SAVE20PERCENT");
        assertTotal("100.00", List.of(item("100.00", 1)), "regular", "save10");
    }

    @Test
    void totalCannotBecomeNegative() {
        assertTotal("0.00", List.of(item("5.00", 1)), "regular", "SAVE10");
        assertTotal("0.00", List.of(item("-4.00", 1)), "regular", null);
        assertTotal("0.00", List.of(), "regular", "SAVE10");
    }

    @Test
    void largeOrderDiscountUsesTotalAfterEarlierDiscounts() {
        assertTotal("1000.00", List.of(item("1000.00", 1)), "regular", null);
        assertTotal("980.01", List.of(item("1000.01", 1)), "regular", null);
        assertTotal("880.00", List.of(item("1100.00", 1)), "regular", "SAVE20PERCENT");
    }

    @Test
    void roundsFinalTotalHalfUpToTwoDecimals() {
        assertTotal("0.02", List.of(item("0.015", 1)), "regular", null);
        assertTotal("0.01", List.of(item("0.014", 1)), "regular", null);
    }

    private PricingCalculator.LineItem item(String price, int quantity) {
        return new PricingCalculator.LineItem(new BigDecimal(price), quantity);
    }

    private void assertTotal(String expected, List<PricingCalculator.LineItem> items,
            String customerType, String couponCode) {
        assertThat(calculator.calculateOrderTotal(items, customerType, couponCode))
                .isEqualByComparingTo(expected);
    }
}
