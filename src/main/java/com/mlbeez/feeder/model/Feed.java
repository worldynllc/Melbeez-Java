package com.mlbeez.feeder.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.hateoas.RepresentationModel;

@Entity
@Table(name = "feeds")
public class Feed extends RepresentationModel<Feed> {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO) // Use GenerationType.IDENTITY for auto-generated IDs
    @Column(columnDefinition = "bigint")

    private Long id;

//	private String title;

    @JsonInclude(JsonInclude.Include.ALWAYS)
    private String link;


    	private String author;
    private String description;


    //	private String category;
//	private String tags;
    private String img;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
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

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


	public String getAuthor() {
		return author;
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

    public String getImg() {
        return img;
    }

    public void setImg(String img) {
        this.img = img;
    }
}
