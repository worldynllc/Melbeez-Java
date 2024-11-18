package com.mlbeez.feeder.repository;

import com.mlbeez.feeder.model.InsurancePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;



@Repository
public interface InsurancePaymentRepository extends JpaRepository<InsurancePayment,Long> {

    

    List<InsurancePayment> findAllByProductId(String productId);

    List<InsurancePayment>  findByUserId(String userId);

    InsurancePayment findByCustomerAndSubscriptionId(String customerId, String subscriptionId);
}