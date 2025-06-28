package com.dolpin.global.redis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    // ===================== 핵심 기본 조작 =====================

    public void set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            log.debug("Redis SET: {}", key);
        } catch (Exception e) {
            log.error("Redis SET failed: {} - {}", key, e.getMessage());
            throw new RuntimeException("Redis 저장 실패", e);
        }
    }

    public void set(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
            log.debug("Redis SET with TTL: {} ({})", key, ttl);
        } catch (Exception e) {
            log.error("Redis SET with TTL failed: {} - {}", key, e.getMessage());
            throw new RuntimeException("Redis 저장 실패", e);
        }
    }

    public <T> T get(String key, Class<T> clazz) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                log.debug("Redis GET miss: {}", key);
                return null;
            }
            log.debug("Redis GET hit: {}", key);
            return clazz.cast(value);
        } catch (Exception e) {
            log.error("Redis GET failed: {} - {}", key, e.getMessage());
            return null;
        }
    }

    public Object get(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                log.debug("Redis GET hit: {}", key);
            } else {
                log.debug("Redis GET miss: {}", key);
            }
            return value;
        } catch (Exception e) {
            log.error("Redis GET failed: {} - {}", key, e.getMessage());
            return null;
        }
    }

    public void delete(String key) {
        try {
            Boolean deleted = redisTemplate.delete(key);
            log.debug("Redis DELETE: {} ({})", key, deleted);
        } catch (Exception e) {
            log.error("Redis DELETE failed: {} - {}", key, e.getMessage());
        }
    }

    public void delete(Collection<String> keys) {
        try {
            Long deleted = redisTemplate.delete(keys);
            log.debug("Redis DELETE batch: {} keys deleted", deleted);
        } catch (Exception e) {
            log.error("Redis DELETE batch failed: {}", e.getMessage());
        }
    }

    public boolean exists(String key) {
        try {
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("Redis EXISTS failed: {} - {}", key, e.getMessage());
            return false;
        }
    }

    public void expire(String key, Duration ttl) {
        try {
            redisTemplate.expire(key, ttl);
            log.debug("Redis EXPIRE: {} ({})", key, ttl);
        } catch (Exception e) {
            log.error("Redis EXPIRE failed: {} - {}", key, e.getMessage());
        }
    }

    // ===================== 배치 조작 (핵심) =====================

    public List<Object> multiGet(Collection<String> keys) {
        try {
            List<Object> values = redisTemplate.opsForValue().multiGet(keys);
            log.debug("Redis MGET: {} keys", keys.size());
            return values != null ? values : Collections.emptyList();
        } catch (Exception e) {
            log.error("Redis MGET failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public void multiSet(Map<String, Object> keyValueMap) {
        try {
            redisTemplate.opsForValue().multiSet(keyValueMap);
            log.debug("Redis MSET: {} keys", keyValueMap.size());
        } catch (Exception e) {
            log.error("Redis MSET failed: {}", e.getMessage());
            throw new RuntimeException("Redis 배치 저장 실패", e);
        }
    }

    // ===================== Set 조작 (토큰 관리용) =====================

    public Long addToSet(String key, Object... values) {
        try {
            Long added = redisTemplate.opsForSet().add(key, values);
            log.debug("Redis SADD: {} -> {} items added", key, added);
            return added;
        } catch (Exception e) {
            log.error("Redis SADD failed: {} - {}", key, e.getMessage());
            throw new RuntimeException("Redis Set 추가 실패", e);
        }
    }

    public Long removeFromSet(String key, Object... values) {
        try {
            Long removed = redisTemplate.opsForSet().remove(key, values);
            log.debug("Redis SREM: {} -> {} items removed", key, removed);
            return removed;
        } catch (Exception e) {
            log.error("Redis SREM failed: {} - {}", key, e.getMessage());
            return 0L;
        }
    }

    public Set<Object> getSetMembers(String key) {
        try {
            Set<Object> members = redisTemplate.opsForSet().members(key);
            log.debug("Redis SMEMBERS: {} -> {} items", key, members != null ? members.size() : 0);
            return members != null ? members : Collections.emptySet();
        } catch (Exception e) {
            log.error("Redis SMEMBERS failed: {} - {}", key, e.getMessage());
            return Collections.emptySet();
        }
    }

    // ===================== 숫자 조작 (카운터용) =====================

    public Long increment(String key) {
        try {
            Long result = stringRedisTemplate.opsForValue().increment(key);
            log.debug("Redis INCR: {} -> {}", key, result);
            return result;
        } catch (Exception e) {
            log.error("Redis INCR failed: {} - {}", key, e.getMessage());
            throw new RuntimeException("Redis 증가 실패", e);
        }
    }

    public Long decrement(String key) {
        try {
            Long result = stringRedisTemplate.opsForValue().decrement(key);
            log.debug("Redis DECR: {} -> {}", key, result);
            return result;
        } catch (Exception e) {
            log.error("Redis DECR failed: {} - {}", key, e.getMessage());
            throw new RuntimeException("Redis 감소 실패", e);
        }
    }

    // ===================== 패턴 조작 (관리용) =====================

    public Set<String> getKeysByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            log.debug("Redis KEYS: {} -> {} matches", pattern, keys != null ? keys.size() : 0);
            return keys != null ? keys : Collections.emptySet();
        } catch (Exception e) {
            log.error("Redis KEYS failed: {} - {}", pattern, e.getMessage());
            return Collections.emptySet();
        }
    }

    public void deleteByPattern(String pattern) {
        try {
            Set<String> keys = getKeysByPattern(pattern);
            if (!keys.isEmpty()) {
                delete(keys);
                log.info("Redis deleted {} keys with pattern: {}", keys.size(), pattern);
            }
        } catch (Exception e) {
            log.error("Redis delete by pattern failed: {} - {}", pattern, e.getMessage());
        }
    }

    // ===================== Pipeline 및 고급 기능 =====================

    public List<Object> executePipelined(RedisCallback<Object> callback) {
        try {
            List<Object> results = redisTemplate.executePipelined(callback);
            log.debug("Redis Pipeline executed: {} operations", results.size());
            return results;
        } catch (Exception e) {
            log.error("Redis Pipeline execution failed", e);
            return Collections.emptyList();
        }
    }

    public void batchSetWithTtlSimple(Map<String, Object> keyValueMap, Duration ttl) {
        try {
            // 1. 먼저 모든 키-값을 배치로 저장
            redisTemplate.opsForValue().multiSet(keyValueMap);

            // 2. 각 키에 TTL 설정
            keyValueMap.keySet().forEach(key -> {
                try {
                    redisTemplate.expire(key, ttl);
                } catch (Exception e) {
                    log.warn("TTL 설정 실패: {}", key, e);
                }
            });

            log.debug("Redis batch set with TTL: {} keys", keyValueMap.size());
        } catch (Exception e) {
            log.error("Redis batch set with TTL failed: {}", e.getMessage());
            throw new RuntimeException("Redis 배치 저장 실패", e);
        }
    }
}
