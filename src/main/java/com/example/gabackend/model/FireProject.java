package com.example.gabackend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "fire_project") // 表名带连字符，需要用反引号包裹
public class FireProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String projectNumber;
    private String projectName;
    private String propertyUnit;
    private String contactPerson;
    private String contactPhone;
    private String maintenanceLeader;
    private String fireHostModel;
    private String platformPractitioner;
    private String contractTerm;
    private String region;
    public String company;
    @Column(columnDefinition="TEXT") public String note;

    public FireProject() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProjectNumber() {
        return projectNumber;
    }

    public void setProjectNumber(String projectNumber) {
        this.projectNumber = projectNumber;
    }

    public String getProjectName() {
        return projectName;
    }

    @JsonAlias("name")
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getPropertyUnit() {
        return propertyUnit;
    }

    @JsonAlias("property")
    public void setPropertyUnit(String propertyUnit) {
        this.propertyUnit = propertyUnit;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getMaintenanceLeader() {
        return maintenanceLeader;
    }

    @JsonAlias("owner")
    public void setMaintenanceLeader(String maintenanceLeader) {
        this.maintenanceLeader = maintenanceLeader;
    }

    public String getFireHostModel() {
        return fireHostModel;
    }

    public void setFireHostModel(String fireHostModel) {
        this.fireHostModel = fireHostModel;
    }

    public String getPlatformPractitioner() {
        return platformPractitioner;
    }

    public void setPlatformPractitioner(String platformPractitioner) {
        this.platformPractitioner = platformPractitioner;
    }

    public String getContractTerm() {
        return contractTerm;
    }

    public void setContractTerm(String contractTerm) {
        this.contractTerm = contractTerm;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    @Column(columnDefinition="TEXT")
    public String projectType;

    @Column(columnDefinition="TEXT")
    public String contractNo;

    @Column(columnDefinition="TEXT")
    public String contact;

    @Column(columnDefinition="TEXT")
    public String settlement;

    @Column(columnDefinition="TEXT")
    public String renewalOwner;

    @Column(columnDefinition="TEXT")
    public String followStatus;

    @Column(columnDefinition="TEXT")
    public String contractStatus;

    @Column(columnDefinition="TEXT")
    public String postedRaw;

    @Column(columnDefinition="TEXT")
    public String contractUploadedRaw;

    @Column(columnDefinition="TEXT")
    public String sourceSheet;

    @Column(unique=true,length=100)
    public String sourceKey;

    @Column(columnDefinition="TEXT")
    public String sourceRaw;

    @Column(columnDefinition="TEXT")
    public String sharedAmountGroup;

    @Column(columnDefinition="TEXT")
    public String sharedAmountRaw;

    @Column(precision=18,scale=2)
    public java.math.BigDecimal area;

    @Column(precision=18,scale=2)
    public java.math.BigDecimal contractAmount;


    public java.time.LocalDate contractStart;


    public java.time.LocalDate contractEnd;


    public java.time.LocalDate followDate;


    public Boolean bankProject;


    public Boolean posted;


    public Boolean contractUploaded;


    public Integer sourceRow;

    public void setName(String value){setProjectName(value);}
    public String getName() { return getProjectName(); }

    public void setProperty(String value){setPropertyUnit(value);}
    public String getProperty() { return getPropertyUnit(); }

    public void setOwner(String value){setMaintenanceLeader(value);}
    public String getOwner() { return getMaintenanceLeader(); }

    public String getCategory(){return "XIAMEN".equals(projectType)?"厦门分公司":"PARTNER".equals(projectType)?"合作挂靠项目":"TERMINATED".equals(projectType)?"合同终止":"总公司";}
    public void setCategory(String category){projectType=switch(category){case "总公司", "泉州项目" -> "QUANZHOU";case "厦门分公司", "厦门项目" -> "XIAMEN";case "合作挂靠项目", "合作伙伴项目" -> "PARTNER";case "合同终止" -> "TERMINATED";default -> category;};}
    @JsonProperty(access=JsonProperty.Access.READ_ONLY)
    public String getWarning(){
        if("TERMINATED".equals(projectType)) return "已终止";
        if(contractEnd==null)return "未填写";
        long days=java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")),contractEnd);
        return days<0?"已到期":days<=30?"30天内到期":"正常";
    }
}
