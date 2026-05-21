package com.cqh.service;

import com.cqh.NotFoundException;
import com.cqh.dao.BlogRepository;
import com.cqh.po.Blog;
import com.cqh.po.Tag;
import com.cqh.po.Type;
import com.cqh.po.User;
import com.cqh.vo.BlogQuery;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BlogServiceImplTest {

    @Mock
    private BlogRepository blogRepository;

    @InjectMocks
    private BlogServiceImpl blogService;

    private Blog testBlog;
    private List<Blog> blogList;
    private Type testType;
    private User testUser;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        testType = new Type();
        testType.setId(1L);
        testType.setName("Technology");

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("admin");

        testBlog = new Blog();
        testBlog.setId(1L);
        testBlog.setTitle("Test Blog");
        testBlog.setContent("# Test Content");
        testBlog.setType(testType);
        testBlog.setUser(testUser);
        testBlog.setViews(100);
        testBlog.setRecommend(true);
        testBlog.setPublished(true);
        testBlog.setCreateTime(new Date());
        testBlog.setUpdateTime(new Date());

        blogList = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Blog blog = new Blog();
            blog.setId((long) i);
            blog.setTitle("Blog " + i);
            blog.setContent("Content " + i);
            blogList.add(blog);
        }
    }

    @Test
    public void testGetBlog() {
        Long blogId = 1L;
        when(blogRepository.findOne(blogId)).thenReturn(testBlog);

        Blog result = blogService.getBlog(blogId);

        assertNotNull(result);
        assertEquals(blogId, result.getId());
        assertEquals("Test Blog", result.getTitle());
        verify(blogRepository, times(1)).findOne(blogId);
    }

    @Test
    public void testGetBlogNotFound() {
        Long blogId = 999L;
        when(blogRepository.findOne(blogId)).thenReturn(null);

        Blog result = blogService.getBlog(blogId);

        assertNull(result);
        verify(blogRepository, times(1)).findOne(blogId);
    }

    @Test
    public void testGetAndConvert() {
        Long blogId = 1L;
        when(blogRepository.findOne(blogId)).thenReturn(testBlog);
        when(blogRepository.updateViews(blogId)).thenReturn(1);

        Blog result = blogService.getAndConvert(blogId);

        assertNotNull(result);
        assertTrue(result.getContent().contains("<h1"));
        verify(blogRepository, times(1)).findOne(blogId);
        verify(blogRepository, times(1)).updateViews(blogId);
    }

    @Test(expected = NotFoundException.class)
    public void testGetAndConvertNotFound() {
        Long blogId = 999L;
        when(blogRepository.findOne(blogId)).thenReturn(null);

        blogService.getAndConvert(blogId);
    }

    @Test
    public void testListBlogWithQuery() {
        Pageable pageable = new PageRequest(0, 10);
        BlogQuery query = new BlogQuery();
        query.setTitle("Test");
        query.setTypeId(1L);
        query.setRecommend(true);

        Page<Blog> page = new PageImpl<>(blogList, pageable, blogList.size());
        when(blogRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<Blog> result = blogService.listBlog(pageable, query);

        assertNotNull(result);
        assertEquals(5, result.getContent().size());
        verify(blogRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    public void testListBlogWithPageable() {
        Pageable pageable = new PageRequest(0, 10);
        Page<Blog> page = new PageImpl<>(blogList, pageable, blogList.size());
        when(blogRepository.findAll(pageable)).thenReturn(page);

        Page<Blog> result = blogService.listBlog(pageable);

        assertNotNull(result);
        assertEquals(5, result.getContent().size());
        verify(blogRepository, times(1)).findAll(pageable);
    }

    @Test
    public void testListBlogByTagId() {
        Long tagId = 1L;
        Pageable pageable = new PageRequest(0, 10);
        Page<Blog> page = new PageImpl<>(blogList, pageable, blogList.size());
        when(blogRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<Blog> result = blogService.listBlog(tagId, pageable);

        assertNotNull(result);
        assertEquals(5, result.getContent().size());
        verify(blogRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    public void testListBlogBySearchQuery() {
        String query = "test search";
        Pageable pageable = new PageRequest(0, 10);
        Page<Blog> page = new PageImpl<>(blogList, pageable, blogList.size());
        when(blogRepository.findByQuery(query, pageable)).thenReturn(page);

        Page<Blog> result = blogService.listBlog(query, pageable);

        assertNotNull(result);
        assertEquals(5, result.getContent().size());
        verify(blogRepository, times(1)).findByQuery(query, pageable);
    }

    @Test
    public void testListRecommendBlogTop() {
        Integer size = 3;
        List<Blog> topBlogs = blogList.subList(0, 3);
        when(blogRepository.findTop(any(Pageable.class))).thenReturn(topBlogs);

        List<Blog> result = blogService.listRecommendBlogTop(size);

        assertNotNull(result);
        assertEquals(3, result.size());
        verify(blogRepository, times(1)).findTop(any(Pageable.class));
    }

    @Test
    public void testArchiveBlog() {
        List<String> years = Arrays.asList("2023", "2024");
        List<Blog> blogs2023 = blogList.subList(0, 2);
        List<Blog> blogs2024 = blogList.subList(2, 5);

        when(blogRepository.findGroupYear()).thenReturn(years);
        when(blogRepository.findByYear("2023")).thenReturn(blogs2023);
        when(blogRepository.findByYear("2024")).thenReturn(blogs2024);

        Map<String, List<Blog>> result = blogService.archiveBlog();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.containsKey("2023"));
        assertTrue(result.containsKey("2024"));
        assertEquals(2, result.get("2023").size());
        assertEquals(3, result.get("2024").size());
        verify(blogRepository, times(1)).findGroupYear();
    }

    @Test
    public void testCountBlog() {
        when(blogRepository.count()).thenReturn(10L);

        Long result = blogService.countBlog();

        assertEquals(Long.valueOf(10L), result);
        verify(blogRepository, times(1)).count();
    }

    @Test
    public void testSaveBlogNew() {
        Blog newBlog = new Blog();
        newBlog.setTitle("New Blog");
        newBlog.setContent("New Content");

        when(blogRepository.save(any(Blog.class))).thenReturn(newBlog);

        Blog result = blogService.saveBlog(newBlog);

        assertNotNull(result);
        assertNotNull(result.getCreateTime());
        assertNotNull(result.getUpdateTime());
        assertEquals(Integer.valueOf(0), result.getViews());
        verify(blogRepository, times(1)).save(any(Blog.class));
    }

    @Test
    public void testSaveBlogExisting() {
        Blog existingBlog = new Blog();
        existingBlog.setId(1L);
        existingBlog.setTitle("Existing Blog");
        existingBlog.setContent("Existing Content");
        existingBlog.setCreateTime(new Date());

        when(blogRepository.save(any(Blog.class))).thenReturn(existingBlog);

        Blog result = blogService.saveBlog(existingBlog);

        assertNotNull(result);
        assertNotNull(result.getUpdateTime());
        verify(blogRepository, times(1)).save(any(Blog.class));
    }

    @Test
    public void testUpdateBlog() {
        Long blogId = 1L;
        Blog updatedBlog = new Blog();
        updatedBlog.setTitle("Updated Title");
        updatedBlog.setContent("Updated Content");

        when(blogRepository.findOne(blogId)).thenReturn(testBlog);
        when(blogRepository.save(any(Blog.class))).thenReturn(testBlog);

        Blog result = blogService.updateBlog(blogId, updatedBlog);

        assertNotNull(result);
        assertNotNull(result.getUpdateTime());
        verify(blogRepository, times(1)).findOne(blogId);
        verify(blogRepository, times(1)).save(any(Blog.class));
    }

    @Test(expected = NotFoundException.class)
    public void testUpdateBlogNotFound() {
        Long blogId = 999L;
        Blog updatedBlog = new Blog();
        updatedBlog.setTitle("Updated Title");

        when(blogRepository.findOne(blogId)).thenReturn(null);

        blogService.updateBlog(blogId, updatedBlog);
    }

    @Test
    public void testDeleteBlog() {
        Long blogId = 1L;
        doNothing().when(blogRepository).delete(blogId);

        blogService.deleteBlog(blogId);

        verify(blogRepository, times(1)).delete(blogId);
    }

    @Test
    public void testListBlogWithEmptyQuery() {
        Pageable pageable = new PageRequest(0, 10);
        BlogQuery query = new BlogQuery();

        Page<Blog> page = new PageImpl<>(blogList, pageable, blogList.size());
        when(blogRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<Blog> result = blogService.listBlog(pageable, query);

        assertNotNull(result);
        verify(blogRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }
}
