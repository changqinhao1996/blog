package com.cqh.service;

import com.cqh.dao.CommentRepository;
import com.cqh.po.Blog;
import com.cqh.po.Comment;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private CommentServiceImpl commentService;

    private Comment testComment;
    private Blog testBlog;
    private List<Comment> commentList;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        testBlog = new Blog();
        testBlog.setId(1L);
        testBlog.setTitle("Test Blog");

        testComment = new Comment();
        testComment.setId(1L);
        testComment.setNickname("John");
        testComment.setEmail("john@example.com");
        testComment.setContent("Test comment");
        testComment.setCreateTime(new Date());
        testComment.setBlog(testBlog);
        testComment.setReplyComments(new ArrayList<>());

        commentList = new ArrayList<>();
        commentList.add(testComment);
    }

    @Test
    public void testListCommentByBlogId() {
        Long blogId = 1L;
        Sort sort = new Sort("createTime");
        when(commentRepository.findByBlogIdAndParentCommentNull(blogId, sort))
                .thenReturn(commentList);

        List<Comment> result = commentService.listCommentByBlogId(blogId);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(commentRepository, times(1))
                .findByBlogIdAndParentCommentNull(blogId, sort);
    }

    @Test
    public void testListCommentByBlogIdWithNoComments() {
        Long blogId = 2L;
        Sort sort = new Sort("createTime");
        when(commentRepository.findByBlogIdAndParentCommentNull(blogId, sort))
                .thenReturn(new ArrayList<>());

        List<Comment> result = commentService.listCommentByBlogId(blogId);

        assertNotNull(result);
        assertEquals(0, result.size());
        verify(commentRepository, times(1))
                .findByBlogIdAndParentCommentNull(blogId, sort);
    }

    @Test
    public void testSaveCommentWithParent() {
        Comment parentComment = new Comment();
        parentComment.setId(10L);

        Comment newComment = new Comment();
        newComment.setNickname("Jane");
        newComment.setEmail("jane@example.com");
        newComment.setContent("Reply comment");
        newComment.setBlog(testBlog);
        newComment.setParentComment(parentComment);

        when(commentRepository.findOne(10L)).thenReturn(parentComment);
        when(commentRepository.save(any(Comment.class))).thenReturn(newComment);

        Comment result = commentService.saveComment(newComment);

        assertNotNull(result);
        assertNotNull(result.getCreateTime());
        verify(commentRepository, times(1)).findOne(10L);
        verify(commentRepository, times(1)).save(any(Comment.class));
    }

    @Test
    public void testSaveCommentWithoutParent() {
        Comment parentComment = new Comment();
        parentComment.setId(-1L);

        Comment newComment = new Comment();
        newComment.setNickname("Jane");
        newComment.setEmail("jane@example.com");
        newComment.setContent("Top level comment");
        newComment.setBlog(testBlog);
        newComment.setParentComment(parentComment);

        when(commentRepository.save(any(Comment.class))).thenReturn(newComment);

        Comment result = commentService.saveComment(newComment);

        assertNotNull(result);
        assertNotNull(result.getCreateTime());
        verify(commentRepository, never()).findOne(anyLong());
        verify(commentRepository, times(1)).save(any(Comment.class));
    }

    @Test
    public void testListCommentByBlogIdWithNestedReplies() {
        Comment parentComment = new Comment();
        parentComment.setId(1L);
        parentComment.setNickname("Parent");
        parentComment.setContent("Parent comment");
        parentComment.setReplyComments(new ArrayList<>());

        Comment childComment = new Comment();
        childComment.setId(2L);
        childComment.setNickname("Child");
        childComment.setContent("Child comment");
        childComment.setParentComment(parentComment);
        childComment.setReplyComments(new ArrayList<>());

        parentComment.getReplyComments().add(childComment);

        List<Comment> comments = new ArrayList<>();
        comments.add(parentComment);

        Long blogId = 1L;
        Sort sort = new Sort("createTime");
        when(commentRepository.findByBlogIdAndParentCommentNull(blogId, sort))
                .thenReturn(comments);

        List<Comment> result = commentService.listCommentByBlogId(blogId);

        assertNotNull(result);
        assertTrue(result.size() > 0);
        verify(commentRepository, times(1))
                .findByBlogIdAndParentCommentNull(blogId, sort);
    }

    @Test
    public void testSaveCommentSetsCreateTime() {
        Comment parentComment = new Comment();
        parentComment.setId(-1L);

        Comment newComment = new Comment();
        newComment.setNickname("Test User");
        newComment.setEmail("test@example.com");
        newComment.setContent("Test content");
        newComment.setParentComment(parentComment);

        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment saved = (Comment) invocation.getArguments()[0];
            assertNotNull("CreateTime should be set", saved.getCreateTime());
            return saved;
        });

        commentService.saveComment(newComment);

        verify(commentRepository, times(1)).save(any(Comment.class));
    }
}
