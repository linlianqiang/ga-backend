package com.example.gabackend.security;
import com.example.gabackend.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class LocalSecretConfigTest {
 @TempDir Path temp;
 @Configuration(proxyBeanMethods=false) @Import(JwtUtil.class) static class Config {}
 @Test void loadsDotEnvThroughPropertiesImport()throws Exception{
 Path env=temp.resolve(".env");Files.writeString(env,"JWT_SECRET=test-only-config-secret-of-at-least-32-bytes\n");
 Path config=temp.resolve("application.properties");Files.writeString(config,"spring.config.import=optional:file:"+env+"[.properties]\njwt.secret=${JWT_SECRET:}\n");
 var app=new SpringApplication(Config.class);app.setWebApplicationType(WebApplicationType.NONE);
 try(var context=app.run("--spring.config.location="+config.toUri(),"--spring.main.banner-mode=off")){
 var jwt=context.getBean(JwtUtil.class);assertEquals("test",jwt.extractUsername(jwt.generateToken("test","USER")));
 }
 }
}
