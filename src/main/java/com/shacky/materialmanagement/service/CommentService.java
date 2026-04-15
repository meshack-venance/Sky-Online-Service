package com.shacky.materialmanagement.service;

import com.shacky.materialmanagement.entity.Comment;
import com.shacky.materialmanagement.entity.Customer;
import com.shacky.materialmanagement.repository.CommentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentService {

    private final CommentRepository commentRepository;

    public CommentService(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    public Comment saveComment(Comment comment) {
        return commentRepository.save(comment);
    }

    public List<Comment> getAllComments() {
        return commentRepository.findAll();
    }

    public Comment getCommentById(Long commentId) {
        return commentRepository.findById(commentId).orElse(null);
    }

    /**
     * Validates, constructs, and persists a comment for the given customer.
     *
     * @throws IllegalArgumentException if customer is null or content is blank
     */
    public Comment postComment(Customer customer, String content) {
        if (customer == null) {
            throw new IllegalArgumentException("You must be logged in to post a comment.");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Comment content cannot be empty.");
        }
        Comment comment = new Comment();
        comment.setContent(content);
        comment.setCustomer(customer);
        return commentRepository.save(comment);
    }

    /**
     * Increments the like count on the specified comment and persists the change.
     *
     * @return the updated like count, or 0 if the comment does not exist
     */
    public int likeComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment == null) {
            return 0;
        }
        comment.incrementLikes();
        commentRepository.save(comment);
        return comment.getLikes();
    }
}
