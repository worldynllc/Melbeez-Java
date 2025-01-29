package com.mlbeez.feeder.config.jwtconfig;
import com.mlbeez.feeder.filter.JwtFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    @Autowired
    private JwtFilter jwtFilter;

    @Bean
    protected SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(
                        request -> request
                                .requestMatchers("/warranty/upload").hasAnyRole("ADMIN","SUPERADMIN")
                                .requestMatchers("/warranty/**").hasAnyRole("USER","ADMIN","SUPERADMIN")
                                .requestMatchers("/feed/**").hasAnyRole("ADMIN","USER","SUPERADMIN")
                                .requestMatchers("/comment/{feedId}/{commentId}").hasAnyRole("ADMIN","SUPERADMIN")
                                .requestMatchers("/comment/post/{feedId}/{userid}/{username}").hasAnyRole("USER","ADMIN","SUPERADMIN")
                                .requestMatchers("/comment/{feedId}/{userId}/{commentId}").hasAnyRole("USER","SUPERADMIN")
                                .requestMatchers("/comment/**").hasAnyRole("ADMIN","USER","SUPERADMIN")
                                .requestMatchers("/like/**").hasAnyRole("ADMIN","USER","SUPERADMIN")
                                .requestMatchers("/create-checkout-session").hasAnyRole("USER","SUPERADMIN")
                                .requestMatchers("/subscriptions/{id}").hasAnyRole("USER","ADMIN","SUPERADMIN")
                                .requestMatchers("transaction/user/all").hasAnyRole("ADMIN","SUPERADMIN")
                                .requestMatchers("transaction/user/{id}").hasAnyRole("USER","ADMIN","SUPERADMIN")
                                .requestMatchers("/insurance-payment/{userId}").hasAnyRole("USER","ADMIN","SUPERADMIN")
                                .requestMatchers("/webhook", "/swagger-ui.html", "/swagger-ui/**","/v3/api-docs/**").permitAll()
                                .requestMatchers("/**").hasRole("SUPERADMIN")
                                .anyRequest()
                                .authenticated())
                .exceptionHandling(exception->exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .sessionManagement(session->session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
        }
    }
