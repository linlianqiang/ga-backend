package com.example.gabackend.controller;
import com.example.gabackend.common.Result;
import com.example.gabackend.model.*;
import com.example.gabackend.repository.*;
import com.example.gabackend.service.AccessService;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.*;
@RestController @RequestMapping("/api") @Transactional
public class AccessController {
 private final UserRepository users;private final AccessRoleRepository roles;private final AccessService access;
 public AccessController(UserRepository u,AccessRoleRepository r,AccessService a){users=u;roles=r;access=a;}
 public record AccountInput(String login,String name,Long role,Boolean enabled,String password){}
 private Map<String,Object> view(User u){Map<String,Object> m=new LinkedHashMap<>();m.put("id",u.getId());m.put("login",u.getUsername());m.put("name",u.name);m.put("role",u.roleId);m.put("enabled",!Boolean.FALSE.equals(u.enabled));m.put("protected",access.protectedAccount(u));return m;}
 @GetMapping("/accounts") public Result<?> accounts(@RequestParam(name="search",defaultValue="") String search){return Result.success(users.findAll().stream().filter(u->(u.getUsername()+Optional.ofNullable(u.name).orElse("")).contains(search)).map(this::view).toList());}
 @GetMapping("/accounts/{id}") public Result<?> account(@PathVariable("id") Long id){return users.findById(id).map(u->Result.success(view(u))).orElseGet(()->Result.error(404,"账号不存在"));}
 @PostMapping("/accounts") public Result<?> create(@RequestBody AccountInput input){return save(null,input);}
 @PutMapping("/accounts/{id}") public Result<?> update(@PathVariable("id") Long id,@RequestBody AccountInput input){if(!users.existsById(id))return Result.error(404,"账号不存在");return save(id,input);}
 private Result<?> save(Long id,AccountInput r){if(r.login()==null||!r.login().matches("[A-Za-z0-9_.@-]{1,100}")||r.name()==null||r.name().isBlank()||r.role()==null||!roles.existsById(r.role()))return Result.error(400,"账号、姓名或角色无效");if(users.findAll().stream().anyMatch(u->!Objects.equals(u.getId(),id)&&u.getUsername().equalsIgnoreCase(r.login())))return Result.error(409,"登录名已存在");var u=id==null?new User():users.findById(id).orElseThrow();if(id!=null&&access.protectedAccount(u)&&(!u.getUsername().equals(r.login())||Boolean.FALSE.equals(r.enabled())||!roles.findById(r.role()).orElseThrow().permissions.containsAll(AccessService.ALL)))return Result.error(400,"管理员必须保持启用和全部权限");if((id==null||r.password()!=null)&& (r.password()==null||r.password().length()<8||r.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72))return Result.error(400,"密码需至少8字符且不超过72字节");u.setUsername(r.login());u.name=r.name();u.roleId=r.role();u.enabled=r.enabled()==null?true:r.enabled();u.setRoles(roles.findById(r.role()).orElseThrow().name);if(r.password()!=null)u.setPassword(new BCryptPasswordEncoder().encode(r.password()));u.tokenVersion=(u.tokenVersion==null?0:u.tokenVersion)+1;return Result.success(view(users.save(u)));}
 @GetMapping("/roles") public Result<?> roles(){return Result.success(roles.findAll());}
 @GetMapping("/roles/permissions") public Result<?> permissions(){return Result.success(AccessService.ALL);}
 @GetMapping("/roles/{id}") public Result<?> role(@PathVariable("id") Long id){return roles.findById(id).map(Result::success).orElseGet(()->Result.error(404,"角色不存在"));}
 @PostMapping("/roles") public Result<?> createRole(@RequestBody AccessRole r){return saveRole(null,r);}
 @PutMapping("/roles/{id}") public Result<?> updateRole(@PathVariable("id") Long id,@RequestBody AccessRole r){if(!roles.existsById(id))return Result.error(404,"角色不存在");return saveRole(id,r);}
 private Result<?> saveRole(Long id,AccessRole r){if(r.name==null||r.name.isBlank())return Result.error(400,"角色名称必填");r.name=r.name.trim();if(roles.findAll().stream().anyMatch(x->!Objects.equals(x.id,id)&&x.name.equalsIgnoreCase(r.name)))return Result.error(409,"角色名已存在");r.permissions=AccessService.normalize(r.permissions);if(id!=null&&users.findAll().stream().anyMatch(u->Objects.equals(u.roleId,id)&&access.protectedAccount(u))&&!r.permissions.containsAll(AccessService.ALL))return Result.error(400,"管理员角色须保留全部权限");r.protectedRole=id!=null&&Boolean.TRUE.equals(roles.findById(id).orElseThrow().protectedRole);if(Boolean.TRUE.equals(r.protectedRole))r.permissions=new HashSet<>(AccessService.ALL);r.id=id;return Result.success(roles.save(r));}
 @DeleteMapping("/roles/{id}") public Result<?> deleteRole(@PathVariable("id") Long id){if(!roles.existsById(id))return Result.error(404,"角色不存在");if(Boolean.TRUE.equals(roles.findById(id).orElseThrow().protectedRole)||users.findAll().stream().anyMatch(u->Objects.equals(u.roleId,id)))return Result.error(409,"角色仍有关联账号");roles.deleteById(id);return Result.success(null);}
}
