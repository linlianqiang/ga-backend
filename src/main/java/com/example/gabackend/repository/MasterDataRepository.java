package com.example.gabackend.repository;
import com.example.gabackend.model.MasterData;
import org.springframework.data.jpa.repository.JpaRepository;
public interface MasterDataRepository extends JpaRepository<MasterData,Long> {}