package com.cqh.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class MD5UtilsTest {

    @Test
    public void testCodeWithValidString() {
        String input = "111111";
        String expected = "96e79218965eb72c92a549dd5a330112";
        String actual = MD5Utils.code(input);
        assertEquals(expected, actual);
    }

    @Test
    public void testCodeWithEmptyString() {
        String input = "";
        String expected = "d41d8cd98f00b204e9800998ecf8427e";
        String actual = MD5Utils.code(input);
        assertEquals(expected, actual);
    }

    @Test
    public void testCodeConsistency() {
        String input = "password123";
        String result1 = MD5Utils.code(input);
        String result2 = MD5Utils.code(input);
        assertEquals("Same input should produce same hash", result1, result2);
    }

    @Test
    public void testCodeReturns32CharacterHash() {
        String input = "test";
        String result = MD5Utils.code(input);
        assertNotNull(result);
        assertEquals("MD5 hash should be 32 characters", 32, result.length());
    }

    @Test
    public void testCodeWithSpecialCharacters() {
        String input = "!@#$%^&*()";
        String result = MD5Utils.code(input);
        assertNotNull(result);
        assertEquals(32, result.length());
    }

    @Test
    public void testCodeWithChineseCharacters() {
        String input = "中文测试";
        String result = MD5Utils.code(input);
        assertNotNull(result);
        assertEquals(32, result.length());
    }

    @Test
    public void testCodeDifferentInputsProduceDifferentHashes() {
        String input1 = "password1";
        String input2 = "password2";
        String hash1 = MD5Utils.code(input1);
        String hash2 = MD5Utils.code(input2);
        assertNotEquals("Different inputs should produce different hashes", hash1, hash2);
    }
}
