package com.puffyna.controller;


import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.puffyna.dto.Result;
import com.puffyna.entity.Shop;
import com.puffyna.entity.ShopType;
import com.puffyna.service.IShopTypeService;
import com.puffyna.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/shop-type")
public class ShopTypeController {
    @Resource
    private IShopTypeService typeService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @GetMapping("list")
    public Result queryTypeList() {
        return typeService.queryTypeList();
    }
}
