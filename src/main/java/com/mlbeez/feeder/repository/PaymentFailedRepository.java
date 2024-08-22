package com.mlbeez.feeder.repository;

import com.mlbeez.feeder.model.PaymentFailed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentFailedRepository extends JpaRepository<PaymentFailed,Long> {
}
