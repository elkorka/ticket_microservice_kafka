package com.elz.bookingService.config;


import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig  {

    @Bean
    public OpenAPI inventoryServiceApi(){
        return new OpenAPI()
                .info(new io.swagger.v3.oas.models.info.Info()
                        .title("Booking Service API")
                        .description("booking Service API for elz")
                        .version("v1.0.0"));


    }
}

