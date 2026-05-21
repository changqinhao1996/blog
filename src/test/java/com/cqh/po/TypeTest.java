package com.cqh.po;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class TypeTest {

  private Type type;

  @Before
  public void setUp() {
    type = new Type();
  }

  private Blog blog(long id, String title) {
    Blog b = new Blog();
    b.setId(id);
    b.setTitle(title);
    return b;
  }

  @Test
  public void testTypeCreation() {
    assertNotNull(type);
  }

  @Test
  public void testSetAndGetId() {
    type.setId(1L);
    assertEquals(Long.valueOf(1L), type.getId());
  }

  @Test
  public void testSetAndGetName() {
    type.setName("Technology");
    assertEquals("Technology", type.getName());
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
    type.setBlogs(blogs);

    assertEquals(2, type.getBlogs().size());
    assertEquals("Blog 1", type.getBlogs().get(0).getTitle());
  }

  @Test
  public void testToString() {
    type.setId(1L);
    type.setName("Technology");
    String result = type.toString();

    assertNotNull(result);
    assertTrue(result.contains("Type{"));
    assertTrue(result.contains("id=1"));
    assertTrue(result.contains("name='Technology'"));
  }

  @Test
  public void testDefaultBlogsList() {
    assertNotNull(type.getBlogs());
    assertEquals(0, type.getBlogs().size());
  }

  @Test
  public void setId_zero_allowedOrRejected() {
    type.setId(0L);
    assertEquals(Long.valueOf(0L), type.getId());
  }

  @Test
  public void setId_negative_allowedOrRejected() {
    type.setId(-1L);
    assertEquals(Long.valueOf(-1L), type.getId());
  }

  @Test
  public void setId_maxValue_storedCorrectly() {
    type.setId(Long.MAX_VALUE);
    assertEquals(Long.valueOf(Long.MAX_VALUE), type.getId());
  }

  // ---- name ----

  @Test
  public void setName_emptyString_allowedOrRejected() {
    type.setName("");
    assertEquals("", type.getName());
  }

  @Test
  public void setName_whitespace_only_allowedOrTrimmedOrRejected() {
    type.setName("   ");
    assertEquals("   ", type.getName()); // stored as-is (no trim)
  }

  @Test
  public void setName_null_allowedOrRejected() {
    type.setName(null);
    assertNull(type.getName());
  }

  @Test
  public void setBlogs_null_normalizedOrAllowed() {
    type.setBlogs(null);
    // Permissive: allow null (no normalization)
    assertNull(type.getBlogs());
  }

  @Test
  public void setBlogs_containsNullElement_behavesPerContract() {
    List<Blog> blogs = Arrays.asList(blog(1L, "A"), null, blog(2L, "B"));
    type.setBlogs(blogs);

    assertNotNull(type.getBlogs()); // assuming setter keeps the reference or copies but not null
    assertEquals(3, type.getBlogs().size());
    assertNull(type.getBlogs().get(1)); // null element preserved
  }

  @Test
  public void setBlogs_withDuplicates_allowedOrDeduped() {
    Blog b1 = blog(1L, "A");
    Blog b1Dup = blog(1L, "A");
    type.setBlogs(Arrays.asList(b1, b1Dup));

    assertNotNull(type.getBlogs());
    // Permissive: duplicates allowed, size remains 2
    assertEquals(2, type.getBlogs().size());
    assertEquals(Long.valueOf(1L), type.getBlogs().get(0).getId());
    assertEquals(Long.valueOf(1L), type.getBlogs().get(1).getId());
  }
}
