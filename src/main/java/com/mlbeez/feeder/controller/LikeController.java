package com.mlbeez.feeder.controller;
import com.mlbeez.feeder.model.Feed;
import com.mlbeez.feeder.model.FeedResponse;
import com.mlbeez.feeder.model.Like;
import com.mlbeez.feeder.model.LikeResponse;
import com.mlbeez.feeder.service.LikeService;
import com.mlbeez.feeder.service.exception.DataNotFoundException;
import com.mlbeez.feeder.service.exception.RateLimitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/like")
public class LikeController {

    private static final Logger logger= LoggerFactory.getLogger(LikeController.class);

    @Autowired
    private LikeService likeService;

    private final ConcurrentHashMap<String, Long> userLastRequest = new ConcurrentHashMap<>();
    private final long MIN_REQUEST_INTERVAL_MS = 2000;

    @PostMapping("/{feedId}/{userId}/{userName}")
    @PreAuthorize("hasAnyRole('ADMIN','USER','SUPERADMIN')")
    public ResponseEntity<String> addLike(@PathVariable("feedId") Long feedId,
                                          @PathVariable("userId") String userId,
                                          @PathVariable("userName") String userName) {
        long startTime = System.currentTimeMillis();
        logger.info("Request to like/unlike for Feed {} by User {}", feedId, userId);

        Long lastRequestTime = userLastRequest.get(userId);
        if (lastRequestTime != null && (startTime - lastRequestTime) < MIN_REQUEST_INTERVAL_MS) {
            long waitTime = MIN_REQUEST_INTERVAL_MS - (startTime - lastRequestTime);
            logger.warn("Rate limit exceeded for user {} on feed {} - must wait {}ms", userId, feedId, waitTime);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", String.valueOf((waitTime / 1000) + 1))
                    .body("Please wait " + ((waitTime / 1000) + 1) + " seconds before making another request.");
        }

        userLastRequest.put(userId, startTime);

        try {
            likeService.addLike(feedId, userId, userName);

            long duration = System.currentTimeMillis() - startTime;
            logger.info("Like operation completed in {}ms", duration);

            return ResponseEntity.ok("Success!");

        } catch (RateLimitException e) {
            logger.warn("Rate limit exceeded for user {} on feed {}: {}", userId, feedId, e.getMessage());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", "5")
                    .body("Too many requests. Please wait.");

        } catch (DataNotFoundException e) {
            logger.warn("Data not found for feed {} or user {}: {}", feedId, userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Resource not found");

        } catch (Exception e) {
            logger.error("Unexpected error processing like for feed {} by user {}", feedId, userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal server error");
        }
    }

    @Scheduled(fixedRate = 600000)
    public void cleanupOldRequests() {
        long cutoffTime = System.currentTimeMillis() - 600000;
        userLastRequest.entrySet().removeIf(entry -> entry.getValue() < cutoffTime);
        logger.debug("Cleaned up old request times. Current size: {}", userLastRequest.size());
    }


    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN','USER','SUPERADMIN')")
    public ResponseEntity<List<LikeResponse>> getAllLikes(){
        logger.info("Request to get All likes from like table");
        List<Like> likes = likeService.getAllLikes();

        List<LikeResponse> likeResponses = likes.stream().map(like->{
            Feed feed = like.getFeed();
            FeedResponse feedResponse = new FeedResponse(feed.getId(), feed.getUserId());
            return new LikeResponse(like.getUserName(), like.getUserId(),feedResponse);}).collect(Collectors.toList());
            return ResponseEntity.ok(likeResponses);
    }
}
