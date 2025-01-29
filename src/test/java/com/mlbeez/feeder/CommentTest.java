package com.mlbeez.feeder;

import com.mlbeez.feeder.model.Comment;
import com.mlbeez.feeder.model.CommentResponse;
import com.mlbeez.feeder.model.Feed;
import com.mlbeez.feeder.repository.CommentRepository;
import com.mlbeez.feeder.repository.FeedRepository;
import com.mlbeez.feeder.service.CommentService;
import com.mlbeez.feeder.service.CommentServiceImplement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CommentTest {

    @InjectMocks
    private CommentServiceImplement commentServiceImplement;

    @Mock
    private FeedRepository feedRepository;

    @Mock
    private CommentRepository commentRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreateComments_Success() {
        Long feedId = 1L;
        String userId = "user123";
        String userName = "John Doe";
        Feed mockFeed = new Feed();
        mockFeed.setId(feedId);
        mockFeed.setCommentCount(0);

        Comment comment = new Comment();
        comment.setText("Hello");

        when(feedRepository.findById(feedId)).thenReturn(Optional.of(mockFeed));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Comment createdComment = commentServiceImplement.createComments(feedId, userId, userName, comment);

        assertNotNull(createdComment);
        assertEquals(mockFeed, createdComment.getFeed());
        assertEquals(userId, createdComment.getUserId());
        assertEquals(userName, createdComment.getUserName());
        assertEquals(1, mockFeed.getCommentCount());

        verify(feedRepository).findById(feedId);
        verify(commentRepository).save(comment);
        verify(feedRepository).save(mockFeed);

        ArgumentCaptor<Feed> feedCaptor = ArgumentCaptor.forClass(Feed.class);
        verify(feedRepository).save(feedCaptor.capture());
        Feed updatedFeed = feedCaptor.getValue();
        assertEquals(1, updatedFeed.getCommentCount());
    }

    @Test
    void testGetAllComments_Success() {
        Long feedId = 1L;
        Feed mockFeed = new Feed();
        mockFeed.setId(feedId);

        Comment commentOne = new Comment();
        commentOne.setId(101L);
        commentOne.setUserName("John");
        commentOne.setText("welcome");
        commentOne.setCreatedAt(ZonedDateTime.now());

        Comment commentTwo = new Comment();
        commentTwo.setId(102L);
        commentTwo.setUserName("Mark");
        commentTwo.setText("super");
        commentTwo.setCreatedAt(ZonedDateTime.now());

        List<Comment> mockComments = List.of(commentOne, commentTwo);

        when(feedRepository.findById(feedId)).thenReturn(Optional.of(mockFeed));
        when(commentRepository.findByFeed(Optional.of(mockFeed))).thenReturn(mockComments);

        List<CommentResponse> response = commentServiceImplement.getAllComments(feedId);

        assertEquals(2, response.size());

        CommentResponse response1 = response.get(0);
        assertEquals("John", response1.getUserName());
        assertEquals("welcome", response1.getText());
        assertEquals(commentOne.getCreatedAt(), response1.getCreatedAt());
        assertEquals(commentOne.getId(), response1.getId());

        CommentResponse response2 = response.get(1);
        assertEquals("Mark", response2.getUserName());
        assertEquals("super", response2.getText());
        assertEquals(commentTwo.getCreatedAt(), response2.getCreatedAt());
        assertEquals(commentTwo.getId(), response2.getId());

        verify(feedRepository).findById(feedId);
        verify(commentRepository).findByFeed(Optional.of(mockFeed));
    }

    @Test
    void testDeleteCommentById_Success() {
        Long feedId = 1L;
        Long commentId = 101L;

        Feed mockFeed = new Feed();
        mockFeed.setId(feedId);
        mockFeed.setCommentCount(5);

        Comment mockComment = new Comment();
        mockComment.setId(commentId);

        commentServiceImplement.deleteComments(mockFeed, commentId);
        verify(commentRepository).deleteById(commentId);
        verify(feedRepository).save(mockFeed);
        assertEquals(4, mockFeed.getCommentCount());
    }
}
