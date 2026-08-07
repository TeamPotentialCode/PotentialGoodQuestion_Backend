package com.potential.goodquestion.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("tts");
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(200)        // 최대 200개 항목
                .expireAfterWrite(6, TimeUnit.HOURS)
        );
        return manager;
    }
}
