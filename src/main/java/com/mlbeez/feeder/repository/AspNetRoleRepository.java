package com.mlbeez.feeder.repository;

import com.mlbeez.feeder.model.AspNetRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AspNetRoleRepository extends JpaRepository<AspNetRole,String> {
}
