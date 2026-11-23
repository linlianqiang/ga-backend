package com.example.gabackend.interceptor;
import com.example.gabackend.model.User;
import com.example.gabackend.service.AccessService;
import com.example.gabackend.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class JwtInterceptorTest {
 @Test void tabsAreCheckedForBothAliasesAndExports() throws Exception {
  var jwt=mock(JwtUtil.class);var access=mock(AccessService.class);var claims=mock(Claims.class);
  var user=new User();when(jwt.extractClaims("token")).thenReturn(claims);
  when(claims.getSubject()).thenReturn("tester");when(claims.get("ver",Integer.class)).thenReturn(0);
  when(access.user("tester")).thenReturn(Optional.of(user));
  when(access.permissions(user)).thenReturn(Set.of("ledger.view","ledger.export","ledger.tab.due"));
  var interceptor=new JwtInterceptor(jwt,access,new ObjectMapper());
  for(String path:List.of("/api/bills","/api/receivables","/api/bills/export","/api/receivables/export")){
   for(String tab:List.of("due","all","unpaid","invalid","")){
    var req=new MockHttpServletRequest("GET",path);req.addHeader("Authorization","Bearer token");
    if(!tab.isEmpty())req.addParameter("tab",tab);
    var res=new MockHttpServletResponse();
    assertEquals(tab.equals("due"),interceptor.preHandle(req,res,new Object()));
    if(!tab.equals("due"))assertEquals(tab.equals("invalid")?400:403,res.getStatus());
   }
  }
 }
 @Test void tabPermissionAddsOnlyMenuView(){
  assertEquals(Set.of("ledger.view","ledger.tab.due"),AccessService.normalize(Set.of("ledger.tab.due")));
 }
}
