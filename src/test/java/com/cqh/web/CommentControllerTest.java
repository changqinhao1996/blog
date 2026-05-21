package com.cqh.web;

import com.cqh.po.Blog;
import com.cqh.po.Comment;
import com.cqh.po.User;
import com.cqh.service.BlogService;
import com.cqh.service.CommentService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
public class CommentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CommentService commentService;

    @Mock
    private BlogService blogService;

    @InjectMocks
    private CommentController commentController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ReflectionTestUtils.setField(commentController, "avatar", "/images/avatar.png");
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");
        mockMvc = MockMvcBuilders.standaloneSetup(commentController)
                .setViewResolvers(viewResolver)
                .build();
    }

    @Test
    public void comments_returnsCommentFragment() throws Exception {
        when(commentService.listCommentByBlogId(1L))
                .thenReturn(Collections.<Comment>emptyList());

        mockMvc.perform(get("/comments/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("blog :: commentList"))
                .andExpect(model().attributeExists("comments"));
    }

    @Test
    public void post_asAnonymous_setsDefaultAvatar() throws Exception {
        Blog blog = new Blog();
        blog.setId(1L);
        when(blogService.getBlog(1L)).thenReturn(blog);
        when(commentService.saveComment(any(Comment.class))).thenReturn(new Comment());

        mockMvc.perform(post("/comments")
                        .param("blog.id", "1")
                        .param("nickname", "visitor")
                        .param("content", "nice post"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/comments/1"));

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentService).saveComment(captor.capture());
        Comment saved = captor.getValue();
        assertEquals("/images/avatar.png", saved.getAvatar());
        assertFalse(saved.isAdminComment());
    }

    @Test
    public void post_asAdmin_setsAdminAvatar() throws Exception {
        Blog blog = new Blog();
        blog.setId(1L);
        when(blogService.getBlog(1L)).thenReturn(blog);
        when(commentService.saveComment(any(Comment.class))).thenReturn(new Comment());

        User admin = new User();
        admin.setAvatar("/images/admin-avatar.png");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", admin);

        mockMvc.perform(post("/comments")
                        .session(session)
                        .param("blog.id", "1")
                        .param("nickname", "admin")
                        .param("content", "thanks"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/comments/1"));

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentService).saveComment(captor.capture());
        Comment saved = captor.getValue();
        assertEquals("/images/admin-avatar.png", saved.getAvatar());
        assertTrue(saved.isAdminComment());
    }
}
