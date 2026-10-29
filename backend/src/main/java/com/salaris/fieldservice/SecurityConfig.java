package com.salaris.fieldservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.*;

@Configuration
class SecurityConfig {
  @Bean BCryptPasswordEncoder passwordEncoder() {return new BCryptPasswordEncoder(12);}
  @Bean UserDetailsService users(@Value("${app.demo-password}") String password) {
    if(password.length()<16) throw new IllegalStateException("Set DEMO_PASSWORD to at least 16 characters, or use run-local.ps1.");
    var encoder = new BCryptPasswordEncoder(12);
    var users = new InMemoryUserDetailsManager();
    for(String role : new String[]{"ADMIN","OFFICE","WORKER","CLIENT"})
      users.createUser(User.withUsername(role.toLowerCase()+"@salaris.demo").password(encoder.encode(password)).roles(role).build());
    return users;
  }
  @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(a -> a.requestMatchers("/", "/index.html", "/styles.css", "/app.js", "/workspace.js", "/api/csrf").permitAll().anyRequest().authenticated())
      .csrf(c -> c.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
      .formLogin(f -> f.loginProcessingUrl("/api/login")
        .successHandler((q,s,a)-> {s.setContentType("application/json");s.getWriter().write("{\"ok\":true}");})
        .failureHandler((q,s,e)->s.sendError(401,"Invalid credentials")))
      .logout(l -> l.logoutUrl("/api/logout").logoutSuccessHandler((q,s,a)->s.setStatus(204)))
      .exceptionHandling(e -> e.authenticationEntryPoint((q,s,x)->s.sendError(401)))
      .headers(h -> h.contentSecurityPolicy(c -> c.policyDirectives("default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; frame-src https://www.google.com; base-uri 'self'; form-action 'self'; frame-ancestors 'none'")));
    return http.build();
  }
}
