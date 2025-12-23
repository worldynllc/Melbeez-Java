package com.mlbeez.feeder.controller;
import com.mlbeez.feeder.model.LikeResponse;
import com.mlbeez.feeder.service.LikeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/like")
public class LikeController {

    private static final Logger logger= LoggerFactory.getLogger(LikeController.class);

    private final LikeService likeService;

    private final ConcurrentHashMap<String, Long> userLastRequest = new ConcurrentHashMap<>();
    private final long MIN_REQUEST_INTERVAL_MS = 2000;

    public LikeController(LikeService likeService) {
        this.likeService = likeService;
    }

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
            likeService.addLike(feedId, userId, userName);

            long duration = System.currentTimeMillis() - startTime;
            logger.info("Like operation completed in {}ms", duration);

            return ResponseEntity.ok("Success");
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
        List<LikeResponse> likes = likeService.getAllLikes();

//        List<LikeResponse> likeResponses = likes.stream().map(like->{
//            Feed feed = like.getFeed();
//            FeedResponse feedResponse = new FeedResponse(feed.getId(), feed.getUserId());
//            return new LikeResponse(like.getUserName(), like.getUserId(),feedResponse);}).collect(Collectors.toList());
            return ResponseEntity.ok(likes);
    }
}
