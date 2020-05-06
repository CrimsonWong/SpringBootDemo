package com.myproject.demo.Redis;


import com.google.gson.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;


public class JedisBean {
    private static JedisPool pool = null;
    private static final String redisHost = "localhost";
    private static final Integer redisPort = 6379;
    private  final Logger logger = LoggerFactory.getLogger(this.getClass());

    public JedisBean() {
        pool = new JedisPool(redisHost, redisPort);
    }

    //单独queue方法
    public boolean insertQueue(JsonObject jsonObject) {
        try{
            Jedis jedis = pool.getResource();
            jedis.rpush("IndexAllQueue", jsonObject.toString());
            jedis.close();
        } catch(Exception e){
            logger.info("加入队列出错", e);
            return false;
        }
        return true;
    }

    public void setHash(String key, String field, String value){
        Jedis jedis = pool.getResource();
        jedis.hset(key, field, value);
        jedis.close();
    }

    public void lpush(String value){
        Jedis jedis = pool.getResource();
        try{
            jedis.lpush("queue", value);
        }finally {
            jedis.close();
        }
    }

    public String rpoplpush(){
        Jedis jedis = pool.getResource();
        try{
            String res = jedis.rpoplpush("queue", "pending");
            return res;
        }finally {
            jedis.close();
        }
    }

    public void lrem(String value){
        Jedis jedis = pool.getResource();
        try{
            jedis.lrem("pending", 0, value);
        }finally {
            jedis.close();
        }
    }

    public Long llen(){
        Jedis jedis = pool.getResource();
        try{
            Long len = jedis.llen("pending");
            return len;
        }finally {
            jedis.close();
        }
    }

    public String rpop(){
        Jedis jedis = pool.getResource();
        try{
            String res = jedis.rpop("pending");
            return res;
        }finally {
            jedis.close();
        }
    }



}
