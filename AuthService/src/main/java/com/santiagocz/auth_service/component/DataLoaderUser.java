package com.santiagocz.auth_service.component;

import com.santiagocz.auth_service.domain.entities.Person;
import com.santiagocz.auth_service.domain.entities.User;
import com.santiagocz.auth_service.domain.entities.SubRole;
import com.santiagocz.auth_service.domain.enums.HierarchyRole;
import com.santiagocz.auth_service.repositories.PersonRepository;
import com.santiagocz.auth_service.repositories.SubRoleRepository;
import com.santiagocz.auth_service.repositories.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;

@Component
@RequiredArgsConstructor
public class DataLoaderUser {

    private final UserRepository userRepository;
    private final PersonRepository personRepository;
    private final SubRoleRepository subRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    public void loadData() {
        loadSuperAdminUser();
        loadSubRoles();
    }

    private void loadSuperAdminUser() {
        String dni = "12345678";

        // Verificar si el usuario ya existe
        if (userRepository.existsByUsername(dni)) {
            return;
        }

        // Crear Person
        Person person = Person.builder()
                .dni(dni)
                .firstName("Santiago")
                .lastName("Czarny")
                .phoneNumber("123456789")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();

        personRepository.save(person);

        // Crear User SUPER_ADMIN
        User superAdmin = User.builder()
                .username(dni)
                .password(passwordEncoder.encode("1234"))
                .hierarchyRole(HierarchyRole.SUPER_ADMIN)
                .person(person)
                .createdBy(1L)
                .createdAt(LocalDateTime.now())
                .subroles(new HashSet<>())
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();

        userRepository.save(superAdmin);
        System.out.println("✅ Super Admin user created: " + dni);
    }

    private void loadSubRoles() {
        String[] subRoleNames = {
                "ODONTOLOGY_CLERK",
                "RRHH_ADMIN",
                "MEDICAL_COVERAGE_CLERK",
                "APPOINTMENTS_ADMIN"
        };

        for (String name : subRoleNames) {
            if (!subRoleRepository.existsByName(name)) {
                SubRole subRole = SubRole.builder()
                        .name(name)
                        .description("Subrol for " + name)
                        .build();

                subRoleRepository.save(subRole);
                System.out.println("✅ SubRole created: " + name);
            }
        }
    }
}