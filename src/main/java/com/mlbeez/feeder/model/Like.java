package com.mlbeez.feeder.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.ZonedDateTime;

@Entity
@Table(name = "likes", uniqueConstraints = @UniqueConstraint(columnNames = {"feed_id", "user_id"}))
public class Like {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "bigint")
    private Long id;

    @Version
    private Integer version;

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    @CreationTimestamp
    @Column(name = "created_at",columnDefinition = "TIMESTAMP WITH TIME ZONE")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ssXXX")
    private ZonedDateTime createdAt;
    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }


    @ManyToOne
    @JoinColumn(name = "feed_id", referencedColumnName = "id",nullable = false)
    private Feed feed;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Column(name = "user_id", nullable = false)
    private String userId;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Column(name = "user_name", nullable = false)
    private String userName;

    public Like() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Feed getFeed() {
        return feed;
    }

    public void setFeed(Feed feed) {
        this.feed = feed;
    }

}
