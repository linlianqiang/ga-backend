package com.example.gabackend.service;
import com.example.gabackend.repository.*;
import com.example.gabackend.model.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Component @ConditionalOnProperty(name="directory.bootstrap",havingValue="true")
public class DirectoryBootstrap implements CommandLineRunner {
 private final MasterDataRepository masters;private final FireProjectRepository projects;private final ReceivableRepository bills;private final UserRepository users;private final AccessRoleRepository roles;
 @org.springframework.beans.factory.annotation.Value("${bootstrap.admin-password:}") private String initialPassword;
 public DirectoryBootstrap(MasterDataRepository m,FireProjectRepository p,ReceivableRepository b,UserRepository u,AccessRoleRepository r){masters=m;projects=p;bills=b;users=u;roles=r;}
 @Transactional public void run(String...args){Set<String> existing=new HashSet<>();for(var m:masters.findAll())existing.add(m.type+":"+m.name);for(String region:List.of("泉州永春","泉州丰泽","泉州洛江区","泉州市泉港区"))add(existing,"region",region);for(var p:projects.findAll()){add(existing,"property",p.getPropertyUnit());add(existing,"company",p.company);}for(var b:bills.findAll()){add(existing,"property",b.property);add(existing,"company",b.company);}var admin=roles.findAll().stream().filter(r->Boolean.TRUE.equals(r.protectedRole)).findFirst().orElseGet(()->{var r=new AccessRole();r.name="系统管理员";r.protectedRole=true;r.permissions=new HashSet<>(AccessService.ALL);return roles.save(r);});if(users.count()==0&&initialPassword!=null&&!initialPassword.isBlank()){if(initialPassword.length()<12||initialPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)throw new IllegalArgumentException("Initial admin password must be 12 characters or more and at most 72 bytes");var first=new User("admin",new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(initialPassword),"ADMIN");first.name="系统管理员";first.roleId=admin.id;first.enabled=true;users.save(first);}
 for(var u:users.findAll()){if(u.roleId==null&&(u.getUsername().equalsIgnoreCase("admin")||Arrays.stream(Optional.ofNullable(u.getRoles()).orElse("").split(",")).anyMatch(x->Set.of("SUPER","ADMIN").contains(x)))){u.roleId=admin.id;u.enabled=true;users.save(u);}}}
 private void add(Set<String> keys,String type,String name){if(name==null||name.isBlank()||!keys.add(type+":"+name.trim()))return;var m=new MasterData();m.type=type;m.name=name.trim();masters.save(m);}
}
