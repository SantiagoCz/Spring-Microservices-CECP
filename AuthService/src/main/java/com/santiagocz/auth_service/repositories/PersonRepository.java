package com.santiagocz.auth_service.repositories;

import com.santiagocz.auth_service.domain.entities.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {

    Optional<Person> findByDni(String dni);

    boolean existsByDni(String dni);
}