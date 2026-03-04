package com.puffyna.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.puffyna.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Component
@Slf4j
public class CacheClient {

    private StringRedisTemplate stringRedisTemplate;
    private static final ExecutorService CACHE_REBUILD_EXECUTOR= Executors.newFixedThreadPool(10);

    public CacheClient(StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate=stringRedisTemplate;
    }

    public void set(String key, Object value, Long time, TimeUnit unit){
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value),time,unit);
    }
    public void setWithLogicExpire(String key, Object value, Long time, TimeUnit unit){
        RedisData redisData=new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData),time,unit);
    }

    //缓存击穿问题
    public <R,ID> R queryWithPassThrough(String prefix, ID id, Class<R> type, Function<ID,R> func,
                                         Long time, TimeUnit unit){
        String key=prefix+id;
        //1.从redis中查询商铺缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        //2.判断缓存是否存在
        if(StrUtil.isNotBlank(json)){
            //3.存在直接返回
            return JSONUtil.toBean(json,type);
        }
        //判断缓存的内容是否为空字符串，如果是，证明没有真实店铺数据，返回错误
        if(json!=null){
            return null;
        }
        //4.不存在，根据id从数据库中查询
        R r=func.apply(id);
        //5.数据库中不存在，返回错误
        if(r==null){
            //给redis中传入空值，同时设置一个较短的有效时间，防止缓存穿透
            stringRedisTemplate.opsForValue().set(key,"",RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        //6.数据库中存在，写入redis，同时设置有效时间
        this.set(prefix+id,r,time,unit);
        //7.返回店铺信息
        return r;
    }

    public boolean getLock(String key){
        Boolean b = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(b);
    }

    public void delLock(String key){
        stringRedisTemplate.delete(key);
    }

    public <R,ID> R queryWithLogic(String prefix,ID id,Class<R> type,String lock,Function<ID,R> func,
    Long time, TimeUnit unit){
        String key=prefix+id;
        //1.从redis中查询商铺缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        //2.判断缓存是否存在
        if(StrUtil.isBlank(json)){
            //3.存在直接返回
            return null;
        }
        //4.命中
        // 4.1将缓存中的json数据反序列化为对象
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        JSONObject data = (JSONObject)redisData.getData();
        R r = JSONUtil.toBean(data, type);
        LocalDateTime expireTime = redisData.getExpireTime();
        //4.2判断缓存是否过期
        if(expireTime.isAfter(LocalDateTime.now())){
            //4.3缓存未过期，返回店铺信息
            return r;
        }
        //5.尝试获取互斥锁
        // 5.1获取锁成功
        String lockKey=lock+id;
        if(getLock(lockKey)){
            //5.2开启独立线程
            try {
                CACHE_REBUILD_EXECUTOR.submit(()->{
                    R r1=func.apply(id);
                    this.setWithLogicExpire(key,r1,time,unit);
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                //6.释放互斥锁
                delLock(lockKey);
            }
        }
        //7.返回店铺信息
        return r;
    }
}
