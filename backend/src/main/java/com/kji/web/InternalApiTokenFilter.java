package com.kji.web;

import com.kji.config.InternalProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Guards everything that changes state behind the shared internal token.
 *
 * <p>Two things are guarded: the whole internal API, and any write to the application CRM.
 * Reads stay open, because the read API is a mirror of job-board content and the console
 * fetches it on every page. Writing an application is different: it records a decision about
 * a real job search, and an anonymous caller has no business making one.
 *
 * <p>The console reaches the write endpoints from its server actions, which run on the Next
 * server and read the token from their own environment, so a browser never holds it.
 */
@Component
@Order(1)
public class InternalApiTokenFilter extends OncePerRequestFilter {

    private static final String INTERNAL_PREFIX = "/api/internal/";
    private static final String APPLICATIONS_PATH = "/api/applications";
    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final String HEADER = "X-Internal-Token";

    private final InternalProperties properties;

    public InternalApiTokenFilter(InternalProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !isGuarded(request.getRequestURI(), request.getMethod());
    }

    static boolean isGuarded(String path, String method) {
        if (path.startsWith(INTERNAL_PREFIX)) {
            return true;
        }
        boolean isApplication = path.equals(APPLICATIONS_PATH)
                || path.startsWith(APPLICATIONS_PATH + "/");
        return isApplication && WRITE_METHODS.contains(method);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!properties.tokenConfigured()) {
            reject(response, "This instance has no internal API token configured, so writing is "
                    + "disabled. Set INTERNAL_API_TOKEN on the API and on the console.");
            return;
        }
        if (!properties.matches(request.getHeader(HEADER))) {
            reject(response, "Invalid or missing " + HEADER);
            return;
        }
        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"unauthorized\",\"message\":\"" + message + "\"}");
    }
}
