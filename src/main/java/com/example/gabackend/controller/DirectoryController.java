package com.example.gabackend.controller;
import com.example.gabackend.common.Result;
import com.example.gabackend.model.*;
import com.example.gabackend.repository.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@RestController @RequestMapping("/api") @Transactional
public class DirectoryController {
 private final MasterDataRepository masters;private final PersonnelRepository people;private final FireProjectRepository projects;private final ReceivableRepository bills;
 public DirectoryController(MasterDataRepository m,PersonnelRepository n,FireProjectRepository p,ReceivableRepository b){masters=m;people=n;projects=p;bills=b;}
 private String get(FireProject p,String type){return type.equals("company")?p.company:type.equals("property")?p.getPropertyUnit():type.equals("region")?p.getRegion():p.getMaintenanceLeader();}
 private String get(Receivable b,String type){return type.equals("company")?b.company:type.equals("property")?b.property:type.equals("region")?null:b.owner;}
 private boolean referenced(String type,String name){return projects.findAll().stream().anyMatch(p->Objects.equals(get(p,type),name))||bills.findAll().stream().anyMatch(b->Objects.equals(get(b,type),name));}
 private void rename(String type,String old,String name){if(old==null||old.equals(name))return;for(var p:projects.findAll())if(Objects.equals(get(p,type),old)){switch(type){case "company"->p.company=name;case "property"->p.setPropertyUnit(name);case "region"->p.setRegion(name);default->p.setMaintenanceLeader(name);}projects.save(p);}for(var b:bills.findAll())if(Objects.equals(get(b,type),old)){switch(type){case "company"->b.company=name;case "property"->b.property=name;default->b.owner=name;}bills.save(b);}}
 @GetMapping("/master-data") public Result<?> list(@RequestParam(name="type",required=false) String type,@RequestParam(name="search",defaultValue="") String search){var allProjects=projects.findAll();return Result.success(masters.findAll().stream().filter(m->(type==null||type.equals(m.type))&&m.name.contains(search)).map(m->masterView(m,allProjects)).toList());}
 @GetMapping("/master-data/{id}") public Result<?> master(@PathVariable("id") Long id){return masters.findById(id).map(m->Result.success(masterView(m,projects.findAll()))).orElseGet(()->Result.error(404,"基础数据不存在"));}
 @PostMapping("/master-data") public Result<?> create(@RequestBody MasterData m){return save(null,m);}
 @PutMapping("/master-data/{id}") public Result<?> update(@PathVariable("id") Long id,@RequestBody MasterData m){if(!masters.existsById(id))return Result.error(404,"基础数据不存在");return save(id,m);}
 private Map<String,Object> masterView(MasterData m,List<FireProject> allProjects){Map<String,Object> view=new LinkedHashMap<>();view.put("id",m.id);view.put("type",m.type);view.put("name",m.name);view.put("projectCount",allProjects.stream().filter(p->Objects.equals(get(p,m.type),m.name)).count());return view;}
 private Result<?> save(Long id,MasterData m){if(m.type==null||!Set.of("company","property","region").contains(m.type)||m.name==null||m.name.isBlank())return Result.error(400,"类型或名称无效");m.name=m.name.trim();if(masters.findAll().stream().anyMatch(x->!Objects.equals(x.id,id)&&x.type.equals(m.type)&&x.name.equalsIgnoreCase(m.name)))return Result.error(409,"名称已存在");if(id!=null){var old=masters.findById(id).orElseThrow();if(!old.type.equals(m.type))return Result.error(400,"不能变更基础数据类型");rename(m.type,old.name,m.name);}m.id=id;return Result.success(masterView(masters.save(m),projects.findAll()));}
 @DeleteMapping("/master-data/{id}") public Result<?> delete(@PathVariable("id") Long id){var old=masters.findById(id);if(old.isEmpty())return Result.error(404,"基础数据不存在");if(referenced(old.get().type,old.get().name))return Result.error(409,"存在项目或账期引用");masters.deleteById(id);return Result.success(null);}
 @GetMapping("/personnel") public Result<?> people(@RequestParam(name="search",defaultValue="") String search,@RequestParam(name="role",defaultValue="") String role){return Result.success(people.findAll().stream().filter(p->(p.name+Optional.ofNullable(p.phone).orElse("")).contains(search)&&(role.isBlank()||role.equals(p.role))).toList());}
 @GetMapping("/personnel/{id}") public Result<?> person(@PathVariable("id") Long id){return people.findById(id).map(Result::success).orElseGet(()->Result.error(404,"人员不存在"));}
 @PostMapping("/personnel") public Result<?> createPerson(@RequestBody Personnel p){return savePerson(null,p);}
 @PutMapping("/personnel/{id}") public Result<?> updatePerson(@PathVariable("id") Long id,@RequestBody Personnel p){if(!people.existsById(id))return Result.error(404,"人员不存在");return savePerson(id,p);}
 private Result<?> savePerson(Long id,Personnel p){if(p.name==null||p.name.isBlank()||p.role==null||!Set.of("组长","组员","师傅").contains(p.role)||(p.phone!=null&&!p.phone.isBlank()&&!p.phone.matches("[+0-9() -]{6,30}")))return Result.error(400,"姓名或电话无效，人员角色必须为组长、组员、师傅之一");p.name=p.name.trim();if(people.findAll().stream().anyMatch(x->!Objects.equals(x.id,id)&&x.name.equalsIgnoreCase(p.name)))return Result.error(409,"姓名已存在");if(id!=null)rename("owner",people.findById(id).orElseThrow().name,p.name);p.id=id;return Result.success(people.save(p));}
 @DeleteMapping("/personnel/{id}") public Result<?> deletePerson(@PathVariable("id") Long id){var old=people.findById(id);if(old.isEmpty())return Result.error(404,"人员不存在");if(referenced("owner",old.get().name))return Result.error(409,"人员仍有业务引用");people.deleteById(id);return Result.success(null);}
}
