package com.mlbeez.feeder.controller;

import com.mlbeez.feeder.model.Like;
import com.mlbeez.feeder.model.LikeResponse;
import com.mlbeez.feeder.service.LikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class LikeController {

    @Autowired
    private LikeService likeService;

    @PostMapping("/{feedId}/{userId}/{userName}/like")
    public ResponseEntity<Void> addLike(@PathVariable("feedId") Long feedId,@PathVariable("userId") String userId,
                                        @PathVariable("userName") String userName){
        likeService.addLike(feedId,userId,userName);
        return ResponseEntity.ok().build();
    }


    @GetMapping("/{feedId}/likes")
    public ResponseEntity<List<LikeResponse>> getLikes(@PathVariable Long feedId){
        List<LikeResponse> like=likeService.getLikes(feedId);
        return new ResponseEntity<>(like, HttpStatus.OK);
    }

    @GetMapping("/all/likes")
    public List<Like> getAllLikes(){
        return likeService.getAllLikes();
    }
}
