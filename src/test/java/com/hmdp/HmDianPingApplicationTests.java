package com.hmdp;

import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@Slf4j
class HmDianPingApplicationTests {
    @Autowired
    private ShopServiceImpl shopService;
    @Autowired
    private RedisIdWorker redisIdWorker;
    private final ExecutorService es = Executors.newFixedThreadPool(500);
    @Resource
    private RedissonClient redissonClient;

    private RLock lock;

    @BeforeEach
    void setUp() {
        lock = redissonClient.getLock("lock");
    }

    @Test
    void method1() {
        boolean success = lock.tryLock();
        if (!success) {
            log.error("获取锁失败，1");
            return;
        }
        try {
            log.info("获取锁成功");
            method2();
        } finally {
            log.info("释放锁，1");
            lock.unlock();
        }
    }

    void method2() {
        RLock lock = redissonClient.getLock("lock");
        boolean success = lock.tryLock();
        if (!success) {
            log.error("获取锁失败，2");
            return;
        }
        try {
            log.info("获取锁成功，2");
        } finally {
            log.info("释放锁，2");
            lock.unlock();
        }
    }

    @Test
    void testIdWorker() throws InterruptedException {
        System.out.println("开始");
        CountDownLatch latch = new CountDownLatch(300);

        Runnable task = () -> {
            for (int i = 0; i < 100; i++) {
                long id = redisIdWorker.nextId("order");
                System.out.println("id = " + id);
            }
            latch.countDown();
        };
        long begin = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            es.submit(task);
        }
        latch.await();
        long end = System.currentTimeMillis();
        System.out.println("time = " + (end - begin));
    }

    @Test
    void testRedisson() throws InterruptedException {
        RLock lock = redissonClient.getLock("anyLock");
        boolean success = lock.tryLock(1, 10, TimeUnit.SECONDS);
        if (success) {
            try {
                System.out.println("执行业务");
            } finally {
                lock.unlock();
            }
        }
    }

    @Test
    public void test() {
        shopService.saveShop2Redis(2L, 2L);
    }
}
