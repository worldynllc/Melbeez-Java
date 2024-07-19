package com.mlbeez.feeder.repository;

import com.mlbeez.feeder.model.Warranty;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarrantyRepository extends JpaRepository<Warranty, Long> {
    List<Warranty> findByStatus(String status);
}
