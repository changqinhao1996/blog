package com.cqh.po;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

public class CommentTest {

    private Comment comment;
    private Blog blog;

    @Before
    public void setUp() {
        comment = new Comment();
        blog = new Blog();
        blog.setId(1L);
        blog.setTitle("Test Blog");
    }

    @Test
    public void testCommentCreation() {
        assertNotNull(comment);
    }

    @Test
    public void testSetAndGetId() {
        comment.setId(1L);
        assertEquals(Long.valueOf(1L), comment.getId());
    }

    @Test
    public void testSetAndGetNickname() {
        comment.setNickname("John Doe");
        assertEquals("John Doe", comment.getNickname());
    }

    @Test
    public void testSetAndGetEmail() {
        comment.setEmail("john@example.com");
        assertEquals("john@example.com", comment.getEmail());
    }

    @Test
    public void testSetAndGetContent() {
        comment.setContent("This is a test comment");
        assertEquals("This is a test comment", comment.getContent());
    }

    @Test
    public void testSetAndGetAvatar() {
        comment.setAvatar("/images/avatar.jpg");
        assertEquals("/images/avatar.jpg", comment.getAvatar());
    }

    @Test
    public void testSetAndGetCreateTime() {
        Date now = new Date();
        comment.setCreateTime(now);
        assertEquals(now, comment.getCreateTime());
    }

    @Test
    public void testSetAndGetBlog() {
        comment.setBlog(blog);
        assertEquals(blog, comment.getBlog());
        assertEquals("Test Blog", comment.getBlog().getTitle());
    }

    @Test
    public void testSetAndGetParentComment() {
        Comment parentComment = new Comment();
        parentComment.setId(10L);
        parentComment.setNickname("Parent");

        comment.setParentComment(parentComment);
        assertEquals(parentComment, comment.getParentComment());
        assertEquals(Long.valueOf(10L), comment.getParentComment().getId());
    }

    @Test
    public void testSetAndGetReplyComments() {
        Comment reply1 = new Comment();
        reply1.setId(2L);
        reply1.setContent("Reply 1");

        Comment reply2 = new Comment();
        reply2.setId(3L);
        reply2.setContent("Reply 2");

        List<Comment> replies = Arrays.asList(reply1, reply2);
        comment.setReplyComments(replies);

        assertEquals(2, comment.getReplyComments().size());
        assertEquals("Reply 1", comment.getReplyComments().get(0).getContent());
    }

    @Test
    public void testSetAndGetAdminComment() {
        comment.setAdminComment(true);
        assertTrue(comment.isAdminComment());

        comment.setAdminComment(false);
        assertFalse(comment.isAdminComment());
    }

    @Test
    public void testToString() {
        comment.setId(1L);
        comment.setNickname("John");
        comment.setContent("Test comment");
        String result = comment.toString();

        assertNotNull(result);
        assertTrue(result.contains("Comment{"));
        assertTrue(result.contains("id=1"));
        assertTrue(result.contains("nickname='John'"));
        assertTrue(result.contains("content='Test comment'"));
    }

    @Test
    public void testDefaultReplyCommentsList() {
        assertNotNull(comment.getReplyComments());
        assertEquals(0, comment.getReplyComments().size());
    }
}
