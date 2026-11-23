package com.authoritypro.platform.identity.infrastructure.http;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.authoritypro.platform.identity.application.AuthenticationService;
import com.authoritypro.platform.identity.application.AuthenticationService.AuthenticatedUser;
import com.authoritypro.platform.identity.application.IdentityAccessException;
import com.authoritypro.platform.identity.application.OwnProfile;
import com.authoritypro.platform.identity.application.OwnProfileReader;
import com.authoritypro.platform.identity.application.SessionLimiter;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Browser, same-origin authentication contract of IDENTITY-AND-ACCESS §10.5. The component only
 * exists alongside the runtime datasource, exactly like {@link AuthSecurityConfiguration}.
 */
@ConditionalOnBean(DataSource.class)
@RestController
@RequestMapping("/api/v1/auth")
public final class AuthController {
    private static final Duration ABSOLUTE_LIFETIME = Duration.ofHours(8);

    private final AuthenticationService authentication;
    private final OwnProfileReader profiles;
    private final SessionLimiter sessions;
    private final HttpSessionSecurityContextRepository contextRepository;
    private final CookieCsrfTokenRepository csrfTokens;
    private final AuthProblems problems;
    private final Clock clock;

    public AuthController(AuthenticationService authentication, OwnProfileReader profiles, SessionLimiter sessions,
            HttpSessionSecurityContextRepository contextRepository, CookieCsrfTokenRepository csrfTokens,
            AuthProblems problems, Clock clock) {
        this.authentication = authentication;
        this.profiles = profiles;
        this.sessions = sessions;
        this.contextRepository = contextRepository;
        this.csrfTokens = csrfTokens;
        this.problems = problems;
        this.clock = clock;
    }

    @GetMapping("/csrf")
    ResponseEntity<Void> csrf(HttpServletRequest request, HttpServletResponse response) {
        CsrfToken token = csrfTokens.generateToken(request);
        csrfTokens.saveToken(token, request, response);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    @PostMapping("/login")
    ResponseEntity<?> login(@Valid @RequestBody LoginRequest body, HttpServletRequest request,
            HttpServletResponse response) {
        AuthenticatedUser user;
        try {
            user = authentication.authenticate(body.login(), body.password(), request.getRemoteAddr());
        } catch (IdentityAccessException refusal) {
            return problems.response(switch (refusal.kind()) {
                case AUTHENTICATION_THROTTLED -> AuthProblems.Code.AUTHENTICATION_THROTTLED;
                case STORE_UNAVAILABLE -> AuthProblems.Code.SESSION_STORE_UNAVAILABLE;
                case AUTHENTICATION_REFUSED -> AuthProblems.Code.AUTHENTICATION_REQUIRED;
            });
        }
        try {
            sessions.makeRoom(user.userId().toString());
            startAuthenticatedSession(user, request, response);
        } catch (RuntimeException unavailable) {
            return problems.response(AuthProblems.Code.SESSION_STORE_UNAVAILABLE);
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    ResponseEntity<?> me(HttpServletRequest request) {
        UUID userId = currentUserId();
        if (userId == null) return problems.response(AuthProblems.Code.AUTHENTICATION_REQUIRED);
        HttpSession session = request.getSession(false);
        if (session != null && absoluteLifetimeExceeded(session)) {
            session.invalidate();
            return problems.response(AuthProblems.Code.SESSION_EXPIRED);
        }
        try {
            Optional<OwnProfile> profile = profiles.read(userId);
            if (profile.isEmpty()) {
                if (session != null) session.invalidate();
                return problems.response(AuthProblems.Code.SESSION_EXPIRED);
            }
            return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(profile.get());
        } catch (IdentityAccessException unavailable) {
            return problems.response(AuthProblems.Code.SESSION_STORE_UNAVAILABLE);
        }
    }

    private void startAuthenticatedSession(AuthenticatedUser user, HttpServletRequest request,
            HttpServletResponse response) {
        request.getSession(true);
        // Fixation protection: the pre-authentication identifier never survives login.
        request.changeSessionId();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                user.userId().toString(), null, List.of()));
        request.getSession(false).setAttribute(
                FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, user.userId().toString());
        contextRepository.saveContext(context, request, response);
    }

    private boolean absoluteLifetimeExceeded(HttpSession session) {
        return clock.millis() - session.getCreationTime() >= ABSOLUTE_LIFETIME.toMillis();
    }

    private static UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException malformed) {
            return null;
        }
    }

    record LoginRequest(@NotBlank @Size(max = 200) String login, @NotBlank @Size(max = 256) String password) { }
}
