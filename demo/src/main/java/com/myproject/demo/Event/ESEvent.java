package com.myproject.demo.Event;

import com.myproject.demo.Redis.JedisBean;
import org.springframework.context.ApplicationEvent;


public class ESEvent extends ApplicationEvent {
    private static final long serialVersionUID = 1L;

    public ESEvent(Object source, String ObjWithParent) {
        super(source);
        JedisBean jedisBean = new JedisBean();
        jedisBean.lpush(ObjWithParent);
        System.out.println("push to queue");
    }
}
