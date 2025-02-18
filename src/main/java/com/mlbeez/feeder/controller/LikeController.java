package com.mlbeez.feeder.controller;

import com.mlbeez.feeder.model.Feed;
import com.mlbeez.feeder.model.Like;
import com.mlbeez.feeder.model.LikeResponse;
import com.mlbeez.feeder.service.LikeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/like")
public class LikeController {

    private static final Logger logger= LoggerFactory.getLogger(LikeController.class);

    @Autowired
    private LikeService likeService;

    @PostMapping("/{feedId}/{userId}/{userName}")
    @PreAuthorize("hasAnyRole('ADMIN','USER','SUPERADMIN')")
    public ResponseEntity<Feed> addLike(@PathVariable("feedId") Long feedId, @PathVariable("userId") String userId,
                                        @PathVariable("userName") String userName){
        logger.info("Request to like or unlike for Feed {}", feedId);
        Feed feed=likeService.addLike(feedId,userId,userName);
        return ResponseEntity.ok(feed);
    }


    @GetMapping("/{feedId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER','SUPERADMIN')")
    public ResponseEntity<List<LikeResponse>> getLikes(@PathVariable Long feedId){
        List<LikeResponse> like=likeService.getLikes(feedId);
        return new ResponseEntity<>(like, HttpStatus.OK);
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN','USER','SUPERADMIN')")
    public List<Like> getAllLikes(){
        logger.info("Request to get All likes from like table");
        return likeService.getAllLikes();
    }
}
