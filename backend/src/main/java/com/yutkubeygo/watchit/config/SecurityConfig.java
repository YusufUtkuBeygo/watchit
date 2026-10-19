package com.yutkubeygo.watchit.config;

import com.yutkubeygo.watchit.security.JwtAuthFilter;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder()

    {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.POST, "/users/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/users").permitAll()
                        //Kendi kanalım giriş ister; alttaki açık /channels/** kuralından önce gelmeli (ilk uyan kural geçerli)
                        .requestMatchers(HttpMethod.GET, "/channels/me").authenticated()
                        .requestMatchers(HttpMethod.GET,
                                "/videos/**", "/channels/**", "/categories/**", "/comments/**").permitAll()
                        //Herkese aÃ§Ä±k oynatma listeleri giriÅŸsiz de okunur (karar 19 Ekim).
                        //Gizli liste: servis sahibi kontrol eder, giriÅŸsiz istekte userId null olduÄŸu iÃ§in 403 dÃ¶ner
                        .requestMatchers(HttpMethod.GET, "/playlists/**", "/playlistvideos/**").permitAll()
                        //API dokümanı (Swagger) herkese açık; uçların kendisi yine kendi kurallarına tabi
                        .requestMatchers(HttpMethod.GET, "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        //Kategori ekleme/değiştirme/silme kimseye açık değil (rol sistemi yok, kategoriler veritabanından yönetilir)
                        //Okuma üstteki GET kuralına uyduğu için buraya hiç gelmez
                        .requestMatchers("/categories/**").denyAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

}
