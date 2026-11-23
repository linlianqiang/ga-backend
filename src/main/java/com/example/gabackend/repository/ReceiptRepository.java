package com.example.gabackend.repository;
import com.example.gabackend.model.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ReceiptRepository extends JpaRepository<Receipt,Long> { boolean existsByBill(Long bill); java.util.List<Receipt> findByBill(Long bill); }
