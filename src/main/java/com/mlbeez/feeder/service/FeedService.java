package com.mlbeez.feeder.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.mlbeez.feeder.controller.FeedController;
import com.mlbeez.feeder.model.Feed;
import com.mlbeez.feeder.service.exception.DataNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.mlbeez.feeder.repository.FeedRepository;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;


@Service
public class FeedService {

    @Autowired
    FeedRepository feedRepository;

    @Autowired
    MediaStoreService service;

    public ResponseEntity<String> createFeed(Feed feed, MultipartFile multipart) {
        String fileName = multipart.getOriginalFilename();
        String[] partStrings = fileName.split("\\.");
        String file = partStrings[0];
        String extension = (partStrings.length > 1) ? partStrings[1] : "";
        file = UUID.randomUUID().toString() + "." + extension;
        String message = "";

        try {
            String s = service.getMediaStoreService().uploadFile(file, multipart.getInputStream());
            if (multipart.isEmpty()) {
                feed.setLink("");
                feed.getCreatedAt();
                feed.setImg("");
                feedRepository.save(feed);

                message = "Your file has been uploaded successfully! here ";
            } else {
                feed.setLink(s);
                feed.setImg(file);
                feed.getCreatedAt();

                feedRepository.save(feed);

                message = "Your file has been uploaded successfully! here " + s;

            }
        } catch (Exception ex) {
            message = "Error uploading file: " + ex.getMessage();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }


    public List<Feed> getAllFeeds() {
        List<Feed> feeds = feedRepository.findAll();
        if (CollectionUtils.isEmpty(feeds)) {
            throw new DataNotFoundException("No feed data in the DataBase", "put the data in database");
        }
        for (Feed feed : feeds) {
            Link selfLink = WebMvcLinkBuilder.linkTo(FeedController.class).withSelfRel();
            feed.add(selfLink);
        }
        Link link = WebMvcLinkBuilder.linkTo(FeedController.class).withSelfRel();
        CollectionModel<Feed> result = CollectionModel.of(feeds, link);
        return feeds;
    }

    public Optional<Feed> getFeedById(Long id) {
        return feedRepository.findById(id);
    }

    public void deleteFeedById(Long id) {
        Optional<Feed> feedOptional = feedRepository.findById(id);

        if (feedOptional.isPresent()) {
            Feed feed = feedOptional.get();
            if (!feed.getImg().isEmpty()) {
                service.getMediaStoreService().deleteFile(feed.getImg());
            }
            feedRepository.deleteById(id);
        }
    }
    public List<String> getImage() {
        return service.getMediaStoreService().getAllImageFileKeys();
    }

}
