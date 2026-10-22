package pcpro.atelier.core.security;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import pcpro.atelier.core.config.SecurityProperties;

/**
 * API stateless : chaque requête porte un token JWT signé (HS256) obtenu via POST /api/auth/login.
 * Pas de session ni de cookie, donc pas de CSRF.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  public static final String ISSUER = "pcpro-atelier";

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      EmployeJwtAuthenticationConverter jwtAuthenticationConverter,
      JsonSecurityErrorHandler errorHandler
  ) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> { })
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/refresh", "/api/auth/logout").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/health").permitAll()
            // Suivi en ligne par le client (protégé par le code de suivi et une limite de tentatives)
            .requestMatchers("/api/public/**").permitAll()
            .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
            .requestMatchers("/error").permitAll()
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .anyRequest().authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
            .authenticationEntryPoint(errorHandler)
            .accessDeniedHandler(errorHandler))
        .exceptionHandling(exceptions -> exceptions
            .authenticationEntryPoint(errorHandler)
            .accessDeniedHandler(errorHandler))
        .addFilterAfter(new EmployeMdcFilter(), BearerTokenAuthenticationFilter.class);
    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    // bcrypt par défaut, préfixe {bcrypt} stocké pour pouvoir changer d'algo plus tard
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  @Bean
  public SecretKey jwtSecretKey(SecurityProperties properties) {
    return new SecretKeySpec(properties.jwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
  }

  @Bean
  public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
    return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
  }

  @Bean
  public JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey)
        .macAlgorithm(MacAlgorithm.HS256)
        .build();
    decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(ISSUER)));
    return decoder;
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource(SecurityProperties properties) {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    if (properties.corsAllowedOrigins().isEmpty()) {
      return source; // aucune origine externe autorisée
    }
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(properties.corsAllowedOrigins());
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Request-Id"));
    config.setExposedHeaders(List.of("X-Request-Id", "Content-Disposition"));
    config.setAllowCredentials(true); // cookie de session (refresh)
    config.setMaxAge(3600L);
    source.registerCorsConfiguration("/api/**", config);
    return source;
  }
}
