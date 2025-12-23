package com.mlbeez.feeder.controller;

import com.mlbeez.feeder.model.Feed;
import com.mlbeez.feeder.service.FeedService;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;



@Controller
@RequestMapping("/feed")
public class FeedMediaController {

    private final FeedService feedService;

    public static final Logger logger= LoggerFactory.getLogger(FeedMediaController.class);

    public FeedMediaController(FeedService feedService) {
        this.feedService = feedService;
    }

    @Operation(summary = "Upload a new Feed")
    @PostMapping(value = "/upload", consumes = {"multipart/form-data"})
    @PreAuthorize("hasAnyRole('ADMIN','USER','SUPERADMIN')")
    public ResponseEntity<String> handleUpload(Feed feed, @RequestPart("file")MultipartFile file) {
        logger.info("Request to Upload Feed {}", feed);
        return feedService.createFeed(feed, file);
    }
}