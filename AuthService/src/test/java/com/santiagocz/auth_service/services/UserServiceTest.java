package com.santiagocz.auth_service.services;

import com.santiagocz.auth_service.domain.entities.Person;
import com.santiagocz.auth_service.domain.entities.SubRole;
import com.santiagocz.auth_service.domain.entities.User;
import com.santiagocz.auth_service.domain.enums.HierarchyRole;
import com.santiagocz.auth_service.dto.request.PersonRequest;
import com.santiagocz.auth_service.dto.request.RegisterRequest;
import com.santiagocz.auth_service.dto.request.UpdatePasswordRequest;
import com.santiagocz.auth_service.exceptions.*;
import com.santiagocz.auth_service.repositories.PersonRepository;
import com.santiagocz.auth_service.repositories.SubRoleRepository;
import com.santiagocz.auth_service.repositories.UserRepository;
import com.santiagocz.common.delegation.Delegation;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PersonRepository personRepository;
    @Mock
    private SubRoleRepository subRoleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private UserService userService;

    private static final Long SUPER_ADMIN_ID = 100L;
    private static final Long OTHER_SUPER_ADMIN_ID = 110L;
    private static final Long ADMIN_ID = 200L;
    private static final Long OTHER_ADMIN_ID = 201L;
    private static final Long USER_ID = 300L;
    private static final Long ANOTHER_CREATOR_ID = 999L;

    private static final String DEFAULT_USERNAME = "12345678";
    private static final String DEFAULT_DNI = "12345678";
    private static final String DEFAULT_PASSWORD = "1234";
    private static final String DEFAULT_SUBROLE = "RRHH_ADMIN";
    private static final Delegation DEFAULT_DELEGATION = Delegation.ALEM;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ──────────── AUTHENTICATED USER ────────────

    @Nested
    @DisplayName("usuario autenticado")
    class AuthenticatedUser {

        @Test
        @DisplayName("Falla si no hay nadie autenticado")
        void shouldThrowAccessDenied_whenThereIsNoAuthentication() {
            // Given:
            // no se llama a authenticateAs, el contexto queda vacío

            // When / Then
            assertThatThrownBy(() -> userService.getUserById(USER_ID))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("No hay usuario autenticado");

            // Corta antes de tocar la base
            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("Falla si el principal no es una entidad User")
        void shouldThrowAccessDenied_whenPrincipalIsNotAUser() {
            // Given: el principal es un String, no un User
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(DEFAULT_DNI, null, List.of()));

            // When / Then
            assertThatThrownBy(() -> userService.getUserById(USER_ID))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("No hay usuario autenticado");

            verifyNoInteractions(userRepository);
        }
    }

    // ──────────── REGISTER USER ────────────

    @Nested
    @DisplayName("registerUser")
    class RegisterUser {

        // ---- Permisos sobre el rol solicitado ----

        @Test
        @DisplayName("El superadmin puede crear otro superadmin")
        void shouldAllowCreatingSuperAdmin_forSuperAdmin() {
            // Given
            authenticateAs(superAdmin());
            stubSuccessfulRegistration();

            // When
            userService.registerUser(registerRequestFor(HierarchyRole.SUPER_ADMIN, DEFAULT_DELEGATION));

            // Then
            assertThat(capturedSavedUser().getHierarchyRole()).isEqualTo(HierarchyRole.SUPER_ADMIN);
        }

        @Test
        @DisplayName("Un admin de primer nivel puede crear otro admin")
        void shouldAllowCreatingAdmin_forFirstLevelAdmin() {
            // Given
            authenticateAs(firstLevelAdmin());
            when(userRepository.findByIdIncludingDeleted(SUPER_ADMIN_ID))
                    .thenReturn(Optional.of(superAdmin()));
            stubSuccessfulRegistration();

            // When
            userService.registerUser(registerRequestFor(HierarchyRole.ADMIN, DEFAULT_DELEGATION));

            // Then
            assertThat(capturedSavedUser().getHierarchyRole()).isEqualTo(HierarchyRole.ADMIN);
        }

        @Test
        @DisplayName("Un admin de segundo nivel no puede crear otro admin")
        void shouldThrowAccessDenied_whenSecondLevelAdminCreatesAdmin() {
            // Given
            authenticateAs(secondLevelAdmin());
            when(userRepository.findByIdIncludingDeleted(ADMIN_ID))
                    .thenReturn(Optional.of(firstLevelAdmin()));

            // When / Then
            assertThatThrownBy(() -> userService.registerUser(registerRequestFor(HierarchyRole.ADMIN, DEFAULT_DELEGATION)))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(personRepository);
        }

        @Test
        @DisplayName("Un admin de segundo nivel puede crear un usuario común")
        void shouldAllowCreatingUser_forSecondLevelAdmin() {
            // Given
            authenticateAs(secondLevelAdmin());
            when(userRepository.findByIdIncludingDeleted(ADMIN_ID))
                    .thenReturn(Optional.of(firstLevelAdmin()));
            stubSuccessfulRegistration();

            // When
            userService.registerUser(registerRequestFor(HierarchyRole.USER, DEFAULT_DELEGATION));

            // Then
            assertThat(capturedSavedUser().getHierarchyRole()).isEqualTo(HierarchyRole.USER);
        }

        @Test
        @DisplayName("Un usuario común no puede crear a nadie")
        void shouldThrowAccessDenied_whenRegularUserRegisters() {
            // Given
            authenticateAs(userCreatedBy(ADMIN_ID));

            // When / Then
            assertThatThrownBy(() -> userService.registerUser(registerRequestFor(HierarchyRole.USER, DEFAULT_DELEGATION)))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(personRepository);
        }

        // ---- Delegación del usuario nuevo ----

        private static final Delegation OTHER_DELEGATION = Delegation.APOSTOLES;

        @Test
        @DisplayName("Un admin de primer nivel puede elegir la delegación")
        void shouldUseRequestedDelegation_forFirstLevelAdmin() {
            // Given
            authenticateAs(firstLevelAdmin());
            when(userRepository.findByIdIncludingDeleted(SUPER_ADMIN_ID))
                    .thenReturn(Optional.of(superAdmin()));
            stubSuccessfulRegistration();

            // When
            userService.registerUser(registerRequestFor(HierarchyRole.ADMIN, OTHER_DELEGATION));

            // Then
            User saved = capturedSavedUser();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(saved.getHierarchyRole()).isEqualTo(HierarchyRole.ADMIN);
                softly.assertThat(saved.getDelegation()).isEqualTo(OTHER_DELEGATION);
            });
        }

        @Test
        @DisplayName("Un admin de segundo nivel crea siempre en su propia delegación")
        void shouldForceOwnDelegation_forSecondLevelAdmin() {
            // Given
            authenticateAs(secondLevelAdmin());
            when(userRepository.findByIdIncludingDeleted(ADMIN_ID))
                    .thenReturn(Optional.of(firstLevelAdmin()));
            stubSuccessfulRegistration();

            // When
            // si ponemos otra delegación, debería guardar con la delegacion por defecto
            userService.registerUser(registerRequestFor(HierarchyRole.USER, OTHER_DELEGATION));

            // Then
            User saved = capturedSavedUser();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(saved.getHierarchyRole()).isEqualTo(HierarchyRole.USER);
                softly.assertThat(saved.getDelegation()).isEqualTo(DEFAULT_DELEGATION);
            });
        }

        @Test
        @DisplayName("Falla si no hay delegación y el usuario nuevo no es superadmin")
        void shouldThrowInvalidUserData_whenDelegationIsMissing() {
            // Given
            User creator = superAdmin();
            creator.setDelegation(null);
            authenticateAs(creator);

            // When / Then
            assertThatThrownBy(() -> userService.registerUser(registerRequestFor(HierarchyRole.ADMIN, null)))
                    .isInstanceOf(InvalidUserDataException.class);

            verifyNoInteractions(personRepository);
        }

        @Test
        @DisplayName("Un superadmin puede crearse sin delegación")
        void shouldAllowNullDelegation_whenCreatingSuperAdmin() {
            // Given
            User creator = superAdmin();
            creator.setDelegation(null);
            authenticateAs(creator);
            stubSuccessfulRegistration();

            // When
            userService.registerUser(registerRequestFor(HierarchyRole.SUPER_ADMIN, null));

            // Then
            User saved = capturedSavedUser();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(saved.getHierarchyRole()).isEqualTo(HierarchyRole.SUPER_ADMIN);
                softly.assertThat(saved.getDelegation()).isNull();
            });
        }

        // ---- Datos del usuario creado ----

        @Test
        @DisplayName("Falla si el DNI ya está en uso")
        void shouldThrowAlreadyExists_whenDniIsTaken() {
            // Given
            authenticateAs(superAdmin());
            when(userRepository.existsByUsername(DEFAULT_DNI)).thenReturn(true);

            // When / Then
            assertThatThrownBy(() -> userService.registerUser(registerRequestFor(HierarchyRole.ADMIN, DEFAULT_DELEGATION)))
                    .isInstanceOf(UserAlreadyExistsException.class);

            verifyNoInteractions(personRepository);
        }

        @Test
        @DisplayName("Guarda la contraseña hasheada, nunca en claro")
        void shouldSaveUserWithEncodedPassword() {
            // Given
            authenticateAs(superAdmin());
            stubSuccessfulRegistration();

            //When
            userService.registerUser(registerRequestFor(HierarchyRole.ADMIN,DEFAULT_DELEGATION));

            // Then
            User saved = capturedSavedUser();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(saved.getPassword()).isEqualTo("hash");
                softly.assertThat(saved.getPassword()).isNotEqualTo(DEFAULT_PASSWORD);
            });
        }

        @Test
        @DisplayName("El username es el DNI de la persona")
        void shouldUseDniAsUsername() {
            // Given
            authenticateAs(superAdmin());
            stubSuccessfulRegistration();

            //When
            userService.registerUser(registerRequestFor(HierarchyRole.ADMIN,DEFAULT_DELEGATION));

            // Then
            assertThat(capturedSavedUser().getUsername()).isEqualTo(DEFAULT_DNI);
        }

        @Test
        @DisplayName("Registra quién creó al usuario")
        void shouldSetCreatedBy_fromAuthenticatedUser() {
            // Given
            authenticateAs(superAdmin());
            stubSuccessfulRegistration();

            // When
            userService.registerUser(registerRequestFor(HierarchyRole.ADMIN, DEFAULT_DELEGATION));

            // Then
            assertThat(capturedSavedUser().getCreatedBy()).isEqualTo(SUPER_ADMIN_ID);
        }
    }

    // ──────────── GET USER BY ID ────────────

    @Nested
    @DisplayName("getUserById")
    class GetUserById {

        private static final Long OTHER_USER_ID = 310L;

        @Test
        @DisplayName("Falla si el usuario no existe")
        void shouldThrowNotFound_whenUserDoesNotExist() {
            // Given
            authenticateAs(firstLevelAdmin());
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> userService.getUserById(USER_ID))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("El superadmin puede ver a un admin")
        void shouldReturnUser_whenSuperAdminAccessesAdmin() {
            // Given
            authenticateAs(superAdmin());
            when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(firstLevelAdmin()));

            // When
            var result = userService.getUserById(ADMIN_ID);

            // Then
            assertThat(result.getId()).isEqualTo(ADMIN_ID);
        }

        @Test
        @DisplayName("Un superadmin puede ver al superadmin que él creó")
        void shouldReturnUser_whenSuperAdminAccessesOwnSuperAdmin() {
            // Given
            authenticateAs(superAdmin());
            when(userRepository.findById(OTHER_SUPER_ADMIN_ID))
                    .thenReturn(Optional.of(userOf(OTHER_SUPER_ADMIN_ID, HierarchyRole.SUPER_ADMIN, SUPER_ADMIN_ID)));

            // When
            var result = userService.getUserById(OTHER_SUPER_ADMIN_ID);

            // Then
            assertThat(result.getId()).isEqualTo(OTHER_SUPER_ADMIN_ID);
        }

        @Test
        @DisplayName("Un superadmin no puede ver a otro superadmin que no creó")
        void shouldThrowAccessDenied_whenSuperAdminAccessesForeignSuperAdmin() {
            // Given
            authenticateAs(superAdmin());
            when(userRepository.findById(OTHER_SUPER_ADMIN_ID))
                    .thenReturn(Optional.of(userOf(OTHER_SUPER_ADMIN_ID, HierarchyRole.SUPER_ADMIN, ANOTHER_CREATOR_ID)));

            // When / Then
            assertThatThrownBy(() -> userService.getUserById(OTHER_SUPER_ADMIN_ID))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("Un admin puede ver a otro admin creado por él")
        void shouldReturnUser_whenAdminAccessesOwnSecondLevelAdmin() {
            // Given
            authenticateAs(firstLevelAdmin());
            when(userRepository.findById(OTHER_ADMIN_ID))
                    .thenReturn(Optional.of(secondLevelAdmin()));

            // When
            var result = userService.getUserById(OTHER_ADMIN_ID);

            // Then
            assertThat(result.getId()).isEqualTo(OTHER_ADMIN_ID);
        }

        @Test
        @DisplayName("Un admin puede ver a un usuario que él creó")
        void shouldReturnUser_whenAdminAccessesOwnUser() {
            // Given
            authenticateAs(firstLevelAdmin());
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(userCreatedBy(ADMIN_ID)));

            // When
            var result = userService.getUserById(USER_ID);

            // Then
            assertThat(result.getId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("Un admin no puede ver a un usuario creado por otro admin")
        void shouldThrowAccessDenied_whenAdminAccessesForeignUser() {
            // Given
            authenticateAs(firstLevelAdmin());
            when(userRepository.findById(USER_ID))
                    .thenReturn(Optional.of(userCreatedBy(OTHER_ADMIN_ID)));
            // El creador es un admin de OTRA rama (lo creó el superadmin, no nuestro admin)
            when(userRepository.findByIdIncludingDeleted(OTHER_ADMIN_ID))
                    .thenReturn(Optional.of(userOf(OTHER_ADMIN_ID, HierarchyRole.ADMIN, SUPER_ADMIN_ID)));

            // When / Then
            assertThatThrownBy(() -> userService.getUserById(USER_ID))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("Un usuario común no puede ver a nadie")
        void shouldThrowAccessDenied_whenRegularUserAccessesAnyone() {
            // Given
            authenticateAs(userCreatedBy(ADMIN_ID));
            when(userRepository.findById(OTHER_USER_ID))
                    .thenReturn(Optional.of(userOf(OTHER_USER_ID, HierarchyRole.USER, ADMIN_ID)));

            // When / Then
            assertThatThrownBy(() -> userService.getUserById(OTHER_USER_ID))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("Un admin puede ver a un usuario creado por su admin secundario")
        void shouldReturnUser_whenAdminAccessesGrandchildUser() {
            // Given
            authenticateAs(firstLevelAdmin());
            // Usuario creado por el admin secundario
            when(userRepository.findById(USER_ID))
                    .thenReturn(Optional.of(userCreatedBy(OTHER_ADMIN_ID)));
            // Ese admin secundario fue creado por el admin autenticado -> el user es nieto
            when(userRepository.findByIdIncludingDeleted(OTHER_ADMIN_ID))
                    .thenReturn(Optional.of(secondLevelAdmin()));

            // When
            var result = userService.getUserById(USER_ID);

            // Then
            assertThat(result.getId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("Un admin no puede ver a un admin de otra rama")
        void shouldThrowAccessDenied_whenAdminAccessesForeignAdmin() {
            // Given
            authenticateAs(firstLevelAdmin());
            when(userRepository.findById(OTHER_ADMIN_ID))
                    .thenReturn(Optional.of(userOf(OTHER_ADMIN_ID, HierarchyRole.ADMIN, SUPER_ADMIN_ID)));
            when(userRepository.findByIdIncludingDeleted(SUPER_ADMIN_ID))
                    .thenReturn(Optional.of(superAdmin()));

            // When / Then
            assertThatThrownBy(() -> userService.getUserById(OTHER_ADMIN_ID))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    // ──────────── LIST USERS ────────────

    @Nested
    @DisplayName("listUsers")
    class ListUsers {

        private final Pageable pageable = PageRequest.of(0, 10);

        @Test
        @DisplayName("El superadmin lista a todos menos a otros superadmin")
        void shouldExcludeSuperAdmins_forSuperAdmin() {
            // Given
            authenticateAs(superAdmin());
            when(userRepository.findByHierarchyRoleNot(any(), any())).thenReturn(Page.empty());

            // When
            userService.listUsers(pageable);

            // Then
            verify(userRepository).findByHierarchyRoleNot(eq(HierarchyRole.SUPER_ADMIN), eq(pageable));
            verify(userRepository, never()).findSubtreeOf(any(), any());
        }

        @Test
        @DisplayName("El admin lista a todo su subárbol")
        void shouldQuerySubtree_forAdmin() {
            // Given
            authenticateAs(firstLevelAdmin());
            when(userRepository.findSubtreeOf(any(), any())).thenReturn(Page.empty());

            // When
            userService.listUsers(pageable);

            // Then
            verify(userRepository).findSubtreeOf(eq(ADMIN_ID), eq(pageable));
            verify(userRepository, never()).findByHierarchyRoleNot(any(), any());
        }

        @Test
        @DisplayName("El usuario común recibe una página vacía sin consultar la base")
        void shouldReturnEmptyPage_forRegularUser() {
            // Given
            authenticateAs(userCreatedBy(ADMIN_ID));

            // When
            var result = userService.listUsers(pageable);

            // Then
            assertThat(result.getContent()).isEmpty();
            verifyNoInteractions(userRepository);
        }
    }

// ──────────── GET BY USERNAME ────────────

    @Nested
    @DisplayName("getUserByUsername")
    class GetUserByUsername {

        @Test
        @DisplayName("Falla si el username no existe")
        void shouldThrowNotFound_whenUsernameDoesNotExist() {
            // Given
            when(userRepository.findByUsername(DEFAULT_USERNAME)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> userService.getUserByUsername(DEFAULT_USERNAME))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("Devuelve el usuario encontrado")
        void shouldReturnUser_whenUsernameExists() {
            // Given
            when(userRepository.findByUsername(DEFAULT_USERNAME))
                    .thenReturn(Optional.of(firstLevelAdmin()));

            // When
            var result = userService.getUserByUsername(DEFAULT_USERNAME);

            // Then
            assertThat(result.getUsername()).isEqualTo(DEFAULT_USERNAME);
        }
    }

// ──────────── GET PERSON BY USERNAME ────────────

    @Nested
    @DisplayName("getPersonByUsername")
    class GetPersonByUsername {

        @Test
        @DisplayName("Falla si el username no existe")
        void shouldThrowNotFound_whenUsernameDoesNotExist() {
            // Given
            when(userRepository.findByUsername(DEFAULT_USERNAME)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> userService.getPersonByUsername(DEFAULT_USERNAME))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("Devuelve los datos de la persona asociada")
        void shouldReturnPersonOfUser_whenUsernameExists() {
            // Given
            when(userRepository.findByUsername(DEFAULT_USERNAME))
                    .thenReturn(Optional.of(firstLevelAdmin()));

            // When
            var result = userService.getPersonByUsername(DEFAULT_USERNAME);

            // Then
            assertThat(result.getDni()).isEqualTo(DEFAULT_DNI);
        }
    }

    // ──────────── UPDATE MY PASSWORD ────────────

    @Nested
    @DisplayName("updateMyPassword")
    class UpdateMyPassword {

        @Test
        @DisplayName("Falla si la contraseña actual no coincide")
        void shouldThrowAccessDenied_whenCurrentPasswordIsWrong() {
            // Given
            authenticateAs(firstLevelAdmin());
            when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(firstLevelAdmin()));
            when(passwordEncoder.matches("oldPassword", "savedHash")).thenReturn(false);

            // When / Then
            assertThatThrownBy(() -> userService.updateMyPassword(updatePasswordRequest("oldPassword", "newPassword")))
                    .isInstanceOf(AccessDeniedException.class);

            verify(passwordEncoder, never()).encode(any());
        }

        @Test
        @DisplayName("Falla si la nueva contraseña es igual a la actual")
        void shouldThrowInvalidPassword_whenNewPasswordEqualsCurrent() {
            // Given
            authenticateAs(firstLevelAdmin());
            when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(firstLevelAdmin()));
            when(passwordEncoder.matches("oldPassword", "savedHash")).thenReturn(true);
            when(passwordEncoder.matches("newPassword", "savedHash")).thenReturn(true);

            // When / Then
            assertThatThrownBy(() -> userService.updateMyPassword(updatePasswordRequest("oldPassword", "newPassword")))
                    .isInstanceOf(InvalidPasswordException.class);

            verify(passwordEncoder, never()).encode(any());
        }

        @Test
        @DisplayName("Guarda la nueva contraseña hasheada")
        void shouldApplyNewPassword_whenCurrentMatchesAndNewIsDifferent() {
            // Given
            authenticateAs(firstLevelAdmin());
            User user = firstLevelAdmin();
            when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("oldPassword", "savedHash")).thenReturn(true);
            when(passwordEncoder.matches("newPassword", "savedHash")).thenReturn(false);
            when(passwordEncoder.encode("newPassword")).thenReturn("newHash");

            // When
            userService.updateMyPassword(updatePasswordRequest("oldPassword", "newPassword"));

            // Then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(user.getPassword()).isEqualTo("newHash");
                softly.assertThat(user.getUpdatedBy()).isEqualTo(ADMIN_ID);
            });
        }
    }

// ──────────── RESET USER PASSWORD ────────────

    @Nested
    @DisplayName("resetUserPassword")
    class ResetUserPassword {

        @Test
        @DisplayName("Un superadmin puede resetear la contraseña de un usuario que no creó")
        void shouldResetPassword_whenSuperAdminNotManagesUser() {
            // Given
            authenticateAs(superAdmin());
            User user = userCreatedBy(ADMIN_ID);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(passwordEncoder.encode("newPassword")).thenReturn("newHash");

            // When
            userService.resetUserPassword(USER_ID, "newPassword");

            // Then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(user.getPassword()).isEqualTo("newHash");
                softly.assertThat(user.getUpdatedBy()).isEqualTo(SUPER_ADMIN_ID);
            });
        }

        @Test
        @DisplayName("Un admin puede resetear la contraseña de un usuario que creó")
        void shouldResetPassword_whenAdminManagesUser() {
            // Given
            authenticateAs(firstLevelAdmin());
            User user = userCreatedBy(ADMIN_ID);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(passwordEncoder.encode("newPassword")).thenReturn("newHash");

            // When
            userService.resetUserPassword(USER_ID, "newPassword");

            // Then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(user.getPassword()).isEqualTo("newHash");
                softly.assertThat(user.getUpdatedBy()).isEqualTo(ADMIN_ID);
            });
        }

        @Test
        @DisplayName("Un admin no puede resetear la contraseña de un usuario ajeno")
        void shouldThrowAccessDenied_whenAdminDoesNotManageUser() {
            // Given
            authenticateAs(firstLevelAdmin());
            when(userRepository.findById(USER_ID))
                    .thenReturn(Optional.of(userCreatedBy(OTHER_ADMIN_ID)));
            // El creador es un admin de otra rama
            when(userRepository.findByIdIncludingDeleted(OTHER_ADMIN_ID))
                    .thenReturn(Optional.of(userOf(OTHER_ADMIN_ID, HierarchyRole.ADMIN, SUPER_ADMIN_ID)));

            // When / Then
            assertThatThrownBy(() -> userService.resetUserPassword(USER_ID, "newPassword"))
                    .isInstanceOf(AccessDeniedException.class);

            verify(passwordEncoder, never()).encode(any());
        }
    }

    // ──────────── SUBROLES ────────────

    @Nested
    @DisplayName("addSubrolToUser")
    class AddSubrolToUser {

        @Test
        @DisplayName("Falla si no tiene permisos sobre el usuario")
        void shouldThrowAccessDenied_whenCallerCannotManageUser() {
            // Given
            authenticateAs(firstLevelAdmin());
            when(userRepository.findById(USER_ID))
                    .thenReturn(Optional.of(userCreatedBy(OTHER_ADMIN_ID)));
            // El creador es un admin de otra rama
            when(userRepository.findByIdIncludingDeleted(OTHER_ADMIN_ID))
                    .thenReturn(Optional.of(userOf(OTHER_ADMIN_ID, HierarchyRole.ADMIN, SUPER_ADMIN_ID)));

            // When / Then
            assertThatThrownBy(() -> userService.addSubrolToUser(USER_ID, DEFAULT_SUBROLE))
                    .isInstanceOf(AccessDeniedException.class);

            // Corta antes de buscar el subrol
            verifyNoInteractions(subRoleRepository);
        }

        @Test
        @DisplayName("Falla si el subrol no existe")
        void shouldThrowSubRoleNotFound_whenSubroleDoesNotExist() {
            // Given
            authenticateAs(firstLevelAdmin());
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(userCreatedBy(ADMIN_ID)));
            when(subRoleRepository.findByName(DEFAULT_SUBROLE)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> userService.addSubrolToUser(USER_ID, DEFAULT_SUBROLE))
                    .isInstanceOf(SubRoleNotFoundException.class);
        }

        @Test
        @DisplayName("Agrega el subrol al usuario")
        void shouldAddSubrole_whenCallerManagesUser() {
            // Given
            SubRole subRole = defaultSubrole();
            User assignerAdmin = firstLevelAdmin();
            assignerAdmin.getSubroles().add(subRole);
            authenticateAs(assignerAdmin);

            User user = userCreatedBy(ADMIN_ID);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(subRoleRepository.findByName(DEFAULT_SUBROLE)).thenReturn(Optional.of(subRole));

            // When
            userService.addSubrolToUser(USER_ID, DEFAULT_SUBROLE);

            // Then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(user.getSubroles()).extracting(SubRole::getName).contains(DEFAULT_SUBROLE);
                softly.assertThat(user.getUpdatedBy()).isEqualTo(ADMIN_ID);
            });
        }

        // ---- Delegación de subroles ----

        @Test
        @DisplayName("Un admin no puede asignar un subrol distinto al que tiene")
        void shouldThrowAccessDenied_whenAdminOwnsADifferentSubrole() {
            // Given
            User assignerAdmin = firstLevelAdmin();
            assignerAdmin.getSubroles().add(defaultSubrole());
            authenticateAs(assignerAdmin);

            User user = userCreatedBy(ADMIN_ID);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            // pero intenta asignar otro distinto
            when(subRoleRepository.findByName("OTHER_SUBROLE"))
                    .thenReturn(Optional.of(subRoleOf("OTHER_SUBROLE")));

            // When / Then
            assertThatThrownBy(() -> userService.addSubrolToUser(USER_ID, "OTHER_SUBROLE"))
                    .isInstanceOf(AccessDeniedException.class);

            assertThat(user.getSubroles()).isEmpty();
        }

        @Test
        @DisplayName("El superadmin puede asignar cualquier subrol")
        void shouldAllowAnySubrole_forSuperAdmin() {
            // Given
            SubRole subRole = defaultSubrole();
            authenticateAs(superAdmin());

            User user = userCreatedBy(ADMIN_ID);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(subRoleRepository.findByName(DEFAULT_SUBROLE)).thenReturn(Optional.of(subRole));

            // When
            userService.addSubrolToUser(USER_ID, DEFAULT_SUBROLE);

            // Then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(user.getSubroles()).extracting(SubRole::getName).contains(DEFAULT_SUBROLE);
                softly.assertThat(user.getUpdatedBy()).isEqualTo(SUPER_ADMIN_ID);
            });
        }
    }

    @Nested
    @DisplayName("removeSubrolFromUser")
    class RemoveSubrolFromUser {

        @Test
        @DisplayName("Falla si no tiene permisos sobre el usuario")
        void shouldThrowAccessDenied_whenCallerCannotManageUser() {
            // Given
            authenticateAs(firstLevelAdmin());
            when(userRepository.findById(USER_ID))
                    .thenReturn(Optional.of(userCreatedBy(OTHER_ADMIN_ID)));
            // El creador es un admin de otra rama
            when(userRepository.findByIdIncludingDeleted(OTHER_ADMIN_ID))
                    .thenReturn(Optional.of(userOf(OTHER_ADMIN_ID, HierarchyRole.ADMIN, SUPER_ADMIN_ID)));

            // When / Then
            assertThatThrownBy(() -> userService.removeSubrolFromUser(USER_ID, DEFAULT_SUBROLE))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(subRoleRepository);
        }

        @Test
        @DisplayName("Quita el subrol del usuario")
        void shouldRemoveSubrole_whenCallerManagesUser() {
            // Given
            authenticateAs(firstLevelAdmin());

            User user = userCreatedBy(ADMIN_ID);
            SubRole subRole = defaultSubrole();
            user.getSubroles().add(subRole);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(subRoleRepository.findByName(DEFAULT_SUBROLE)).thenReturn(Optional.of(subRole));

            // When
            userService.removeSubrolFromUser(USER_ID, DEFAULT_SUBROLE);

            // Then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(user.getSubroles()).extracting(SubRole::getName).doesNotContain((DEFAULT_SUBROLE));
                softly.assertThat(user.getUpdatedBy()).isEqualTo(ADMIN_ID);
            });
        }
    }

    // ──────────── DELETE — RESTORE ────────────

    @Nested
    @DisplayName("deleteUser")
    class DeleteUser {

        @Test
        @DisplayName("Nadie puede darse de baja a sí mismo")
        void shouldThrowAccessDenied_whenUserDeletesThemselves() {
            // Given
            authenticateAs(firstLevelAdmin());
            when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(firstLevelAdmin()));

            // When / Then
            assertThatThrownBy(() -> userService.deleteUser(ADMIN_ID))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("Un superadmin puede dar de baja al superadmin que él creó")
        void shouldDeleteSuperAdmin_whenCreatedByTheCaller() {
            // Given
            authenticateAs(superAdmin());
            User target = userOf(OTHER_SUPER_ADMIN_ID, HierarchyRole.SUPER_ADMIN, SUPER_ADMIN_ID);
            when(userRepository.findById(OTHER_SUPER_ADMIN_ID)).thenReturn(Optional.of(target));

            // When
            userService.deleteUser(OTHER_SUPER_ADMIN_ID);

            // Then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(target.getEnabled()).isFalse();
                softly.assertThat(target.getDeletedBy()).isEqualTo(SUPER_ADMIN_ID);
                softly.assertThat(target.getDeletedAt()).isNotNull();
            });
        }

        @Test
        @DisplayName("Un superadmin no puede dar de baja a un superadmin que no creó")
        void shouldThrowAccessDenied_whenSuperAdminWasCreatedByAnother() {
            // Given
            authenticateAs(superAdmin());
            // Este superadmin lo creó un tercero, no el autenticado
            when(userRepository.findById(OTHER_SUPER_ADMIN_ID))
                    .thenReturn(Optional.of(userOf(OTHER_SUPER_ADMIN_ID, HierarchyRole.SUPER_ADMIN, ANOTHER_CREATOR_ID)));

            // When / Then
            assertThatThrownBy(() -> userService.deleteUser(OTHER_SUPER_ADMIN_ID))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("Un admin no puede dar de baja a un usuario ajeno")
        void shouldThrowAccessDenied_whenCallerCannotManageUser() {
            // Given
            authenticateAs(firstLevelAdmin());
            when(userRepository.findById(USER_ID))
                    .thenReturn(Optional.of(userCreatedBy(OTHER_ADMIN_ID)));
            // El creador es un admin de otra rama
            when(userRepository.findByIdIncludingDeleted(OTHER_ADMIN_ID))
                    .thenReturn(Optional.of(userOf(OTHER_ADMIN_ID, HierarchyRole.ADMIN, SUPER_ADMIN_ID)));

            // When / Then
            assertThatThrownBy(() -> userService.deleteUser(USER_ID))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("Da de baja al usuario y registra quién y cuándo")
        void shouldDisableUserAndRecordDeletionMetadata() {
            // Given
            authenticateAs(firstLevelAdmin());
            User user = userCreatedBy(ADMIN_ID);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            // When
            userService.deleteUser(USER_ID);

            // Then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(user.getEnabled()).isFalse();
                softly.assertThat(user.getDeletedAt()).isNotNull();
                softly.assertThat(user.getDeletedBy()).isEqualTo(ADMIN_ID);
                softly.assertThat(user.getUpdatedBy()).isEqualTo(ADMIN_ID);
            });
        }
    }

    @Nested
    @DisplayName("restoreUser")
    class RestoreUser {

        @Test
        @DisplayName("Falla si el usuario no existe ni entre los eliminados")
        void shouldThrowNotFound_whenUserDoesNotExist() {
            // Given
            authenticateAs(firstLevelAdmin());
            when(userRepository.findByIdIncludingDeleted(USER_ID)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> userService.restoreUser(USER_ID))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("Falla si no tiene permisos sobre el usuario")
        void shouldThrowAccessDenied_whenCallerCannotManageUser() {
            // Given
            authenticateAs(firstLevelAdmin());
            when(userRepository.findByIdIncludingDeleted(USER_ID))
                    .thenReturn(Optional.of(userCreatedBy(OTHER_ADMIN_ID)));
            // El creador es un admin de otra rama
            when(userRepository.findByIdIncludingDeleted(OTHER_ADMIN_ID))
                    .thenReturn(Optional.of(userOf(OTHER_ADMIN_ID, HierarchyRole.ADMIN, SUPER_ADMIN_ID)));

            // When / Then
            assertThatThrownBy(() -> userService.restoreUser(USER_ID))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("Falla si el usuario ya está activo")
        void shouldThrowConflict_whenUserIsAlreadyEnabled() {
            // Given
            authenticateAs(firstLevelAdmin());
            User user = userCreatedBy(ADMIN_ID);
            when(userRepository.findByIdIncludingDeleted(USER_ID)).thenReturn(Optional.of(user));

            // When / Then
            assertThatThrownBy(() -> userService.restoreUser(USER_ID))
                    .isInstanceOf(UserConflictException.class);

            assertThat(user.getUpdatedBy()).isNull();
        }

        @Test
        @DisplayName("Reactiva al usuario y limpia los datos de baja")
        void shouldEnableUserAndClearDeletionMetadata() {
            // Given
            authenticateAs(firstLevelAdmin());

            User user = userCreatedBy(ADMIN_ID);
            user.setEnabled(false);
            user.setDeletedAt(LocalDateTime.now().minusDays(1));
            user.setDeletedBy(ADMIN_ID);

            when(userRepository.findByIdIncludingDeleted(USER_ID)).thenReturn(Optional.of(user));

            // When
            userService.restoreUser(USER_ID);

            // Then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(user.getEnabled()).isTrue();
                softly.assertThat(user.getDeletedAt()).isNull();
                softly.assertThat(user.getDeletedBy()).isNull();
                softly.assertThat(user.getUpdatedBy()).isEqualTo(ADMIN_ID);
            });
        }
    }

    // ──────────── HELPERS ────────────

    // ---- User ----

    private void authenticateAs(User user) {
        var authentication = new UsernamePasswordAuthenticationToken(user, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    // ---- Entities ----

    private User userOf(Long id, HierarchyRole role, Long createdBy) {
        return User.builder()
                .id(id)
                .username(DEFAULT_DNI)
                .password("savedHash")
                .hierarchyRole(role)
                .createdBy(createdBy)
                .person(defaultPerson())
                .delegation(DEFAULT_DELEGATION)
                .subroles(new HashSet<>())
                .enabled(true)
                .build();
    }

    private User superAdmin() {
        return userOf(SUPER_ADMIN_ID, HierarchyRole.SUPER_ADMIN, null);
    }

    private User firstLevelAdmin() {
        return userOf(ADMIN_ID, HierarchyRole.ADMIN, SUPER_ADMIN_ID);
    }

    private User secondLevelAdmin() {
        return userOf(OTHER_ADMIN_ID, HierarchyRole.ADMIN, ADMIN_ID);
    }

    private User userCreatedBy(Long creatorId) {
        return userOf(USER_ID, HierarchyRole.USER, creatorId);
    }

    private Person defaultPerson() {
        return Person.builder()
                .id(1L)
                .dni(DEFAULT_DNI)
                .firstName("Juan")
                .lastName("Pérez")
                .phoneNumber("3764000000")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();
    }

    private SubRole subRoleOf(String name) {
        return SubRole.builder()
                .id(1L)
                .name(name)
                .description(name)
                .build();
    }

    private SubRole defaultSubrole() {
        return subRoleOf(DEFAULT_SUBROLE);
    }

    // ---- Capture ----

    private User capturedSavedUser() {
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        return captor.getValue();
    }

    // ---- Stub Success registration ----

    private void stubSuccessfulRegistration() {
        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(personRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---- Default Dtos----

    private RegisterRequest registerRequestFor(HierarchyRole role, Delegation delegation) {
        return RegisterRequest.builder()
                .person(defaultPersonRequest())
                .password(DEFAULT_PASSWORD)
                .hierarchyRole(role)
                .delegation(delegation)
                .build();
    }

    private PersonRequest defaultPersonRequest() {
        return PersonRequest.builder()
                .dni(DEFAULT_DNI)
                .firstName("Juan")
                .lastName("Pérez")
                .phoneNumber("3764000000")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();
    }

    private UpdatePasswordRequest updatePasswordRequest(String currentPassword, String newPassword) {
        UpdatePasswordRequest dto = new UpdatePasswordRequest();
        dto.setCurrentPassword(currentPassword);
        dto.setNewPassword(newPassword);
        return dto;
    }

}