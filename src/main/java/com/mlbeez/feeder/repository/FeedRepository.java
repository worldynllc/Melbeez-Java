package com.mlbeez.feeder.repository;

import com.mlbeez.feeder.model.Feed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FeedRepository extends JpaRepository<Feed,Long> {
    Optional<Feed> findById(@Param("id") Long id);

}
