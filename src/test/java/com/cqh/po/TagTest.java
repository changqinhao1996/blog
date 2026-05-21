package com.cqh.po;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class TagTest {

    private Tag tag;

    @Before
    public void setUp() {
        tag = new Tag();
    }

    @Test
    public void testTagCreation() {
        assertNotNull(tag);
    }

    @Test
    public void testSetAndGetId() {
        tag.setId(1L);
        assertEquals(Long.valueOf(1L), tag.getId());
    }

    @Test
    public void testSetAndGetName() {
        tag.setName("Java");
        assertEquals("Java", tag.getName());
    }

    @Test
    public void testSetAndGetBlogs() {
        Blog blog1 = new Blog();
        blog1.setId(1L);
        blog1.setTitle("Blog 1");

        Blog blog2 = new Blog();
        blog2.setId(2L);
        blog2.setTitle("Blog 2");

        List<Blog> blogs = Arrays.asList(blog1, blog2);
        tag.setBlogs(blogs);

        assertEquals(2, tag.getBlogs().size());
        assertEquals("Blog 1", tag.getBlogs().get(0).getTitle());
    }

    @Test
    public void testToString() {
        tag.setId(1L);
        tag.setName("Java");
        String result = tag.toString();

        assertNotNull(result);
        assertTrue(result.contains("Tag{"));
        assertTrue(result.contains("id=1"));
        assertTrue(result.contains("name='Java'"));
    }

    @Test
    public void testDefaultBlogsList() {
        assertNotNull(tag.getBlogs());
        assertEquals(0, tag.getBlogs().size());
    }
}
