package com.cqh.service;

import com.cqh.NotFoundException;
import com.cqh.dao.TagRepository;
import com.cqh.po.Tag;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TagServiceImplTest {

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private TagServiceImpl tagService;

    private Tag testTag;
    private List<Tag> tagList;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        testTag = new Tag();
        testTag.setId(1L);
        testTag.setName("Java");

        tagList = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Tag tag = new Tag();
            tag.setId((long) i);
            tag.setName("Tag " + i);
            tagList.add(tag);
        }
    }

    @Test
    public void testSaveTag() {
        when(tagRepository.save(testTag)).thenReturn(testTag);

        Tag result = tagService.saveTag(testTag);

        assertNotNull(result);
        assertEquals(testTag.getId(), result.getId());
        assertEquals(testTag.getName(), result.getName());
        verify(tagRepository, times(1)).save(testTag);
    }

    @Test
    public void testGetTag() {
        Long tagId = 1L;
        when(tagRepository.findOne(tagId)).thenReturn(testTag);

        Tag result = tagService.getTag(tagId);

        assertNotNull(result);
        assertEquals(tagId, result.getId());
        assertEquals("Java", result.getName());
        verify(tagRepository, times(1)).findOne(tagId);
    }

    @Test
    public void testGetTagNotFound() {
        Long tagId = 999L;
        when(tagRepository.findOne(tagId)).thenReturn(null);

        Tag result = tagService.getTag(tagId);

        assertNull(result);
        verify(tagRepository, times(1)).findOne(tagId);
    }

    @Test
    public void testGetTagByName() {
        String tagName = "Java";
        when(tagRepository.findByName(tagName)).thenReturn(testTag);

        Tag result = tagService.getTagByName(tagName);

        assertNotNull(result);
        assertEquals(tagName, result.getName());
        verify(tagRepository, times(1)).findByName(tagName);
    }

    @Test
    public void testGetTagByNameNotFound() {
        String tagName = "Nonexistent";
        when(tagRepository.findByName(tagName)).thenReturn(null);

        Tag result = tagService.getTagByName(tagName);

        assertNull(result);
        verify(tagRepository, times(1)).findByName(tagName);
    }

    @Test
    public void testListTagWithPageable() {
        Pageable pageable = new PageRequest(0, 10);
        Page<Tag> page = new PageImpl<>(tagList, pageable, tagList.size());
        when(tagRepository.findAll(pageable)).thenReturn(page);

        Page<Tag> result = tagService.listTag(pageable);

        assertNotNull(result);
        assertEquals(5, result.getContent().size());
        assertEquals(5, result.getTotalElements());
        verify(tagRepository, times(1)).findAll(pageable);
    }

    @Test
    public void testListTag() {
        when(tagRepository.findAll()).thenReturn(tagList);

        List<Tag> result = tagService.listTag();

        assertNotNull(result);
        assertEquals(5, result.size());
        verify(tagRepository, times(1)).findAll();
    }

    @Test
    public void testListTagTop() {
        Integer size = 3;
        List<Tag> topTags = tagList.subList(0, 3);
        when(tagRepository.findTop(any(Pageable.class))).thenReturn(topTags);

        List<Tag> result = tagService.listTagTop(size);

        assertNotNull(result);
        assertEquals(3, result.size());
        verify(tagRepository, times(1)).findTop(any(Pageable.class));
    }

    @Test
    public void testListTagByIds() {
        String ids = "1,2,3";
        List<Long> idList = Arrays.asList(1L, 2L, 3L);
        List<Tag> expectedTags = tagList.subList(0, 3);
        when(tagRepository.findAll(idList)).thenReturn(expectedTags);

        List<Tag> result = tagService.listTag(ids);

        assertNotNull(result);
        assertEquals(3, result.size());
        verify(tagRepository, times(1)).findAll(idList);
    }

    @Test
    public void testListTagByIdsEmptyString() {
        String ids = "";
        when(tagRepository.findAll(new ArrayList<Long>())).thenReturn(new ArrayList<>());

        List<Tag> result = tagService.listTag(ids);

        assertNotNull(result);
        assertEquals(0, result.size());
        verify(tagRepository, times(1)).findAll(any(List.class));
    }

    @Test
    public void testListTagByIdsNull() {
        String ids = null;
        when(tagRepository.findAll(new ArrayList<Long>())).thenReturn(new ArrayList<>());

        List<Tag> result = tagService.listTag(ids);

        assertNotNull(result);
        assertEquals(0, result.size());
        verify(tagRepository, times(1)).findAll(any(List.class));
    }

    @Test
    public void testListTagByIdsSingleId() {
        String ids = "1";
        List<Long> idList = Arrays.asList(1L);
        List<Tag> expectedTags = Arrays.asList(testTag);
        when(tagRepository.findAll(idList)).thenReturn(expectedTags);

        List<Tag> result = tagService.listTag(ids);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(tagRepository, times(1)).findAll(idList);
    }

    @Test
    public void testUpdateTag() {
        Long tagId = 1L;
        Tag updatedTag = new Tag();
        updatedTag.setName("Updated Java");

        when(tagRepository.findOne(tagId)).thenReturn(testTag);
        when(tagRepository.save(any(Tag.class))).thenReturn(testTag);

        Tag result = tagService.updateTag(tagId, updatedTag);

        assertNotNull(result);
        verify(tagRepository, times(1)).findOne(tagId);
        verify(tagRepository, times(1)).save(any(Tag.class));
    }

    @Test(expected = NotFoundException.class)
    public void testUpdateTagNotFound() {
        Long tagId = 999L;
        Tag updatedTag = new Tag();
        updatedTag.setName("Updated Java");

        when(tagRepository.findOne(tagId)).thenReturn(null);

        tagService.updateTag(tagId, updatedTag);
    }

    @Test
    public void testDeleteTag() {
        Long tagId = 1L;
        doNothing().when(tagRepository).delete(tagId);

        tagService.deleteTag(tagId);

        verify(tagRepository, times(1)).delete(tagId);
    }

    @Test
    public void testSaveNewTag() {
        Tag newTag = new Tag();
        newTag.setName("New Tag");

        when(tagRepository.save(newTag)).thenReturn(newTag);

        Tag result = tagService.saveTag(newTag);

        assertNotNull(result);
        assertEquals("New Tag", result.getName());
        verify(tagRepository, times(1)).save(newTag);
    }
}
