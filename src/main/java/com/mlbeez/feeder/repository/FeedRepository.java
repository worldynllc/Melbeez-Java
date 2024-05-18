package com.mlbeez.feeder.repository;

import com.mlbeez.feeder.model.Feed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedRepository extends JpaRepository<Feed,Long> {
	Feed findByImg(String img);

	

}
