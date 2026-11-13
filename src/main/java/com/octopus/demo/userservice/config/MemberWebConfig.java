package com.octopus.demo.userservice.config;

import com.octopus.demo.userservice.dao.UserDao;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers MemberStatusInterceptor for C-side paths only.
 * order(1) keeps it after the common AuthInterceptor (default order 0),
 * which is what populates UserContext.
 *
 * <p>The DAO is resolved lazily so that {@code @WebMvcTest} slices (which have no
 * UserDao bean but do load WebMvcConfigurers) simply skip the registration instead
 * of failing to start the context.
 */
@Configuration
public class MemberWebConfig implements WebMvcConfigurer {

    private final ObjectProvider<UserDao> userDaoProvider;

    public MemberWebConfig(ObjectProvider<UserDao> userDaoProvider) {
        this.userDaoProvider = userDaoProvider;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        UserDao userDao = userDaoProvider.getIfAvailable();
        if (userDao == null) {
            return;
        }
        registry.addInterceptor(new MemberStatusInterceptor(userDao))
                .addPathPatterns("/api/member/**")
                .order(1);
    }
}
