package com.cqh.util;

import com.cqh.po.Type;
import com.cqh.po.User;
import org.junit.Test;

import static org.junit.Assert.*;

public class MyBeanUtilsTest {

    @Test
    public void testGetNullPropertyNamesWithAllNullProperties() {
        User user = new User();
        String[] nullProperties = MyBeanUtils.getNullPropertyNames(user);

        assertTrue("Should contain null properties", nullProperties.length > 0);
        assertTrue(containsProperty(nullProperties, "username"));
        assertTrue(containsProperty(nullProperties, "password"));
        assertTrue(containsProperty(nullProperties, "email"));
    }

    @Test
    public void testGetNullPropertyNamesWithSomeNullProperties() {
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");

        String[] nullProperties = MyBeanUtils.getNullPropertyNames(user);

        assertFalse(containsProperty(nullProperties, "username"));
        assertFalse(containsProperty(nullProperties, "email"));
        assertTrue(containsProperty(nullProperties, "password"));
        assertTrue(containsProperty(nullProperties, "nickname"));
    }

    @Test
    public void testGetNullPropertyNamesWithNoNullProperties() {
        Type type = new Type();
        type.setId(1L);
        type.setName("Technology");

        String[] nullProperties = MyBeanUtils.getNullPropertyNames(type);

        assertFalse(containsProperty(nullProperties, "id"));
        assertFalse(containsProperty(nullProperties, "name"));
    }

    @Test
    public void testGetNullPropertyNamesReturnsStringArray() {
        User user = new User();
        String[] nullProperties = MyBeanUtils.getNullPropertyNames(user);

        assertNotNull("Should return non-null array", nullProperties);
        assertTrue("Should be a String array", nullProperties instanceof String[]);
    }

    @Test
    public void testGetNullPropertyNamesWithMixedProperties() {
        User user = new User();
        user.setId(100L);
        user.setUsername("john");
        user.setType(1);

        String[] nullProperties = MyBeanUtils.getNullPropertyNames(user);

        assertFalse(containsProperty(nullProperties, "id"));
        assertFalse(containsProperty(nullProperties, "username"));
        assertFalse(containsProperty(nullProperties, "type"));
        assertTrue(containsProperty(nullProperties, "password"));
        assertTrue(containsProperty(nullProperties, "email"));
        assertTrue(containsProperty(nullProperties, "nickname"));
        assertTrue(containsProperty(nullProperties, "avatar"));
    }

    private boolean containsProperty(String[] properties, String propertyName) {
        for (String prop : properties) {
            if (prop.equals(propertyName)) {
                return true;
            }
        }
        return false;
    }
}
