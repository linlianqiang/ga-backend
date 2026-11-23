package com.example.gabackend.service;
import com.example.gabackend.model.*;
import com.example.gabackend.repository.*;
import org.springframework.stereotype.Service;
import java.util.*;
@Service
public class AccessService {
 public static final Set<String> ALL=Set.of("ledger.tab.all","ledger.tab.unpaid","ledger.tab.due","ledger.view","ledger.edit","ledger.receipt","ledger.export","projects.view","projects.edit","personnel.view","personnel.edit","base.view","base.edit","access.view","access.edit");
 private final UserRepository users; private final AccessRoleRepository roles;
 public AccessService(UserRepository u,AccessRoleRepository r){users=u;roles=r;}
 public Optional<User> user(String name){return users.findAll().stream().filter(u->u.getUsername().equalsIgnoreCase(name)).findFirst();}
 public boolean protectedAccount(User u){return "admin".equalsIgnoreCase(u.getUsername());}
 public boolean admin(User u){return (u.roleId!=null&&roles.findById(u.roleId).map(r->Boolean.TRUE.equals(r.protectedRole)).orElse(false))||"admin".equalsIgnoreCase(u.getUsername())||(u.roleId==null&&Arrays.stream(Optional.ofNullable(u.getRoles()).orElse("").split(",")).anyMatch(s->Set.of("SUPER","ADMIN").contains(s)));}
 public Set<String> permissions(User u){if(admin(u))return ALL;if(u.roleId!=null)return roles.findById(u.roleId).map(r->r.permissions).orElse(Set.of());return Set.of("projects.view","ledger.view");}
 public static Set<String> normalize(Set<String> input){if(input==null||!ALL.containsAll(input))throw new IllegalArgumentException("权限代码无效");var result=new HashSet<>(input);for(String p:input)result.add(p.split("\\.")[0]+".view");return result;}
}
