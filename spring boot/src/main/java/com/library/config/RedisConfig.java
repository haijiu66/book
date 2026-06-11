package com.library.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.*;

@Slf4j
@Configuration
public class RedisConfig implements CachingConfigurer {

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @PostConstruct
    public void clearOldCacheOnStartup() {
        try {
            redisConnectionFactory.getConnection().serverCommands().flushDb();
            log.info("Redis 已清空（含旧版 Hibernate 实体脏缓存）");
        } catch (Exception e) {
            log.warn("Redis 清空失败: {}", e.getMessage());
        }
    }

    /**
     * Redis 主缓存
     * 不启用全局 DefaultTyping——实体类通过 @JsonTypeInfo(use=CLASS) 自带类型信息
     * 集合/Hibernate代理无 @JsonTypeInfo → 不加类型信息 → 干净序列化
     */
    @Primary
    @Bean("cacheManager")
    @Override
    public CacheManager cacheManager() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        // 安全类型验证：允许实体类和标准集合；@JsonSerialize(as=HashSet) 已防止PersistentSet泄露
        mapper.activateDefaultTyping(
            BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.library.entity.")
                .allowIfSubType("com.library.dto.")
                .allowIfSubType("com.library.dto.cache.")
                .allowIfSubType("org.springframework.data.domain.")
                .allowIfSubType("java.util.ArrayList")
                .allowIfSubType("java.util.LinkedList")
                .allowIfSubType("java.util.HashSet")
                .allowIfSubType("java.util.LinkedHashSet")
                .allowIfSubType("java.util.TreeSet")
                .allowIfSubType("java.util.HashMap")
                .allowIfSubType("java.util.LinkedHashMap")
                .allowIfSubType("java.util.TreeMap")
                .build(),
            ObjectMapper.DefaultTyping.NON_FINAL);

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(mapper);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(serializer))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> configs = new HashMap<>();
        configs.put("books", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        configs.put("book", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        configs.put("ebooks", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        configs.put("ebook", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        configs.put("categories", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        configs.put("ranking", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        configs.put("users", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        configs.put("admins", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        configs.put("borrows", defaultConfig.entryTtl(Duration.ofMinutes(3)));
        configs.put("blacklist", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        configs.put("favorites", defaultConfig.entryTtl(Duration.ofMinutes(3)));
        configs.put("loginLogs", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        configs.put("auditLogs", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        configs.put("superAdmins", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(configs)
                .build();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException ex, Cache cache, Object key) {
                log.warn("Cache get error key={}: {}", key, ex.getMessage());
                try { cache.evict(key); } catch (Exception ignored) {}
            }
            @Override
            public void handleCachePutError(RuntimeException ex, Cache cache, Object key, Object value) {
                log.warn("Cache put error (Redis may be down): key={}", key);
            }
            @Override
            public void handleCacheEvictError(RuntimeException ex, Cache cache, Object key) {
                log.warn("Cache evict error (Redis may be down): key={}", key);
            }
            @Override
            public void handleCacheClearError(RuntimeException ex, Cache cache) {
                log.warn("Cache clear error (Redis may be down)");
            }
        };
    }
}
