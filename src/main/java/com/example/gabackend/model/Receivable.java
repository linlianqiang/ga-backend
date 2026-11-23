package com.example.gabackend.model;
import jakarta.persistence.*;
@Entity
@Table(name="project_receivable")
public class Receivable {
public Long projectId;
@Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
@Column(length=1000)
public String name;
@Column(length=1000)
public String company;
@Column(length=1000)
public String property;
@Column(length=1000)
public String owner;
@Column(length=1000)
public String quarter;
@Column(length=1000)
public String status;
@Column(length=1000)
public String account;
@Column(length=1000)
public String method;
@Column(precision=18, scale=2)
public java.math.BigDecimal amount;
@Column(precision=18, scale=2)
public java.math.BigDecimal net;
public java.time.LocalDate start;
public java.time.LocalDate end;
public java.time.LocalDate invoice;
public java.time.LocalDate due;
public java.time.LocalDate follow;
public Boolean bad;
@Column(columnDefinition="TEXT")
public String note;
@Column(unique=true, length=100)
public String sourceKey;
@Column(columnDefinition="TEXT")
public String sourceRaw;
}
