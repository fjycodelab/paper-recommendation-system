package com.lencode.paper.behavior.service;

import java.time.Duration;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class RedisLockService {

    private static final Logger log = LoggerFactory.getLogger(RedisLockService.class);
    private static final String RELEASE_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('del', KEYS[1]) "
                    + "else return 0 end";

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> releaseScript;

    public RedisLockService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.releaseScript = new DefaultRedisScript<>();
        this.releaseScript.setScriptText(RELEASE_SCRIPT);
        this.releaseScript.setResultType(Long.class);
    }

    public boolean tryLock(String key, String token, long ttlMs) {
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(key, token, Duration.ofMillis(ttlMs));
        return Boolean.TRUE.equals(locked);
    }

    public void release(String key, String token) {
        if (key == null || token == null) {
            return;
        }
        try {
            // 用 token 比对后再删除，避免误删另一个请求刚拿到的锁。
            redisTemplate.execute(releaseScript, Collections.singletonList(key), token);
        } catch (RuntimeException ex) {
            log.warn("Failed to release redis lock {}", key, ex);
        }
    }
}
