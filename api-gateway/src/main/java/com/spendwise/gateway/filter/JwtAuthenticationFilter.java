package com.spendwise.gateway.filter;

import com.spendwise.gateway.security.JwtTokenValidator;
import io.jsonwebtoken.Claims;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtTokenValidator jwtTokenValidator;

    public JwtAuthenticationFilter(
            JwtTokenValidator jwtTokenValidator
    ) {
        this.jwtTokenValidator = jwtTokenValidator;
    }

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain
    ) {

        String path = exchange
                .getRequest()
                .getURI()
                .getPath();

        HttpMethod method = exchange
                .getRequest()
                .getMethod();

        /*
         * Allow browser CORS preflight requests.
         */
        if (HttpMethod.OPTIONS.equals(method)) {
            return chain.filter(exchange);
        }

        /*
         * Public endpoints do not require JWT.
         */
        if (isPublicEndpoint(path, method)) {
            return chain.filter(exchange);
        }

        String authorizationHeader = exchange
                .getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        /*
         * Authorization header missing.
         */
        if (authorizationHeader == null ||
                authorizationHeader.isBlank()) {

            return unauthorized(
                    exchange,
                    "Authorization token is required"
            );
        }

        /*
         * Header must use Bearer authentication.
         */
        if (!authorizationHeader.startsWith("Bearer ")) {

            return unauthorized(
                    exchange,
                    "Authorization header must use Bearer token"
            );
        }

        String token = authorizationHeader.substring(7);

        if (token.isBlank()) {

            return unauthorized(
                    exchange,
                    "Authorization token is required"
            );
        }

        try {

            Claims claims =
                    jwtTokenValidator.validateAndGetClaims(token);

            /*
             * We validated the token successfully.
             *
             * For now we simply forward the request.
             *
             * Later we can propagate userId/role information
             * safely to downstream services.
             */

            return chain.filter(exchange);

        } catch (Exception exception) {

            return unauthorized(
                    exchange,
                    "Invalid or expired authentication token"
            );
        }
    }


    private boolean isPublicEndpoint(
            String path,
            HttpMethod method
    ) {

        if ("/api/users/login".equals(path)
                && HttpMethod.POST.equals(method)) {
            return true;
        }

        if ("/api/users/register".equals(path)
                && HttpMethod.POST.equals(method)) {
            return true;
        }

        if ("/actuator/health".equals(path)) {
            return true;
        }

        if ("/actuator/info".equals(path)) {
            return true;
        }

        /*
         * Keep this temporarily public while developing Day 8.
         * Later we should restrict gateway actuator endpoints.
         */
        if (path.startsWith("/actuator/gateway")) {
            return true;
        }

        return false;
    }


    private Mono<Void> unauthorized(
            ServerWebExchange exchange,
            String message
    ) {

        var response = exchange.getResponse();

        response.setStatusCode(HttpStatus.UNAUTHORIZED);

        response
                .getHeaders()
                .setContentType(MediaType.APPLICATION_JSON);

        String json = """
                {
                  "status": 401,
                  "error": "Unauthorized",
                  "message": "%s"
                }
                """.formatted(message);

        byte[] bytes =
                json.getBytes(StandardCharsets.UTF_8);

        DataBuffer buffer =
                response.bufferFactory().wrap(bytes);

        return response.writeWith(
                Mono.just(buffer)
        );
    }


    @Override
    public int getOrder() {
        return -100;
    }
}