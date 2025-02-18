package com.mlbeez.feeder.model;

public class LikeResponse {
    private Feed feed;  // Renamed for clarity
    private String userId;
    private String userName;

    public LikeResponse(String userName, String userId, Feed feed) {
        this.userName = userName;
        this.userId = userId;
        this.feed = feed;
    }

    public Feed getFeed() {
        return feed;
    }

    public void setFeed(Feed feed) {
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

