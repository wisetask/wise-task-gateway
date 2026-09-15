package ru.leti.wise.task.gateway.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import ru.leti.graphql.types.Profile;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class CacheConfiguration {
    private final ObjectMapper objectMapper;
    @Value("$cache.ttl")
    private Duration cacheTtl;

    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
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
    public CacheManager cacheManager(
            RedisConnectionFactory connectionFactory
    ) {
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(cacheConfiguration())
                .transactionAware()
                .build();
    }
}
