package com.flossk.tts.config;

import com.flossk.tts.filter.ApiKeyAuthenticationFilter;
import com.flossk.tts.filter.EmbedFrameOptionsFilter;
import com.flossk.tts.service.AdminUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    private final ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;
    private final AdminUserDetailsService adminUserDetailsService;
    private final EmbedFrameOptionsFilter embedFrameOptionsFilter;
    
    public SecurityConfig(ApiKeyAuthenticationFilter apiKeyAuthenticationFilter,
                         AdminUserDetailsService adminUserDetailsService,
                         EmbedFrameOptionsFilter embedFrameOptionsFilter) {
        this.apiKeyAuthenticationFilter = apiKeyAuthenticationFilter;
        this.adminUserDetailsService = adminUserDetailsService;
        this.embedFrameOptionsFilter = embedFrameOptionsFilter;
    }
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .addFilterAfter(embedFrameOptionsFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/login-error").permitAll()
                .requestMatchers("/admin/**").authenticated()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/index.html").permitAll()
                .requestMatchers("/embed/**").permitAll()
                .requestMatchers("/api/**").permitAll()
                .anyRequest().permitAll()
            )
            .formLogin(formLogin -> formLogin
                .loginPage("/login")
                .defaultSuccessUrl("/admin", true)
                .failureUrl("/login-error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .userDetailsService(adminUserDetailsService)
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**", "/embed/**", "/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html")
            )
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions
                    .disable()
                )
            );
        
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
