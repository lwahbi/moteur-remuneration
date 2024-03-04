package ma.globalperformance.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.FeignClientFactoryBean;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

public class FeignConfig {

//    @Autowired
//    private RestTemplateBuilder restTemplateBuilder;
//
//    @Bean
//    public FeignClientFactoryBean feignClientFactoryBean() {
//        FeignClientFactoryBean factoryBean = new FeignClientFactoryBean();
//        factoryBean.setClient(restTemplateBuilder.build()); // Set the built RestTemplate as the client
//        factoryBean.setEncoder(new SpringEncoder(new Jackson2ObjectMapperBuilder().create().build()));
//        factoryBean.setDecoder(new SpringDecoder(new Jackson2ObjectMapperBuilder().create().build()));
//        factoryBean.setContract(new SpringMvcContract());
//        return factoryBean;
//    }
}
