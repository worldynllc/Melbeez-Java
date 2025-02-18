package com.mlbeez.feeder.repository;

import com.mlbeez.feeder.model.UserResponseBaseModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AspNetUserRepository extends JpaRepository<UserResponseBaseModel, String> {

    UserResponseBaseModel findByUsername(String userName);

    @Query("SELECT u FROM UserResponseBaseModel u LEFT JOIN FETCH u.userAddresses WHERE u.id = :userId")
    UserResponseBaseModel findUserWithAddresses(@Param("userId") String userId);
}
