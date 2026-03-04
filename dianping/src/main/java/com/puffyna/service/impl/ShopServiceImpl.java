package com.puffyna.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.puffyna.dto.Result;
import com.puffyna.entity.Shop;
import com.puffyna.mapper.ShopMapper;
import com.puffyna.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.puffyna.utils.CacheClient;
import com.puffyna.utils.RedisConstants;
import com.puffyna.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;



    @Override
    public Result queryShopById(Long id) throws InterruptedException {
        //解决缓存穿透
//        Shop shop=queryWithPassThrough(id);
        Shop shop1=cacheClient.queryWithPassThrough(RedisConstants.CACHE_SHOP_KEY,id,Shop.class,(id1)->getById(id1)
        ,RedisConstants.CACHE_SHOP_TTL,TimeUnit.MINUTES);
        //使用互斥锁解决缓存击穿
//        Shop shop=queryWithMutex(id);
        //使用逻辑日期解决缓存击穿
//        Shop shop=queryWithLogic(id);
        Shop shop2=cacheClient.queryWithLogic(RedisConstants.CACHE_SHOP_KEY,id,Shop.class,RedisConstants.LOCK_SHOP_KEY,
                (id1)->getById(id1),RedisConstants.CACHE_SHOP_TTL,TimeUnit.MINUTES);
        if(shop2==null){
            return Result.fail("店铺信息不存在");
        }
        //7.返回店铺信息
        return Result.ok(shop2);
    }
//    public Shop queryWithLogic(Long id){
//        String key=RedisConstants.CACHE_SHOP_KEY+id;
//        //1.从redis中查询商铺缓存
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//        //2.判断缓存是否存在
//        if(StrUtil.isBlank(shopJson)){
//            //3.存在直接返回
//            return null;
//        }
//        //4.命中
//        // 4.1将缓存中的json数据反序列化为对象
//        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
//        JSONObject data = (JSONObject)redisData.getData();
//        Shop shop = JSONUtil.toBean(data, Shop.class);
//        LocalDateTime expireTime = redisData.getExpireTime();
//        //4.2判断缓存是否过期
//        if(expireTime.isAfter(LocalDateTime.now())){
//            //4.3缓存未过期，返回店铺信息
//            return shop;
//        }
//        //5.尝试获取互斥锁
//        // 5.1获取锁成功
//        String lockKey=RedisConstants.LOCK_SHOP_KEY+id;
//        if(getLock(lockKey)){
//            //5.2开启独立线程
//            try {
//                CACHE_REBUILD_EXECUTOR.submit(()->{
//                    this.saveShop2Redis(id,1800L);
//                });
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            } finally {
//                //6.释放互斥锁
//                delLock(lockKey);
//            }
//        }
//        //7.返回店铺信息
//        return shop;
//    }

//    public Shop queryWithMutex(Long id) throws InterruptedException {
//        String key=RedisConstants.CACHE_SHOP_KEY+id;
//        //1.从redis中查询商铺缓存
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//        //2.判断缓存是否存在
//        if(StrUtil.isNotBlank(shopJson)){
//            //3.存在直接返回
//            return JSONUtil.toBean(shopJson,Shop.class);
//        }
//        //(防止缓存穿透)判断缓存的内容是否为空字符串，如果是，证明没有真实店铺数据，返回错误
//        if(shopJson!=null){
//            return null;
//        }
//        //4.不存在
//        // 4.1首先获取互斥锁
//        String lockKey="cache:lock"+id;
//        Shop shop = null;
//        try {
//            if(!getLock(lockKey)){
//                //4.2如果获取互斥锁失败，休眠一段时间
//                Thread.sleep(200);
//                queryWithMutex(id);
//            }
//            //4.2互斥锁获取成功，先判断缓存中是否存在内容
//            if(StrUtil.isNotBlank(stringRedisTemplate.opsForValue().get(key))){
//                return JSONUtil.toBean(shopJson,Shop.class);
//            }
//            //4.3根据id查询数据
//            shop = getById(id);
//            //5.数据库中不存在，返回错误
//            if(shop==null){
//                //给redis中传入空值，同时设置一个较短的有效时间，防止缓存穿透
//                stringRedisTemplate.opsForValue().set(key,"",RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
//                return null;
//            }
//            //6.数据库中存在，写入redis，同时设置有效时间
//            stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        } finally {
//            delLock(lockKey);
//        }
//
//        //7.返回店铺信息
//        return shop;
//    }

//    public Shop queryWithPassThrough(Long id){
//        String key=RedisConstants.CACHE_SHOP_KEY+id;
//        //1.从redis中查询商铺缓存
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//        //2.判断缓存是否存在
//        if(StrUtil.isNotBlank(shopJson)){
//            //3.存在直接返回
//            return JSONUtil.toBean(shopJson,Shop.class);
//        }
//        //判断缓存的内容是否为空字符串，如果是，证明没有真实店铺数据，返回错误
//        if(shopJson!=null){
//            return null;
//        }
//        //4.不存在，根据id从数据库中查询
//        Shop shop = getById(id);
//        //5.数据库中不存在，返回错误
//        if(shop==null){
//            //给redis中传入空值，同时设置一个较短的有效时间，防止缓存穿透
//            stringRedisTemplate.opsForValue().set(key,"",RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
//            return null;
//        }
//        //6.数据库中存在，写入redis，同时设置有效时间
//        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
//        //7.返回店铺信息
//        return shop;
//    }

//    public boolean getLock(String key){
//        Boolean b = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.MILLISECONDS);
//        return BooleanUtil.isTrue(b);
//    }
//
//    public void delLock(String key){
//        stringRedisTemplate.delete(key);
//    }

    public void saveShop2Redis(Long id,Long expireTime){
        Shop shop = getById(id);
        RedisData redisData=new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireTime));
        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY+id,JSONUtil.toJsonStr(redisData));
    }

    @Override
    public Result update(Shop shop) {
        Long id = shop.getId();
        if(id==null){
            return Result.fail("店铺id不能为空");
        }
        //1.更新数据库
         updateById(shop);
         //2.删除缓存
        String key=RedisConstants.CACHE_SHOP_KEY+id;
        stringRedisTemplate.delete(key);
        return Result.ok();
    }


}
