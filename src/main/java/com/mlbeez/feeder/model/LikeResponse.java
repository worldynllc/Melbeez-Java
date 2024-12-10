package com.mlbeez.feeder.model;

public class LikeResponse {

    private Feed feedId;

    public Feed getFeedId() {
        return feedId;
    }

    public void setFeedId(Feed feedId) {
        this.feedId = feedId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    private String userId;

    public LikeResponse(String userName,String userId,Feed feedId) {
        this.userName = userName;
        this.userId=userId;
        this.feedId=feedId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    private String userName;


}
