package com.mlbeez.feeder.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.mlbeez.feeder.controller.FeedController;
import com.mlbeez.feeder.model.Comment;
import com.mlbeez.feeder.model.Feed;
import com.mlbeez.feeder.repository.CommentRepository;
import com.mlbeez.feeder.repository.LikeRepository;
import com.mlbeez.feeder.service.exception.DataNotFoundException;
import com.mlbeez.feeder.service.exception.IllegalArgumentPassedException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.mlbeez.feeder.repository.FeedRepository;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;


@Service
public class FeedService{

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private MediaStoreService service;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private LikeRepository likeRepository;


    public ResponseEntity<String> createFeed(Feed feed, MultipartFile multipart) {
        String fileName = multipart.getOriginalFilename();
        String[] partStrings = fileName.split("\\.");
        String file = partStrings[0];
        String extension = (partStrings.length > 1) ? partStrings[1] : "";
        file = UUID.randomUUID().toString() + "." + extension;
        String message = "";
        String folderName ="feeds";

        try {
            String s = service.getMediaStoreService().uploadFile(file, multipart.getInputStream(),folderName);
            if (multipart.isEmpty()) {
                feed.setLink("");
                feed.setImg("");
                message = "Your file has been uploaded successfully! here ";
            } else {
                feed.setLink(s);
                feed.setImg(file);
                message = "Your file has been uploaded successfully! here " + s;

            }
            feed.setLikesCount(0);
            feed.setCommentCount(0);
            feedRepository.save(feed);
        } catch (Exception ex) {
            message = "Error uploading file: " + ex.getMessage();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }



    public List<Feed> getAllFeeds(int page,int size) {

        if(size==0){
            throw new IllegalArgumentPassedException("Page size must not be less than one");
        }
        Pageable pageable= PageRequest.of(page,size);
        Page<Feed> feedPage=feedRepository.findAll(pageable);
        List<Feed>feeds=feedPage.getContent();

        if (CollectionUtils.isEmpty(feeds)) {
            throw new DataNotFoundException("No feed data in the DataBase");
        }
        for (Feed feed : feeds) {
            Link selfLink =
                    WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(FeedController.class)
                            .getFeedById(feed.getId())).withRel("share");
            feed.add(selfLink);
        }
        return feeds;
    }


    public Optional<Feed> getFeedById(Long id) {
        Optional<Feed> feed=feedRepository.findById(id);

        if (feed.isPresent()){
            Feed feeds= feed.get();
            Link selfLink =
                    WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(FeedController.class).getFeedById(feeds.getId()))
                            .withRel("share");
            feeds.add(selfLink);
        }
        return feed;
    }

    @Transactional
    public void deleteFeedById(Long id) {
        Optional<Feed> feedOptional = feedRepository.findById(id);

        if (feedOptional.isPresent()) {
            Feed feed = feedOptional.get();
            if (feed.getImg()!=null &&  !feed.getImg().isEmpty()) {
                service.getMediaStoreService().deleteFile(feed.getImg());
            }
            List<Comment>comments=commentRepository.findAllByFeed(feed);
            if(!comments.isEmpty()){

                commentRepository.deleteAll(comments);
            }
            boolean like=likeRepository.existsByFeed(feed);
            if(like){
                likeRepository.deleteByFeed(feed);
            }
            feedRepository.deleteById(id);
        }
    }
    public List<String> getImage() {
        return service.getMediaStoreService().getAllImageFileKeys();
    }

}
