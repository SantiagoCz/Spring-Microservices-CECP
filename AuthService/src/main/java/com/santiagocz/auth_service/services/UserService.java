package com.santiagocz.auth_service.services;

import com.santiagocz.auth_service.domain.entities.Person;
import com.santiagocz.auth_service.domain.entities.SubRole;
import com.santiagocz.auth_service.domain.entities.User;
import com.santiagocz.auth_service.domain.enums.HierarchyRole;
import com.santiagocz.auth_service.dto.request.RegisterRequest;
import com.santiagocz.auth_service.dto.request.UpdatePasswordRequest;
import com.santiagocz.auth_service.dto.response.PersonResponse;
import com.santiagocz.auth_service.dto.response.RegisterResponse;
import com.santiagocz.auth_service.exceptions.InvalidPasswordException;
import com.santiagocz.auth_service.exceptions.SubRoleNotFoundException;
import com.santiagocz.auth_service.exceptions.UserAlreadyExistsException;
import com.santiagocz.auth_service.exceptions.UserNotFoundException;
import com.santiagocz.auth_service.repositories.PersonRepository;
import com.santiagocz.auth_service.repositories.SubRoleRepository;
import com.santiagocz.auth_service.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

        Person person = personRepository.save(Person.builder()
                .dni(request.getPerson().getDni())
                .firstName(request.getPerson().getFirstName())
                .lastName(request.getPerson().getLastName())
                .phoneNumber(request.getPerson().getPhoneNumber())
                .birthDate(request.getPerson().getBirthDate())
                .build());

        User user = User.builder()
                .username(request.getPerson().getDni())
                .password(passwordEncoder.encode(request.getPassword()))
                .hierarchyRole(request.getHierarchyRole())
                .person(person)
                .build();

        setCreatorUser(user);

        return buildResponse(userRepository.save(user));
    }

    // ──────────── READ ────────────

    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con ID: " + userId));
    }

    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado: " + username));
    }

    @Transactional(readOnly = true)
    public PersonResponse getPersonByUsername(String username) {
        return buildPersonResponse(getUserByUsername(username).getPerson());
    }

    // ──────────── UPDATE — PASSWORD ────────────

    //TODO: faltan métodos de actualización de atributos de persona.

    @Transactional
    public void updateMyPassword(UpdatePasswordRequest request) {
        User user = getAuthenticatedUser();

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new AccessDeniedException("La contraseña actual no es correcta");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new InvalidPasswordException("La nueva contraseña debe ser distinta de la actual");
        }

        applyNewPassword(user, request.getNewPassword());
    }

    @Transactional
    public void resetUserPassword(Long userId, String newPassword) {
        User actor = getAuthenticatedUser();
        User target = getUserById(userId);

        if (!canManage(actor, target)) {
            throw new AccessDeniedException("No tenés permisos para modificar la contraseña de este usuario");
        }

        applyNewPassword(target, newPassword);
    }

    private void applyNewPassword(User user, String rawPassword) {
        user.setPassword(passwordEncoder.encode(rawPassword));
        setUpdaterUser(user);
    }

    // ──────────── UPDATE — SUBROLES ────────────

    @Transactional
    public void addSubrolToUser(Long userId, String subrolName) {
        User user = getUserById(userId);
        user.getSubroles().add(getSubRoleByName(subrolName));
        setUpdaterUser(user);
    }

    @Transactional
    public void removeSubrolFromUser(Long userId, String subrolName) {
        User user = getUserById(userId);
        user.getSubroles().remove(getSubRoleByName(subrolName));
        setUpdaterUser(user);
    }

    // ──────────── DELETE — RESTORE ────────────

    @Transactional
    public void deleteUser(Long id) {
        User actor = getAuthenticatedUser();
        User user = getUserById(id);

        if (!canDelete(actor, user)) {
            throw new AccessDeniedException("No tenés permisos para dar de baja a este usuario");
        }

        user.setEnabled(false);
        setDeleterUser(user);
        setUpdaterUser(user);
    }

    @Transactional
    public void restoreUser(Long id) {
        User actor = getAuthenticatedUser();
        User user = userRepository.findByIdIncludingDeleted(id)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con ID: " + id));

        if (!canManage(actor, user)) {
            throw new AccessDeniedException("No tenés permisos para dar de alta a este usuario");
        }

        user.setEnabled(true);
        user.setDeletedAt(null);
        user.setDeletedBy(null);
        setUpdaterUser(user);
    }

    // ──────────── AUTHORIZATION ────────────

    private boolean canManage(User actor, User target) {
        return switch (actor.getHierarchyRole()) {
            case SUPER_ADMIN -> target.getHierarchyRole() != HierarchyRole.SUPER_ADMIN;
            case ADMIN -> target.getHierarchyRole() == HierarchyRole.USER
                    && actor.getId().equals(target.getCreatedBy());
            default -> false;
        };
    }

    private boolean canDelete(User actor, User target) {
        if (actor.getId().equals(target.getId())) {
            return false;   // nadie se da de baja a sí mismo
        }
        if (target.getHierarchyRole() == HierarchyRole.SUPER_ADMIN) {
            return false;   // los superadministradores no se dan de baja
        }
        return canManage(actor, target);
    }

    // ──────────── AUDIT METADATA ────────────

    private void setCreatorUser(User user) {
        user.setCreatedBy(getAuthenticatedUser().getId());
    }

    private void setUpdaterUser(User user) {
        user.setUpdatedBy(getAuthenticatedUser().getId());
    }

    private void setDeleterUser(User user) {
        user.setDeletedBy(getAuthenticatedUser().getId());
        user.setDeletedAt(LocalDateTime.now());
    }

    // ──────────── PRIVATE HELPERS ────────────

    private void validateDniNotInUse(String dni) {
        if (userRepository.existsByUsername(dni)) {
            throw new UserAlreadyExistsException("El usuario con DNI " + dni + " ya existe");
        }
    }

    private SubRole getSubRoleByName(String subrolName) {
        return subRoleRepository.findByName(subrolName)
                .orElseThrow(() -> new SubRoleNotFoundException("Subrol no encontrado: " + subrolName));
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("No hay usuario autenticado");
        }
        return getUserByUsername(authentication.getName());
    }

    // ──────────── MAPPERS ────────────

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