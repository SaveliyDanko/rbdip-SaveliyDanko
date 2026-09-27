package com.rbdip.bookstore.review;

/** Result of accepting a review and checking whether its author bought the product. */
public record ReviewReceipt(Long id, boolean verifiedPurchase) {
}
