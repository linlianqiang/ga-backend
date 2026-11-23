package com.example.gabackend.util;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.*;
@Component
public class JwtUtil {
 private final SecretKey key; private final String issuer="gangan-api";
 public JwtUtil(@Value("${jwt.secret:}") String secret){if(secret==null||secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length<32)throw new IllegalStateException("JWT_SECRET must contain at least 32 UTF-8 bytes");key=Keys.hmacShaKeyFor(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
 public String generateToken(String username,String roles){return generateToken(username,roles,0);}
 public String generateToken(String username,String roles,int version){return Jwts.builder().setIssuer(issuer).setAudience("gangan-web").setSubject(username).setId(UUID.randomUUID().toString()).setIssuedAt(new Date()).setExpiration(new Date(System.currentTimeMillis()+7200000)).claim("roles",roles).claim("ver",version).signWith(key,SignatureAlgorithm.HS256).compact();}
 public Claims extractClaims(String token){var j=Jwts.parserBuilder().setSigningKey(key).requireIssuer(issuer).requireAudience("gangan-web").setAllowedClockSkewSeconds(30).build().parseClaimsJws(token);var c=j.getBody();if(!"HS256".equals(j.getHeader().getAlgorithm())||c.getExpiration()==null||c.getIssuedAt()==null||c.getIssuedAt().after(new Date(System.currentTimeMillis()+30000))||c.getSubject()==null||c.getSubject().isBlank()||c.getId()==null||c.get("ver",Integer.class)==null)throw new MalformedJwtException("Invalid required claims");return c;}
 public String extractUsername(String token){return extractClaims(token).getSubject();}
 public List<String> extractRoles(String token){String roles=extractClaims(token).get("roles",String.class);return roles==null?List.of():Arrays.asList(roles.split(","));}
 public boolean isTokenExpired(String token){return extractClaims(token).getExpiration().before(new Date());}
}
