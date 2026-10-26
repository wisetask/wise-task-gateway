package ru.leti.wise.task.gateway.configuration;

import lombok.RequiredArgsConstructor;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import ru.leti.graphql.types.Profile;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

@Slf4j
@EnableCaching
@Configuration
@RequiredArgsConstructor
public class CacheConfiguration {
    private final ObjectMapper objectMapper;

    @Value("${app.cache.ttl}")
    private Duration cacheTtl;

    @Bean
    public CacheManager localCacheManager(
            @Value("${cache.max-size:1000}") long maxSize,
            @Value("${cache.ttl:1h}") Duration ttl
    ) {
        log.debug("localCacheManager configuration, configuring cache with maxSize={} and ttl={}", maxSize, ttl);
        var cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(ttl));
        return cacheManager;
    }

    @Bean
    public RedisCacheConfiguration redisCacheConfiguration() {
        var keySerializer = new StringRedisSerializer();
        var valueSerializer = new JacksonJsonRedisSerializer<>(objectMapper, Profile.class);
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(cacheTtl)
                .disableCachingNullValues()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(keySerializer))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(
                                        valueSerializer
                                ));
    }

    @Bean
    @Primary
    public CacheManager cacheManager(
            RedisConnectionFactory connectionFactory
    ) {
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(redisCacheConfiguration())
                .transactionAware()
                .build();
    }
}
