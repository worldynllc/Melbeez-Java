package com.mlbeez.feeder.service;

import com.mlbeez.feeder.model.*;
import com.mlbeez.feeder.repository.*;
import com.mlbeez.feeder.service.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CommentServiceImplement implements CommentService {

    private final CommentRepository commentRepository;

    private final FeedRepository feedRepository;

    public CommentServiceImplement(CommentRepository commentRepository, FeedRepository feedRepository) {
        this.commentRepository = commentRepository;
        this.feedRepository = feedRepository;
    }

    private static final Logger logger = LoggerFactory.getLogger(CommentServiceImplement.class);

    @Override
    @Transactional(readOnly = false)
    public List<CommentResponse> getAllComments(Long feedId) {
        Feed feed = feedRepository.findById(feedId).orElseThrow(()->{
            logger.error("feed not found!");
            return new FeedNotFoundException("feed not found!");
        });
        List<Comment> comments = commentRepository.findByFeed(feed);
        return comments.stream().map(comment -> new CommentResponse(comment.getUserName(), comment.getText(),
                comment.getCreatedAt(), comment.getId())).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Comment createComments(Long feedId, String userid, String username, Comment comments) {
        if (feedId == null) {
            logger.error("Feed ID must not be null");
            throw new FeedIdRequiredException("Feed ID must not be null");
        }
        if(userid == null){
            logger.error("User ID must not be null");
            throw new UserIdRequiredException("User ID must not be null");
        }

        if(comments.getText() == null || comments.getText().isEmpty()){
            logger.error("comment is empty,so type the comment");
            throw new CommentEmptyException("comment is empty,so type the comment");
        }

        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() ->{
                    logger.error("Feed not found with id: {}", feedId);
                    return new FeedNotFoundException("Feed not found with id");
                });

        Integer currentCommentCount = Optional.ofNullable(feed.getCommentCount())
                .map(count -> count + 1)
                .orElse(1);

        feed.setCommentCount(currentCommentCount);
        comments.setFeed(feed);
        comments.setUserId(userid);
        comments.setUserName(username);
        commentRepository.save(comments);
        feedRepository.save(feed);
        return comments;
    }

    @Override
    public void deleteCommentByUser(Long feedId, String userId, Long commentId) {
        if (feedId == null) {
            logger.error("Feed id must not be null");
            throw new FeedIdRequiredException("Feed ID must not be null");
        }

        if (userId == null) {
            logger.error("User id must not be null");
            throw new UserIdRequiredException("User ID must not be null");
        }

        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() ->{
                    logger.error("feed not found with id: {}", feedId);
                    return new FeedNotFoundException("Feed not found with id");
                });


        boolean commentExists = commentRepository.existsByFeedAndUserId(feed, userId);
        if (commentExists) {

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserName = authentication.getName();
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String userRole = userDetails.getAuthorities().toString();

            String[] partString = userRole.split("_");
            String split = partString[1];
            String tokenUserRole = split.substring(0, split.length() - 1);

            Comment commentByUsername = commentRepository.findById(commentId).orElseThrow(() ->{
                logger.error("Comment not found for feed id : {}",feedId);
                return new DataNotFoundException("Comment not found in records!");
            });

            String commentAuthor = commentByUsername.getUserName();

            if (commentAuthor.equals(currentUserName) || tokenUserRole.equals("SUPERADMIN")) {
                deleteComments(feed, commentId);
            }
        }
    }

    @Override
    public void deleteCommentByAdmin(Long feedId, Long commentId) {
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> {
                    logger.error("Feed not found with id {}",feedId);
                    return new FeedNotFoundException("Feed not found with id");
                });
        Optional<Comment> optionalComment = commentRepository.findByIdAndFeed(commentId, feed);

        if (optionalComment.isPresent()) {

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserName = authentication.getName();
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String userRole = userDetails.getAuthorities().toString();

            String[] partString = userRole.split("_");
            String split = partString[1];
            String tokenUserRole = split.substring(0, split.length() - 1);

            Comment comment = optionalComment.get();

            if (comment.getUserName().equals(currentUserName) || tokenUserRole.equals("SUPERADMIN")) {
                deleteComments(feed, commentId);
            } else {
                throw new UserAccessDeniedException("You are not authorized to delete this feed");
            }
        } else {
            throw new DataNotFoundException("No value present in comment record");
        }
    }

    public void deleteComments(Feed feed, Long commentId) {
        commentRepository.deleteById(commentId);
        feed.setCommentCount(feed.getCommentCount() - 1);
        feedRepository.save(feed);
    }
}
