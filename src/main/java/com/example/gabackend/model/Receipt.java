package com.example.gabackend.model;
import jakarta.persistence.*;
@Entity
@Table(name="project_receipt")
public class Receipt {
@Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
public Long bill;
@Column(precision=18, scale=2)
public java.math.BigDecimal amount;
public java.time.LocalDate date;
@Column(length=1000)
public String method;
@Column(length=1000)
public String reference;
@Column(columnDefinition="TEXT")
public String note;
}
