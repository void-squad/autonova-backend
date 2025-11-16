package com.autonova.progressmonitoring.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Use /app/uploads in Docker, or ./uploads for local development
        String uploadPath = System.getenv("UPLOADS_DIR");
        if (uploadPath == null || uploadPath.isBlank()) {
            uploadPath = "uploads";
        }
        
        Path uploadsPath = Path.of(uploadPath).toAbsolutePath();
        String fileUrl = "file:" + uploadsPath + "/";
        
        registry.addResourceHandler("/api/projects/progress/uploads/**")
                .addResourceLocations(fileUrl);
    }
}
