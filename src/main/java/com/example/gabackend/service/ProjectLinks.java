package com.example.gabackend.service;
import com.example.gabackend.model.*;
import com.example.gabackend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service @Transactional
public class ProjectLinks {
 private final FireProjectRepository projects;private final ReceivableRepository bills;
 public ProjectLinks(FireProjectRepository p,ReceivableRepository b){projects=p;bills=b;}
 public void apply(Receivable b){if(b.projectId==null)return;var p=projects.findById(b.projectId).orElseThrow(()->new IllegalArgumentException("关联项目不存在"));b.name=p.getProjectName();if(p.company!=null&&!p.company.isBlank())b.company=p.company;b.property=p.getPropertyUnit();b.owner=p.getMaintenanceLeader();}
 public void sync(FireProject p){for(var b:bills.findAll())if(Objects.equals(b.projectId,p.getId())){b.name=p.getProjectName();if(p.company!=null&&!p.company.isBlank())b.company=p.company;b.property=p.getPropertyUnit();b.owner=p.getMaintenanceLeader();bills.save(b);}}
 public boolean referenced(Long id){return bills.findAll().stream().anyMatch(b->Objects.equals(b.projectId,id));}
 public boolean duplicate(FireProject p){return projects.findAll().stream().anyMatch(x->!Objects.equals(x.getId(),p.getId())&&Objects.equals(trim(x.company),trim(p.company))&&trim(x.getProjectName()).equalsIgnoreCase(trim(p.getProjectName())));}
 private String trim(String s){return s==null?"":s.trim();}
}
