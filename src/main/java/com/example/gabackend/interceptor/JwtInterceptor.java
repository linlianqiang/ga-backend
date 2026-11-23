package com.example.gabackend.interceptor;
import com.example.gabackend.common.Result;
import com.example.gabackend.service.AccessService;
import com.example.gabackend.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
@Component
public class JwtInterceptor implements HandlerInterceptor {
 private final JwtUtil jwt;private final AccessService access;private final ObjectMapper mapper;
 public JwtInterceptor(JwtUtil j,AccessService a,ObjectMapper m){jwt=j;access=a;mapper=m;}
 public boolean preHandle(HttpServletRequest req,HttpServletResponse res,Object handler)throws Exception{
 if("OPTIONS".equals(req.getMethod()))return true;
 var h=req.getHeader("Authorization");
 try{if(h==null||!h.startsWith("Bearer ")||h.length()>8192)return error(res,401,"缺少有效登录凭据");var c=jwt.extractClaims(h.substring(7));var u=access.user(c.getSubject()).orElse(null);if(u==null||Boolean.FALSE.equals(u.enabled)||!java.util.Objects.equals(c.get("ver",Integer.class),u.tokenVersion==null?0:u.tokenVersion))return error(res,401,"账号或登录凭据已失效");
 String path=req.getRequestURI().substring(req.getContextPath().length());String resource=path.startsWith("/api/projects")?"projects":path.startsWith("/api/personnel")?"personnel":path.startsWith("/api/master-data")?"base":path.startsWith("/api/accounts")||path.startsWith("/api/roles")?"access":path.startsWith("/api/receivables")||path.startsWith("/api/bills")||path.startsWith("/api/receipts")?"ledger":null;
 if(resource==null&&!path.equals("/api/user/info")&&!path.equals("/api/auth/logout"))return error(res,403,"接口未授权");
 String action="GET".equals(req.getMethod())?"view":"edit";if(path.endsWith("/export"))action="export";else if(resource!=null&&resource.equals("ledger")&&path.contains("receipts")&&!"GET".equals(req.getMethod()))action="receipt";
 if("ledger".equals(resource)&&"GET".equals(req.getMethod())&&(path.equals("/api/bills")||path.equals("/api/receivables")||path.equals("/api/bills/export")||path.equals("/api/receivables/export"))){
  String[] tabs=req.getParameterValues("tab");
  if(tabs!=null&&tabs.length!=1)return error(res,400,"tab参数无效");
  String tab=tabs==null||tabs[0].isEmpty()?"all":tabs[0];
  if(!java.util.Set.of("all","unpaid","due").contains(tab))return error(res,400,"tab参数无效");
  if(!access.permissions(u).contains("ledger.tab."+tab))return error(res,403,"没有该回款tab的查看权限");
 }
 if(resource!=null&&!access.permissions(u).contains(resource+"."+action))return error(res,403,"没有操作权限");req.setAttribute("currentUser",u);req.setAttribute("permissions",access.permissions(u));return true;
 }catch(io.jsonwebtoken.JwtException|IllegalArgumentException e){return error(res,401,"Token无效或过期");}}
 private boolean error(HttpServletResponse r,int status,String msg)throws Exception{r.setStatus(status);r.setContentType("application/json;charset=UTF-8");mapper.writeValue(r.getWriter(),Result.error(status,msg));return false;}
}
