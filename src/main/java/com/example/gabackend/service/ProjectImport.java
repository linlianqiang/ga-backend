package com.example.gabackend.service;
import com.example.gabackend.model.FireProject;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import java.io.File;
import java.util.*;
import java.lang.reflect.Field;
@Component
@ConditionalOnProperty(name="projects.import-file")
public class ProjectImport implements CommandLineRunner {
 private final JdbcTemplate jdbc; private final ObjectMapper mapper;
 @Value("${projects.import-file}") private String file;
 public ProjectImport(JdbcTemplate j,ObjectMapper m){jdbc=j;mapper=m;}
 @Transactional public void run(String... args)throws Exception{
 var keys=new HashSet<>(jdbc.queryForList("select source_key from fire_project where source_key is not null",String.class));
 var fields=Arrays.stream(FireProject.class.getDeclaredFields()).filter(f->!f.getName().equals("id")).toList();
 for(var field:fields)field.setAccessible(true);
 var columns=fields.stream().map(f->"`"+f.getName().replaceAll("([a-z])([A-Z])","$1_$2").toLowerCase()+"`").toList();
 var rows=new ArrayList<Object[]>();
 for(FireProject b:mapper.readValue(new File(file),FireProject[].class)){
 if(b.sourceKey==null||b.getProjectName()==null)throw new IllegalArgumentException("Invalid import record");
 if(keys.add(b.sourceKey)){Object[] values=new Object[fields.size()];for(int i=0;i<fields.size();i++)values[i]=fields.get(i).get(b);rows.add(values);}
 }
 // Multi-row inserts avoid thousands of network round trips; all batches share one transaction.
 for(int offset=0;offset<rows.size();offset+=100){var batch=rows.subList(offset,Math.min(offset+100,rows.size()));
 String group="("+String.join(",",Collections.nCopies(fields.size(),"?"))+")";
 jdbc.update("insert into fire_project ("+String.join(",",columns)+") values "+String.join(",",Collections.nCopies(batch.size(),group)),batch.stream().flatMap(Arrays::stream).toArray());}
 System.out.println("PROJECT_IMPORT inserted="+rows.size()+" total="+jdbc.queryForObject("select count(*) from fire_project",Long.class));
 }
}
