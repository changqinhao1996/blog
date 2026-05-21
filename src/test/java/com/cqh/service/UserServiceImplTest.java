package com.cqh.service;

import com.cqh.dao.UserRepository;
import com.cqh.po.User;
import com.cqh.util.MD5Utils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class UserServiceImplTest {

  @Mock private UserRepository userRepository;

  @InjectMocks private UserServiceImpl userService;

  private User testUser;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    testUser = new User();
    testUser.setId(1L);
    testUser.setUsername("admin");
    testUser.setPassword(MD5Utils.code("123456"));
    testUser.setNickname("Admin User");
    testUser.setEmail("admin@example.com");
  }

  @Test
  public void testCheckUserWithValidCredentials() {
    String username = "admin";
    String password = "123456";
    String hashedPassword = MD5Utils.code(password);

    when(userRepository.findByUsernameAndPassword(username, hashedPassword)).thenReturn(testUser);

    User result = userService.checkUser(username, password);

    assertNotNull(result);
    assertEquals(username, result.getUsername());
    assertEquals(hashedPassword, result.getPassword());
    verify(userRepository, times(1)).findByUsernameAndPassword(username, hashedPassword);
  }

  @Test
  public void testCheckUserWithInvalidPassword() {
    String username = "admin";
    String wrongPassword = "wrongpassword";
    String hashedWrongPassword = MD5Utils.code(wrongPassword);

    when(userRepository.findByUsernameAndPassword(username, hashedWrongPassword)).thenReturn(null);

    User result = userService.checkUser(username, wrongPassword);

    assertNull(result);
    verify(userRepository, times(1)).findByUsernameAndPassword(username, hashedWrongPassword);
  }

  @Test
  public void testCheckUserWithInvalidUsername() {
    String wrongUsername = "nonexistent";
    String password = "123456";
    String hashedPassword = MD5Utils.code(password);

    when(userRepository.findByUsernameAndPassword(wrongUsername, hashedPassword)).thenReturn(null);

    User result = userService.checkUser(wrongUsername, password);

    assertNull(result);
    verify(userRepository, times(1)).findByUsernameAndPassword(wrongUsername, hashedPassword);
  }

  @Test
  public void testCheckUserPasswordIsHashed() {
    String username = "admin";
    String password = "123456";
    String hashedPassword = MD5Utils.code(password);

    userService.checkUser(username, password);

    verify(userRepository, times(1)).findByUsernameAndPassword(eq(username), eq(hashedPassword));
    verify(userRepository, never()).findByUsernameAndPassword(eq(username), eq(password));
  }

  @Test
  public void testCheckUserWithEmptyPassword() {
    String username = "admin";
    String emptyPassword = "";
    String hashedEmptyPassword = MD5Utils.code(emptyPassword);

    when(userRepository.findByUsernameAndPassword(username, hashedEmptyPassword)).thenReturn(null);

    User result = userService.checkUser(username, emptyPassword);

    assertNull(result);
    verify(userRepository, times(1)).findByUsernameAndPassword(username, hashedEmptyPassword);
  }

  @Test
  public void testCheckUserWithEmptyUsername() {
    String emptyUsername = "";
    String password = "123456";
    String hashedPassword = MD5Utils.code(password);

    when(userRepository.findByUsernameAndPassword(emptyUsername, hashedPassword)).thenReturn(null);

    User result = userService.checkUser(emptyUsername, password);

    assertNull(result);
    verify(userRepository, times(1)).findByUsernameAndPassword(emptyUsername, hashedPassword);
  }
}
