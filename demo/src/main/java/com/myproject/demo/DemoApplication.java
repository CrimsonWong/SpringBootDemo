package com.myproject.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

//    @Bean
//    public ShallowEtagHeaderFilter shallowEtagHeaderFilter() {
//        return new ShallowEtagHeaderFilter();
//    }

    @Bean
    public FilterRegistrationBean filterRegistrationBean () {
        ShallowEtagHeaderFilter eTagFilter = new ShallowEtagHeaderFilter();
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(eTagFilter);
        registration.addUrlPatterns("/*");
        return registration;
    }
}
