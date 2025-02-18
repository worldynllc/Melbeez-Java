package com.mlbeez.feeder.controller;

import com.mlbeez.feeder.model.Feed;
import com.mlbeez.feeder.repository.FeedRepository;
import com.mlbeez.feeder.service.FeedService;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;



@Controller
@RequestMapping("/feed")
public class FeedMediaController {

    @Autowired
    FeedService feedService;

    public static final Logger logger= LoggerFactory.getLogger(FeedMediaController.class);

    @GetMapping("")
    public String viewHomePage() {
        return "upload";
    }
    @Operation(summary = "Upload a new Feed")
    @PostMapping(value = "/upload", consumes = {"multipart/form-data"})
    @PreAuthorize("hasAnyRole('ADMIN','USER','SUPERADMIN')")
    public ResponseEntity<String> handleUpload(Feed feed, @RequestPart("file")MultipartFile file) {
        logger.info("Request to Upload Feed {}", feed);
        return feedService.createFeed(feed, file);
    }
}