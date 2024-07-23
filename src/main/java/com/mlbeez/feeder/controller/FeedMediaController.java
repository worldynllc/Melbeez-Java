package com.mlbeez.feeder.controller;

import com.mlbeez.feeder.model.Feed;
import com.mlbeez.feeder.repository.FeedRepository;
import com.mlbeez.feeder.service.FeedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.ui.Model;


@Controller
public class FeedMediaController {

    @Autowired
    FeedService feedService;

    @Autowired
    private FeedRepository feedRepository;

    public static final Logger logger= LoggerFactory.getLogger(FeedMediaController.class);

    @GetMapping("")
    public String viewHomePage() {
        return "upload";
    }

    @PostMapping("/upload")
    public ResponseEntity<String> handleUpload(Model model,@ModelAttribute Feed feed, @RequestParam("file") MultipartFile multipart) {
        logger.debug("Request to Upload Feed {}",feed);
        return feedService.createFeed(feed,multipart);

    }

}