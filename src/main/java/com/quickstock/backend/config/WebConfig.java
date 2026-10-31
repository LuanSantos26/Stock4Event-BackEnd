package com.quickstock.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${upload.dir:uploads/produtos}")
    private String uploadDir;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path produtosDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path uploadsRoot = produtosDir.getParent();
        if (uploadsRoot != null) {
            String fileLocation = "file:" + uploadsRoot.toString().replace('\\', '/') + "/";
            registry.addResourceHandler("/uploads/**")
                    .addResourceLocations(fileLocation, "classpath:/static/uploads/")
                    .setCachePeriod(3600);
        }
    }
}