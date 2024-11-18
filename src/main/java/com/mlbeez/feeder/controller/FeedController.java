package com.mlbeez.feeder.controller;

import com.mlbeez.feeder.model.Feed;
import com.mlbeez.feeder.service.FeedService;
import com.mlbeez.feeder.service.MediaStoreService;
import com.mlbeez.feeder.service.awss3.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;


@RestController

@Tag(name = "Feed", description = "Everything about your Feeds")
public class FeedController {

    @Autowired
    S3Service Services;

    @Autowired
    MediaStoreService service;

    @Autowired
    FeedService feedService;

    public static final Logger logger = LoggerFactory.getLogger(FeedController.class);


    public FeedController(FeedService feedService) {
        this.feedService = feedService;
    }

    @Operation(summary = "Delete a feed by ID")
    @DeleteMapping("feeds/id/{id}")
    public ResponseEntity<?> deleteFeedById(@PathVariable("id") Long id) {
        logger.info("Request to Delete Feed {}", id);
        feedService.deleteFeedById(id);
        return ResponseEntity.ok().build();
    }


    @Operation(summary = "Get all feeds")
    @GetMapping("/feeds")
    public List<Feed> getAllFeeds() {
        logger.info("Request to GetAllFeeds");
        return feedService.getAllFeeds();
    }

    @Operation(summary = "Get file location by ID")
    @GetMapping("/file/{id}")
    public String handleGet(@PathVariable String id) {
        logger.info("Request to Get file {}", id);
        return service.getMediaStoreService().getFileLocation(id + ".jpeg");
    }


//  To use all images get from s3

    @Operation(summary = "Get all image file keys")
    @GetMapping("/image/all")
    public List<String> getAllImageFileKeys() {
        return feedService.getImage();
    }
}
