package com.puffyna.service;

import com.puffyna.dto.Result;
import com.puffyna.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IShopService extends IService<Shop> {

    Result queryShopById(Long id) throws InterruptedException;

    Result update(Shop shop);
}
