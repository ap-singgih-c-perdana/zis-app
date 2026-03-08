package com.zakat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain configure(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        // Pages
                        .requestMatchers("/user/**", "/admin/**").hasRole("ADMIN")
                        .requestMatchers("/zakat-payments/new", "/zakat-payments/*/edit").hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers("/zakat-payments/list").hasAnyRole("ADMIN", "OPERATOR", "VIEWER")
                        .requestMatchers("/reports/**").hasAnyRole("ADMIN", "OPERATOR", "VIEWER")
                        .requestMatchers("/").hasAnyRole("ADMIN", "OPERATOR", "VIEWER")

                        // Users
                        .requestMatchers("/api/users/**").hasRole("ADMIN")

                        // Zakat qualities (master data): anyone can read options, only ADMIN can mutate
                        .requestMatchers(HttpMethod.GET, "/api/zakat-qualities/**").hasAnyRole("ADMIN", "OPERATOR", "VIEWER")
                        .requestMatchers(HttpMethod.POST, "/api/zakat-qualities/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/zakat-qualities/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/zakat-qualities/**").hasRole("ADMIN")

                        // Institution profile: read for all authenticated, update only ADMIN
                        .requestMatchers(HttpMethod.GET, "/api/institution-profile/**").hasAnyRole("ADMIN", "OPERATOR", "VIEWER")
                        .requestMatchers(HttpMethod.PUT, "/api/institution-profile/**").hasRole("ADMIN")

                        // Payments: OPERATOR/ADMIN can create & list
                        .requestMatchers(HttpMethod.POST, "/api/zakat-payments/**").hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers(HttpMethod.PUT, "/api/zakat-payments/**").hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers(HttpMethod.GET, "/api/zakat-payments/**").hasAnyRole("ADMIN", "OPERATOR", "VIEWER")

                        // Reports: VIEWER and above
                        .requestMatchers(HttpMethod.GET, "/api/reports/**").hasAnyRole("ADMIN", "OPERATOR", "VIEWER")

                        // Dashboard: VIEWER and above
                        .requestMatchers(HttpMethod.GET, "/api/dashboard/**").hasAnyRole("ADMIN", "OPERATOR", "VIEWER")

                        .requestMatchers("/login", "/register", "/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon" +
                                ".ico").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error")
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                );
        return http.build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers("/static/**", "/favicon.ico", "/assets/**", "/css/**", "/img" +
                "/**", "/js**", "/admin/**", "/webjars/**", "/templates/**");
    }
}
