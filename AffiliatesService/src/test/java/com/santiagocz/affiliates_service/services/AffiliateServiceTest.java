package com.santiagocz.affiliates_service.services;

import com.santiagocz.affiliates_service.component.AffiliateMapper;
import com.santiagocz.affiliates_service.domain.entities.Affiliate;
import com.santiagocz.affiliates_service.domain.enums.AffiliateType;
import com.santiagocz.affiliates_service.domain.enums.RelationType;
import com.santiagocz.affiliates_service.domain.enums.Status;
import com.santiagocz.affiliates_service.dto.affiliates.AffiliateRequestDto;
import com.santiagocz.affiliates_service.dto.affiliates.AffiliateResponseDto;
import com.santiagocz.affiliates_service.dto.affiliates.AffiliateSummaryDto;
import com.santiagocz.affiliates_service.repositories.AffiliateRepository;
import com.santiagocz.common.exceptions.EntityConflictException;
import com.santiagocz.common.exceptions.EntityNotFoundException;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AffiliateServiceTest {

    @Mock
    private AffiliateRepository affiliateRepository;
    @Spy
    private AffiliateMapper mapper = new AffiliateMapper();
    @InjectMocks
    private AffiliateService affiliateService;

    private static final Long PRIMARY_ID = 100L;
    private static final Long DEPENDENT_ID = 200L;
    private static final Long OTHER_PRIMARY_ID = 110L;

    private static final String DEFAULT_DNI = "12345678";
    private static final String OTHER_DNI = "87654321";
    private static final String DEFAULT_PHONE = "3764000000";

    // ──────────── CREATE PRIMARY ────────────

    @Nested
    @DisplayName("createPrimary")
    class CreatePrimary {

        @Test
        @DisplayName("Falla si el DNI ya está en uso")
        void shouldThrowConflict_whenDniIsTaken() {
            // Given
            when(affiliateRepository.existsByDni(DEFAULT_DNI))
                    .thenReturn(true);

            // When / Then
            assertThatThrownBy(() -> affiliateService.createPrimary(requestDto()))
                    .isInstanceOf(EntityConflictException.class);

            verify(affiliateRepository, never()).save(any());
        }

        @Test
        @DisplayName("Falla si el titular es menor de edad")
        void shouldThrowConflict_whenPrimaryIsUnderage() {
            // Given
            AffiliateRequestDto dto = requestDto();
            dto.setBirthDate(LocalDate.now().minusYears(17));

            // When / Then
            assertThatThrownBy(() -> affiliateService.createPrimary(dto))
                    .isInstanceOf(EntityConflictException.class);

            verify(affiliateRepository, never()).save(any());
        }

        @Test
        @DisplayName("Un titular de exactamente 18 años se puede registrar")
        void shouldAllowRegistration_atTheAgeBoundary() {
            // Given
            stubSuccessfulCreation();
            AffiliateRequestDto dto = requestDto();
            dto.setBirthDate(LocalDate.now().minusYears(18));

            // When / Then
            assertThatCode(() -> affiliateService.createPrimary(dto))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Guarda el titular activo y con tipo PRIMARY")
        void shouldSaveAsActivePrimary() {
            // Given
            stubSuccessfulCreation();
            AffiliateRequestDto dto = requestDto();

            // When
            affiliateService.createPrimary(dto);

            // Then
            Affiliate saved = capturedSavedAffiliate();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(saved.getStatus()).isEqualTo(Status.ACTIVE);
                softly.assertThat(saved.getAffiliateType()).isEqualTo(AffiliateType.PRIMARY);
                softly.assertThat(saved.getRelation()).isNull();
                softly.assertThat(saved.getPrimaryAffiliate()).isNull();
            });
        }

        @Test
        @DisplayName("Normaliza el formato de nombre y apellido")
        void shouldFormatNamesBeforeSaving() {
            // Given
            stubSuccessfulCreation();
            AffiliateRequestDto dto = requestDto();
            dto.setFirstName("  juan   CARLOS ");
            dto.setLastName("pérez");

            // When
            affiliateService.createPrimary(dto);

            // Then
            Affiliate saved = capturedSavedAffiliate();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(saved.getFirstName()).isEqualTo("Juan Carlos");
                softly.assertThat(saved.getLastName()).isEqualTo("Pérez");
            });
        }
    }

    // ──────────── CREATE DEPENDENT ────────────

    @Nested
    @DisplayName("createDependent")
    class CreateDependent {

        private static final String OTHER_PHONE = "3764123456";

        @Test
        @DisplayName("Falla si no se indica la relación con el titular")
        void shouldThrowConflict_whenRelationIsMissing() {
            // Given
            AffiliateRequestDto dto = requestDto();
            dto.setRelation(null);

            // When / Then
            assertThatThrownBy(() -> affiliateService.createDependent(PRIMARY_ID, dto))
                    .isInstanceOf(EntityConflictException.class);

            verifyNoInteractions(affiliateRepository);
        }

        @Test
        @DisplayName("Falla si el titular no existe")
        void shouldThrowNotFound_whenPrimaryDoesNotExist() {
            // Given
            when(affiliateRepository.findById(PRIMARY_ID)).thenReturn(Optional.empty());

            AffiliateRequestDto dto = dependentRequestDto();

            // When / Then
            assertThatThrownBy(() -> affiliateService.createDependent(PRIMARY_ID, dto))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("Falla si el afiliado indicado no es titular")
        void shouldThrowConflict_whenPrimaryIsActuallyADependent() {
            // Given
            when(affiliateRepository.findById(DEPENDENT_ID))
                    .thenReturn(Optional.of(dependentOf(DEPENDENT_ID, Status.ACTIVE, activePrimary())));

            AffiliateRequestDto dto = dependentRequestDto();

            // When / Then
            assertThatThrownBy(() -> affiliateService.createDependent(DEPENDENT_ID, dto))
                    .isInstanceOf(EntityConflictException.class);

            verify(affiliateRepository, never()).save(any());
        }

        @Test
        @DisplayName("Falla si el titular está inactivo")
        void shouldThrowConflict_whenPrimaryIsInactive() {
            // Given
            when(affiliateRepository.findById(PRIMARY_ID))
                    .thenReturn(Optional.of(primaryOf(PRIMARY_ID, Status.INACTIVE)));

            AffiliateRequestDto dto = dependentRequestDto();

            // When / Then
            assertThatThrownBy(() -> affiliateService.createDependent(PRIMARY_ID, dto))
                    .isInstanceOf(EntityConflictException.class);

            verify(affiliateRepository, never()).save(any());
        }

        @Test
        @DisplayName("Falla si el DNI del familiar ya está en uso")
        void shouldThrowConflict_whenDependentDniIsTaken() {
            // Given
            when(affiliateRepository.findById(PRIMARY_ID))
                    .thenReturn(Optional.of(activePrimary()));
            when(affiliateRepository.existsByDni(OTHER_DNI)).thenReturn(true);

            AffiliateRequestDto dto = dependentRequestDto();

            // When / Then
            assertThatThrownBy(() -> affiliateService.createDependent(PRIMARY_ID, dto))
                    .isInstanceOf(EntityConflictException.class);

            verify(affiliateRepository, never()).save(any());
        }

        @Test
        @DisplayName("Hereda el teléfono del titular si el familiar no tiene uno")
        void shouldInheritPhoneFromPrimary_whenPhoneIsNull() {
            // Given
            when(affiliateRepository.findById(PRIMARY_ID)).thenReturn(Optional.of(activePrimary()));
            stubSuccessfulCreation();

            AffiliateRequestDto dto = dependentRequestDto();
            dto.setPhoneNumber(null);

            // When
            affiliateService.createDependent(PRIMARY_ID, dto);

            // Then
            assertThat(capturedSavedAffiliate().getPhoneNumber()).isEqualTo(DEFAULT_PHONE);
        }

        @Test
        @DisplayName("Hereda el teléfono del titular si el del familiar viene en blanco")
        void shouldInheritPhoneFromPrimary_whenPhoneIsBlank() {
            // Given
            when(affiliateRepository.findById(PRIMARY_ID)).thenReturn(Optional.of(activePrimary()));
            stubSuccessfulCreation();

            AffiliateRequestDto dto = dependentRequestDto();
            dto.setPhoneNumber("");

            // When
            affiliateService.createDependent(PRIMARY_ID, dto);

            // Then
            assertThat(capturedSavedAffiliate().getPhoneNumber()).isEqualTo(DEFAULT_PHONE);
        }

        @Test
        @DisplayName("Conserva el teléfono propio del familiar si lo trae")
        void shouldKeepOwnPhone_whenProvided() {
            // Given
            when(affiliateRepository.findById(PRIMARY_ID)).thenReturn(Optional.of(activePrimary()));
            stubSuccessfulCreation();

            AffiliateRequestDto dto = dependentRequestDto();
            dto.setPhoneNumber(OTHER_PHONE);

            // When
            affiliateService.createDependent(PRIMARY_ID, dto);

            // Then
            assertThat(capturedSavedAffiliate().getPhoneNumber()).isEqualTo(OTHER_PHONE);
        }

        @Test
        @DisplayName("Un familiar menor de edad se puede registrar")
        void shouldAllowUnderageDependent() {
            // Given
            when(affiliateRepository.findById(PRIMARY_ID)).thenReturn(Optional.of(activePrimary()));
            stubSuccessfulCreation();

            AffiliateRequestDto dto = dependentRequestDto();
            dto.setBirthDate(LocalDate.now().minusYears(3));

            // When / Then
            assertThatCode(() -> affiliateService.createDependent(PRIMARY_ID, dto))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Guarda el familiar activo, con su relación y su titular")
        void shouldSaveAsActiveDependent() {
            // Given
            when(affiliateRepository.findById(PRIMARY_ID)).thenReturn(Optional.of(activePrimary()));
            stubSuccessfulCreation();

            AffiliateRequestDto dto = dependentRequestDto();

            // When
            affiliateService.createDependent(PRIMARY_ID, dto);

            // Then
            Affiliate saved = capturedSavedAffiliate();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(saved.getStatus()).isEqualTo(Status.ACTIVE);
                softly.assertThat(saved.getAffiliateType()).isEqualTo(AffiliateType.DEPENDENT);
                softly.assertThat(saved.getRelation()).isEqualTo(RelationType.CHILD);
                softly.assertThat(saved.getPrimaryAffiliate().getId()).isEqualTo(PRIMARY_ID);
            });
        }
    }

    // ──────────── READ ────────────

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("Falla si el afiliado no existe")
        void shouldThrowNotFound_whenAffiliateDoesNotExist() {
            // Given
            when(affiliateRepository.findById(PRIMARY_ID))
                    .thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> affiliateService.getById(PRIMARY_ID))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("Devuelve el afiliado encontrado")
        void shouldReturnAffiliate_whenItExists() {
            // Given
            when(affiliateRepository.findById(PRIMARY_ID))
                    .thenReturn(Optional.of(activePrimary()));

            // When
            var result = affiliateService.getById(PRIMARY_ID);

            // Then
            assertThat(result.getId()).isEqualTo(PRIMARY_ID);
        }
    }

    @Nested
    @DisplayName("getByDni")
    class GetByDni {

        @Test
        @DisplayName("Falla si no hay afiliado con ese DNI")
        void shouldThrowNotFound_whenDniDoesNotExist() {
            // Given
            when(affiliateRepository.findByDni(DEFAULT_DNI))
                    .thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> affiliateService.getByDni(DEFAULT_DNI))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("Devuelve el afiliado con ese DNI")
        void shouldReturnAffiliate_whenDniExists() {
            // Given
            when(affiliateRepository.findByDni(DEFAULT_DNI))
                    .thenReturn(Optional.of(activePrimary()));

            // When
            var result = affiliateService.getByDni(DEFAULT_DNI);

            // Then
            assertThat(result.getDni()).isEqualTo(DEFAULT_DNI);
        }
    }

    @Nested
    @DisplayName("getFamilyGroup")
    class GetFamilyGroup {

        @Test
        @DisplayName("Falla si el afiliado no existe")
        void shouldThrowNotFound_whenAffiliateDoesNotExist() {
            // Given
            when(affiliateRepository.findById(PRIMARY_ID))
                    .thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> affiliateService.getFamilyGroup(PRIMARY_ID))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("Si el afiliado es titular, consulta con su propio id")
        void shouldUseOwnId_whenAffiliateIsPrimary() {
            // Given
            Affiliate primary = activePrimary();
            Affiliate dependent = dependentOf(DEPENDENT_ID, Status.ACTIVE, primary);
            List<Affiliate> familyGroup = List.of(primary, dependent);

            when(affiliateRepository.findById(PRIMARY_ID))
                    .thenReturn(Optional.of(primary));
            when(affiliateRepository.findFamilyGroupByPrimaryId(PRIMARY_ID))
                    .thenReturn(familyGroup);

            // When
            var result = affiliateService.getFamilyGroup(PRIMARY_ID);

            // Then
            assertThat(result)
                    .hasSize(2)
                    .extracting(AffiliateResponseDto::getId, AffiliateResponseDto::getDni)
                    .containsExactly(
                            tuple(PRIMARY_ID, primary.getDni()),
                            tuple(DEPENDENT_ID, dependent.getDni())
                    );
        }

        @Test
        @DisplayName("Si el afiliado es familiar, consulta con el id del titular")
        void shouldUsePrimaryId_whenAffiliateIsDependent() {
            // Given
            Affiliate primary = activePrimary();
            Affiliate dependent = dependentOf(DEPENDENT_ID, Status.ACTIVE, primary);
            List<Affiliate> familyGroup = List.of(primary, dependent);

            when(affiliateRepository.findById(DEPENDENT_ID))
                    .thenReturn(Optional.of(dependent));
            when(affiliateRepository.findFamilyGroupByPrimaryId(PRIMARY_ID))
                    .thenReturn(familyGroup);

            // When
            var result = affiliateService.getFamilyGroup(DEPENDENT_ID);

            // Then
            assertThat(result)
                    .hasSize(2)
                    .extracting(AffiliateResponseDto::getId, AffiliateResponseDto::getDni)
                    .containsExactly(
                            tuple(PRIMARY_ID, primary.getDni()),
                            tuple(DEPENDENT_ID, dependent.getDni())
                    );
        }
    }

    @Nested
    @DisplayName("getPrimaryWithFamily")
    class GetPrimaryWithFamily {

        @Test
        @DisplayName("Falla si no existe un titular con ese id")
        void shouldThrowNotFound_whenPrimaryDoesNotExist() {
            // Given
            when(affiliateRepository.findPrimaryWithFamilyById(PRIMARY_ID))
                    .thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> affiliateService.getPrimaryWithFamily(PRIMARY_ID))
                    .isInstanceOf(EntityNotFoundException.class);
        }


        @Test
        @DisplayName("Devuelve el titular con su grupo familiar")
        void shouldReturnPrimaryWithFamily() {
            // Given
            Affiliate primary = activePrimary();
            Affiliate dependent = dependentOf(DEPENDENT_ID, Status.ACTIVE, primary);
            primary.setFamilyMembers(List.of(dependent));

            when(affiliateRepository.findPrimaryWithFamilyById(PRIMARY_ID))
                    .thenReturn(Optional.of(primary));

            // When
            var result = affiliateService.getPrimaryWithFamily(PRIMARY_ID);

            // Then
            SoftAssertions.assertSoftly(softly -> {
                // Datos del titular
                softly.assertThat(result.getId()).isEqualTo(PRIMARY_ID);
                softly.assertThat(result.getAffiliateType()).isEqualTo(AffiliateType.PRIMARY);
                softly.assertThat(result.getPrimaryAffiliate()).isNull();

                // Lista de familiares
                softly.assertThat(result.getFamilyMembers())
                        .hasSize(1)
                        .extracting(AffiliateResponseDto::getId, AffiliateResponseDto::getDni)
                        .containsExactly(tuple(DEPENDENT_ID, dependent.getDni()));
            });
        }
    }

    @Nested
    @DisplayName("lists")
    class Listings {

        private final Pageable pageable = PageRequest.of(0, 10);

        @Test
        @DisplayName("listAll consulta ordenando por apellido y mapea la página")
        void shouldQueryOrderedByLastName_onListAll() {
            // Given
            Affiliate primary = activePrimary();
            Affiliate dependent = dependentOf(DEPENDENT_ID, Status.ACTIVE, primary);
            Affiliate otherPrimary = primaryOf(OTHER_PRIMARY_ID, Status.ACTIVE);

            Page<Affiliate> affiliatePage = new PageImpl<>(List.of(primary, dependent, otherPrimary));

            when(affiliateRepository.findAllByOrderByLastNameAsc(pageable))
                    .thenReturn(affiliatePage);

            // When
            var result = affiliateService.listAll(pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(3);
            assertThat(result.getTotalElements()).isEqualTo(3);

            assertThat(result.getContent())
                    .extracting(AffiliateResponseDto::getId)
                    .containsExactly(PRIMARY_ID, DEPENDENT_ID, OTHER_PRIMARY_ID);
        }

        @Test
        @DisplayName("listPrimaries filtra por tipo PRIMARY")
        void shouldFilterByPrimaryType_onListPrimaries() {
            // Given
            Affiliate primary = activePrimary();
            Affiliate otherPrimary = primaryOf(OTHER_PRIMARY_ID, Status.ACTIVE);

            Page<Affiliate> primariesPage = new PageImpl<>(List.of(primary, otherPrimary));

            when(affiliateRepository.findByAffiliateTypeOrderByLastNameAsc(AffiliateType.PRIMARY, pageable))
                    .thenReturn(primariesPage);

            // When
            var result = affiliateService.listPrimaries(pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);

            assertThat(result.getContent())
                    .extracting(AffiliateResponseDto::getId, AffiliateResponseDto::getAffiliateType)
                    .containsExactly(
                            tuple(PRIMARY_ID, AffiliateType.PRIMARY),
                            tuple(OTHER_PRIMARY_ID, AffiliateType.PRIMARY)
                    );
        }

        @Test
        @DisplayName("search usa el término tanto para nombre como para apellido")
        void shouldSearchByFirstAndLastName() {
            // Given
            Affiliate primary = activePrimary();
            Affiliate dependent = dependentOf(DEPENDENT_ID, Status.ACTIVE, primary);
            Page<Affiliate> affiliatePage = new PageImpl<>(List.of(primary, dependent));

            String term = "Pérez";

            when(affiliateRepository
                    .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(term, term, pageable))
                    .thenReturn(affiliatePage);

            // When
            var result = affiliateService.search(term, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);

            assertThat(result.getContent())
                    .extracting(AffiliateResponseDto::getId, AffiliateResponseDto::getLastName)
                    .containsExactly(
                            tuple(PRIMARY_ID, "Pérez"),
                            tuple(DEPENDENT_ID, "Pérez")
                    );
        }
    }

    // ──────────── FOR OTHER SERVICES ────────────

    @Nested
    @DisplayName("isActive")
    class IsActive {

        @Test
        @DisplayName("Devuelve false si el afiliado no existe")
        void shouldReturnFalse_whenAffiliateDoesNotExist() {
            // Given
            when(affiliateRepository.findById(PRIMARY_ID)).thenReturn(Optional.empty());

            // When
            boolean result = affiliateService.isActive(PRIMARY_ID);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Devuelve false si el afiliado está inactivo")
        void shouldReturnFalse_whenAffiliateIsInactive() {
            // Given
            when(affiliateRepository.findById(PRIMARY_ID))
                    .thenReturn(Optional.of(primaryOf(PRIMARY_ID, Status.INACTIVE)));

            // When
            boolean result = affiliateService.isActive(PRIMARY_ID);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Devuelve true si el afiliado está activo")
        void shouldReturnTrue_whenAffiliateIsActive() {
            // Given
            when(affiliateRepository.findById(PRIMARY_ID))
                    .thenReturn(Optional.of(primaryOf(PRIMARY_ID, Status.ACTIVE)));

            // When
            boolean result = affiliateService.isActive(PRIMARY_ID);

            // Then
            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("lookupByIds")
    class LookupByIds {

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Devuelve lista vacía si no se piden ids, sin consultar la base")
        void shouldReturnEmptyList_whenIdsAreNullOrEmpty(List<Long> ids) {
            // When
            var result = affiliateService.lookupByIds(ids);

            // Then
            assertThat(result).isEmpty();
            verifyNoInteractions(affiliateRepository);
        }

        @Test
        @DisplayName("Mapea a resumen los afiliados encontrados")
        void shouldMapFoundAffiliatesToSummary() {
            // Given
            List<Long> ids = List.of(PRIMARY_ID, DEPENDENT_ID);

            Affiliate primary = activePrimary();
            Affiliate dependent = dependentOf(DEPENDENT_ID, Status.ACTIVE, primary);

            when(affiliateRepository.findAllById(ids))
                    .thenReturn(List.of(primary, dependent));

            // When
            var result = affiliateService.lookupByIds(ids);

            // Then
            assertThat(result)
                    .hasSize(2)
                    .extracting(AffiliateSummaryDto::getId, AffiliateSummaryDto::getDni)
                    .containsExactly(
                            tuple(PRIMARY_ID, DEFAULT_DNI),
                            tuple(DEPENDENT_ID, OTHER_DNI));
        }
    }

    @Nested
    @DisplayName("filterActiveDnis")
    class FilterActiveDnis {

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Devuelve conjunto vacío si no se piden DNIs, sin consultar la base")
        void shouldReturnEmptySet_whenDnisAreNullOrEmpty(List<String> dnis) {
            // When
            var result = affiliateService.filterActiveDnis(dnis);

            // Then
            assertThat(result).isEmpty();
            verifyNoInteractions(affiliateRepository);
        }

        @Test
        @DisplayName("Devuelve solo los DNIs activos, sin duplicados")
        void shouldReturnOnlyActiveDnis() {
            // Given
            List<String> dnis = List.of(DEFAULT_DNI, OTHER_DNI, DEFAULT_DNI);

            when(affiliateRepository.findActiveDnisIn(dnis))
                    .thenReturn(List.of(DEFAULT_DNI, OTHER_DNI, DEFAULT_DNI));

            // When
            var result = affiliateService.filterActiveDnis(dnis);

            // Then
            assertThat(result)
                    .hasSize(2)
                    .containsExactlyInAnyOrder(DEFAULT_DNI, OTHER_DNI);
        }
    }

    // ──────────── UPDATE ────────────

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("Falla si el afiliado no existe")
        void shouldThrowNotFound_whenAffiliateDoesNotExist() {
            // Given
            when(affiliateRepository.findById(PRIMARY_ID))
                    .thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> affiliateService.update(PRIMARY_ID, requestDto()))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("Falla si el afiliado está inactivo")
        void shouldThrowConflict_whenAffiliateIsInactive() {
            // Given
            when(affiliateRepository.findById(PRIMARY_ID))
                    .thenReturn(Optional.of(primaryOf(PRIMARY_ID, Status.INACTIVE)));

            // When / Then
            assertThatThrownBy(() -> affiliateService.update(PRIMARY_ID, requestDto()))
                    .isInstanceOf(EntityConflictException.class);
        }

        @Test
        @DisplayName("Falla si se quiere dejar menor de edad a un titular")
        void shouldThrowConflict_whenPrimaryBecomesUnderage() {
            // Given
            when(affiliateRepository.findById(PRIMARY_ID))
                    .thenReturn(Optional.of(activePrimary()));
            AffiliateRequestDto dto = requestDto();
            dto.setBirthDate(LocalDate.now().minusYears(17));

            // When / Then
            assertThatThrownBy(() -> affiliateService.update(PRIMARY_ID, dto))
                    .isInstanceOf(EntityConflictException.class);
        }

        @Test
        @DisplayName("Un familiar puede tener fecha de nacimiento de menor de edad")
        void shouldAllowUnderageBirthDate_forDependent() {
            // Given
            when(affiliateRepository.findById(DEPENDENT_ID))
                    .thenReturn(Optional.of(dependentOf(DEPENDENT_ID,Status.ACTIVE, activePrimary())));

            AffiliateRequestDto dto = dependentRequestDto();
            dto.setBirthDate(LocalDate.now().minusYears(10));

            // When / Then
            assertThatCode(() -> affiliateService.update(DEPENDENT_ID, dto))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("No verifica duplicados si el DNI no cambia")
        void shouldNotCheckDuplicates_whenDniIsUnchanged() {
            // Given
            when(affiliateRepository.findById(PRIMARY_ID))
                    .thenReturn(Optional.of(activePrimary()));
            AffiliateRequestDto dto = requestDto();
            dto.setDni(DEFAULT_DNI);      // dejamos explicito mismo DNI
            dto.setLastName("Fernández"); // solo modificamos apellido

            // When
            affiliateService.update(PRIMARY_ID, dto);

            // Then
            verify(affiliateRepository, never()).existsByDni(any());
        }

        @Test
        @DisplayName("Falla si el nuevo DNI ya pertenece a otro afiliado")
        void shouldThrowConflict_whenNewDniIsTaken() {
            // Given
            when(affiliateRepository.findById(PRIMARY_ID))
                    .thenReturn(Optional.of(activePrimary()));
            when(affiliateRepository.existsByDni(OTHER_DNI))
                    .thenReturn(true);

            AffiliateRequestDto dto = requestDto();
            dto.setDni(OTHER_DNI);

            // When / Then
            assertThatThrownBy(() -> affiliateService.update(PRIMARY_ID, dto))
                    .isInstanceOf(EntityConflictException.class);
        }

        @Test
        @DisplayName("Actualiza los datos personales y normaliza los nombres")
        void shouldUpdatePersonalData() {
            // Given
            when(affiliateRepository.findById(PRIMARY_ID))
                    .thenReturn(Optional.of(activePrimary()));

            AffiliateRequestDto dto = requestDto();
            dto.setDni(OTHER_DNI);
            dto.setFirstName("  juan   CARLOS ");
            dto.setLastName("fErnándEz");

            // When
            var result = affiliateService.update(PRIMARY_ID, dto);

            // Then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.getDni()).isEqualTo(OTHER_DNI);
                softly.assertThat(result.getFirstName()).isEqualTo("Juan Carlos");
                softly.assertThat(result.getLastName()).isEqualTo("Fernández");
            });
        }

        @Test
        @DisplayName("Actualiza la relación de un familiar si se indica")
        void shouldUpdateRelation_forDependent() {
            // Given
            when(affiliateRepository.findById(DEPENDENT_ID))
                    .thenReturn(Optional.of(dependentOf(DEPENDENT_ID, Status.ACTIVE, activePrimary())));

            AffiliateRequestDto dto = dependentRequestDto();
            dto.setRelation(RelationType.DOMESTIC_PARTNER);

            // When
            var result = affiliateService.update(DEPENDENT_ID, dto);

            // Then
            assertThat(result.getRelation()).isEqualTo(RelationType.DOMESTIC_PARTNER);
        }

        @Test
        @DisplayName("No toca la relación de un familiar si el dto no la trae")
        void shouldKeepRelation_whenDtoHasNone() {
            // Given
            when(affiliateRepository.findById(DEPENDENT_ID))
                    .thenReturn(Optional.of(dependentOf(DEPENDENT_ID, Status.ACTIVE, activePrimary())));

            AffiliateRequestDto dto = dependentRequestDto();
            dto.setFirstName("juan carlos"); // cambiamos solo el nombre
            dto.setRelation(null);

            // When
            var result = affiliateService.update(DEPENDENT_ID, dto);

            // Then
            assertThat(result.getRelation()).isEqualTo(RelationType.CHILD);
        }

        @Test
        @DisplayName("Un titular nunca recibe relación, aunque el dto la traiga")
        void shouldIgnoreRelation_forPrimary() {
            // Given
            when(affiliateRepository.findById(PRIMARY_ID))
                    .thenReturn(Optional.of(activePrimary()));

            AffiliateRequestDto dto = requestDto();
            dto.setRelation(RelationType.CHILD);

            // When
            var result = affiliateService.update(PRIMARY_ID, dto);

            // Then
            assertThat(result.getRelation()).isNull();
        }

    }

    // ──────────── DEACTIVATE / ACTIVATE ────────────

    @Nested
    @DisplayName("deactivate")
    class Deactivate {

        @Test
        @DisplayName("Falla si el afiliado no existe")
        void shouldThrowNotFound_whenAffiliateDoesNotExist() {
            // Given
            when(affiliateRepository.findById(PRIMARY_ID))
                    .thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> affiliateService.deactivate(PRIMARY_ID))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("Falla si el afiliado ya está inactivo")
        void shouldThrowConflict_whenAlreadyInactive() {
            // Given
            when(affiliateRepository.findById(PRIMARY_ID))
                    .thenReturn(Optional.of(primaryOf(PRIMARY_ID, Status.INACTIVE)));

            // When / Then
            assertThatThrownBy(() -> affiliateService.deactivate(PRIMARY_ID))
                    .isInstanceOf(EntityConflictException.class);
        }

        @Test
        @DisplayName("Dar de baja a un titular da de baja a todo su grupo familiar")
        void shouldCascadeToDependents_whenPrimaryIsDeactivated() {
            // Given
            Affiliate primary = activePrimary();
            Affiliate dependent = dependentOf(DEPENDENT_ID, Status.ACTIVE, primary);

            when(affiliateRepository.findById(PRIMARY_ID))
                    .thenReturn(Optional.of(primary));
            when(affiliateRepository.findByPrimaryAffiliateIdOrderByLastNameAsc(PRIMARY_ID))
                    .thenReturn(List.of(dependent));

            // When
            affiliateService.deactivate(PRIMARY_ID);

            // Then
            assertThat(primary.getStatus()).isEqualTo(Status.INACTIVE);
            assertThat(dependent.getStatus()).isEqualTo(Status.INACTIVE);
        }

        @Test
        @DisplayName("No consulta familiares al dar de baja a un familiar")
        void shouldNotLookForDependents_whenDeactivatingADependent() {
            // Given
            Affiliate dependent = dependentOf(DEPENDENT_ID, Status.ACTIVE, activePrimary());

            when(affiliateRepository.findById(DEPENDENT_ID))
                    .thenReturn(Optional.of(dependent));

            // When
            affiliateService.deactivate(DEPENDENT_ID);

            // Then
            assertThat(dependent.getStatus()).isEqualTo(Status.INACTIVE);
            verify(affiliateRepository, never()).findByPrimaryAffiliateIdOrderByLastNameAsc(any());
        }
    }

    @Nested
    @DisplayName("activate")
    class Activate {

        @Test
        @DisplayName("Falla si el afiliado no existe")
        void shouldThrowNotFound_whenAffiliateDoesNotExist() {
            // Given
            when(affiliateRepository.findById(PRIMARY_ID))
                    .thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> affiliateService.activate(PRIMARY_ID))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("Falla si el afiliado ya está activo")
        void shouldThrowConflict_whenAlreadyActive() {
            // Given
            when(affiliateRepository.findById(PRIMARY_ID))
                    .thenReturn(Optional.of(activePrimary()));

            // When / Then
            assertThatThrownBy(() -> affiliateService.activate(PRIMARY_ID))
                    .isInstanceOf(EntityConflictException.class);
        }

        @Test
        @DisplayName("Falla si se quiere dar de alta a un familiar con el titular inactivo")
        void shouldThrowConflict_whenPrimaryIsStillInactive() {
            // Given
            Affiliate primary = primaryOf(PRIMARY_ID, Status.INACTIVE);
            Affiliate dependent = dependentOf(DEPENDENT_ID, Status.INACTIVE, primary);

            when(affiliateRepository.findById(DEPENDENT_ID))
                    .thenReturn(Optional.of(dependent));

            // When
            assertThatThrownBy(() -> affiliateService.activate(DEPENDENT_ID))
                    .isInstanceOf(EntityConflictException.class);
        }

        @Test
        @DisplayName("Da de alta al familiar si el titular está activo")
        void shouldActivateDependent_whenPrimaryIsActive() {
            // Given
            Affiliate primary = primaryOf(PRIMARY_ID, Status.ACTIVE);
            Affiliate dependent = dependentOf(DEPENDENT_ID, Status.INACTIVE, primary);

            when(affiliateRepository.findById(DEPENDENT_ID))
                    .thenReturn(Optional.of(dependent));

            // When
            affiliateService.activate(DEPENDENT_ID);

            // Then
            assertThat(dependent.getStatus()).isEqualTo(Status.ACTIVE);
        }

        @Test
        @DisplayName("Da de alta a un titular, y no reactiva a su grupo familiar")
        void shouldActivatePrimary_andNotCascadeToDependents() {
            // Given
            Affiliate primary = primaryOf(PRIMARY_ID, Status.INACTIVE);

            when(affiliateRepository.findById(PRIMARY_ID))
                    .thenReturn(Optional.of(primary));

            // When
            affiliateService.activate(PRIMARY_ID);

            // Then
            assertThat(primary.getStatus()).isEqualTo(Status.ACTIVE);
            verify(affiliateRepository, never()).findByPrimaryAffiliateIdOrderByLastNameAsc(any());
        }
    }

    // ──────────── HELPERS ────────────

    // ---- Entities ----

    private Affiliate primaryOf(Long id, Status status) {
        return Affiliate.builder()
                .id(id)
                .dni(DEFAULT_DNI)
                .firstName("Juan")
                .lastName("Pérez")
                .phoneNumber(DEFAULT_PHONE)
                .birthDate(LocalDate.now().minusYears(40))
                .status(status)
                .affiliateType(AffiliateType.PRIMARY)
                .build();
    }

    private Affiliate activePrimary() {
        return primaryOf(PRIMARY_ID, Status.ACTIVE);
    }

    private Affiliate dependentOf(Long id, Status status, Affiliate primary) {
        return Affiliate.builder()
                .id(id)
                .dni(OTHER_DNI)
                .firstName("Ana")
                .lastName("Pérez")
                .phoneNumber(DEFAULT_PHONE)
                .birthDate(LocalDate.now().minusYears(10))
                .status(status)
                .affiliateType(AffiliateType.DEPENDENT)
                .relation(RelationType.CHILD)
                .primaryAffiliate(primary)
                .build();
    }

    // ---- Capture ----

    private Affiliate capturedSavedAffiliate() {
        ArgumentCaptor<Affiliate> captor = ArgumentCaptor.forClass(Affiliate.class);
        verify(affiliateRepository).save(captor.capture());
        return captor.getValue();
    }

    // ---- Stub Success creation ----

    private void stubSuccessfulCreation() {
        when(affiliateRepository.existsByDni(any())).thenReturn(false);
        when(affiliateRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ---- Default Dtos----

    private AffiliateRequestDto requestDto() {
        AffiliateRequestDto dto = new AffiliateRequestDto();
        dto.setDni(DEFAULT_DNI);
        dto.setFirstName("Juan");
        dto.setLastName("Pérez");
        dto.setPhoneNumber(DEFAULT_PHONE);
        dto.setBirthDate(LocalDate.now().minusYears(40));
        return dto;
    }

    private AffiliateRequestDto dependentRequestDto() {
        AffiliateRequestDto dto = requestDto();
        dto.setDni(OTHER_DNI);
        dto.setRelation(RelationType.CHILD);
        return dto;
    }
}