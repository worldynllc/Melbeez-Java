package com.mlbeez.feeder.repository;

import com.mlbeez.feeder.model.AspNetUserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AspNetUserRoleRepository extends JpaRepository<AspNetUserRole,String> {
    Optional<AspNetUserRole> findByUserId(String id);
}
