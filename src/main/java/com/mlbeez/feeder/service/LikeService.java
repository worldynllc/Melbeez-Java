package com.mlbeez.feeder.service;

import com.mlbeez.feeder.model.Feed;
import com.mlbeez.feeder.model.Like;
import com.mlbeez.feeder.model.LikeResponse;
import com.mlbeez.feeder.repository.FeedRepository;
import com.mlbeez.feeder.repository.LikeRepository;
import com.mlbeez.feeder.service.exception.ConstraintViolationException;
import com.mlbeez.feeder.service.exception.DataNotFoundException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    private final Logger logger = LoggerFactory.getLogger(LikeService.class);

    @Transactional
    public synchronized Feed addLike(Long feedId, String userId, String userName) {
        Feed response = null;
        Feed likeResponse;
        try {

            if (userId.equals("null") || userId.equals("undefined")) {
                throw new DataNotFoundException("");
            }

            Feed feed = feedRepository.findById(feedId)
                    .orElseThrow(() -> new DataNotFoundException("Feed not found with id: " + feedId));


            Optional<Like> existingLike = likeRepository.findByFeedAndUserId(feed, userId);

            if (existingLike.isPresent()) {
                likeResponse = unlikeFeed(feed, existingLike);
            } else {
                likeResponse = likeFeed(feed, userId, userName);
            }
            response = likeResponse;
        } catch (ConstraintViolationException e) {
            logger.error("Duplicate like detected: {}", e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return response;
    }

    public Feed likeFeed(Feed feed, String userId, String userName) {
        Like newLike = new Like();
        newLike.setFeed(feed);
        newLike.setUserId(userId);
        newLike.setUserName(userName);
        likeRepository.save(newLike);
        feed.setLikesCount(feed.getLikesCount() + 1);
        return feedRepository.save(feed);
    }

    public Feed unlikeFeed(Feed feed, Optional<Like> like) {
        if (like.isPresent()) {
            likeRepository.deleteById(like.get().getId());
            feed.setLikesCount(feed.getLikesCount() - 1);
        }
        return feedRepository.save(feed);
    }

    public List<Like> getAllLikes() {
        return likeRepository.findAll();
    }

    public List<LikeResponse> getLikes(Long feedId) {
        Optional<Feed> feed = feedRepository.findById(feedId);
        return likeRepository.findByFeed(feed).stream().map(like -> new LikeResponse(like.getUserName(),
                like.getUserId(), like.getFeed())).collect(Collectors.toList());
    }

}
