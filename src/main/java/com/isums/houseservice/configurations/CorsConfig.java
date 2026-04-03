//package com.isums.houseservice.configurations;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.cors.CorsConfiguration;
//import org.springframework.web.cors.reactive.CorsWebFilter;
//import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
//
//import java.util.List;
//
//@Configuration
//public class CorsConfig {
//
//    @Bean
//    public CorsWebFilter corsWebFilter() {
//        CorsConfiguration config = new CorsConfiguration();
//        config.setAllowedOrigins(List.of(
//                "http://localhost:5173/",
//                "http://localhost:5173",
//                "http://localhost:8000/",
//                "http://localhost:8000",
//                "https://autopsic-kristan-sublabially.ngrok-free.dev",
//                "https://autopsic-kristan-sublabially.ngrok-free.dev/"
//        ));
//        config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
//        config.addAllowedHeader("*");
//
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", config);
//        return new CorsWebFilter(source);
//    }
//}
