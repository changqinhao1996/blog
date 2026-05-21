package com.cqh.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class MarkdownUtilsTest {

    @Test
    public void testMarkdownToHtmlWithSimpleText() {
        String markdown = "Hello World";
        String result = MarkdownUtils.markdownToHtml(markdown);

        assertNotNull(result);
        assertTrue(result.contains("Hello World"));
    }

    @Test
    public void testMarkdownToHtmlWithHeading() {
        String markdown = "# Heading 1";
        String result = MarkdownUtils.markdownToHtml(markdown);

        assertNotNull(result);
        assertTrue(result.contains("<h1>"));
        assertTrue(result.contains("Heading 1"));
        assertTrue(result.contains("</h1>"));
    }

    @Test
    public void testMarkdownToHtmlWithParagraph() {
        String markdown = "This is a paragraph.";
        String result = MarkdownUtils.markdownToHtml(markdown);

        assertNotNull(result);
        assertTrue(result.contains("<p>"));
        assertTrue(result.contains("This is a paragraph."));
        assertTrue(result.contains("</p>"));
    }

    @Test
    public void testMarkdownToHtmlWithLink() {
        String markdown = "[Example](http://example.com)";
        String result = MarkdownUtils.markdownToHtml(markdown);

        assertNotNull(result);
        assertTrue(result.contains("<a href=\"http://example.com\">"));
        assertTrue(result.contains("Example"));
        assertTrue(result.contains("</a>"));
    }

    @Test
    public void testMarkdownToHtmlExtensionsWithLink() {
        String markdown = "[imCoding 爱编程](http://www.lirenmi.cn)";
        String result = MarkdownUtils.markdownToHtmlExtensions(markdown);

        assertNotNull(result);
        assertTrue("Link should have target=\"_blank\"", result.contains("target=\"_blank\""));
        assertTrue(result.contains("imCoding 爱编程"));
        assertTrue(result.contains("http://www.lirenmi.cn"));
    }

    @Test
    public void testMarkdownToHtmlExtensionsWithTable() {
        String markdown = "| Header 1 | Header 2 |\n" +
                          "| -------- | -------- |\n" +
                          "| Cell 1   | Cell 2   |";
        String result = MarkdownUtils.markdownToHtmlExtensions(markdown);

        assertNotNull(result);
        assertTrue("Table should have class=\"ui celled table\"", result.contains("class=\"ui celled table\""));
        assertTrue(result.contains("<table"));
        assertTrue(result.contains("Header 1"));
        assertTrue(result.contains("Header 2"));
    }

    @Test
    public void testMarkdownToHtmlExtensionsWithHeading() {
        String markdown = "# Main Heading";
        String result = MarkdownUtils.markdownToHtmlExtensions(markdown);

        assertNotNull(result);
        assertTrue(result.contains("<h1"));
        assertTrue(result.contains("Main Heading"));
    }

    @Test
    public void testMarkdownToHtmlExtensionsWithComplexTable() {
        String markdown = "| hello | hi   | 哈哈哈   |\n" +
                          "| ----- | ---- | ----- |\n" +
                          "| 斯维尔多  | 士大夫  | f啊    |\n" +
                          "| 阿什顿发  | 非固定杆 | 撒阿什顿发 |";
        String result = MarkdownUtils.markdownToHtmlExtensions(markdown);

        assertNotNull(result);
        assertTrue(result.contains("<table"));
        assertTrue(result.contains("ui celled table"));
        assertTrue(result.contains("hello"));
        assertTrue(result.contains("哈哈哈"));
    }

    @Test
    public void testMarkdownToHtmlWithEmptyString() {
        String markdown = "";
        String result = MarkdownUtils.markdownToHtml(markdown);

        assertNotNull(result);
        assertEquals("", result);
    }

    @Test
    public void testMarkdownToHtmlExtensionsWithEmptyString() {
        String markdown = "";
        String result = MarkdownUtils.markdownToHtmlExtensions(markdown);

        assertNotNull(result);
        assertEquals("", result);
    }

    @Test
    public void testMarkdownToHtmlWithBoldText() {
        String markdown = "**Bold Text**";
        String result = MarkdownUtils.markdownToHtml(markdown);

        assertNotNull(result);
        assertTrue(result.contains("<strong>"));
        assertTrue(result.contains("Bold Text"));
        assertTrue(result.contains("</strong>"));
    }

    @Test
    public void testMarkdownToHtmlWithItalicText() {
        String markdown = "*Italic Text*";
        String result = MarkdownUtils.markdownToHtml(markdown);

        assertNotNull(result);
        assertTrue(result.contains("<em>"));
        assertTrue(result.contains("Italic Text"));
        assertTrue(result.contains("</em>"));
    }
}
