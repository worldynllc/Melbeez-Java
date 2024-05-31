package com.mlbeez.feeder.service;


import java.util.List;
import java.util.Optional;

import com.mlbeez.feeder.controller.FeedController;
import com.mlbeez.feeder.model.Feed;
import com.mlbeez.feeder.service.exception.DataNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.stereotype.Service;

import com.mlbeez.feeder.repository.FeedRepository;
import org.springframework.util.CollectionUtils;


@Service
public class FeedService {

    @Autowired
    FeedRepository feedRepository;
    @Value("${test}")
    private String key;

    public List<Feed> getAllFeeds() {
        System.out.println(key);
        List<Feed> feeds = feedRepository.findAll();
        if (CollectionUtils.isEmpty(feeds)) {
            throw new DataNotFoundException("No feed data in the DataBase","put the data in database");
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

    public void deleteFeedId(Long id) {
        if (feedRepository.existsById(id)) {
            feedRepository.deleteById(id);
        }



    }

}
