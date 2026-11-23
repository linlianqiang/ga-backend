package com.example.gabackend.repository;
import com.example.gabackend.model.Personnel;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PersonnelRepository extends JpaRepository<Personnel,Long> {}