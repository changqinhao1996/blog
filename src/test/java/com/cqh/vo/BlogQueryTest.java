package com.cqh.vo;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class BlogQueryTest {

    private BlogQuery blogQuery;

    @Before
    public void setUp() {
        blogQuery = new BlogQuery();
    }

    @Test
    public void testBlogQueryCreation() {
        assertNotNull(blogQuery);
    }

    @Test
    public void testSetAndGetTitle() {
        blogQuery.setTitle("Test Title");
        assertEquals("Test Title", blogQuery.getTitle());
    }

    @Test
    public void testSetAndGetTypeId() {
        blogQuery.setTypeId(1L);
        assertEquals(Long.valueOf(1L), blogQuery.getTypeId());
    }

    @Test
    public void testSetAndGetRecommend() {
        blogQuery.setRecommend(true);
        assertTrue(blogQuery.isRecommend());

        blogQuery.setRecommend(false);
        assertFalse(blogQuery.isRecommend());
    }

    @Test
    public void testDefaultRecommendValue() {
        assertFalse(blogQuery.isRecommend());
    }

    @Test
    public void testSetTitleNull() {
        blogQuery.setTitle(null);
        assertNull(blogQuery.getTitle());
    }

    @Test
    public void testSetTypeIdNull() {
        blogQuery.setTypeId(null);
        assertNull(blogQuery.getTypeId());
    }

    @Test
    public void testSetTitleEmpty() {
        blogQuery.setTitle("");
        assertEquals("", blogQuery.getTitle());
    }
}
