package com.rbdip.bookstore.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "customer_address")
    private String address;

    @Column(name = "customer_phone")
    private String phone;

    protected Customer() {
        // for JPA
    }

    public Customer(String fullName, String address, String phone) {
        int separator = fullName.indexOf(' ');
        this.firstName = separator < 0 ? fullName : fullName.substring(0, separator);
        this.lastName = separator < 0 ? null : fullName.substring(separator + 1);
        this.address = address;
        this.phone = phone;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return lastName == null ? firstName : firstName + " " + lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getAddress() {
        return address;
    }

    public String getPhone() {
        return phone;
    }
}
