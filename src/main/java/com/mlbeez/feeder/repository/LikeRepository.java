package com.mlbeez.feeder.repository;

import com.mlbeez.feeder.model.Feed;
import com.mlbeez.feeder.model.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like,Long> {

    boolean existsByFeed(Feed feed);

    void deleteByFeed(Feed feed);

    List<Like> findByFeed(Optional<Feed> feed);

    Optional<Like> findByFeedAndUserId(Feed feed, String userId);
}
