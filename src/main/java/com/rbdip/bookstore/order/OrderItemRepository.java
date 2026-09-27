package com.rbdip.bookstore.order;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("select i from OrderItem i join fetch i.product where i.orderId in :orderIds")
    List<OrderItem> findWithProductByOrderIdIn(@Param("orderIds") Collection<Long> orderIds);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM order_items oi
                JOIN orders o ON o.id = oi.order_id
                JOIN customers c ON c.id = o.customer_id
                WHERE oi.product_id = :productId
                  AND c.first_name || CASE WHEN c.last_name IS NULL THEN '' ELSE ' ' || c.last_name END
                      = :customerName
            )
            """, nativeQuery = true)
    boolean existsPurchase(@Param("productId") Long productId, @Param("customerName") String customerName);
}
