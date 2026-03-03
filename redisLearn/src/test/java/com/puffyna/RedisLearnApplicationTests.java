package com.puffyna;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puffyna.redis.pojo.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest
class RedisLearnApplicationTests {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final ObjectMapper mapper=new ObjectMapper();

    @Test
    void contextLoads() {

    }

    void testString() throws JsonProcessingException {
        User user=new User("puuu",19);
        String s = mapper.writeValueAsString(user);
        redisTemplate.opsForValue().set("user:puffyna",s);
        String s1 = redisTemplate.opsForValue().get("user:puffyna");
        User user1 = mapper.readValue(s1, User.class);
        System.out.println(user1);
    }

    @Test
    void testHash(){
        redisTemplate.opsForHash().put("user:puffy","name","puffy");
        redisTemplate.opsForHash().put("user:puffy","age","19");
        Map<Object, Object> entries = redisTemplate.opsForHash().entries("user:puffy");
        System.out.println(entries);
    }
}
