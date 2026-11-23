package com.example.gabackend.repository;
import com.example.gabackend.model.Receivable;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ReceivableRepository extends JpaRepository<Receivable,Long> {
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 java.util.Optional<Receivable> findById(Long id);
 boolean existsBySourceKey(String sourceKey); }
