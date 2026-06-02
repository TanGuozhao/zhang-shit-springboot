package com.example.platform.topbiz.config;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import org.apache.shiro.mgt.SecurityManager;
import com.example.platform.topbiz.security.TopbizAuthorizingRealm;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.session.mgt.SessionManager;
import org.apache.shiro.spring.LifecycleBeanPostProcessor;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.web.filter.authc.FormAuthenticationFilter;
import org.apache.shiro.web.filter.mgt.DefaultFilterChainManager;
import org.apache.shiro.web.filter.mgt.PathMatchingFilterChainResolver;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.mgt.WebSecurityManager;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.apache.shiro.web.servlet.ShiroFilter;

import java.util.EnumSet;

@Configuration
public class ShiroConfig {

    @Bean
    public Realm realm(TopbizAuthorizingRealm topbizAuthorizingRealm) {
        return topbizAuthorizingRealm;
    }

    @Bean
    public LifecycleBeanPostProcessor lifecycleBeanPostProcessor() {
        return new LifecycleBeanPostProcessor();
    }

    @Bean
    public SessionManager sessionManager() {
        DefaultWebSessionManager sessionManager = new DefaultWebSessionManager();
        sessionManager.setSessionIdUrlRewritingEnabled(false);
        sessionManager.setSessionValidationSchedulerEnabled(false);
        return sessionManager;
    }

    @Bean
    public SecurityManager securityManager(Realm realm, SessionManager sessionManager) {
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager(realm);
        securityManager.setSessionManager(sessionManager);
        org.apache.shiro.SecurityUtils.setSecurityManager(securityManager);
        return securityManager;
    }

    @Bean(name = "shiroFilter")
    @DependsOn("lifecycleBeanPostProcessor")
    public Filter shiroFilter(SecurityManager securityManager) throws Exception {
        DefaultFilterChainManager chainManager = new DefaultFilterChainManager();
        FormAuthenticationFilter authenticationFilter = (FormAuthenticationFilter) chainManager.getFilter("authc");
        authenticationFilter.setLoginUrl("/api/topbiz/auth/login");
        chainManager.createChain("/actuator/**", "anon");
        chainManager.createChain("/api/topbiz/auth/login", "anon");
        chainManager.createChain("/api/topbiz/auth/logout", "authc");
        chainManager.createChain("/api/topbiz/**", "authc");

        PathMatchingFilterChainResolver resolver = new PathMatchingFilterChainResolver();
        resolver.setFilterChainManager(chainManager);

        ShiroFilter shiroFilter = new ShiroFilter();
        shiroFilter.setSecurityManager((WebSecurityManager) securityManager);
        shiroFilter.setFilterChainResolver(resolver);
        shiroFilter.setStaticSecurityManagerEnabled(true);
        return shiroFilter;
    }

    @Bean
    public FilterRegistrationBean<Filter> shiroFilterRegistration(Filter shiroFilter) {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>(shiroFilter);
        registration.setName("shiroFilter");
        registration.addUrlPatterns("/*");
        registration.setDispatcherTypes(EnumSet.allOf(DispatcherType.class));
        registration.setOrder(1);
        return registration;
    }

    @Bean
    @DependsOn("lifecycleBeanPostProcessor")
    public DefaultAdvisorAutoProxyCreator defaultAdvisorAutoProxyCreator() {
        DefaultAdvisorAutoProxyCreator creator = new DefaultAdvisorAutoProxyCreator();
        creator.setProxyTargetClass(true);
        return creator;
    }

    @Bean
    public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor(SecurityManager securityManager) {
        AuthorizationAttributeSourceAdvisor advisor = new AuthorizationAttributeSourceAdvisor();
        advisor.setSecurityManager(securityManager);
        return advisor;
    }
}
