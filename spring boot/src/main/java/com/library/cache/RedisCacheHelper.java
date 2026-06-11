package com.library.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.function.Supplier;

/**
 * Redis 缓存工具：查缓存优先，未命中再查库；增改同步写入；删精确失效。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisCacheHelper {

    private final CacheManager cacheManager;
    private final StringRedisTemplate stringRedisTemplate;

    @SuppressWarnings("unchecked")
    public <T> T get(String cacheName, Object key) {
        Cache cache = getCache(cacheName);
        if (cache == null) {
            return null;
        }
        try {
            Cache.ValueWrapper wrapper = cache.get(key);
            return wrapper != null ? (T) wrapper.get() : null;
        } catch (RuntimeException ex) {
            log.warn("缓存反序列化失败，已清除脏数据 cache={} key={}: {}", cacheName, key, ex.getMessage());
            cache.evict(key);
            return null;
        }
    }

    public void put(String cacheName, Object key, Object value) {
        if (value == null) {
            return;
        }
        Cache cache = getCache(cacheName);
        if (cache != null) {
            cache.put(key, value);
        }
    }

    public void evict(String cacheName, Object key) {
        Cache cache = getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }

  /** 先查缓存，未命中则执行 loader 并写入缓存 */
    public <T> T getOrLoad(String cacheName, Object key, Supplier<T> loader) {
        T cached = get(cacheName, key);
        if (cached != null) {
            return cached;
        }
        T loaded = loader.get();
        if (loaded != null) {
            put(cacheName, key, loaded);
        }
        return loaded;
    }

    /** 按 key 前缀批量失效（如 search_v2_ 开头的搜索缓存） */
    public void evictByKeyPrefix(String cacheName, String keyPrefix) {
        String pattern = cacheName + "::" + keyPrefix + "*";
        try {
            Set<String> keys = stringRedisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("按前缀失效缓存失败 pattern={}: {}", pattern, e.getMessage());
        }
    }

    /** 清空某个缓存区域的所有 key */
    public void evictAll(String cacheName) {
        evictByKeyPrefix(cacheName, "");
    }

    /** 生成分页查询缓存 key */
    public static String pageKey(Pageable pageable) {
        String sortPart = pageable.getSort().stream()
                .map(o -> o.getProperty() + "_" + o.getDirection())
                .reduce((a, b) -> a + "," + b)
                .orElse("unsorted");
        return "page_" + pageable.getPageNumber() + "_" + pageable.getPageSize() + "_" + sortPart;
    }

    private Cache getCache(String cacheName) {
        return cacheManager.getCache(cacheName);
    }
}
