package com.mlbeez.feeder.repository;

import com.mlbeez.feeder.model.InsurancePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;


@Repository
public interface InsurancePaymentRepository extends JpaRepository<InsurancePayment,Long> {


    List<InsurancePayment>  findByUserId(String userId);

    InsurancePayment findByCustomerAndSubscriptionId(String customerId, String subscriptionId);

    Optional<InsurancePayment> findByUserIdAndWarrantyId(String userId, String warrantyId);

    Optional<InsurancePayment> findBySubscriptionIdAndUserIdAndWarrantyId(String subscriptionId, String userId,
                                                                String warrantyId);
}