package com.iceblyte.aicodemother.ratelimit.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private Integer redisPort;

    @Value("${spring.data.redis.username:}") // 冒号空字符串，配置缺失时不会抛异常
    private String redisUsername;

    @Value("${spring.data.redis.password}")
    private String redisPassword;

    @Value("${spring.data.redis.database}")
    private Integer redisDatabase;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        String address = "redis://" + redisHost + ":" + redisPort;
        SingleServerConfig singleServerConfig = config.useSingleServer()
                .setAddress(address)
                .setDatabase(redisDatabase)
                .setConnectionMinimumIdleSize(1)
                .setConnectionPoolSize(10)
                .setIdleConnectionTimeout(30000)
                .setConnectTimeout(5000)
                .setTimeout(3000)
                .setRetryAttempts(3)
                .setRetryInterval(1500);
        // 设置用户名，非空才赋值
        if (redisUsername != null && !redisUsername.isEmpty()) {
            singleServerConfig.setUsername(redisUsername);
        }
        // 如果有密码则设置密码
        if (redisPassword != null && !redisPassword.isEmpty()) {
            singleServerConfig.setPassword(redisPassword);
        }
        return Redisson.create(config);
    }
}
