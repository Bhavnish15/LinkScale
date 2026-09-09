package filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


/**
 * * JWT Authentication Filter
 * Applied to all routes except /api/v1/auth/**
 */

/**
 * * FLOW :
 * * 1. Extract JWT from Authorization header.
 * * 2. Validate JWT signature and expiry
 * * 3. Extract userId from token claims
 * * 4. Add userId to request header for downstream services
 * * 5. Forward request to correct service
 * *
 * * if JWT is invalid or missing -> 401 Unauthorized
 */


@Component
@Slf4j
public class JwtAuthFilter extends AbstractGatewayFilterFactory<JwtAuthFilter.Config{
}
