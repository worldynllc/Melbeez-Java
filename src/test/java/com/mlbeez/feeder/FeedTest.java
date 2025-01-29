package com.mlbeez.feeder;
import com.mlbeez.feeder.model.Comment;
import com.mlbeez.feeder.model.Feed;
import com.mlbeez.feeder.repository.CommentRepository;
import com.mlbeez.feeder.repository.FeedRepository;
import com.mlbeez.feeder.repository.LikeRepository;
import com.mlbeez.feeder.service.FeedService;
import com.mlbeez.feeder.service.IMediaStore;
import com.mlbeez.feeder.service.MediaStoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import java.io.InputStream;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FeedTest {

    @InjectMocks
    private FeedService feedService;

    @Mock
    private FeedRepository feedRepository;

    @Mock
    private MediaStoreService mediaStoreService;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private Supplier<String> uuidGenerator;

    @Mock
    private IMediaStore mediaStore;

    @BeforeEach
    void setUp() {
        feedService = new FeedService(mediaStoreService, feedRepository, commentRepository, likeRepository, uuidGenerator);
    }


@Test
void testCreateFeed_Success() throws Exception {
    // Arrange
    Feed feed = new Feed();
    feed.setUserId("user123");
    feed.setAuthor("John Doe");
    feed.setDescription("Sample description");

    MockMultipartFile multipartFile = new MockMultipartFile(
            "file", "test.jpg", "image/jpeg", "Sample Content".getBytes()
    );

    String folderName = "feeds";
    String mockedUUID = "0fed178c-c924-41f6-a463-f884215763bb";
    String generatedFile = mockedUUID + ".jpg";
    String uploadedFileLink = "https://s3.example.com/" + folderName + "/" + generatedFile;

    // Mock behaviors
    when(uuidGenerator.get()).thenReturn(mockedUUID);
    when(mediaStoreService.getMediaStoreService()).thenReturn(mediaStore);
    when(mediaStore.uploadFile(anyString(), any(InputStream.class), eq(folderName)))
            .thenReturn(uploadedFileLink);
    when(feedRepository.save(any(Feed.class))).thenAnswer(invocation -> {
        Feed savedFeed = invocation.getArgument(0);
        savedFeed.setId(1L);
        return savedFeed;
    });

    ResponseEntity<String> response = feedService.createFeed(feed, multipartFile);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertTrue(response.getBody().contains("Your file has been uploaded successfully! here "));
    assertTrue(response.getBody().contains(uploadedFileLink));

    ArgumentCaptor<Feed> feedCaptor = ArgumentCaptor.forClass(Feed.class);
    verify(feedRepository).save(feedCaptor.capture());
    Feed savedFeed = feedCaptor.getValue();
    assertEquals(feed.getUserId(), savedFeed.getUserId());
    assertEquals(feed.getDescription(), savedFeed.getDescription());
    assertEquals(generatedFile, savedFeed.getImg());
    assertEquals(0, savedFeed.getLikesCount());
    assertEquals(0, savedFeed.getCommentCount());

    verify(mediaStore).uploadFile(anyString(), any(InputStream.class), eq(folderName));
}

    @Test
    void testGetAllFeeds_Success() {
        // Arrange
        Feed feed1 = new Feed();
        feed1.setId(1L);
        feed1.setUserId("user123");
        feed1.setAuthor("John Doe");
        feed1.setDescription("Sample description");
        feed1.setImg("https://s3.example.com/feeds/0fed178c-c924-41f6-a463-f884215763bb.jpg");
        feed1.setLikesCount(0);
        feed1.setCommentCount(0);

        Page<Feed> feedPage = new PageImpl<>(List.of(feed1));
        when(feedRepository.findAll(any(Pageable.class))).thenReturn(feedPage);


            int page = 0;
            int size = 1;

            List<Feed> response = feedService.getAllFeeds(page, size);

            assertEquals(1, response.size());
            Feed returnedFeed = response.get(0);
            assertEquals(feed1.getId(), returnedFeed.getId());
            assertEquals(feed1.getUserId(), returnedFeed.getUserId());
            assertEquals(feed1.getDescription(), returnedFeed.getDescription());
            assertEquals(feed1.getImg(), returnedFeed.getImg());
            assertEquals(feed1.getLikesCount(), returnedFeed.getLikesCount());
            assertEquals(feed1.getCommentCount(), returnedFeed.getCommentCount());

            verify(feedRepository).findAll(any(Pageable.class));
    }

    @Test
    void testDeleteById_Success() {
        Feed feed = new Feed();
        feed.setId(1L);
        feed.setImg("https://s3.example.com/feeds/sample-image.jpg");

        when(commentRepository.findAllByFeed(feed)).thenReturn(List.of(new Comment(), new Comment()));
        when(likeRepository.existsByFeed(feed)).thenReturn(true);
        when(mediaStoreService.getMediaStoreService()).thenReturn(mediaStore);
        when(mediaStore.deleteFile(feed.getImg())).thenReturn(true);

        feedService.deleteById(feed);

        verify(mediaStore, times(1)).deleteFile(feed.getImg());
        verify(commentRepository, times(1)).findAllByFeed(feed);
        verify(commentRepository, times(1)).deleteAll(anyList());
        verify(likeRepository, times(1)).existsByFeed(feed);
        verify(likeRepository, times(1)).deleteByFeed(feed);
        verify(feedRepository, times(1)).deleteById(feed.getId());
    }

}


