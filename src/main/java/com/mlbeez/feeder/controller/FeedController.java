package com.mlbeez.feeder.controller;

import com.mlbeez.feeder.model.Feed;
import com.mlbeez.feeder.service.FeedService;
import com.mlbeez.feeder.service.MediaStoreService;
import com.mlbeez.feeder.service.awss3.S3Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;


@RestController

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

    @DeleteMapping("/feeds/id/{id}")
    public ResponseEntity<?> deleteFeedById(@PathVariable("id") Long id) {
        logger.debug("Request to Delete Feed {}", id);
        feedService.deleteFeedById(id);
        return ResponseEntity.ok().build();
    }


    @GetMapping("/feeds")
    public List<Feed> getAllFeeds() {
        logger.debug("Request to GetAllFeeds");
        return feedService.getAllFeeds();
    }

    @GetMapping("/file/{id}")
    public String handleGet(@PathVariable String id) {
        logger.debug("Request to Get file {}", id);
        return service.getMediaStoreService().getFileLocation(id + ".jpeg");
    }


//  To use all images get from s3

    @GetMapping("image/all")
    public List<String> getAllImageFileKeys() {
        return feedService.getImage();
    }
}
