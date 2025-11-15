package com.autonova.auth_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import java.io.IOException;

/**
 * Security Headers Configuration for OAuth2 and Content Security Policy
 * Configures CSP headers to allow Google OAuth2 login while maintaining security
 */
@Configuration
public class SecurityHeadersConfig implements WebMvcConfigurer {

    @Bean
    public FilterRegistrationBean<ContentSecurityPolicyFilter> cspFilter() {
        FilterRegistrationBean<ContentSecurityPolicyFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new ContentSecurityPolicyFilter());
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(1);
        return registrationBean;
    }

    /**
     * Filter to add Content Security Policy headers
     */
    public static class ContentSecurityPolicyFilter implements Filter {
        
        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            
            // Content Security Policy for OAuth2 with Google
            // Allow scripts from self and Google domains
            String cspPolicy = String.join("; ",
                "default-src 'self'",
                "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://accounts.google.com https://apis.google.com",
                "style-src 'self' 'unsafe-inline' https://accounts.google.com",
                "img-src 'self' data: https: blob:",
                "font-src 'self' data:",
                "connect-src 'self' https://accounts.google.com https://oauth2.googleapis.com",
                "frame-src 'self' https://accounts.google.com",
                "frame-ancestors 'self'"
            );
            
            httpResponse.setHeader("Content-Security-Policy", cspPolicy);
            
            // Additional security headers
            httpResponse.setHeader("X-Content-Type-Options", "nosniff");
            httpResponse.setHeader("X-Frame-Options", "SAMEORIGIN");
            httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
            httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
            
            chain.doFilter(request, response);
        }
    }
}
