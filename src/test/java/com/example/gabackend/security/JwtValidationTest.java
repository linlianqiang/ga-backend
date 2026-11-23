package com.example.gabackend.security;
import com.example.gabackend.util.JwtUtil;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class JwtValidationTest {
 private final String secret="test-only-long-secret-for-jwt-verification-at-least-48-bytes";
 private JwtBuilder builder(){return Jwts.builder().setIssuer("gangan-api").setAudience("gangan-web").setSubject("user").setId("test").setIssuedAt(new Date()).setExpiration(new Date(System.currentTimeMillis()+60000)).claim("ver",0);}
 private String sign(JwtBuilder b){return b.signWith(Keys.hmacShaKeyFor(secret.getBytes()),SignatureAlgorithm.HS256).compact();}
 @Test void validToken(){var jwt=new JwtUtil(secret);assertEquals("user",jwt.extractUsername(jwt.generateToken("user","USER")));}
 @Test void missingExpirationRejected(){assertThrows(JwtException.class,()->new JwtUtil(secret).extractClaims(sign(builder().setExpiration(null))));}
 @Test void wrongAudienceRejected(){assertThrows(JwtException.class,()->new JwtUtil(secret).extractClaims(sign(builder().setAudience("other"))));}
 @Test void wrongIssuerRejected(){assertThrows(JwtException.class,()->new JwtUtil(secret).extractClaims(sign(builder().setIssuer("other"))));}
 @Test void expiredRejected(){assertThrows(JwtException.class,()->new JwtUtil(secret).extractClaims(sign(builder().setExpiration(new Date(System.currentTimeMillis()-60000)))));}
 @Test void futureIssuedRejected(){assertThrows(JwtException.class,()->new JwtUtil(secret).extractClaims(sign(builder().setIssuedAt(new Date(System.currentTimeMillis()+60000)))));}
 @Test void weakKeyRejected(){assertThrows(IllegalStateException.class,()->new JwtUtil("short"));}
 @Test void alternateAlgorithmRejected(){String t=builder().signWith(Keys.hmacShaKeyFor(secret.getBytes()),SignatureAlgorithm.HS384).compact();assertThrows(JwtException.class,()->new JwtUtil(secret).extractClaims(t));}
}
