package com.mlbeez.feeder.repository;

import com.mlbeez.feeder.model.Feed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface FeedRepository extends JpaRepository<Feed,Long> {
    Optional<Feed> findById(@Param("id") Long id);


    @Modifying
    @Transactional
    @Query("UPDATE Feed f SET f.likesCount = f.likesCount + 1 WHERE f.id = :feedId")
    void incrementLikes(@Param("feedId") Long feedId);

    @Modifying
    @Transactional
    @Query("UPDATE Feed f SET f.likesCount = f.likesCount - 1 WHERE f.id = :feedId AND f.likesCount > 0")
    void decrementLikes(@Param("feedId") Long feedId);

}
