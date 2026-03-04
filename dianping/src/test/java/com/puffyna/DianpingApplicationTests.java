package com.puffyna;

import com.puffyna.service.IShopService;
import com.puffyna.service.impl.ShopServiceImpl;
import com.puffyna.utils.CacheClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class DianpingApplicationTests {

    @Resource
    private ShopServiceImpl shopService;

    @Test
    void contextLoads() {
    }

    @Test
    void testShop(){
        shopService.saveShop2Redis(1L,10L);
    }
}
