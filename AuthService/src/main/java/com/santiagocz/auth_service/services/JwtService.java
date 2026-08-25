package com.santiagocz.auth_service.services;

import com.santiagocz.auth_service.domain.entities.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, org.springframework.security.core.userdetails.UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public Claims extractAllClaims(String token) {
        return Jwts
                .parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String generateAccessToken(User user) {
        String roles = buildRolesString(user);
        String fullName = buildFullName(user);

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("roles", roles)
                .claim("uid", user.getId())
                .claim("name", fullName)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(User user) {
        return Jwts.builder()
                .subject(user.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private String buildFullName(User user) {
        if (user.getPerson() == null) {
            return null;
        }
        return user.getPerson().getFirstName() + " " + user.getPerson().getLastName();
    }

    // Construir cadena de roles
    private String buildRolesString(User user) {
        StringBuilder rolesBuilder = new StringBuilder();

        // Agregar rol jerárquico
        rolesBuilder.append("ROLE_").append(user.getHierarchyRole().name());

        // Agregar subroles
        if (user.getSubroles() != null && !user.getSubroles().isEmpty()) {
            String subroles = user.getSubroles().stream()
                    .map(subRole -> "SUB_" + subRole.getName())
                    .collect(Collectors.joining(","));
            rolesBuilder.append(",").append(subroles);
        }

        log.debug("Roles para usuario {}: {}", user.getUsername(), rolesBuilder.toString());
        return rolesBuilder.toString();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}