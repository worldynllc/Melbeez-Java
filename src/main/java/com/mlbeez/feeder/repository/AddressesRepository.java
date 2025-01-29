package com.mlbeez.feeder.repository;

import com.mlbeez.feeder.model.UserAddressesModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface AddressesRepository extends JpaRepository<UserAddressesModel,Long> {
  UserAddressesModel findByCreatedBy(String id);
}
