package com.mlbeez.feeder.service;

import com.mlbeez.feeder.model.*;
import com.mlbeez.feeder.repository.*;
import com.mlbeez.feeder.service.exception.DataNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
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
    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private FeedRepository feedRepository;

    @Override
    @Transactional(readOnly = false)
    public List<CommentResponse> getAllComments(Long feedId) {
        Optional<Feed> feed = feedRepository.findById(feedId);
        return commentRepository.findByFeed(feed).stream().map(comment -> new CommentResponse(comment.getUserName(), comment.getText(),
                comment.getCreatedAt(), comment.getId())).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Comment createComments(Long feedId, String userid, String username, Comment comments) {
        if (feedId == null || userid == null) {
            throw new IllegalArgumentException("Feed ID and user ID must not be null");
        }

        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new DataNotFoundException("Feed not found with id: " + feedId));

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
        if (feedId == null || userId == null) {
            throw new IllegalArgumentException("Feed ID and user ID must not be null");
        }

        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new DataNotFoundException("Feed not found with id: " + feedId));


        boolean commentExists = commentRepository.existsByFeedAndUserId(feed, userId);
        if (commentExists) {

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserName = authentication.getName();
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String userRole = userDetails.getAuthorities().toString();

            String[] partString = userRole.split("_");
            String split = partString[1];
            String tokenUserRole = split.substring(0, split.length() - 1);

            Comment commentByUsername = commentRepository.findById(commentId).orElseThrow(() -> new DataNotFoundException("Data not found!"));

            String commentAuthor = commentByUsername.getUserName();

            if (commentAuthor.equals(currentUserName) || tokenUserRole.equals("SUPERADMIN")) {
                deleteComments(feed, commentId);
            }
        }
    }

    @Override
    public void deleteCommentByAdmin(Long feedId, Long commentId) {
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new DataNotFoundException("Feed not found with id: " + feedId));
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
                throw new AccessDeniedException("You are not authorized to delete this feed");
            }
        } else {
            throw new DataNotFoundException("No value present in comment table");
        }
    }

    public void deleteComments(Feed feed, Long commentId) {
        commentRepository.deleteById(commentId);
        feed.setCommentCount(feed.getCommentCount() - 1);
        feedRepository.save(feed);
    }
}
