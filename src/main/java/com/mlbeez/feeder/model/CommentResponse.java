package com.mlbeez.feeder.model;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import org.hibernate.annotations.CreationTimestamp;
import java.time.ZonedDateTime;

public class CommentResponse {
    private String text;
    @CreationTimestamp
    @Column(name = "created_at",columnDefinition = "TIMESTAMP WITH TIME ZONE")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ssXXX")
    private ZonedDateTime createdAt;

    private String userName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    private Long id;

    public CommentResponse(String userName,String text, ZonedDateTime createdAt, Long id) {

        this.userName=userName;
        this.text=text;
        this.createdAt=createdAt;
        this.id=id;
    }
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
