package com.mlbeez.feeder.repository;

import com.mlbeez.feeder.model.Customers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customers,Long> {

    Optional<Customers> findByUserId(String userId);

    Customers findByCustomerId(String customerId);
}
