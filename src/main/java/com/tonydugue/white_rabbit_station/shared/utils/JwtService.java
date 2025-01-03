package com.tonydugue.white_rabbit_station.shared.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

  @Value("${application.security.jwt.expiration}")
  private Long jwtExpiration;
  @Value("${application.security.jwt.secret-key}")
  private String secretKey;

  public String generateToken(UserDetails userDetails) {
    return generateToken(new HashMap<>(), userDetails);
  }

  public String extractUsername(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  public <T> T extractClaim(String token, Function<Claims, T> claimResolver) {
    final Claims claims = extractAllClaims(token);
    return claimResolver.apply(claims);
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parser()
            .verifyWith(getSignInKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
  }

  public String generateToken(Map<String, Object> claims, UserDetails userDetails) {
    return buildToken(claims, userDetails, jwtExpiration);
  }

  private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, Long jwtExpiration) {
    var authorities = userDetails.getAuthorities()
            .stream()
            .map(GrantedAuthority::getAuthority)
            .toList();

    return Jwts
            .builder()
            .claims(extraClaims)
            .subject(userDetails.getUsername())
            .issuedAt(new Date(System.currentTimeMillis()))
            .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
            .claim("authorities", authorities)
            .signWith(getSignInKey(), Jwts.SIG.HS256)
            .compact();
  }

  public boolean isTokenValid(String token, UserDetails userDetails) {
    final String username = extractUsername(token);
    return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
  }

  private boolean isTokenExpired(String token) {
    return extractExpiration(token).before(new Date());
  }

  private Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

  private SecretKey getSignInKey() {
    byte[] keyBytes = Decoders.BASE64.decode(secretKey);
    return Keys.hmacShaKeyFor(keyBytes);
  }
}