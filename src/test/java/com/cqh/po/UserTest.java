package com.cqh.po;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

public class UserTest {

  private User user;

  @Before
  public void setUp() {
    user = new User();
  }

  @Test
  public void testUserCreation() {
    assertNotNull(user);
  }

  @Test
  public void testSetAndGetId() {
    user.setId(1L);
    assertEquals(Long.valueOf(1L), user.getId());
  }

  @Test
  public void testSetAndGetUsername() {
    user.setUsername("admin");
    assertEquals("admin", user.getUsername());
  }

  @Test
  public void testSetAndGetPassword() {
    user.setPassword("password123");
    assertEquals("password123", user.getPassword());
  }

  @Test
  public void testSetAndGetNickname() {
    user.setNickname("Admin User");
    assertEquals("Admin User", user.getNickname());
  }

  @Test
  public void testSetAndGetEmail() {
    user.setEmail("admin@example.com");
    assertEquals("admin@example.com", user.getEmail());
  }

  @Test
  public void testSetAndGetAvatar() {
    user.setAvatar("/images/avatar.jpg");
    assertEquals("/images/avatar.jpg", user.getAvatar());
  }

  @Test
  public void testSetAndGetType() {
    user.setType(1);
    assertEquals(Integer.valueOf(1), user.getType());
  }

  @Test
  public void testSetAndGetDates() {
    Date now = new Date();
    user.setCreateTime(now);
    user.setUpdateTime(now);

    assertEquals(now, user.getCreateTime());
    assertEquals(now, user.getUpdateTime());
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
    user.setBlogs(blogs);

    assertEquals(2, user.getBlogs().size());
    assertEquals("Blog 1", user.getBlogs().get(0).getTitle());
  }

  @Test
  public void testToString() {
    user.setId(1L);
    user.setUsername("admin");
    user.setNickname("Admin");
    String result = user.toString();

    assertNotNull(result);
    assertTrue(result.contains("User{"));
    assertTrue(result.contains("id=1"));
    assertTrue(result.contains("username='admin'"));
    assertTrue(result.contains("nickname='Admin'"));
  }

  @Test
  public void testDefaultBlogsList() {
    assertNotNull(user.getBlogs());
    assertEquals(0, user.getBlogs().size());
  }

  @Test
  public void setId_zero_boundary() {
    user.setId(0L);
    assertEquals(Long.valueOf(0L), user.getId());
  }

  @Test
  public void setId_negative_boundary() {
    user.setId(-1L);
    assertEquals(Long.valueOf(-1L), user.getId());
  }

  @Test
  public void setId_max_boundary() {
    user.setId(Long.MAX_VALUE);
    assertEquals(Long.valueOf(Long.MAX_VALUE), user.getId());
  }

  @Test
  public void setType_zero_boundary() {
    user.setType(0);
    assertEquals(Integer.valueOf(0), user.getType());
  }

  @Test
  public void setType_one_boundary() {
    user.setType(1);
    assertEquals(Integer.valueOf(1), user.getType());
  }

  @Test
  public void setType_negative_boundary() {
    user.setType(-1);
    assertEquals(Integer.valueOf(-1), user.getType());
  }

  @Test
  public void setType_max_boundary() {
    user.setType(Integer.MAX_VALUE);
    assertEquals(Integer.valueOf(Integer.MAX_VALUE), user.getType());
  }

  @Test
  public void setUsername_len0_empty() {
    user.setUsername("");
    assertEquals("", user.getUsername());
  }

  @Test
  public void setUsername_len1_singleChar() {
    user.setUsername("a");
    assertEquals("a", user.getUsername());
  }

  @Test
  public void setUsername_lenHuge_longStringAccepted() {
    String longName = new String(new char[10_000]).replace('\0', 'u');
    user.setUsername(longName);
    assertEquals(longName.length(), user.getUsername().length());
  }

  @Test
  public void setPassword_len0_empty() {
    user.setPassword("");
    assertEquals("", user.getPassword());
  }

  @Test
  public void setDates_epoch_boundary() {
    Date epoch = new Date(0L); // 1970-01-01T00:00:00Z
    user.setCreateTime(epoch);
    user.setUpdateTime(epoch);
    assertEquals(epoch, user.getCreateTime());
    assertEquals(epoch, user.getUpdateTime());
  }

  @Test
  public void setDates_farFuture_boundary() {
    // Practical “high” boundary (2100-01-01T00:00:00Z) – avoids absurd Date.MAX ranges
    Date y2100 = new Date(4102444800000L);
    user.setCreateTime(y2100);
    user.setUpdateTime(y2100);
    assertEquals(y2100, user.getCreateTime());
    assertEquals(y2100, user.getUpdateTime());
  }

  @Test
  public void setDates_null_boundary() {
    user.setCreateTime(null);
    user.setUpdateTime(null);
    assertNull(user.getCreateTime());
    assertNull(user.getUpdateTime());
  }
}
