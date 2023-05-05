package com.nowcoder.community.service;

import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FollowService implements CommunityConstant{
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UserMapper userMapper;

    // followee:userId:entityType->zset(entityId,now)
    // follower:entityType:entityId->zset(userId,now)

    // 关注
    public void follow(int userId, int entityType, int entityId){
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
                operations.multi();
                redisTemplate.opsForZSet().add(followeeKey, entityId, System.currentTimeMillis());
                redisTemplate.opsForZSet().add(followerKey, userId, System.currentTimeMillis());
                return operations.exec();
            }
        });
    }
    // 取消关注
    public void unfollow(int userId, int entityType, int entityId){
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
                operations.multi();
                redisTemplate.opsForZSet().remove(followeeKey, entityId);
                redisTemplate.opsForZSet().remove(followerKey, userId);
                return operations.exec();
            }
        });
    }
    // 统计关注的实体数
    public long findFolloweeCount(int userId, int entityType){
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        return redisTemplate.opsForZSet().zCard(followeeKey);
    }
    // 统计某个用户拥有的粉丝数
    public long findFollowerCount(int userId, int entityType){
        String followerKey = RedisKeyUtil.getFollowerKey(entityType, userId);
        return redisTemplate.opsForZSet().zCard(followerKey);
    }
    // 查询某人对某实体的关注状态
    public boolean hasFollowed(int userId, int entityType, int entityId){
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        return redisTemplate.opsForZSet().score(followeeKey, entityId) != null;
    }
    // 获得某人的关注列表
    public List<Map<String, Object>> findFollowees(int userId, int offset, int limit){
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, ENTITY_TYPE_USER);
        Set<Integer> ids = redisTemplate.opsForZSet().range(followeeKey, offset, offset+limit-1);

        if (ids == null){
            return null;
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for(Integer followeeId:ids){
            Map<String, Object> map = new HashMap<>();
            User u = userMapper.selectById(followeeId);
            map.put("user", u);
            Double score = redisTemplate.opsForZSet().score(followeeKey, followeeId);
            map.put("followTime", new Date(score.longValue()));
            list.add(map);
        }
        return list;
    }
    // 获得某人的粉丝列表
    public List<Map<String, Object>> findFollowers(int userId, int offset, int limit){
        String followerKey = RedisKeyUtil.getFollowerKey(ENTITY_TYPE_USER, userId);
        Set<Integer> ids = redisTemplate.opsForZSet().range(followerKey, offset, offset+limit-1);

        if (ids == null){
            return null;
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for(Integer followerId:ids){
            Map<String, Object> map = new HashMap<>();
            User u = userMapper.selectById(followerId);
            map.put("user", u);
            Double score = redisTemplate.opsForZSet().score(followerKey, followerId);
            map.put("followTime", new Date(score.longValue()));
            list.add(map);
        }
        return list;
    }
}
