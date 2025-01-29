package com.mlbeez.feeder;

import com.mlbeez.feeder.model.Feed;
import com.mlbeez.feeder.model.Like;
import com.mlbeez.feeder.repository.FeedRepository;
import com.mlbeez.feeder.repository.LikeRepository;
import com.mlbeez.feeder.service.LikeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.when;

import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class LikeTest {


    @InjectMocks
    private LikeService likeService;

    @Mock
    private FeedRepository feedRepository;

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private Logger logger;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testLikeFeed_Success() {
        Long feedId = 1L;
        String userId = "user123";
        String userName = "John Doe";

        Feed mockFeed = new Feed();
        mockFeed.setId(feedId);
        mockFeed.setLikesCount(5);

        Like expectedLike = new Like();
        expectedLike.setFeed(mockFeed);
        expectedLike.setUserId(userId);
        expectedLike.setUserName(userName);

        Feed updatedFeed = likeService.likeFeed(mockFeed, userId, userName);

        ArgumentCaptor<Feed> feedArgumentCaptor = ArgumentCaptor.forClass(Feed.class);
        verify(feedRepository).save(feedArgumentCaptor.capture());

        Feed capturedFeed = feedArgumentCaptor.getValue();
        assertNotNull(capturedFeed);
        assertEquals(6, capturedFeed.getLikesCount());
        assertEquals(mockFeed.getId(), capturedFeed.getId());

        ArgumentCaptor<Like> likeArgumentCaptor = ArgumentCaptor.forClass(Like.class);
        verify(likeRepository).save(likeArgumentCaptor.capture());
        Like capturedLike = likeArgumentCaptor.getValue();
        assertEquals(expectedLike.getFeed(), capturedLike.getFeed());
        assertEquals(expectedLike.getUserId(), capturedLike.getUserId());
        assertEquals(expectedLike.getUserName(), capturedLike.getUserName());
    }

    @Test
    void testUnlikeFeed_Success() {
        Long feedId = 1L;
        Long likeId = 123L;
        String userId = "user123";
        String userName = "John Doe";

        Feed mockFeed = new Feed();
        mockFeed.setId(feedId);
        mockFeed.setLikesCount(5);

        Like mockLike = new Like();
        mockLike.setId(likeId);
        mockLike.setFeed(mockFeed);
        mockLike.setUserId(userId);
        mockLike.setUserName(userName);

        Feed updatedFeed = likeService.unlikeFeed(mockFeed, Optional.of(mockLike));
        ArgumentCaptor<Long> likeIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Feed> feedArgumentCaptor = ArgumentCaptor.forClass(Feed.class);

        verify(likeRepository).deleteById(likeIdCaptor.capture());
        assertEquals(likeId, likeIdCaptor.getValue());

        verify(feedRepository).save(feedArgumentCaptor.capture());

        Feed capturedFeed = feedArgumentCaptor.getValue();
        assertNotNull(capturedFeed);
        assertEquals(4, capturedFeed.getLikesCount());
    }

    @Test
    void testGetAllLikes_Success() {
        Long feedId = 1L;
        Feed mockFeed = new Feed();
        mockFeed.setId(feedId);

        Like likeOne = new Like();
        likeOne.setId(101L);
        likeOne.setUserName("John");
        likeOne.setCreatedAt(null);
        likeOne.setFeed(mockFeed);

        Like likeTwo = new Like();
        likeTwo.setId(102L);
        likeTwo.setUserName("Mark");
        likeTwo.setCreatedAt(null);
        likeTwo.setFeed(mockFeed);

        Like likeThree = new Like();
        likeThree.setId(103L);
        likeThree.setUserName("Victor");
        likeThree.setCreatedAt(null);
        likeThree.setFeed(mockFeed);

        when(likeRepository.findAll()).thenReturn(Arrays.asList(likeOne, likeTwo, likeThree));

        List<Like> allLikes = likeService.getAllLikes();

        assertNotNull(allLikes);
        assertEquals(3, allLikes.size());
        assertEquals("John", allLikes.get(0).getUserName());
        assertEquals("Mark", allLikes.get(1).getUserName());
        assertEquals("Victor", allLikes.get(2).getUserName());

    }
}
