package com.example.polls.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 敏感词过滤器（基于DFA算法）
 */
public class SensitiveWordFilter {

    // 字典树节点
    private static class TrieNode {
        private final Map<Character, TrieNode> children = new HashMap<>();
        private boolean isEnd;
    }

    private final TrieNode root = new TrieNode(); // 根节点
    private static final String REPLACEMENT = "***"; // 替换字符串
    private static final Set<Character> SKIP_CHARS = Set.of(' ', '*', '_'); // 跳过的特殊字符

    /**
     * 初始化敏感词过滤器
     * 
     * @param wordFilePath 敏感词文件路径（classpath相对路径）
     */
    public SensitiveWordFilter(String wordFilePath) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(wordFilePath);
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            String word;
            while ((word = reader.readLine()) != null) {
                addWord(word.trim().toLowerCase());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load sensitive words", e);
        }
    }

    /**
     * 添加单个敏感词到字典树
     */
    public void addWord(String word) {
        TrieNode node = root;
        for (char c : word.toLowerCase().toCharArray()) {
            if (SKIP_CHARS.contains(c))
                continue;
            node.children.putIfAbsent(c, new TrieNode());
            node = node.children.get(c);
        }
        node.isEnd = true;
    }

    /**
     * 检测文本是否包含敏感词
     */
    public boolean containsSensitiveWords(String text) {
        text = preprocess(text);
        for (int i = 0; i < text.length(); i++) {
            TrieNode node = root;
            int pointer = i;
            while (pointer < text.length()) {
                char c = text.charAt(pointer);
                if (SKIP_CHARS.contains(c)) {
                    pointer++;
                    continue;
                }
                node = node.children.get(c);
                if (node == null)
                    break;
                if (node.isEnd)
                    return true;
                pointer++;
            }
        }
        return false;
    }

    /**
     * 替换文本中的敏感词
     */
    public String filter(String text) {
        char[] chars = preprocess(text).toCharArray();
        List<int[]> sensitiveRanges = new ArrayList<>();

        for (int i = 0; i < chars.length; i++) {
            TrieNode node = root;
            int end = -1;
            int pointer = i;

            while (pointer < chars.length) {
                char c = chars[pointer];
                if (SKIP_CHARS.contains(c)) {
                    pointer++;
                    continue;
                }
                node = node.children.get(c);
                if (node == null)
                    break;
                if (node.isEnd)
                    end = pointer;
                pointer++;
            }

            if (end != -1) {
                sensitiveRanges.add(new int[] { i, end });
                i = end; // 跳过已检测部分
            }
        }

        return replaceSensitiveWords(chars, sensitiveRanges);
    }

    private String preprocess(String text) {
        return text.toLowerCase()
                .replaceAll("[\\s*_]+", ""); // 移除所有空格和特殊符号
    }

    private String replaceSensitiveWords(char[] chars, List<int[]> ranges) {
        StringBuilder sb = new StringBuilder();
        int lastEnd = 0;

        for (int[] range : ranges) {
            sb.append(chars, lastEnd, range[0] - lastEnd);
            sb.append(REPLACEMENT);
            lastEnd = range[1] + 1;
        }
        sb.append(chars, lastEnd, chars.length - lastEnd);
        return sb.toString();
    }

    // 测试用例
    public static void main(String[] args) {
        SensitiveWordFilter filter = new SensitiveWordFilter("sensitive_words.txt");

        String testText = "这个内容包含敏感词和正常内容";
        System.out.println("包含敏感词: " + filter.containsSensitiveWords(testText));
        System.out.println("过滤结果: " + filter.filter(testText));
    }
}