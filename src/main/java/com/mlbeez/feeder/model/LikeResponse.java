package com.mlbeez.feeder.model;

public class LikeResponse {
    public FeedResponse getFeed() {
        return feed;
    }

    public void setFeed(FeedResponse feed) {
        this.feed = feed;
    }

    private FeedResponse feed;
    private String userId;
    private String userName;

    public LikeResponse(String userName, String userId, FeedResponse feed) {
        this.userName = userName;
        this.userId = userId;
        this.feed = feed;
    }


    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}

