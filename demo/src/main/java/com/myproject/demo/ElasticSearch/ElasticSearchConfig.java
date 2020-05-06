package com.myproject.demo.ElasticSearch;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticSearchConfig {
    @Bean("elasticSearchBean")
    public ElasticSearchBean elasticSearchBean() {
        return new ElasticSearchBean() ;
    }

}
