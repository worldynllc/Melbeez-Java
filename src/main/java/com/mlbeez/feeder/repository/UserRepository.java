package com.mlbeez.feeder.repository;
import com.mlbeez.feeder.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByUserId(String userId);

    User findByCustomerId(String customer);

}
