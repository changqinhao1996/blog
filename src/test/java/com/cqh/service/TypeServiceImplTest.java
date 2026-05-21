package com.cqh.service;

import com.cqh.NotFoundException;
import com.cqh.dao.TypeRepository;
import com.cqh.po.Type;
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
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TypeServiceImplTest {

    @Mock
    private TypeRepository typeRepository;

    @InjectMocks
    private TypeServiceImpl typeService;

    private Type testType;
    private List<Type> typeList;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        testType = new Type();
        testType.setId(1L);
        testType.setName("Technology");

        typeList = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Type type = new Type();
            type.setId((long) i);
            type.setName("Type " + i);
            typeList.add(type);
        }
    }

    @Test
    public void testSaveType() {
        when(typeRepository.save(testType)).thenReturn(testType);

        Type result = typeService.saveType(testType);

        assertNotNull(result);
        assertEquals(testType.getId(), result.getId());
        assertEquals(testType.getName(), result.getName());
        verify(typeRepository, times(1)).save(testType);
    }

    @Test
    public void testGetType() {
        Long typeId = 1L;
        when(typeRepository.findOne(typeId)).thenReturn(testType);

        Type result = typeService.getType(typeId);

        assertNotNull(result);
        assertEquals(typeId, result.getId());
        assertEquals("Technology", result.getName());
        verify(typeRepository, times(1)).findOne(typeId);
    }

    @Test
    public void testGetTypeNotFound() {
        Long typeId = 999L;
        when(typeRepository.findOne(typeId)).thenReturn(null);

        Type result = typeService.getType(typeId);

        assertNull(result);
        verify(typeRepository, times(1)).findOne(typeId);
    }

    @Test
    public void testGetTypeByName() {
        String typeName = "Technology";
        when(typeRepository.findByName(typeName)).thenReturn(testType);

        Type result = typeService.getTypeByName(typeName);

        assertNotNull(result);
        assertEquals(typeName, result.getName());
        verify(typeRepository, times(1)).findByName(typeName);
    }

    @Test
    public void testGetTypeByNameNotFound() {
        String typeName = "Nonexistent";
        when(typeRepository.findByName(typeName)).thenReturn(null);

        Type result = typeService.getTypeByName(typeName);

        assertNull(result);
        verify(typeRepository, times(1)).findByName(typeName);
    }

    @Test
    public void testListTypeWithPageable() {
        Pageable pageable = new PageRequest(0, 10);
        Page<Type> page = new PageImpl<>(typeList, pageable, typeList.size());
        when(typeRepository.findAll(pageable)).thenReturn(page);

        Page<Type> result = typeService.listType(pageable);

        assertNotNull(result);
        assertEquals(5, result.getContent().size());
        assertEquals(5, result.getTotalElements());
        verify(typeRepository, times(1)).findAll(pageable);
    }

    @Test
    public void testListType() {
        when(typeRepository.findAll()).thenReturn(typeList);

        List<Type> result = typeService.listType();

        assertNotNull(result);
        assertEquals(5, result.size());
        verify(typeRepository, times(1)).findAll();
    }

    @Test
    public void testListTypeTop() {
        Integer size = 3;
        List<Type> topTypes = typeList.subList(0, 3);
        when(typeRepository.findTop(any(Pageable.class))).thenReturn(topTypes);

        List<Type> result = typeService.listTypeTop(size);

        assertNotNull(result);
        assertEquals(3, result.size());
        verify(typeRepository, times(1)).findTop(any(Pageable.class));
    }

    @Test
    public void testUpdateType() {
        Long typeId = 1L;
        Type updatedType = new Type();
        updatedType.setName("Updated Technology");

        when(typeRepository.findOne(typeId)).thenReturn(testType);
        when(typeRepository.save(any(Type.class))).thenReturn(testType);

        Type result = typeService.updateType(typeId, updatedType);

        assertNotNull(result);
        verify(typeRepository, times(1)).findOne(typeId);
        verify(typeRepository, times(1)).save(any(Type.class));
    }

    @Test(expected = NotFoundException.class)
    public void testUpdateTypeNotFound() {
        Long typeId = 999L;
        Type updatedType = new Type();
        updatedType.setName("Updated Technology");

        when(typeRepository.findOne(typeId)).thenReturn(null);

        typeService.updateType(typeId, updatedType);
    }

    @Test
    public void testDeleteType() {
        Long typeId = 1L;
        doNothing().when(typeRepository).delete(typeId);

        typeService.deleteType(typeId);

        verify(typeRepository, times(1)).delete(typeId);
    }

    @Test
    public void testSaveNewType() {
        Type newType = new Type();
        newType.setName("New Type");

        when(typeRepository.save(newType)).thenReturn(newType);

        Type result = typeService.saveType(newType);

        assertNotNull(result);
        assertEquals("New Type", result.getName());
        verify(typeRepository, times(1)).save(newType);
    }
}
