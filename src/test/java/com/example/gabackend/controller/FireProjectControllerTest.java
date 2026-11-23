package com.example.gabackend.controller;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.example.gabackend.model.FireProject;
import com.example.gabackend.repository.FireProjectRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.math.BigDecimal;
import java.time.LocalDate;
class FireProjectControllerTest {
 @Test void frontendFieldsBind() throws Exception {var p=new ObjectMapper().readValue("{\"name\":\"测试项目\",\"property\":\"物业\",\"owner\":\"负责人\",\"category\":\"厦门分公司\"}",FireProject.class);assertEquals("测试项目",p.getProjectName());assertEquals("负责人",p.getMaintenanceLeader());assertEquals("XIAMEN",p.projectType);}
 @Test void rejectsInvalidRangeAndPagination(){var c=new FireProjectController(mock(FireProjectRepository.class),mock(com.example.gabackend.service.ProjectLinks.class));assertEquals(400,c.page(Map.of("page","0")).getCode());assertEquals(400,c.list(Map.of("termStart","2026-12-01","termEnd","2026-01-01")).getCode());assertEquals(400,c.list(Map.of("areaMin","bad")).getCode());}
 @Test void filteredSummaryIncludesAllPages(){var r=mock(FireProjectRepository.class);var a=new FireProject();a.setId(1L);a.setProjectName("甲");a.projectType="QUANZHOU";a.contractAmount=new BigDecimal("100");var b=new FireProject();b.setId(2L);b.setProjectName("乙");b.projectType="QUANZHOU";b.contractAmount=new BigDecimal("200");when(r.findAll()).thenReturn(List.of(a,b));var result=new FireProjectController(r,mock(com.example.gabackend.service.ProjectLinks.class)).page(Map.of("projectType","QUANZHOU","size","1"));var data=(Map<?,?>)result.getData();assertEquals(2,data.get("total"));assertEquals(1,((List<?>)data.get("items")).size());assertEquals(new BigDecimal("300"),((Map<?,?>)data.get("summary")).get("contractAmount"));}
 @Test void rejectsInvalidProject(){var r=mock(FireProjectRepository.class);var c=new FireProjectController(r,mock(com.example.gabackend.service.ProjectLinks.class));assertEquals(400,c.create(new FireProject()).getCode());var p=new FireProject();p.setProjectName("甲");p.contractStart=LocalDate.of(2026,2,1);p.contractEnd=LocalDate.of(2026,1,1);assertEquals(400,c.create(p).getCode());verifyNoInteractions(r);}
 @Test void updatePreservesProvenance(){var r=mock(FireProjectRepository.class);var old=new FireProject();old.sourceKey="original";when(r.findById(1L)).thenReturn(Optional.of(old));when(r.save(any())).thenAnswer(i->i.getArgument(0));var p=new FireProject();p.setProjectName("甲");p.sourceKey="forged";assertEquals(200,new FireProjectController(r,mock(com.example.gabackend.service.ProjectLinks.class)).update(1L,p).getCode());assertEquals("original",p.sourceKey);}
}
