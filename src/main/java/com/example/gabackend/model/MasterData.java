package com.example.gabackend.model;
import jakarta.persistence.*;
@Entity @Table(name="master_data",uniqueConstraints=@UniqueConstraint(columnNames={"type","name"})) public class MasterData { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id; public String type; public String name; }