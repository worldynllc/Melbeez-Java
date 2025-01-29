package com.mlbeez.feeder.controller;

import com.mlbeez.feeder.model.Comment;
import com.mlbeez.feeder.model.CommentResponse;
import com.mlbeez.feeder.service.CommentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/comment")
public class CommentController {

    @Autowired
    private CommentService commentService;

    private final Logger logger= LoggerFactory.getLogger(CommentController.class);

    @GetMapping("/{feedId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER','SUPERADMIN')")
    public ResponseEntity<List<CommentResponse>> getAllComments(@PathVariable Long feedId) {
        logger.info("Requested to get all comments for feed {}", feedId);
        List<CommentResponse> comments = commentService.getAllComments(feedId);
        return new ResponseEntity<>(comments, HttpStatus.OK);
    }


    @PostMapping("/post/{feedId}/{userid}/{username}")
    @PreAuthorize("hasAnyRole('ADMIN','USER','SUPERADMIN')")
    public ResponseEntity<Comment> createComments(@PathVariable("feedId") Long feedId, @PathVariable("userid") String userid,
                                                  @PathVariable("username") String username,
                                                        @RequestBody Comment comments) {
        logger.info("Requested to create comments for feed {}", feedId);
       Comment createdComments = commentService.createComments(feedId,userid,username,comments);
        return new ResponseEntity<>(createdComments, HttpStatus.CREATED);
    }

    @DeleteMapping("/{feedId}/{userId}/{commentId}")
    @PreAuthorize("hasAnyRole('USER','SUPERADMIN')")
    public ResponseEntity<String> deleteCommentByUser(@PathVariable Long feedId, @PathVariable String userId,
                                                    @PathVariable Long commentId) {
        logger.info("Requested to delete commentsByUser for feed {}", feedId);
        commentService.deleteCommentByUser(feedId,userId,commentId);
        return new ResponseEntity<>("comment deletedByUser",HttpStatus.OK);
    }

    @DeleteMapping("/{feedId}/{commentId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<String> deleteCommentByAdmin(@PathVariable(
            "feedId") Long feedId,@PathVariable(
            "commentId") Long commentId){
        logger.info("Requested to delete commentsBy admin for feed {}", feedId);
        commentService.deleteCommentByAdmin(feedId,commentId);
        return new ResponseEntity<>("comment deletedByAdmin",HttpStatus.OK);
    }
}
