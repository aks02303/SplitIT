package org.nosql.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // This perfectly replicates express.static("files")
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Maps requests to /files/** to a physical folder named "files" in your project root
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:files/");
    }
}