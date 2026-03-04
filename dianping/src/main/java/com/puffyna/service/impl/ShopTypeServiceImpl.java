package com.puffyna.service.impl;

import cn.hutool.json.JSONUtil;
import com.puffyna.dto.Result;
import com.puffyna.entity.ShopType;
import com.puffyna.mapper.ShopTypeMapper;
import com.puffyna.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.puffyna.utils.RedisConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public Result queryTypeList() {
        //1.从redis中查询商品类型信息
        List<String> type = stringRedisTemplate.opsForList().range(RedisConstants.CACHE_SHOPTYPE_KEY, 0, -1);
        //2.存在，直接返回
        if(type!=null&&!type.isEmpty()){
            List<ShopType> shopType = type.stream().map(json -> JSONUtil.toBean(json, ShopType.class)).collect(Collectors.toList());
            return Result.ok(shopType);
        }
        //3.不存在，从数据库中查询商品类型信息
        List<ShopType> typeList = query().orderByAsc("sort").list();
        if (typeList == null || typeList.isEmpty()) {
            return Result.fail("店铺类型不存在");
        }
        stringRedisTemplate.delete(RedisConstants.CACHE_SHOPTYPE_KEY);
        //4.将结果封装到redis中
        for(ShopType s:typeList){
            stringRedisTemplate.opsForList().rightPush(RedisConstants.CACHE_SHOPTYPE_KEY,JSONUtil.toJsonStr(s));
        }
        //5.返回商品类型信息结果
        return Result.ok(typeList);
    }
}
