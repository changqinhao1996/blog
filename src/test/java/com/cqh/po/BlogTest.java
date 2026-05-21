package com.cqh.po;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

public class BlogTest {

    private Blog blog;
    private Type type;
    private User user;

    @Before
    public void setUp() {
        blog = new Blog();
        type = new Type();
        type.setId(1L);
        type.setName("Technology");

        user = new User();
        user.setId(1L);
        user.setUsername("admin");
    }

    @Test
    public void testBlogCreation() {
        assertNotNull(blog);
    }

    @Test
    public void testSetAndGetId() {
        blog.setId(1L);
        assertEquals(Long.valueOf(1L), blog.getId());
    }

    @Test
    public void testSetAndGetTitle() {
        blog.setTitle("Test Title");
        assertEquals("Test Title", blog.getTitle());
    }

    @Test
    public void testSetAndGetContent() {
        blog.setContent("Test Content");
        assertEquals("Test Content", blog.getContent());
    }

    @Test
    public void testSetAndGetType() {
        blog.setType(type);
        assertEquals(type, blog.getType());
    }

    @Test
    public void testSetAndGetUser() {
        blog.setUser(user);
        assertEquals(user, blog.getUser());
    }

    @Test
    public void testSetAndGetViews() {
        blog.setViews(100);
        assertEquals(Integer.valueOf(100), blog.getViews());
    }

    @Test
    public void testBooleanProperties() {
        blog.setAppreciation(true);
        blog.setShareStatement(true);
        blog.setCommentabled(true);
        blog.setPublished(true);
        blog.setRecommend(true);

        assertTrue(blog.isAppreciation());
        assertTrue(blog.isShareStatement());
        assertTrue(blog.isCommentabled());
        assertTrue(blog.isPublished());
        assertTrue(blog.isRecommend());
    }

    @Test
    public void testSetAndGetDates() {
        Date now = new Date();
        blog.setCreateTime(now);
        blog.setUpdateTime(now);

        assertEquals(now, blog.getCreateTime());
        assertEquals(now, blog.getUpdateTime());
    }

    @Test
    public void testSetAndGetTags() {
        Tag tag1 = new Tag();
        tag1.setId(1L);
        tag1.setName("Java");

        Tag tag2 = new Tag();
        tag2.setId(2L);
        tag2.setName("Spring");

        List<Tag> tags = Arrays.asList(tag1, tag2);
        blog.setTags(tags);

        assertEquals(2, blog.getTags().size());
        assertEquals("Java", blog.getTags().get(0).getName());
    }

    @Test
    public void testSetAndGetComments() {
        Comment comment = new Comment();
        comment.setId(1L);
        comment.setContent("Test comment");

        List<Comment> comments = Arrays.asList(comment);
        blog.setComments(comments);

        assertEquals(1, blog.getComments().size());
    }

    @Test
    public void testInitMethodWithTags() {
        Tag tag1 = new Tag();
        tag1.setId(1L);
        tag1.setName("Java");

        Tag tag2 = new Tag();
        tag2.setId(2L);
        tag2.setName("Spring");

        Tag tag3 = new Tag();
        tag3.setId(3L);
        tag3.setName("Boot");

        List<Tag> tags = Arrays.asList(tag1, tag2, tag3);
        blog.setTags(tags);
        blog.init();

        assertEquals("1,2,3", blog.getTagIds());
    }

    @Test
    public void testInitMethodWithEmptyTags() {
        blog.setTags(new ArrayList<>());
        blog.setTagIds("5,6");
        blog.init();

        assertEquals("5,6", blog.getTagIds());
    }

    @Test
    public void testInitMethodWithSingleTag() {
        Tag tag = new Tag();
        tag.setId(1L);
        tag.setName("Java");

        blog.setTags(Arrays.asList(tag));
        blog.init();

        assertEquals("1", blog.getTagIds());
    }

    @Test
    public void testSetAndGetDescription() {
        blog.setDescription("Test Description");
        assertEquals("Test Description", blog.getDescription());
    }

    @Test
    public void testSetAndGetFirstPicture() {
        blog.setFirstPicture("/images/test.jpg");
        assertEquals("/images/test.jpg", blog.getFirstPicture());
    }

    @Test
    public void testSetAndGetFlag() {
        blog.setFlag("original");
        assertEquals("original", blog.getFlag());
    }

    @Test
    public void testToString() {
        blog.setId(1L);
        blog.setTitle("Test");
        String result = blog.toString();

        assertNotNull(result);
        assertTrue(result.contains("Blog{"));
        assertTrue(result.contains("id=1"));
        assertTrue(result.contains("title='Test'"));
    }
}
