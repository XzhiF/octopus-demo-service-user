package com.octopus.demo.userservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers MemberStatusInterceptor for C-side paths only.
 * order(1) keeps it after the common AuthInterceptor (default order 0),
 * which is what populates UserContext.
 */
@Configuration
public class MemberWebConfig implements WebMvcConfigurer {

    private final MemberStatusInterceptor memberStatusInterceptor;

    public MemberWebConfig(MemberStatusInterceptor memberStatusInterceptor) {
        this.memberStatusInterceptor = memberStatusInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(memberStatusInterceptor)
                .addPathPatterns("/api/member/**")
                .order(1);
    }
}
