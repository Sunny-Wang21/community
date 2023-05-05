package com.nowcoder.community.service;

import com.nowcoder.community.config.RedisConfig;
import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

@Service
public class LikeService {

    @Autowired
    private RedisTemplate redisTemplate;

    public void like(int userId, int entityType, int entityId, int entityUserId){
/*        String redisKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        if (redisTemplate.opsForSet().isMember(redisKey, userId)){
            redisTemplate.opsForSet().remove(redisKey, userId);
        }else{
            redisTemplate.opsForSet().add(redisKey, userId);
        }*/
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
                String userLikeKey = RedisKeyUtil.getUserLikeKey(entityUserId);
                boolean ismember = redisTemplate.opsForSet().isMember(entityLikeKey, userId);
                operations.multi();
                if (ismember){
                    redisTemplate.opsForSet().remove(entityLikeKey, userId);
                    redisTemplate.opsForValue().decrement(userLikeKey);
                }else{
                    redisTemplate.opsForSet().add(entityLikeKey, userId);
                    redisTemplate.opsForValue().increment(userLikeKey);
                }
                return operations.exec();
            }
        });

    }
    // 查询某实体的点赞数量
    public long getEntityLikeCount(int entityType, int entityId){
        String redisKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        return redisTemplate.opsForSet().size(redisKey);
    }
    // 查询某人对某实体的点赞状态
    public int getEntityLikeStatus(int userId, int entityType, int entityId){
        String redisKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        if (redisTemplate.opsForSet().isMember(redisKey, userId)){
            return 1;//已赞
        }else{
            return 0;
        }
    }

    public int getUserLikeCount(int userId){
        String userLikeKey = RedisKeyUtil.getUserLikeKey(userId);
        Integer userLikeCount = (Integer) redisTemplate.opsForValue().get(userLikeKey);
        if (userLikeCount != null) {
            return userLikeCount.intValue();
        }else{
            return 0;
        }
    }
}
