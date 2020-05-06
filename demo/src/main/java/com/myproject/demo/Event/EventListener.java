package com.myproject.demo.Event;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.myproject.demo.ElasticSearch.ElasticSearchBean;
import com.myproject.demo.Redis.JedisBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class EventListener implements ApplicationListener<ESEvent> {
    @Autowired
    private JedisBean jedisBean;
    @Autowired
    private ElasticSearchBean elasticSearchBean;

    @Override
    public void onApplicationEvent(ESEvent esEvent) {
        String ObjWithParent = jedisBean.rpoplpush();
        //分开object和它对应parent的信息
        try {
            String response = elasticSearchBean.putCreateDoc(ObjWithParent);
            System.out.println(response);
            System.out.println("consume items in queue");
            jedisBean.lrem(ObjWithParent);
            if(jedisBean.llen() > 0){
                String restReq = jedisBean.rpop();
                String restResponse = elasticSearchBean.putCreateDoc(ObjWithParent);
                System.out.println(restResponse);
                System.out.println("consume items in pending queue");
            }
        } catch (IOException e) {
            System.out.println("action failed");
            e.printStackTrace();
        } catch (Exception e){
            System.out.println("action failed");
            e.printStackTrace();
        }
    }
}
