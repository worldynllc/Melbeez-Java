package com.mlbeez.feeder.repository;

import com.mlbeez.feeder.model.InsurancePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;



@Repository
public interface InsurancePaymentRepository extends JpaRepository<InsurancePayment,Long> {

    
    InsurancePayment findByCustomer(String customer);
    List<InsurancePayment> findAllByProductId(String productId);

}