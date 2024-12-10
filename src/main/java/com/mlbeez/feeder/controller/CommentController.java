package com.mlbeez.feeder.controller;

import com.mlbeez.feeder.model.Comment;
import com.mlbeez.feeder.model.CommentResponse;
import com.mlbeez.feeder.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CommentController {

    @Autowired
    private CommentService commentService;

    @GetMapping("/{feedId}/comments")
    public ResponseEntity<List<CommentResponse>> getAllComments(@PathVariable Long feedId) {
        List<CommentResponse> comments = commentService.getAllComments(feedId);
        return new ResponseEntity<>(comments, HttpStatus.OK);
    }


    @PostMapping("/{feedId}/{userid}/{username}/comment")
    public ResponseEntity<Comment> createComments(@PathVariable("feedId") Long feedId, @PathVariable("userid") String userid,
                                                  @PathVariable("username") String username,
                                                        @RequestBody Comment comments) {
       Comment createdComments = commentService.createComments(feedId,userid,username,comments);
        return new ResponseEntity<>(createdComments, HttpStatus.CREATED);
    }

    @DeleteMapping("/{feedId}/{userId}/{commentId}/comment")
    public ResponseEntity<String> deleteCommentByUser(@PathVariable Long feedId, @PathVariable String userId,
                                                    @PathVariable Long commentId) {
        commentService.deleteCommentByUser(feedId,userId,commentId);
        return new ResponseEntity<>("comment deletedByUser",HttpStatus.OK);
    }

    @DeleteMapping("/{feedId}/{commentId}/comment")
    public ResponseEntity<String> deleteCommentByAdmin(@PathVariable(
            "feedId") Long feedId,@PathVariable(
            "commentId") Long commentId){
        commentService.deleteCommentByAdmin(feedId,commentId);
        return new ResponseEntity<>("comment deletedByAdmin",HttpStatus.OK);
    }
}
