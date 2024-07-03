package com.mlbeez.feeder.repository;

import com.mlbeez.feeder.model.Warrenty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WarrentyRepository extends JpaRepository<Warrenty,Long> {
}
