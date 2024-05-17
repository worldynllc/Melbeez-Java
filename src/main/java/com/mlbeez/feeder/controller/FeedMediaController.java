package com.mlbeez.feeder.controller;

import com.mlbeez.feeder.model.Feed;
import com.mlbeez.feeder.repository.FeedRepository;

import com.mlbeez.feeder.service.FeedService;
import com.mlbeez.feeder.service.MediaStoreService;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;

@Controller
public class FeedMediaController {

    @Autowired
    MediaStoreService service;

    @Autowired
    FeedService feedService;

    @Autowired
    private FeedRepository feedRepository;

    @GetMapping("")
    public String viewHomePage() {
        return "upload";
    }

    @PostMapping("/upload")
    public ResponseEntity<String> handleUpload(Model model, @ModelAttribute Feed feed, @RequestParam("file") MultipartFile multipart) {
        String fileName = multipart.getOriginalFilename();
        String[] partStrings = fileName.split("\\.");
        String file = partStrings[0];
        String extension = (partStrings.length > 1) ? partStrings[1] : "";
        fileName = UUID.randomUUID().toString() + "." + extension;
        String message = "";

        try {

            String s = service.getMediaStoreService().uploadFile(fileName, multipart.getInputStream());
            if (multipart.isEmpty()) {
                feed.setLink("");
                feed.getCreatedAt();
                feed.setImg("");
                feedRepository.save(feed);

                message = "Your file has been uploaded successfully! here ";
            } else {
                feed.setLink(s);
                feed.setImg(fileName);
                feed.getCreatedAt();

                feedRepository.save(feed);

                message = "Your file has been uploaded successfully! here " + s;

            }

        } catch (Exception ex) {
            message = "Error uploading file: " + ex.getMessage();
        }


        return ResponseEntity.status(HttpStatus.OK).body(message);
    }

}