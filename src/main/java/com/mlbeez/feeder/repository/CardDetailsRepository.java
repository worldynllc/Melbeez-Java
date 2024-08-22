package com.mlbeez.feeder.repository;

import com.mlbeez.feeder.model.CardDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CardDetailsRepository extends JpaRepository<CardDetails,Long> {
}
