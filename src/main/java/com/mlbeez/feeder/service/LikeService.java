package com.mlbeez.feeder.service;

import com.mlbeez.feeder.config.LikeLockManager;
import com.mlbeez.feeder.model.Feed;
import com.mlbeez.feeder.model.FeedResponse;
import com.mlbeez.feeder.model.Like;
import com.mlbeez.feeder.model.LikeResponse;
import com.mlbeez.feeder.repository.FeedRepository;
import com.mlbeez.feeder.repository.LikeRepository;
import com.mlbeez.feeder.service.exception.DataNotFoundException;
import com.mlbeez.feeder.service.exception.RateLimitException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LikeService {

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private RateLimiterService rateLimiterService;

    @Autowired
    private LikeLockManager likeLockManager;

    private final Logger logger = LoggerFactory.getLogger(LikeService.class);

    @Transactional
    public void addLike(Long feedId, String userId, String userName) {
        Object lock = likeLockManager.getLock(userId, feedId);
        synchronized (lock) {
            try {
                if (userId.equals("null") || userId.equals("undefined")) {
                    throw new DataNotFoundException("");
                }
                if (!rateLimiterService.tryConsume(userId, feedId)) {
                    throw new RateLimitException("Too many requests. Please wait.");
                }

                Feed feed = feedRepository.findById(feedId)
                        .orElseThrow(() -> new DataNotFoundException("Feed not found with id: " + feedId));

                Optional<Like> existingLike = likeRepository.findByFeedIdAndUserId(feedId, userId);

                if (existingLike.isPresent()) {
                    unlikeFeed(feed, existingLike);
                } else {
                    likeFeed(feed, userId, userName);
                }
            }
            catch (RateLimitException e) {
                logger.warn("Rate limit hit for user {} on feed {}", userId, feedId);
                throw e;
            }
            finally {
                likeLockManager.releaseLock(userId, feedId);
            }
        }
    }


    public void likeFeed(Feed feed, String userId, String userName) {
        try {
            Like newLike = new Like();
            newLike.setFeed(feed);
            newLike.setUserId(userId);
            newLike.setUserName(userName);
            likeRepository.saveAndFlush(newLike);

            feedRepository.incrementLikes(feed.getId());

        } catch (DataIntegrityViolationException e) {
            logger.warn("Duplicate like caught at DB level:");
        }
    }

    public void unlikeFeed(Feed feed, Optional<Like> like) {
        try {
            if (like.isPresent()) {
                likeRepository.deleteById(like.get().getId());
                feedRepository.decrementLikes(feed.getId());

            }
        } catch (DataIntegrityViolationException e) {
            logger.warn("User already liked feed {}", feed.getId());
        }
    }

    public List<Like> getAllLikes() {
        return likeRepository.findAll();
    }

    public List<LikeResponse> getLikes(Long feedId) {
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new DataNotFoundException("Feed not found with id: " + feedId));

        return likeRepository.findByFeed(feed).stream().map(like -> {
            Feed f = like.getFeed();
            FeedResponse feedResponse = new FeedResponse(f.getId(), f.getUserId());
            return new LikeResponse(like.getUserName(), like.getUserId(), feedResponse);
        }).collect(Collectors.toList());
    }
}
