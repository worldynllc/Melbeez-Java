package com.mlbeez.feeder.service;

import com.mlbeez.feeder.config.LikeLockManager;
import com.mlbeez.feeder.model.Feed;
import com.mlbeez.feeder.model.FeedResponse;
import com.mlbeez.feeder.model.Like;
import com.mlbeez.feeder.model.LikeResponse;
import com.mlbeez.feeder.repository.FeedRepository;
import com.mlbeez.feeder.repository.LikeRepository;
import com.mlbeez.feeder.service.exception.*;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LikeService {

    private final LikeRepository likeRepository;

    private final FeedRepository feedRepository;

    private final RateLimiterService rateLimiterService;

    private final LikeLockManager likeLockManager;

    private final Logger logger = LoggerFactory.getLogger(LikeService.class);

    public LikeService(LikeRepository likeRepository, FeedRepository feedRepository, RateLimiterService rateLimiterService, LikeLockManager likeLockManager) {
        this.likeRepository = likeRepository;
        this.feedRepository = feedRepository;
        this.rateLimiterService = rateLimiterService;
        this.likeLockManager = likeLockManager;
    }

    @Transactional
    public void addLike(Long feedId, String userId, String userName) {
        Object lock = likeLockManager.getLock(userId, feedId);
        synchronized (lock) {
            try {
                if (userId.equals("null") || userId.equals("undefined")) {
                    logger.error("user id is missing!");
                    throw new UserIdRequiredException("user id is missing!");
                }
                if (!rateLimiterService.tryConsume(userId, feedId)) {
                    throw new RateLimitException("Too many requests. Please wait.");
                }
                Feed feed = feedRepository.findById(feedId)
                        .orElseThrow(() ->{
                            logger.error("Feed not found with id: {}",feedId);
                            return new FeedNotFoundException("Feed not found with id");
                        });

                Optional<Like> existingLike = likeRepository.findByFeedIdAndUserId(feedId, userId);

                if (existingLike.isPresent()) {
                    unlikeFeed(feed, existingLike);
                } else {
                    likeFeed(feed, userId, userName);
                }
            } catch (Exception e) {
                logger.error("Unexpected error processing like for feed {} by user {}", feedId, userId, e);
                 ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Internal server error");
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
            logger.error("Duplicate like caught at DB level:");
        }
    }

    public void unlikeFeed(Feed feed, Optional<Like> like) {
        try {
            if (like.isPresent()) {
                likeRepository.deleteById(like.get().getId());
                feedRepository.decrementLikes(feed.getId());

            }
        } catch (DataIntegrityViolationException e) {
            logger.error("User already liked feed {}", feed.getId());
        }
    }

    public List<LikeResponse> getAllLikes() {
        try {
        List<Like> likes = likeRepository.findAll();
        if(likes.isEmpty()){
            logger.error("user likes not found!");
            throw new UserLikesNotFoundException("user likes not found!");
        }
        return likes.stream().map(like->{
        Feed feed = like.getFeed();
        FeedResponse feedResponse = new FeedResponse(feed.getId(), feed.getUserId());
        return new LikeResponse(like.getUserName(), like.getUserId(),feedResponse);}).collect(Collectors.toList());
        }catch (Exception ex){
            logger.error(ex.getMessage());
            throw new InternalServerException(ex.getMessage());
        }
    }

    public List<LikeResponse> getLikes(Long feedId) {
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() ->{
                    logger.error("feed not found with id: {}",feedId);
                    return new FeedNotFoundException("Feed not found with id");
                });

        return likeRepository.findByFeed(feed).stream().map(like -> {
            Feed f = like.getFeed();
            FeedResponse feedResponse = new FeedResponse(f.getId(), f.getUserId());
            return new LikeResponse(like.getUserName(), like.getUserId(), feedResponse);
        }).collect(Collectors.toList());
    }
}
