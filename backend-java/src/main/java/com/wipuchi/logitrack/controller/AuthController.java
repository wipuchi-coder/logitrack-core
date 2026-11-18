package com.wipuchi.logitrack.controller;

import com.wipuchi.logitrack.config.SecurityConfig;
import com.wipuchi.logitrack.dto.AuthDto.*;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final long TOKEN_SECONDS = 900;
    private final AuthenticationManager authentication;
    private final JwtEncoder encoder;

    public AuthController(AuthenticationManager authentication, JwtEncoder encoder) {
        this.authentication = authentication;
        this.encoder = encoder;
    }

    @PostMapping("/token")
    public ResponseEntity<Token> token(@Valid @RequestBody Login login) {
        var principal = authentication.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(login.username(), login.password()));
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer(SecurityConfig.ISSUER).subject(principal.getName())
                .issuedAt(now).expiresAt(now.plusSeconds(TOKEN_SECONDS)).build();
        var headers = JwsHeader.with(MacAlgorithm.HS256).build();
        var jwt = encoder.encode(JwtEncoderParameters.from(headers, claims));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new Token(jwt.getTokenValue(), "Bearer", TOKEN_SECONDS));
    }
}
