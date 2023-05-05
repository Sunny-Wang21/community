package com.nowcoder.community.util;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.rmi.server.SkeletonMismatchException;
import java.util.HashMap;
import java.util.Map;

@Component
public class SensitiveFilter {

    private static final Logger logger = LoggerFactory.getLogger(SensitiveFilter.class);

    /**
     * 根据敏感词文件构造敏感词前缀树
     */
    private TrieNode rootNode = new TrieNode();
    // 类常量
    private static final String REPLACEMENT = "***";

    @PostConstruct
    public void init(){
        try (
                InputStream is = this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));
        ) {
            String keyword;
            while ((keyword = bufferedReader.readLine()) != null){
                this.addKeyword(keyword);
            }
        } catch (Exception e) {
            logger.error("获取敏感词文件失败！" + e.getMessage());
        }
    }

    /**
     * 将一个敏感词添加到前缀树
     */
    private void addKeyword(String keyword){
        TrieNode tempNode = rootNode;
        for (int i=0;i<keyword.length();i++){
            char c = keyword.charAt(i);
            TrieNode subNode = tempNode.getSubTrieNode(c);
            if (subNode == null){
                subNode = new TrieNode();
                tempNode.addSubTrieNode(c, subNode);
            }
            tempNode = subNode;
            if (i == keyword.length()-1){
                tempNode.setKeyWordEnd(true);
            }
        }
    }
    /**
     * 过滤敏感词的方法
     */
    public String filter(String text){
        if (StringUtils.isBlank(text)){
            return null;
        }
        TrieNode cur = rootNode;
        int prev = 0;
        int post = 0;
        StringBuilder sb = new StringBuilder();
        while (prev < text.length()) {
            // 文本中没有以prev打头的敏感词，所以要回到根节点，看看有没有以prev下一个字符打头的敏感词
            if (post == text.length()){
                cur = rootNode;
                sb.append(text.charAt(prev));
                prev++;
                post = prev;
            }
            char c = text.charAt(post);
            // post和prev指向同一个字符c,且c是符号或者c不属于cur的子结点，则直接加入最终结果串sb中
            if ((post == prev) && (isSymbol(c) || cur.getSubTrieNode(c) == null)){
                sb.append(c);
                post++;
                prev++;
            }
            // c是cur的子结点
            else if (cur.getSubTrieNode(c) != null){
                post++;
                cur = cur.getSubTrieNode(c);
                // 成功找到了一个敏感词，要接着sb加入替代字串，然后prev和post定位到一块，cur回到根，再继续找敏感词
                if (cur.isKeyWordEnd()){
                    sb.append(REPLACEMENT);
                    prev = post;
                    cur = rootNode;
                }
            }
            // 有的人会在敏感词之间加符号想规避过滤机制，那我们就要越过这个字符去判断
            else if (isSymbol(c) || cur.getSubTrieNode(c) == null){
                post++;
            }
        }
        return sb.toString();
    }
    /**
     * 判断是否为符号
     */
    public boolean isSymbol(Character c){
        return !CharUtils.isAsciiAlphanumeric(c) && (c < 0x2E80 || c > 0x9FFF);
    }


    /**
     * 自定义前缀树（结点）的属性和操作功能
     * 内部类
     */
    private class TrieNode {
        // 关键词结束标识符
        private boolean KeyWordEnd = false;
        // 当前结点的子结点
        private Map<Character, TrieNode> SubTrieNodes = new HashMap<>();

        public boolean isKeyWordEnd() {
            return KeyWordEnd;
        }

        public void setKeyWordEnd(boolean keyWordEnd) {
            KeyWordEnd = keyWordEnd;
        }
        // private里的public方法？
        public void addSubTrieNode(Character c, TrieNode node){
            SubTrieNodes.put(c, node);
        }
        public TrieNode getSubTrieNode(Character c){
            return SubTrieNodes.get(c);
        }
    }
}
