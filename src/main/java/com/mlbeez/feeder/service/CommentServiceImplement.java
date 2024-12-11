package com.mlbeez.feeder.service;
import com.mlbeez.feeder.model.Comment;
import com.mlbeez.feeder.model.CommentResponse;
import com.mlbeez.feeder.model.Feed;
import com.mlbeez.feeder.repository.CommentRepository;
import com.mlbeez.feeder.repository.FeedRepository;
import com.mlbeez.feeder.service.exception.DataNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CommentServiceImplement implements CommentService{
    @Autowired
    private CommentRepository commentRepository;



    @Autowired
    private FeedRepository feedRepository;

    @Override
    public List<CommentResponse> getAllComments(Long feedId) {
        Optional<Feed> feed=feedRepository.findById(feedId);
        return commentRepository.findByFeed(feed).stream().map(comment->new CommentResponse(comment.getUserName(),comment.getText(),
                comment.getCreatedAt(),comment.getId())).collect(Collectors.toList());
    }

    @Override
    public Comment createComments(Long feedId, String userid,String username, Comment comments) {
        if (feedId == null || userid == null) {
            throw new IllegalArgumentException("Feed ID and user ID must not be null");
        }

        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new DataNotFoundException("Feed not found with id: " + feedId));

        Integer currentCommentCount = Optional.ofNullable(feed.getCommentCount())
                .map(count -> count + 1)
                .orElse(1);

        feed.setCommentCount(currentCommentCount);
        comments.setFeed(feed);
        comments.setUserId(userid);
        comments.setUserName(username);
        commentRepository.save(comments);
        feedRepository.save(feed);
        return comments;
    }
    @Override
    public void deleteCommentByUser(Long feedId, String userId,Long commentId) {
        if (feedId == null || userId == null) {
            throw new IllegalArgumentException("Feed ID and user ID must not be null");
        }

        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new DataNotFoundException("Feed not found with id: " + feedId));


        boolean commentExists = commentRepository.existsByFeedAndUserId(feed,userId);
        if (commentExists) {
            commentRepository.deleteById(commentId);

            feed.setCommentCount(feed.getCommentCount() - 1);
            feedRepository.save(feed);
        }
    }

    @Override
    public void deleteCommentByAdmin(Long feedId,Long commentId) {
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new DataNotFoundException("Feed not found with id: " + feedId));
        Optional<Comment>optionalComment=commentRepository.findByIdAndFeed(commentId,feed);

            if(optionalComment.isPresent()){
                Comment comment=optionalComment.get();
                commentRepository.deleteById(comment.getId());
                feed.setCommentCount(feed.getCommentCount() - 1);
                feedRepository.save(feed);
            }
            else {
                throw new DataNotFoundException("No value present in comment table");
            }
    }
}
