package com.mlbeez.feeder.repository;

import com.mlbeez.feeder.model.Comment;
import com.mlbeez.feeder.model.Feed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment,Long> {

    List<Comment> findByFeed(Optional<Feed> feedId);

    List<Comment> findAllByFeed(Feed feed);

    boolean existsByFeedAndUserId(Feed feed, String userId);

    Optional<Comment> findByIdAndFeed(Long commentId, Feed feedId);
}
