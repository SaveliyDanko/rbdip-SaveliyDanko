package com.rbdip.bookstore.order;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByFullNameAndAddressAndPhone(String fullName, String address, String phone);
}
