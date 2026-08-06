package com.santiagocz.auth_service.services;

import com.santiagocz.auth_service.domain.entities.Person;
import com.santiagocz.auth_service.domain.entities.SubRole;
import com.santiagocz.auth_service.domain.entities.User;
import com.santiagocz.auth_service.domain.enums.HierarchyRole;
import com.santiagocz.auth_service.dto.request.RegisterRequest;
import com.santiagocz.auth_service.dto.response.PersonResponse;
import com.santiagocz.auth_service.repositories.PersonRepository;
import com.santiagocz.auth_service.repositories.SubRoleRepository;
import com.santiagocz.auth_service.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PersonRepository personRepository;
    private final SubRoleRepository subRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User registerUser(RegisterRequest request) {
        // Verificar si el DNI ya existe
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("El usuario con DNI " + request.getUsername() + " ya existe");
        }

        if (personRepository.existsByDni(request.getUsername())) {
            throw new IllegalArgumentException("Ya existe una persona con este DNI");
        }

        // Crear Person
        Person person = Person.builder()
                .dni(request.getUsername())
                .firstName(request.getPerson().getFirstName())
                .lastName(request.getPerson().getLastName())
                .phoneNumber(request.getPerson().getPhoneNumber())
                .birthDate(request.getPerson().getBirthDate())
                .build();

        personRepository.save(person);

        // Crear User
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .hierarchyRole(HierarchyRole.valueOf(request.getHierarchyRole()))
                .person(person)
                .subroles(new HashSet<>())
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();

        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username));
    }

    @Transactional
    public void addSubrolToUser(Long userId, String subrolName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        SubRole subRole = subRoleRepository.findByName(subrolName)
                .orElseThrow(() -> new IllegalArgumentException("Subrol no encontrado: " + subrolName));

        user.getSubroles().add(subRole);
        userRepository.save(user);
    }

    @Transactional
    public void removeSubrolFromUser(Long userId, String subrolName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        SubRole subRole = subRoleRepository.findByName(subrolName)
                .orElseThrow(() -> new IllegalArgumentException("Subrol no encontrado: " + subrolName));

        user.getSubroles().remove(subRole);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public PersonResponse getPersonByUsername(String username) {
        User user = getUserByUsername(username);
        Person person = user.getPerson();

        return PersonResponse.builder()
                .id(person.getId())
                .dni(person.getDni())
                .firstName(person.getFirstName())
                .lastName(person.getLastName())
                .phoneNumber(person.getPhoneNumber())
                .birthDate(person.getBirthDate())
                .build();
    }
}