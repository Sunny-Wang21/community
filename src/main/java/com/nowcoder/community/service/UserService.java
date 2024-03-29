package com.nowcoder.community.service;

import com.nowcoder.community.dao.LoginTicketMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UserService implements CommunityConstant {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private UserMapper userMapper;

    //@Autowired
    //private LoginTicketMapper loginTicketMapper;

    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    public User findUserById(int id){
        User user = getCache(id);
        // return userMapper.selectById(id);
        if (user == null){
            user = initCache(id);
        }
        return user;
    }

    public Map<String,Object> register(User user){
        Map<String,Object> map = new HashMap<>();

        //空值处理
        if (user == null){
            throw new IllegalArgumentException("参数不能为空！");
        }

        if (StringUtils.isBlank(user.getUsername())){
            map.put("usernameMSG", "账号不能为空!");
            return map;
        }
        if (StringUtils.isBlank(user.getPassword())){
            map.put("passwordMSG", "密码不能为空!");
            return map;
        }
        if (StringUtils.isBlank(user.getEmail())){
            map.put("emailMSG", "邮箱不能为空!");
            return map;
        }

        //验证账号
        User u = userMapper.selectByName(user.getUsername());
        if (u != null){
            map.put("usernameMSG", "该账号已存在！");
            return map;
        }

        //验证邮箱
        u = userMapper.selectByEmail(user.getEmail());
        if (u != null){
            map.put("emailMSG", "该邮箱已被注册！");
            return map;
        }

        //注册用户
        user.setSalt(CommunityUtil.generateUUID().substring(0,5));
        user.setPassword(CommunityUtil.md5(user.getPassword() + user.getSalt()));
        user.setType(0);
        user.setStatus(0);
        user.setActivationCode(CommunityUtil.generateUUID());
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000)));
        user.setCreateTime(new Date());
        userMapper.insertUser(user);

        //发送激活邮件
        Context context = new Context();
        context.setVariable("email", user.getEmail());
        //http://localhost:8080/activation/101/activationcode
        String url = domain + contextPath + "/activation/" + user.getId() + "/" + user.getActivationCode();
        context.setVariable("url", url);
        String content = templateEngine.process("/mail/activation", context);
        mailClient.sendMail(user.getEmail(), "激活账号", content);

        return map;
    }

    public int activation(int userId, String code){
        User user = userMapper.selectById(userId);
        if (user.getStatus() == 1){
            return ACTIVATION_REPEAT;
        } else if (user.getActivationCode().equals(code)) {
            userMapper.updateStatus(userId, 1);
            deleteCache(userId);
            return ACTIVATION_SUCCESS;
        }
        else{
            return ACTIVATION_FAILURE;
        }
    }

    public Map<String, Object> login(String username, String password, long expiredSeconds){
        Map<String, Object> map = new HashMap<>();
        // 空值处理
        if (StringUtils.isBlank(username)){
            map.put("usernameMsg", "账号不能为空！");
            return map;
        }
        if (StringUtils.isBlank(password)){
            map.put("passwordMsg", "密码不能为空！");
            return map;
        }
        // 验证账号
        User user = userMapper.selectByName(username);
        if (user == null){
            map.put("usernameMsg", "该账号不存在！");
            return map;
        }
        // 验证状态
        if (user.getStatus() == 0){
            map.put("usernameMsg", "该账号未激活！");
            return map;
        }
        // 验证密码
        password = CommunityUtil.md5(password + user.getSalt());
        if (!user.getPassword().equals(password)){
            map.put("passwordMsg", "密码不正确！");
            return map;
        }
        // 以上这些验证没有问题，才会生成登陆凭证
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID());
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expiredSeconds * 1000));

        String redisKey = RedisKeyUtil.getTicketKey(loginTicket.getTicket());
        redisTemplate.opsForValue().set(redisKey, loginTicket);

        map.put("ticket", loginTicket.getTicket());
        return map;

    }
    public void logout(String ticket){
        //loginTicketMapper.updateStatus(ticket, 1);
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        LoginTicket loginTicket = (LoginTicket) redisTemplate.opsForValue().get(redisKey);
        loginTicket.setStatus(1);
        redisTemplate.opsForValue().set(redisKey, loginTicket);
    }

    public LoginTicket findLoginTicket(String ticket){
        //return loginTicketMapper.selectByTicket(ticket);
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        LoginTicket loginTicket = (LoginTicket) redisTemplate.opsForValue().get(redisKey);
        return loginTicket;
    }

    public void updateHeader(int userId, String headerUrl){
        userMapper.updateHeader(userId, headerUrl);
        deleteCache(userId);
    }

    public Map<String, Object> updatePassword(String oldPassword, String newPassword, String confirmPassword){
        Map<String, Object> map = new HashMap<>();
        // 验证密码是否为空
        if (StringUtils.isBlank(oldPassword)){
            map.put("oldMsg", "原始密码不能为空！");
            return map;
        }
        if (StringUtils.isBlank(newPassword)){
            map.put("newMsg", "新的密码不能为空！");
            return map;
        }
        if (StringUtils.isBlank(confirmPassword)){
            map.put("confirmMsg", "确认密码不能为空！");
            return map;
        }
        User user = hostHolder.getUser();
        // 验证原始密码是否正确
        oldPassword = CommunityUtil.md5(oldPassword+ user.getSalt());
        if (!user.getPassword().equals(oldPassword)){
            map.put("oldMsg", "您输入的原始密码不正确！");
            return map;
        }
        // 验证新密码长度
        if (newPassword.length() < 8){
            map.put("newMsg", "密码长度不能小于8位!");
            return map;
        }
        // 验证确认密码和新密码是否一致
        if (!confirmPassword.equals(newPassword)){
            map.put("confirmMsg", "两次输入的密码不一致!");
            return map;
        }
        // 没有以上的错误，更新密码
        newPassword = CommunityUtil.md5(newPassword + user.getSalt());
        userMapper.updatePassword(user.getId(), newPassword);
        deleteCache(user.getId());
        return map;
    }
    public User findUserByName(String username){
        return userMapper.selectByName(username);
    }

    // 1.优先从缓存中获取用户信息
    private User getCache(int userId){
        String redisKey = RedisKeyUtil.getUserKey(userId);
        return (User) redisTemplate.opsForValue().get(redisKey);
    }
    // 2.缓存中没有则初始化并放入缓存
    private User initCache(int userId){
        User user = userMapper.selectById(userId);
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.opsForValue().set(redisKey, user, 3600, TimeUnit.SECONDS);
        return user;
    }
    // 3.更新(不包括insert，只有update)用户信息时删除缓存中的数据,先更新再删除缓存
    private void deleteCache(int userId){
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.delete(redisKey);
    }

    // 获取权限
    public Collection<? extends GrantedAuthority> getAuthorities(int userId){
        User user = this.findUserById(userId);

        List<GrantedAuthority> list = new ArrayList<>();
        list.add(new GrantedAuthority() {
            @Override
            public String getAuthority() {
                switch (user.getType()) {
                    case 1:
                        return AUTHORITY_ADMIN;
                    case 2:
                        return AUTHORITY_MODERATOR;
                    default:
                        return AUTHORITY_USER;
                }
            }
        });
        return list;
    }
}
