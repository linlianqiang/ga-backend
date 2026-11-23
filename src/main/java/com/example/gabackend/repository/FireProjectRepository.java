package com.example.gabackend.repository;

import com.example.gabackend.model.FireProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FireProjectRepository extends JpaRepository<FireProject, Long> {
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 java.util.Optional<FireProject> findById(Long id);

}
