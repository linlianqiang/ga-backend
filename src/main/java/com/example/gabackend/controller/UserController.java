package com.example.gabackend.controller;
import com.example.gabackend.common.Result;
import com.example.gabackend.model.User;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
@RestController @RequestMapping("/api/user") public class UserController {
 @GetMapping("/info") public Result<?> info(HttpServletRequest r){var u=(User)r.getAttribute("currentUser");return Result.success(Map.of("id",u.getId(),"username",u.getUsername(),"name",u.name==null?u.getUsername():u.name,"roles",Arrays.asList(Optional.ofNullable(u.getRoles()).orElse("").split(",")),"permissions",r.getAttribute("permissions")));}
}
