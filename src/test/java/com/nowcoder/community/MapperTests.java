package com.nowcoder.community;
import com.nowcoder.community.dao.*;
import com.nowcoder.community.entity.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class MapperTests {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DiscussPostMapper discussPostMapper;
    @Autowired
    private LoginTicketMapper loginTicketMapper;
    @Autowired
    private MessageMapper messageMapper;

    @Test
    public void testSelectById(){
        User user = userMapper.selectById(101);
        System.out.println(user);
    }
    @Test
    public void testSelectPosts(){
        List<DiscussPost> dp = discussPostMapper.selectDiscussPosts(149,0,10,0);
        for (DiscussPost a:dp)
            System.out.println(a);

        int rows = discussPostMapper.selectDiscussPostRows(149);
        System.out.println(rows);
    }

    @Test
    public void testInsertLoginTicket(){
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(101);
        loginTicket.setTicket("abc");
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis() + 1000 * 60 * 10));

        loginTicketMapper.insertLoginTicket(loginTicket);
    }

    @Test
    public void testSelectLoginTicket(){
        LoginTicket loginTicket = loginTicketMapper.selectByTicket("abc");
        System.out.println(loginTicket);

        loginTicketMapper.updateStatus("abc", 1);
        loginTicket = loginTicketMapper.selectByTicket("abc");
        System.out.println(loginTicket);
    }

    @Test
    public void testSelectDiscussPostById(){
        DiscussPost post = discussPostMapper.selectDiscussPostById(280);
        System.out.println(post);
    }



    @Test
    public void messageTest(){
        List<Message> list = messageMapper.selectConversations(111,0,20);
        for (Message a:list){
            System.out.println(a);
        }
        int count = messageMapper.selectConversationCount(111);
        System.out.println(count);

        List<Message> ls = messageMapper.selectLetters("111_112", 0, 10);
        for (Message message:ls){
            System.out.println(message);
        }

        count = messageMapper.selectLetterCount("111_112");
        System.out.println(count);

        System.out.println(messageMapper.selectLetterUnreadCount(131, "111_131"));

    }
}
