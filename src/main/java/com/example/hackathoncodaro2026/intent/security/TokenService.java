package com.example.hackathoncodaro2026.intent.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

/**
 * Issues and verifies HMAC-SHA256 signed bearer tokens for the {@code /api/**}
 * surface. The app's own auth is a server-side session via form login, which
 * a browser app on another origin cannot use — this is the bridge.
 *
 * Token shape: {@code base64url(username|expiryEpochSeconds).base64url(hmac)}.
 * The payload is not secret (it is just a username and a timestamp); only its
 * integrity matters, which is why the whole scheme is one HMAC check. This is
 * a real trust boundary — {@link #verify(String)} rejects anything unsigned,
 * tampered, or expired, and compares the signature with {@link MessageDigest#isEqual}
 * so a timing attack can't be used to guess it byte by byte.
 *
 * The secret comes from {@code intent.auth.secret}; if that property is unset
 * a random one is generated at startup. That means tokens do not survive an
 * app restart in the default configuration — acceptable for a hackathon
 * build, and a one-property fix (set {@code intent.auth.secret} in
 * application.yml) to make it stable across restarts.
 */
@Component
public class TokenService {

    private static final String ALGORITHM = "HmacSHA256";
    private static final Duration DEFAULT_TTL = Duration.ofHours(12);

    private final byte[] secretKey;

    public TokenService(@Value("${intent.auth.secret:}") String configuredSecret) {
        this.secretKey = (configuredSecret == null || configuredSecret.isBlank())
                ? randomSecret()
                : configuredSecret.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] randomSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

    /** Issues a token for {@code username}, valid for the default 12 hour TTL. */
    public IssuedToken issue(String username) {
        return issue(username, Instant.now().plus(DEFAULT_TTL));
    }

    /**
     * Issues a token for {@code username} with an explicit expiry. Exposed
     * (rather than hardcoding the TTL in one method) so tests can construct
     * an already-expired token without any bypass of the signing path.
     */
    public IssuedToken issue(String username, Instant expiresAt) {
        String payload = username + "|" + expiresAt.getEpochSecond();
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
        byte[] signature = hmac(payloadBytes);
        String token = base64Url(payloadBytes) + "." + base64Url(signature);
        return new IssuedToken(token, expiresAt);
    }

    /** Verifies signature and expiry. Empty means "reject" — caller returns 401, never a stack trace. */
    public Optional<VerifiedToken> verify(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        int dot = token.indexOf('.');
        if (dot <= 0 || dot == token.length() - 1) {
            return Optional.empty();
        }
        byte[] payloadBytes;
        byte[] providedSignature;
        try {
            payloadBytes = Base64.getUrlDecoder().decode(token.substring(0, dot));
            providedSignature = Base64.getUrlDecoder().decode(token.substring(dot + 1));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
        byte[] expectedSignature = hmac(payloadBytes);
        if (!MessageDigest.isEqual(expectedSignature, providedSignature)) {
            return Optional.empty();
        }
        String payload = new String(payloadBytes, StandardCharsets.UTF_8);
        int sep = payload.lastIndexOf('|');
        if (sep <= 0) {
            return Optional.empty();
        }
        String username = payload.substring(0, sep);
        long expiryEpoch;
        try {
            expiryEpoch = Long.parseLong(payload.substring(sep + 1));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
        Instant expiresAt = Instant.ofEpochSecond(expiryEpoch);
        if (Instant.now().isAfter(expiresAt)) {
            return Optional.empty();
        }
        return Optional.of(new VerifiedToken(username, expiresAt));
    }

    private byte[] hmac(byte[] payload) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secretKey, ALGORITHM));
            return mac.doFinal(payload);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Unable to sign token", ex);
        }
    }

    private static String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record IssuedToken(String token, Instant expiresAt) {
    }

    public record VerifiedToken(String username, Instant expiresAt) {
    }
}
