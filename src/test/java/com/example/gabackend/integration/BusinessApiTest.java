package com.example.gabackend.integration;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.example.gabackend.model.*;
import com.example.gabackend.repository.*;
import com.example.gabackend.util.JwtUtil;
import com.example.gabackend.service.AccessService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:api;MODE=MySQL;NON_KEYWORDS=USER,END,START;DB_CLOSE_DELAY=-1","spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=","spring.jpa.hibernate.ddl-auto=create-drop","spring.jpa.show-sql=false","jwt.secret=test-only-secret-at-least-thirty-two-bytes","directory.bootstrap=false"})
@AutoConfigureMockMvc @Transactional
class BusinessApiTest {
 @Autowired MasterDataRepository masters;
 @Autowired MockMvc mvc;@Autowired UserRepository users;@Autowired AccessRoleRepository roles;@Autowired FireProjectRepository projects;@Autowired ReceivableRepository bills;@Autowired JwtUtil jwt;@Autowired ObjectMapper json;
 String token;User admin;
 @BeforeEach void setup(){admin=new User("admin","legacy-password","ADMIN");admin=users.saveAndFlush(admin);token=jwt.generateToken("admin","ADMIN",0);}
 @Test void adminCanChangeAssignedAdministratorRoleButBuiltinAdminIsProtected()throws Exception{
 var privileged=new AccessRole();privileged.name="系统管理员测试";privileged.protectedRole=true;privileged.permissions=com.example.gabackend.service.AccessService.ALL;privileged=roles.saveAndFlush(privileged);
 var ordinary=new AccessRole();ordinary.name="普通测试";ordinary.permissions=Set.of("projects.view");ordinary=roles.saveAndFlush(ordinary);
 var u=new User("assignedAdmin","x","ADMIN");u.roleId=privileged.id;u=users.saveAndFlush(u);
 mvc.perform(get("/api/accounts/"+u.getId()).header("Authorization","Bearer "+token)).andExpect(jsonPath("$.data.protected").value(false));
 String body=json.writeValueAsString(Map.of("login","assignedAdmin","name","普通用户","role",ordinary.id,"enabled",true));
 mvc.perform(put("/api/accounts/"+u.getId()).header("Authorization","Bearer "+token).contentType("application/json").content(body)).andExpect(jsonPath("$.code").value(200));
 assertEquals(ordinary.id,users.findById(u.getId()).orElseThrow().roleId);
 mvc.perform(put("/api/accounts/"+admin.getId()).header("Authorization","Bearer "+token).contentType("application/json").content(body.replace("assignedAdmin","admin"))).andExpect(jsonPath("$.code").value(400));
 }
 @Test void noTokenAndTamperedTokenRejected()throws Exception{mvc.perform(get("/api/projects")).andExpect(status().isUnauthorized());mvc.perform(get("/api/projects").header("Authorization","Bearer "+token+"bad")).andExpect(status().isUnauthorized());}
 @Test void permissionIsCheckedOnEveryRequest()throws Exception{var role=new AccessRole();role.name="只读";role.permissions=Set.of("projects.view");role=roles.saveAndFlush(role);var u=new User("reader","x","VIEWER");u.roleId=role.id;u=users.saveAndFlush(u);String t=jwt.generateToken("reader","VIEWER",0);mvc.perform(get("/api/projects").header("Authorization","Bearer "+t)).andExpect(status().isOk());mvc.perform(post("/api/projects").header("Authorization","Bearer "+t).contentType("application/json").content("{\"name\":\"不能新建\"}")).andExpect(status().isForbidden());u.enabled=false;users.saveAndFlush(u);mvc.perform(get("/api/projects").header("Authorization","Bearer "+t)).andExpect(status().isUnauthorized());}
 @Test void legacyLoginHashesPasswordAndLogoutRevokesToken()throws Exception{var result=mvc.perform(post("/api/auth/login").contentType("application/json").content("{\"username\":\"admin\",\"password\":\"legacy-password\"}")).andExpect(jsonPath("$.code").value(200)).andReturn();assertTrue(users.findById(admin.getId()).orElseThrow().getPassword().startsWith("$2"));String t=json.readTree(result.getResponse().getContentAsString()).path("data").path("token").asText();mvc.perform(post("/api/auth/logout").header("Authorization","Bearer "+t)).andExpect(jsonPath("$.code").value(200));mvc.perform(get("/api/user/info").header("Authorization","Bearer "+t)).andExpect(status().isUnauthorized());}
 @Test void linkedProjectDeletionProtection()throws Exception{var p=new FireProject();p.setProjectName("统一名称");p.company="港安";p=projects.saveAndFlush(p);var b=new Receivable();b.name="统一名称";b.projectId=p.getId();bills.saveAndFlush(b);mvc.perform(delete("/api/projects/"+p.getId()).header("Authorization","Bearer "+token)).andExpect(jsonPath("$.code").value(409));}
 @Test void directoryRenameSynchronizesAndCannotDeleteReferenced()throws Exception{var p=new FireProject();p.setProjectName("测试");p.setPropertyUnit("旧物业");projects.saveAndFlush(p);var result=mvc.perform(post("/api/master-data").header("Authorization","Bearer "+token).contentType("application/json").content("{\"type\":\"property\",\"name\":\"旧物业\"}")).andExpect(jsonPath("$.code").value(200)).andReturn();long id=json.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();mvc.perform(put("/api/master-data/"+id).header("Authorization","Bearer "+token).contentType("application/json").content("{\"type\":\"property\",\"name\":\"新物业\"}")).andExpect(jsonPath("$.code").value(200));assertEquals("新物业",projects.findById(p.getId()).orElseThrow().getPropertyUnit());mvc.perform(delete("/api/master-data/"+id).header("Authorization","Bearer "+token)).andExpect(jsonPath("$.code").value(409));}
 @Test void permissionsNormalizeAndReferencedRoleCannotDelete()throws Exception{var result=mvc.perform(post("/api/roles").header("Authorization","Bearer "+token).contentType("application/json").content("{\"name\":\"编辑\",\"permissions\":[\"projects.edit\"]}")).andExpect(jsonPath("$.code").value(200)).andReturn();long id=json.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();assertTrue(roles.findById(id).orElseThrow().permissions.contains("projects.view"));var u=new User("editor","x","editor");u.roleId=id;users.saveAndFlush(u);mvc.perform(delete("/api/roles/"+id).header("Authorization","Bearer "+token)).andExpect(jsonPath("$.code").value(409));}

 @Test void personnelValidationAndRename()throws Exception{mvc.perform(post("/api/personnel").header("Authorization","Bearer "+token).contentType("application/json").content("{\"name\":\"张三\",\"role\":\"组长\",\"phone\":\"bad\"}")).andExpect(jsonPath("$.code").value(400));mvc.perform(post("/api/personnel").header("Authorization","Bearer "+token).contentType("application/json").content("{\"name\":\"张三\",\"role\":\"组长\",\"phone\":\"13800000000\"}")).andExpect(jsonPath("$.code").value(200));mvc.perform(post("/api/personnel").header("Authorization","Bearer "+token).contentType("application/json").content("{\"name\":\"张三\",\"role\":\"组员\"}")).andExpect(jsonPath("$.code").value(409));}
 @Test void accountPasswordNeverReturnedAndCaseInsensitiveDuplicate()throws Exception{var r=new AccessRole();r.name="普通";r=roles.saveAndFlush(r);String body=json.writeValueAsString(Map.of("login","NewUser","name","新用户","role",r.id,"password","password123","enabled",true));mvc.perform(post("/api/accounts").header("Authorization","Bearer "+token).contentType("application/json").content(body)).andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.data.password").doesNotExist());mvc.perform(post("/api/accounts").header("Authorization","Bearer "+token).contentType("application/json").content(body.replace("NewUser","newuser"))).andExpect(jsonPath("$.code").value(409));assertTrue(users.findByUsername("NewUser").orElseThrow().getPassword().startsWith("$2"));}
 @Test void csvEscapesQuotesAndFormulaPrefix()throws Exception{var b=new Receivable();b.name="=公式";b.note="含\"引号";b=bills.saveAndFlush(b);var response=mvc.perform(get("/api/bills/export").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);assertTrue(response.contains("\"'=公式\""));assertTrue(response.contains("\"含\"\"引号\""));}

 @Test void regionMaintenanceSynchronizesProjectAndProtectsDeletion() throws Exception {
 var p=new FireProject();p.setProjectName("地区测试");p.setRegion("泉州永春");p=projects.saveAndFlush(p);
 var result=mvc.perform(post("/api/master-data").header("Authorization","Bearer "+token).contentType("application/json").content("{\"type\":\"region\",\"name\":\"泉州永春\"}")).andExpect(jsonPath("$.code").value(200)).andReturn();
 long id=json.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
 mvc.perform(put("/api/master-data/"+id).header("Authorization","Bearer "+token).contentType("application/json").content("{\"type\":\"region\",\"name\":\"永春县\"}")).andExpect(jsonPath("$.code").value(200));
 assertEquals("永春县",projects.findById(p.getId()).orElseThrow().getRegion());
 mvc.perform(delete("/api/master-data/"+id).header("Authorization","Bearer "+token)).andExpect(jsonPath("$.code").value(409));
 }

 @Test void ownerFilterIncludesSharedResponsibilitiesAndExport() throws Exception {
 for(String owner:List.of("庄耀铭","庄耀铭（主）王志强","其他人")){var b=new Receivable();b.name="负责人测试";b.owner=owner;bills.saveAndFlush(b);}
 mvc.perform(get("/api/bills").param("owner","庄耀铭").header("Authorization","Bearer "+token)).andExpect(jsonPath("$.data.total").value(2));
 var csv=mvc.perform(get("/api/bills/export").param("owner","庄耀铭").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
 assertTrue(csv.contains("庄耀铭（主）王志强"));assertFalse(csv.contains("其他人"));
 }

 @Test void personnelRolesAreFixedAndIndependentOfAccessRoles() throws Exception {
 for(String role:List.of("组长","组员","师傅")){
 var response=mvc.perform(post("/api/personnel").header("Authorization","Bearer "+token).contentType("application/json").content(json.writeValueAsString(Map.of("name","测试"+role,"role",role)))).andExpect(jsonPath("$.code").value(200)).andReturn();
 long id=json.readTree(response.getResponse().getContentAsString()).path("data").path("id").asLong();
 mvc.perform(put("/api/personnel/"+id).header("Authorization","Bearer "+token).contentType("application/json").content(json.writeValueAsString(Map.of("name","测试"+role,"role","系统管理员")))).andExpect(jsonPath("$.code").value(400));
 }
 mvc.perform(post("/api/personnel").header("Authorization","Bearer "+token).contentType("application/json").content(json.writeValueAsString(Map.of("name","无效角色","role","系统管理员")))).andExpect(jsonPath("$.code").value(400));
 }

 @Test void allBillsIncludeUnlinkedRecordsBeyondOneHundred() throws Exception {
 for(int i=0;i<105;i++){var b=new Receivable();b.name="全量测试";bills.save(b);}bills.flush();
 mvc.perform(get("/api/bills").header("Authorization","Bearer "+token)).andExpect(jsonPath("$.data.items.length()").value(105)).andExpect(jsonPath("$.data.total").value(105));
 mvc.perform(get("/api/receivables").param("size","0").header("Authorization","Bearer "+token)).andExpect(jsonPath("$.data.items.length()").value(105));
 mvc.perform(get("/api/bills").param("size","100").param("page","2").header("Authorization","Bearer "+token)).andExpect(jsonPath("$.data.items.length()").value(5)).andExpect(jsonPath("$.data.total").value(105));
 }

 @Test void masterDataCountsAllMatchingProjects() throws Exception {
 String property="深圳市万象美物业管理有限公司泉州分公司";
 var m=new MasterData();m.type="property";m.name=property;m=masters.saveAndFlush(m);
 for(String type:List.of("QUANZHOU","PARTNER")){var p=new FireProject();p.setProjectName(type);p.projectType=type;p.setPropertyUnit(property);projects.saveAndFlush(p);}
 var other=new FireProject();other.setProjectName("其他");other.setPropertyUnit("其他物业");projects.saveAndFlush(other);
 mvc.perform(get("/api/master-data").param("type","property").param("search",property).header("Authorization","Bearer "+token)).andExpect(jsonPath("$.data[0].projectCount").value(2));
 mvc.perform(get("/api/master-data/"+m.id).header("Authorization","Bearer "+token)).andExpect(jsonPath("$.data.projectCount").value(2));
 }

 @Test void initialAdministratorRequiresExplicitPasswordAndDoesNotOverwrite() {
 users.deleteAll();users.flush();
 var init=new com.example.gabackend.service.DirectoryBootstrap(masters,projects,bills,users,roles);
 org.springframework.test.util.ReflectionTestUtils.setField(init,"initialPassword","");init.run();assertEquals(0,users.count());
 org.springframework.test.util.ReflectionTestUtils.setField(init,"initialPassword","a-secure-test-password");init.run();assertEquals(1,users.count());var u=users.findByUsername("admin").orElseThrow();String hash=u.getPassword();assertTrue(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().matches("a-secure-test-password",hash));assertNotNull(u.roleId);
 org.springframework.test.util.ReflectionTestUtils.setField(init,"initialPassword","a-different-test-password");init.run();assertEquals(hash,users.findByUsername("admin").orElseThrow().getPassword());
 }
}
