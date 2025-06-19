package com.mlbeez.feeder.repository;

import com.mlbeez.feeder.model.Feed;
import com.mlbeez.feeder.model.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;



import java.util.List;
import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like,Long> {

    boolean existsByFeed(Feed feed);

    void deleteByFeed(Feed feed);

    List<Like> findByFeed(Feed feed);

    @Query("SELECT l FROM Like l WHERE l.feed.id = :feedId AND l.userId = :userId")
    Optional<Like> findByFeedIdAndUserId(@Param("feedId") Long feedId, @Param("userId") String userId);

}
