package com.santiagocz.auth_service.services;

import com.santiagocz.auth_service.domain.entities.Person;
import com.santiagocz.auth_service.domain.entities.SubRole;
import com.santiagocz.auth_service.domain.entities.User;
import com.santiagocz.auth_service.domain.enums.HierarchyRole;
import com.santiagocz.auth_service.dto.request.RegisterRequest;
import com.santiagocz.auth_service.dto.response.PersonResponse;
import com.santiagocz.auth_service.dto.response.RegisterResponse;
import com.santiagocz.auth_service.exceptions.UserAlreadyExistsException;
import com.santiagocz.auth_service.exceptions.UserNotFoundException;
import com.santiagocz.auth_service.repositories.PersonRepository;
import com.santiagocz.auth_service.repositories.SubRoleRepository;
import com.santiagocz.auth_service.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PersonRepository personRepository;
    private final SubRoleRepository subRoleRepository;
    private final PasswordEncoder passwordEncoder;

    // ──────────── CREATE ────────────

    @Transactional
    public RegisterResponse registerUser(RegisterRequest request) {

        validateDniNotInUse(request.getPerson().getDni());

        Person person = Person.builder()
                .dni(request.getPerson().getDni())
                .firstName(request.getPerson().getFirstName())
                .lastName(request.getPerson().getLastName())
                .phoneNumber(request.getPerson().getPhoneNumber())
                .birthDate(request.getPerson().getBirthDate())
                .build();

        personRepository.save(person);

        User user = User.builder()
                .username(request.getPerson().getDni())
                .password(passwordEncoder.encode(request.getPassword()))
                .hierarchyRole(HierarchyRole.valueOf(request.getHierarchyRole()))
                .person(person)
                .subroles(new HashSet<>())
                .createdBy(getAuthenticatedUsername())
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();

        User savedUser = userRepository.save(user);

        return buildResponse(savedUser);
    }

    // ──────────── READ ────────────

    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado: " + username));
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

    // ──────────── UPDATE - SUBROLES ────────────

    @Transactional
    public void addSubrolToUser(Long userId, String subrolName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado"));

        SubRole subRole = subRoleRepository.findByName(subrolName)
                .orElseThrow(() -> new IllegalArgumentException("Subrol no encontrado: " + subrolName));

        user.getSubroles().add(subRole);
        userRepository.save(user);
    }

    @Transactional
    public void removeSubrolFromUser(Long userId, String subrolName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado"));

        SubRole subRole = subRoleRepository.findByName(subrolName)
                .orElseThrow(() -> new IllegalArgumentException("Subrol no encontrado: " + subrolName));

        user.getSubroles().remove(subRole);
        userRepository.save(user);
    }

    // ──────────── DELETE - RESTORE ────────────

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado"));

        user.setEnabled(false);
        user.setDeletedAt(LocalDateTime.now());
        user.setDeletedBy(getAuthenticatedUsername());
        user.setUpdatedBy(getAuthenticatedUsername());
        userRepository.save(user);
    }

    @Transactional
    public void restoreUser(Long id) {
        User user = userRepository.findByIdIncludingDeleted(id)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado"));

        user.setEnabled(true);
        user.setDeletedAt(null);
        user.setDeletedBy(null);
        user.setUpdatedBy(getAuthenticatedUsername());
        userRepository.save(user);
    }

    // ──────────── PRIVATES AND AUX METHODS ────────────

    private void validateDniNotInUse(String dni) {
        if (userRepository.existsByUsername(dni)) {
            throw new UserAlreadyExistsException("El usuario con DNI " + dni + " ya existe");
        }
    }

    private String getAuthenticatedUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return null;
    }

    // Mappers
    private RegisterResponse buildResponse(User user) {
        return RegisterResponse.builder()
                .hierarchyRole(user.getHierarchyRole())
                .subroles(user.getSubroles().stream()
                        .map(SubRole::getName)
                        .collect(Collectors.toSet()))
                .person(buildPersonResponse(user.getPerson()))
                .build();
    }

    private PersonResponse buildPersonResponse(Person person) {
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