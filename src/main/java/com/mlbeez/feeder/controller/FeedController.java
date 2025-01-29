package com.mlbeez.feeder.controller;

import com.mlbeez.feeder.model.Feed;
import com.mlbeez.feeder.service.FeedService;
import com.mlbeez.feeder.service.MediaStoreService;
import com.mlbeez.feeder.service.exception.DataNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;


@RestController
@RequestMapping("/feed")
@Tag(name = "Feed", description = "Everything about your Feeds")
public class FeedController {

    @Autowired
    MediaStoreService service;

    @Autowired
    FeedService feedService;

    public static final Logger logger = LoggerFactory.getLogger(FeedController.class);


    public FeedController(FeedService feedService) {
        this.feedService = feedService;
    }

    @Operation(summary = "Delete a feed by ID")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER','SUPERADMIN')")
    public ResponseEntity<?> deleteFeedById(@PathVariable("id") Long id) {
        logger.info("Request to Delete Feed {}", id);
        feedService.deleteFeedById(id);
        return ResponseEntity.ok().build();
    }


    @Operation(summary = "Get all feeds")
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN','USER','SUPERADMIN')")
    public List<Feed> getAllFeeds(@RequestParam int page,
                                  @RequestParam int size) {
        logger.info("Request to GetAllFeeds");
        return feedService.getAllFeeds(page,size);
    }

    @GetMapping("/{feedId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER','SUPERADMIN')")
    public ResponseEntity<Feed> getById(@PathVariable Long feedId) {
        Feed feed = feedService.getFeedById(feedId)
                .orElseThrow(() -> new DataNotFoundException("Feed not found with id: " + feedId));
        logger.info("Request to GetFeedBy {}", feedId);
        return ResponseEntity.ok(feed);
    }

//    @GetMapping("/all/feeds")
//    @PreAuthorize("hasAnyRole('ADMIN','USER','SUPERADMIN')")
//    public List<Feed> getAllFeed() {
//        logger.info("Request to GetAllFeeds");
//        return feedRepository.findAll();
//    }

    @Operation(summary = "Get file location by ID")
    @GetMapping("/file/{id}")
    public String handleGet(@PathVariable String id) {
        logger.info("Request to Get file {}", id);
        return service.getMediaStoreService().getFileLocation(id + ".jpeg");
    }

    @Operation(summary = "Get all image file keys")
    @GetMapping("/image/all")
    public List<String> getAllImageFileKeys() {
        return feedService.getImage();
    }
}
