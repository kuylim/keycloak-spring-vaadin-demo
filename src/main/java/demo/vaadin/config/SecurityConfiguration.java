package demo.vaadin.config;

import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver;
import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter;
import org.keycloak.adapters.springsecurity.filter.KeycloakAuthenticationProcessingFilter;
import org.keycloak.adapters.springsecurity.filter.KeycloakPreAuthActionsFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.vaadin.spring.http.HttpService;
import org.vaadin.spring.security.annotation.EnableVaadinSharedSecurity;
import org.vaadin.spring.security.config.VaadinSharedSecurityConfiguration;
import org.vaadin.spring.security.shared.VaadinAuthenticationSuccessHandler;
import org.vaadin.spring.security.shared.VaadinLogoutHandler;
import org.vaadin.spring.security.shared.VaadinRedirectLogoutHandler;
import org.vaadin.spring.security.shared.VaadinSessionClosingLogoutHandler;
import org.vaadin.spring.security.shared.VaadinUrlAuthenticationSuccessHandler;
import org.vaadin.spring.security.web.VaadinRedirectStrategy;

@Configuration
@EnableWebSecurity
@EnableVaadinSharedSecurity
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true, proxyTargetClass = false)
public class SecurityConfiguration extends KeycloakWebSecurityConfigurerAdapter {
  @Override
  protected void configure(AuthenticationManagerBuilder auth) throws Exception {
    auth.authenticationProvider(keycloakAuthenticationProvider());
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.httpBasic().disable();
    http.formLogin().disable();
    http.csrf().disable();
    // http.csrf().requireCsrfProtectionMatcher(keycloakCsrfRequestMatcher());
    http
        .authorizeRequests()
        .antMatchers("/vaadinServlet/UIDL/**").permitAll()
        .antMatchers("/vaadinServlet/HEARTBEAT/**").permitAll()
        .anyRequest().authenticated();
    http
        .logout()
        .addLogoutHandler(new VaadinSessionClosingLogoutHandler())
        .addLogoutHandler(keycloakLogoutHandler())
        .logoutUrl("/sso/logout").permitAll()
        .logoutSuccessUrl("/");
    http
        .addFilterBefore(keycloakPreAuthActionsFilter(), LogoutFilter.class)
        .addFilterBefore(keycloakAuthenticationProcessingFilter(), BasicAuthenticationFilter.class);
    http
        .exceptionHandling()
        .authenticationEntryPoint(authenticationEntryPoint());
    http
        .sessionManagement()
        .sessionAuthenticationStrategy(sessionAuthenticationStrategy());
  }

  @Override
  public void configure(WebSecurity web) throws Exception {
    web.ignoring().antMatchers("/VAADIN/**");
  }

  @Bean
  public KeycloakConfigResolver KeycloakConfigResolver() {
    return new KeycloakSpringBootConfigResolver();
  }

  @Bean
  @Override
  public AuthenticationManager authenticationManagerBean() throws Exception {
    return super.authenticationManagerBean();
  }

  @Bean
  @Override
  protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
    return new RegisterSessionAuthenticationStrategy(new SessionRegistryImpl());
  }

  @Bean
  public FilterRegistrationBean keycloakAuthenticationProcessingFilterRegistrationBean(
      KeycloakAuthenticationProcessingFilter filter) {
    FilterRegistrationBean registrationBean = new FilterRegistrationBean(filter);
    registrationBean.setEnabled(false);
    return registrationBean;
  }

  @Bean
  public FilterRegistrationBean keycloakPreAuthActionsFilterRegistrationBean(
      KeycloakPreAuthActionsFilter filter) {
    FilterRegistrationBean registrationBean = new FilterRegistrationBean(filter);
    registrationBean.setEnabled(false);
    return registrationBean;
  }

  @Bean(name = VaadinSharedSecurityConfiguration.VAADIN_LOGOUT_HANDLER_BEAN)
  VaadinLogoutHandler vaadinLogoutHandler(VaadinRedirectStrategy vaadinRedirectStrategy) {
    VaadinRedirectLogoutHandler handler = new VaadinRedirectLogoutHandler(vaadinRedirectStrategy);
    handler.setLogoutUrl("sso/logout");
    return handler;
  }

  @Bean(name = VaadinSharedSecurityConfiguration.VAADIN_AUTHENTICATION_SUCCESS_HANDLER_BEAN)
  VaadinAuthenticationSuccessHandler vaadinAuthenticationSuccessHandler(HttpService httpService,
      VaadinRedirectStrategy vaadinRedirectStrategy) {
    return new VaadinUrlAuthenticationSuccessHandler(httpService, vaadinRedirectStrategy, "/");
  }
}