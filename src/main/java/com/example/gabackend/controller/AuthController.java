package com.example.gabackend.controller;
import com.example.gabackend.common.Result;
import com.example.gabackend.dto.LoginRequest;
import com.example.gabackend.model.User;
import com.example.gabackend.repository.UserRepository;
import com.example.gabackend.service.AccessService;
import com.example.gabackend.util.JwtUtil;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
@RestController @RequestMapping("/api/auth") @Transactional
public class AuthController {
 private final AccessService access;private final UserRepository users;private final JwtUtil jwt;private final BCryptPasswordEncoder passwords=new BCryptPasswordEncoder();
 public AuthController(AccessService a,UserRepository u,JwtUtil j){access=a;users=u;jwt=j;}
 @PostMapping("/login") public Result<?> login(@RequestBody LoginRequest r){if(r.getUsername()==null||r.getPassword()==null||r.getPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)return Result.error(401,"用户名或密码错误");var u=access.user(r.getUsername().trim()).orElse(null);if(u==null||Boolean.FALSE.equals(u.enabled))return Result.error(401,"用户名或密码错误");String stored=u.getPassword();boolean hash=stored!=null&&stored.matches("^\\$2[aby]\\$.*");boolean ok=stored!=null&&(hash?passwords.matches(r.getPassword(),stored):java.security.MessageDigest.isEqual(stored.getBytes(java.nio.charset.StandardCharsets.UTF_8),r.getPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8)));if(!ok)return Result.error(401,"用户名或密码错误");if(!hash){u.setPassword(passwords.encode(r.getPassword()));users.save(u);}return Result.success(Map.of("token",jwt.generateToken(u.getUsername(),u.getRoles(),u.tokenVersion==null?0:u.tokenVersion),"username",u.getUsername(),"permissions",access.permissions(u),"roles",Arrays.asList(Optional.ofNullable(u.getRoles()).orElse("").split(","))));}
 @PostMapping("/logout") public Result<?> logout(HttpServletRequest request){var u=(User)request.getAttribute("currentUser");u.tokenVersion=(u.tokenVersion==null?0:u.tokenVersion)+1;users.save(u);return Result.success(null);}
}
