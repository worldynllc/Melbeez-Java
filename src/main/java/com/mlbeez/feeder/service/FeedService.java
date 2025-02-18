package com.mlbeez.feeder.service;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import com.mlbeez.feeder.controller.FeedController;
import com.mlbeez.feeder.model.*;
import com.mlbeez.feeder.repository.*;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;


@Service
public class FeedService {

    @Autowired
    private AspNetUserRoleRepository aspNetUserRoleRepository;

    @Autowired
    private AspNetRoleRepository aspNetRoleRepository;


    private final MediaStoreService service;
    private final FeedRepository feedRepository;
    private final CommentRepository commentRepository;
    private final LikeRepository likeRepository;


    private final Supplier<String> uuidGenerator;

    public FeedService(MediaStoreService mediaStoreService, FeedRepository feedRepository, CommentRepository commentRepository, LikeRepository likeRepository, Supplier<String> uuidGenerator) {
        this.service = mediaStoreService;
        this.feedRepository = feedRepository;
        this.commentRepository = commentRepository;
        this.likeRepository = likeRepository;
        this.uuidGenerator = uuidGenerator;
    }

    public ResponseEntity<String> createFeed(Feed feed, MultipartFile multipart) {
        String fileName = multipart.getOriginalFilename();
        assert fileName != null;
        String[] partStrings = fileName.split("\\.");
        String file = partStrings[0];
        String extension = (partStrings.length > 1) ? partStrings[1] : "";
        file = generateUniqueFileName() + "." + extension;
        String message = "";
        String folderName = "feeds";

        try {
            String s = service.getMediaStoreService().uploadFile(file, multipart.getInputStream(), folderName);
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

    public String generateUniqueFileName() {
        return uuidGenerator.get();
    }

    public List<Feed> getAllFeeds(int page, int size) {

        if (size == 0) {
            throw new IllegalArgumentPassedException("Page size must not be less than one");
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<Feed> feedPage = feedRepository.findAll(pageable);
        List<Feed> feeds = feedPage.getContent();

        if (CollectionUtils.isEmpty(feeds)) {
            throw new DataNotFoundException("No feed data in the DataBase");
        }
        for (Feed feed : feeds) {
            Link selfLink =
                    WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(FeedController.class)
                            .getById(feed.getId())).withRel("share");
            feed.add(selfLink);
        }
        return feeds;
    }


    public Optional<Feed> getFeedById(Long id) {
        Optional<Feed> feed = feedRepository.findById(id);
        if (feed.isPresent()) {
            Feed feeds = feed.get();
            Link selfLink =
                    WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(FeedController.class).getById(feeds.getId()))
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

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserName = authentication.getName();
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String userRole = userDetails.getAuthorities().toString();

            String[] partString = userRole.split("_");
            String split = partString[1];
            String tokenUserRole = split.substring(0, split.length() - 1);

            String feedAuthorId = feed.getUserId();

            Optional<AspNetUserRole> aspNetUserRole = aspNetUserRoleRepository.findByUserId(feedAuthorId);
            AspNetUserRole getRole = aspNetUserRole.orElseThrow(() -> new DataNotFoundException("Role not found for user"));

            Optional<AspNetRole> aspNetRole = aspNetRoleRepository.findById(getRole.getRoleId());
            AspNetRole getRoleId = aspNetRole.orElseThrow(() -> new DataNotFoundException("Role not found for user"));

            String roleName = getRoleId.getNormalizedName();

            if (feed.getAuthor().equals(currentUserName) || tokenUserRole.equals("SUPERADMIN")) {
                deleteById(feed);

            } else if (tokenUserRole.equals("ADMIN") && !roleName.equals("ADMIN")) {
                deleteById(feed);
            } else {
                throw new AccessDeniedException("You are not authorized to delete this feed");
            }
        } else {
            throw new DataNotFoundException("Feed not with ID" + id);
        }
    }

    public void deleteById(Feed feed) {
        if (feed.getImg() != null && !feed.getImg().isEmpty()) {
            service.getMediaStoreService().deleteFile(feed.getImg());
        }
        List<Comment> comments = commentRepository.findAllByFeed(feed);
        if (!comments.isEmpty()) {
            commentRepository.deleteAll(comments);
        }
        boolean like = likeRepository.existsByFeed(feed);
        if (like) {
            likeRepository.deleteByFeed(feed);
        }
        feedRepository.deleteById(feed.getId());
    }

    public List<String> getImage() {

        return service.getMediaStoreService().getAllImageFileKeys();
    }

}
