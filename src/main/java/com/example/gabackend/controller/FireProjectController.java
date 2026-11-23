package com.example.gabackend.controller;
import com.example.gabackend.common.Result;
import com.example.gabackend.model.FireProject;
import com.example.gabackend.repository.FireProjectRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.math.BigDecimal;
import java.time.*;

@RestController
@RequestMapping("/api/projects")
@CrossOrigin
@Transactional
public class FireProjectController {
 private final FireProjectRepository repository;
 private final com.example.gabackend.service.ProjectLinks links;
 public FireProjectController(FireProjectRepository repository,com.example.gabackend.service.ProjectLinks links){this.repository=repository;this.links=links;}
 private static final Set<String> TYPES=Set.of("QUANZHOU","XIAMEN","PARTNER","TERMINATED");
 private boolean contains(String v,String q){return q==null||q.isBlank()||(v!=null&&v.contains(q));}
 private boolean range(BigDecimal value,String min,String max){return (min==null||min.isBlank()||(value!=null&&value.compareTo(new BigDecimal(min))>=0))&&(max==null||max.isBlank()||(value!=null&&value.compareTo(new BigDecimal(max))<=0));}
 private List<FireProject> filtered(Map<String,String> q){
 for(String key:List.of("areaMin","areaMax","contractAmountMin","contractAmountMax"))if(q.containsKey(key)&&!q.get(key).isBlank())new BigDecimal(q.get(key));
 for(String key:List.of("area","contractAmount"))if(q.containsKey(key+"Min")&&q.containsKey(key+"Max")&&!q.get(key+"Min").isBlank()&&!q.get(key+"Max").isBlank()&&new BigDecimal(q.get(key+"Min")).compareTo(new BigDecimal(q.get(key+"Max")))>0)throw new IllegalArgumentException();
 if(q.containsKey("projectType")&&!q.get("projectType").isBlank()&&!TYPES.contains(q.get("projectType")))throw new IllegalArgumentException();
 if(q.containsKey("bankProject")&&!Set.of("true","false").contains(q.get("bankProject")))throw new IllegalArgumentException();
 LocalDate from=q.getOrDefault("termStart","").isBlank()?null:LocalDate.parse(q.get("termStart"));
 LocalDate to=q.getOrDefault("termEnd","").isBlank()?null:LocalDate.parse(q.get("termEnd"));
 if(from!=null&&to!=null&&from.isAfter(to))throw new IllegalArgumentException();
 return repository.findAll().stream().filter(p->contains(p.getProjectName(),q.getOrDefault("name",q.get("projectName")))&&contains(p.contractNo,q.get("contractNo"))&&contains(p.getMaintenanceLeader(),q.get("owner"))&&contains(p.getPropertyUnit(),q.get("property"))&&contains(p.getRegion(),q.get("region"))&&contains(p.contact,q.get("contact"))&&contains(p.settlement,q.get("settlement"))&&(!q.containsKey("projectType")||q.get("projectType").isBlank()||q.get("projectType").equals(p.projectType))&&(!q.containsKey("category")||q.get("category").isBlank()||q.get("category").equals(p.getCategory()))&&(!q.containsKey("warning")||q.get("warning").isBlank()||q.get("warning").equals(p.getWarning()))&&(!q.containsKey("bankProject")||Boolean.valueOf(q.get("bankProject")).equals(p.bankProject))&&range(p.area,q.get("areaMin"),q.get("areaMax"))&&range(p.contractAmount,q.get("contractAmountMin"),q.get("contractAmountMax"))&&(from==null||(p.contractEnd!=null&&!p.contractEnd.isBefore(from)))&&(to==null||(p.contractStart!=null&&!p.contractStart.isAfter(to)))).sorted(Comparator.comparing(FireProject::getId).reversed()).toList();
 }
 @GetMapping public Result<?> list(@RequestParam Map<String,String> q){try{return Result.success(filtered(q));}catch(IllegalArgumentException|java.time.DateTimeException e){return Result.error(400,"筛选参数无效");}}
 @GetMapping("/page") public Result<?> page(@RequestParam Map<String,String> q){try{
 int page=Integer.parseInt(q.getOrDefault("page","1")),size=Integer.parseInt(q.getOrDefault("size","10"));if(page<1||size<1||size>100)return Result.error(400,"分页参数无效");
 var all=filtered(q);var month=YearMonth.now(ZoneId.of("Asia/Shanghai"));
 var amount=all.stream().map(p->p.contractAmount==null?BigDecimal.ZERO:p.contractAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
 var expiring=all.stream().filter(p->!"TERMINATED".equals(p.projectType)&&p.contractEnd!=null&&YearMonth.from(p.contractEnd).equals(month)).count();
 return Result.success(Map.of("items",all.stream().skip((long)(page-1)*size).limit(size).toList(),"total",all.size(),"page",page,"size",size,"summary",Map.of("projectCount",all.size(),"contractAmount",amount,"expiringThisMonth",expiring)));
 }catch(IllegalArgumentException|java.time.DateTimeException e){return Result.error(400,"筛选参数无效");}}
 @GetMapping("/{id}") public Result<FireProject> get(@PathVariable("id") Long id){return repository.findById(id).map(Result::success).orElseGet(()->Result.error(404,"项目不存在"));}
 private boolean money(BigDecimal v){return v==null||(v.signum()>=0&&v.scale()<=2&&v.precision()-v.scale()<=16);}
 private boolean valid(FireProject p){if(p.projectType==null)p.projectType="QUANZHOU";return p.getProjectName()!=null&&!p.getProjectName().isBlank()&&p.getProjectName().length()<=255&&TYPES.contains(p.projectType)&&money(p.area)&&money(p.contractAmount)&&(p.contractStart==null||p.contractEnd==null||!p.contractStart.isAfter(p.contractEnd));}
 @PostMapping public Result<?> create(@RequestBody FireProject p){if(!valid(p))return Result.error(400,"项目名称、分类、金额、面积或合同期限无效");p.setId(null);if(links.duplicate(p))return Result.error(409,"同公司下项目名称已存在");p.sourceKey=null;p.sourceRaw=null;p.sourceSheet=null;p.sourceRow=null;p.sharedAmountGroup=null;p.sharedAmountRaw=null;var saved=repository.save(p);links.sync(saved);return Result.success(saved);}
 @PutMapping("/{id}") public Result<?> update(@PathVariable("id") Long id,@RequestBody FireProject p){var old=repository.findById(id);if(old.isEmpty())return Result.error(404,"项目不存在");if(!valid(p))return Result.error(400,"项目名称、分类、金额、面积或合同期限无效");p.setId(id);if(links.duplicate(p))return Result.error(409,"同公司下项目名称已存在");p.sourceKey=old.get().sourceKey;p.sourceRaw=old.get().sourceRaw;p.sourceSheet=old.get().sourceSheet;p.sourceRow=old.get().sourceRow;p.sharedAmountGroup=old.get().sharedAmountGroup;p.sharedAmountRaw=old.get().sharedAmountRaw;var saved=repository.save(p);links.sync(saved);return Result.success(saved);}
 @DeleteMapping("/{id}") public Result<?> delete(@PathVariable("id") Long id){if(repository.findById(id).isEmpty())return Result.error(404,"项目不存在");if(links.referenced(id))return Result.error(409,"项目仍有关联账期");repository.deleteById(id);return Result.success(null);}
}
