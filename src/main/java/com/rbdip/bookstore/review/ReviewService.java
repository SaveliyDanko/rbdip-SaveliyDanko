package com.rbdip.bookstore.review;

import com.rbdip.bookstore.review.api.PurchaseHistory;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final PurchaseHistory purchaseHistory;

    public ReviewService(ReviewRepository reviewRepository, PurchaseHistory purchaseHistory) {
        this.reviewRepository = reviewRepository;
        this.purchaseHistory = purchaseHistory;
    }

    public ReviewReceipt addReview(Long productId, String authorName, Integer rating, String comment) {
        String author = authorName == null ? "anonymous" : authorName;
        boolean verifiedPurchase = purchaseHistory.hasPurchased(productId, author);
        Review review = reviewRepository.save(new Review(productId, author, rating, comment));
        return new ReviewReceipt(review.getId(), verifiedPurchase);
    }

    public List<Review> listReviews(Long productId) {
        return reviewRepository.findByProductId(productId);
    }
}
