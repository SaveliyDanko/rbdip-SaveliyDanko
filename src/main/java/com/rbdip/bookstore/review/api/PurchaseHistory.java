package com.rbdip.bookstore.review.api;

/** Contract used by reviews without depending on order persistence details. */
public interface PurchaseHistory {

    boolean hasPurchased(Long productId, String customerName);
}
