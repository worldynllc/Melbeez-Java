package com.mlbeez.feeder.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serial;
import java.io.Serializable;
import java.time.ZonedDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.hateoas.RepresentationModel;


@Entity
@Getter
@Table(name = "feeds")
public class Feed extends RepresentationModel<Feed>implements Serializable {


    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "bigint")
    private Long id;

    @Serial
    private static final long serialVersionUID = 1L;

//	private String title;
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private String link;


    private String author;
    private String description;

    @Column(name = "comment_count")
    private Integer commentCount;

    public void setLikesCount(Integer likesCount) {
        this.likesCount = likesCount;
    }

    @Column(name = "likes_count")
    private Integer likesCount;

    public void setCommentCount(Integer commentCount) {
        this.commentCount = commentCount;
    }


    //	private String category;
//	private String tags;
    private String img;

    @CreationTimestamp
    @Column(name = "created_at",columnDefinition = "TIMESTAMP WITH TIME ZONE")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ssXXX")
    private ZonedDateTime createdAt;

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

//	public String getTitle() {
//		return title;
//	}

//	public void setTitle(String title) {
//		this.title = title;
//	}

    public void setLink(String link) {
        this.link = link;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    public void setAuthor(String author) {
        this.author = author;
    }


//	public String getCategory() {
//		return category;
//	}
//
//	public void setCategory(String category) {
//		this.category = category;
//	}

//

//	public String getTags() {
//		return tags;
//	}
//
//	public void setTags(String tags) {
//		this.tags = tags;
//	}

    public void setImg(String img) {
        this.img = img;
    }
}