package com.mlbeez.feeder.model;

public class FeedResponse {

    private Long id;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    private String userId;

    public FeedResponse(Long id, String userID) {
        this.id = id;
        this.userId = userID;
    }
}
