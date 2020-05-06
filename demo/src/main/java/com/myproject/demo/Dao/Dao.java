package com.myproject.demo.Dao;


import javax.annotation.Resource;

import com.google.gson.JsonObject;
import com.myproject.demo.Redis.JedisBean;
import com.myproject.demo.Redis.RedisUtil;
import org.springframework.stereotype.Repository;


/**
 * @Title: Dao
 * @author shaodong
 */
@Repository
public class Dao {

    @Resource
    private RedisUtil redisUtil;

    /** Add the input JSON */
    public void addInputJSON(String key, Object value) {
        redisUtil.set(key, value);
    }

    /** Delete the input JSON */
    public void deleteInputJSON(String objectId) {
        redisUtil.del(objectId);
    }

    /** Find the input JSON */
    public Object findInputJSON(String objectId) {
        Object data;
        try{
            data = redisUtil.get(objectId);
        }catch (NullPointerException e){
            return null;
        }
        return data;
    }



}

