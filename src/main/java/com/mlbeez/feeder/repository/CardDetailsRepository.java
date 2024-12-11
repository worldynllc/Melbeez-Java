package com.mlbeez.feeder.repository;

import com.mlbeez.feeder.model.CardDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CardDetailsRepository extends JpaRepository<CardDetails,Long> {
    Optional<CardDetails> findByUserId(String userId);
}
