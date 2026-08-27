package com.santiagocz.auth_service.services;

import com.santiagocz.auth_service.domain.entities.Person;
import com.santiagocz.auth_service.domain.entities.SubRole;
import com.santiagocz.auth_service.domain.entities.User;
import com.santiagocz.auth_service.domain.enums.HierarchyRole;
import com.santiagocz.auth_service.dto.request.RegisterRequest;
import com.santiagocz.auth_service.dto.request.UpdatePasswordRequest;
import com.santiagocz.auth_service.dto.response.PageResponse;
import com.santiagocz.auth_service.dto.response.PersonResponse;
import com.santiagocz.auth_service.dto.response.UserResponse;
import com.santiagocz.auth_service.exceptions.InvalidPasswordException;
import com.santiagocz.auth_service.exceptions.SubRoleNotFoundException;
import com.santiagocz.auth_service.exceptions.UserAlreadyExistsException;
import com.santiagocz.auth_service.exceptions.UserNotFoundException;
import com.santiagocz.auth_service.repositories.PersonRepository;
import com.santiagocz.auth_service.repositories.SubRoleRepository;
import com.santiagocz.auth_service.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    public UserResponse registerUser(RegisterRequest request) {

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
                .createdBy(getAuthenticatedUserId())
                .build();

        return buildUserResponse(userRepository.save(user));
    }

    // ──────────── READ ────────────

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId) {
        User authenticatedUser = getAuthenticatedPrincipal();
        User currentUser = findUserById(userId);

        if (!canManage(authenticatedUser, currentUser)) {
            throw new AccessDeniedException("No tenés permisos para acceder a este usuario");
        }
        return buildUserResponse(currentUser);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listUsers(Pageable pageable) {
        User authenticatedUser = getAuthenticatedPrincipal();

        Page<User> users = switch (authenticatedUser.getHierarchyRole()) {
            case SUPER_ADMIN -> userRepository.findByHierarchyRoleNot(HierarchyRole.SUPER_ADMIN, pageable);
            case ADMIN -> userRepository.findByCreatedBy(authenticatedUser.getId(), pageable);
            default -> Page.empty(pageable);
        };

        return PageResponse.from(users.map(this::buildUserResponse));
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
        User authenticatedUser = getAuthenticatedUserManaged();

        if (!passwordEncoder.matches(request.getCurrentPassword(), authenticatedUser.getPassword())) {
            throw new AccessDeniedException("La contraseña actual no es correcta");
        }
        if (passwordEncoder.matches(request.getNewPassword(), authenticatedUser.getPassword())) {
            throw new InvalidPasswordException("La nueva contraseña debe ser distinta de la actual");
        }

        applyNewPassword(authenticatedUser, request.getNewPassword());
    }

    @Transactional
    public void resetUserPassword(Long userId, String newPassword) {
        User authenticatedUser = getAuthenticatedPrincipal();
        User currentUser = findUserById(userId);

        if (!canManage(authenticatedUser, currentUser)) {
            throw new AccessDeniedException("No tenés permisos para modificar la contraseña de este usuario");
        }

        applyNewPassword(currentUser, newPassword);
    }

    private void applyNewPassword(User user, String rawPassword) {
        user.setPassword(passwordEncoder.encode(rawPassword));
        setUpdaterUser(user);
    }

    // ──────────── UPDATE — SUBROLES ────────────

    @Transactional
    public void addSubrolToUser(Long userId, String subrolName) {
        User authenticatedUser = getAuthenticatedPrincipal();
        User currentUser = findUserById(userId);

        if (!canManage(authenticatedUser, currentUser)) {
            throw new AccessDeniedException("No tenés permisos para modificar los subroles de este usuario");
        }

        currentUser.getSubroles().add(getSubRoleByName(subrolName));
        setUpdaterUser(currentUser);
    }

    @Transactional
    public void removeSubrolFromUser(Long userId, String subrolName) {
        User authenticatedUser = getAuthenticatedPrincipal();
        User currentUser = findUserById(userId);

        if (!canManage(authenticatedUser, currentUser)) {
            throw new AccessDeniedException("No tenés permisos para modificar los subroles de este usuario");
        }

        currentUser.getSubroles().remove(getSubRoleByName(subrolName));
        setUpdaterUser(currentUser);
    }

    // ──────────── DELETE — RESTORE ────────────

    @Transactional
    public void deleteUser(Long id) {
        User authenticatedUser = getAuthenticatedPrincipal();
        User currentUser = findUserById(id);

        if (!canDelete(authenticatedUser, currentUser)) {
            throw new AccessDeniedException("No tenés permisos para dar de baja a este usuario");
        }

        currentUser.setEnabled(false);
        currentUser.setDeletedBy(authenticatedUser.getId());
        currentUser.setDeletedAt(LocalDateTime.now());
        setUpdaterUser(currentUser);
    }

    @Transactional
    public void restoreUser(Long id) {
        User authenticatedUser = getAuthenticatedPrincipal();
        User currentUser = userRepository.findByIdIncludingDeleted(id)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con ID: " + id));

        if (!canManage(authenticatedUser, currentUser)) {
            throw new AccessDeniedException("No tenés permisos para dar de alta a este usuario");
        }

        currentUser.setEnabled(true);
        currentUser.setDeletedAt(null);
        currentUser.setDeletedBy(null);
        setUpdaterUser(currentUser);
    }

    // ──────────── AUTHORIZATION ────────────

    private boolean canManage(User authenticatedUser, User currentUser) {
        return switch (authenticatedUser.getHierarchyRole()) {
            case SUPER_ADMIN -> currentUser.getHierarchyRole() != HierarchyRole.SUPER_ADMIN;
            case ADMIN -> currentUser.getHierarchyRole() == HierarchyRole.USER
                    && authenticatedUser.getId().equals(currentUser.getCreatedBy());
            default -> false;
        };
    }

    private boolean canDelete(User authenticatedUser, User currentUser) {
        if (authenticatedUser.getId().equals(currentUser.getId())) {
            return false;   // nadie se da de baja a sí mismo
        }
        if (currentUser.getHierarchyRole() == HierarchyRole.SUPER_ADMIN) {
            return false;   // los superadministradores no se dan de baja
        }
        return canManage(authenticatedUser, currentUser);
    }

    // ──────────── AUDIT METADATA ────────────

    private void setUpdaterUser(User user) {
        user.setUpdatedBy(getAuthenticatedUserId());
    }

    // ──────────── PRIVATE HELPERS ────────────

    // Busca sin aplicar reglas de permiso. Para uso interno del service
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con ID: " + userId));
    }

    private void validateDniNotInUse(String dni) {
        if (userRepository.existsByUsername(dni)) {
            throw new UserAlreadyExistsException("El usuario con DNI " + dni + " ya existe");
        }
    }

    private SubRole getSubRoleByName(String subrolName) {
        return subRoleRepository.findByName(subrolName)
                .orElseThrow(() -> new SubRoleNotFoundException("Subrol no encontrado: " + subrolName));
    }

    // ──────────── AUTHENTICATED USER ────────────

    private Long getAuthenticatedUserId() {
        return getAuthenticatedPrincipal().getId();
    }

    // Principal en memoria. Solo para leer campos simples: id, hierarchyRole, username
    private User getAuthenticatedPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof User user)) {
            throw new AccessDeniedException("No hay usuario autenticado");
        }
        return user;
    }

    // Entidad gestionada. Solo cuando hay que modificar al usuario autenticado
    private User getAuthenticatedUserManaged() {
        return userRepository.findById(getAuthenticatedUserId())
                .orElseThrow(() -> new AccessDeniedException("No hay usuario autenticado"));
    }

    // ──────────── MAPPERS ────────────

    private UserResponse buildUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .hierarchyRole(user.getHierarchyRole())
                .subroles(user.getSubroles().stream()
                        .map(SubRole::getName)
                        .collect(Collectors.toSet()))
                .enabled(user.getEnabled())
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