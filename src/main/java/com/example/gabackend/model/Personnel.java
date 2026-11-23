package com.example.gabackend.model;
import jakarta.persistence.*;
@Entity @Table(name="personnel",uniqueConstraints=@UniqueConstraint(columnNames={"name"})) public class Personnel { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id; public String name; public String role; public String phone; public java.time.LocalDate joined; public String education; }