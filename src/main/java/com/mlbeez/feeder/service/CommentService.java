package com.mlbeez.feeder.service;

import com.mlbeez.feeder.model.Comment;
import com.mlbeez.feeder.model.CommentResponse;
import java.util.List;

public interface CommentService {

    List<CommentResponse> getAllComments(Long feedId);

    Comment createComments(Long feedId, String userid,String username,Comment comments);

    void deleteCommentByUser(Long feedId, String userid, Long commentId);

    void deleteCommentByAdmin(Long feedId,Long commentId);
}
