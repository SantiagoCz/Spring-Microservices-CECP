package com.santiagocz.medical_coverage_service.services;

import com.santiagocz.common.auth.AuthenticatedUser;
import com.santiagocz.common.delegation.Delegation;
import com.santiagocz.common.exceptions.EntityConflictException;
import com.santiagocz.common.exceptions.EntityNotFoundException;
import com.santiagocz.medical_coverage_service.client.AffiliateClient;
import com.santiagocz.medical_coverage_service.domain.entities.MedicalOrder;
import com.santiagocz.medical_coverage_service.domain.entities.Payment;
import com.santiagocz.medical_coverage_service.domain.enums.MedicalOrderType;
import com.santiagocz.medical_coverage_service.domain.enums.Status;
import com.santiagocz.medical_coverage_service.dto.medicalOrder.MedicalOrderRequestDto;
import com.santiagocz.medical_coverage_service.dto.payment.PaymentRequestDto;
import com.santiagocz.medical_coverage_service.dto.payment.PaymentResponseDto;
import com.santiagocz.medical_coverage_service.dto.payment.PaymentUpdateDto;
import com.santiagocz.medical_coverage_service.repositories.PaymentRepository;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private MedicalOrderService medicalOrderService;
    @Mock
    private AffiliateClient affiliateClient;
    @InjectMocks
    private PaymentService paymentService;

    //Variables globales por defecto para Medical Order
    private static final Long DEFAULT_ORDER_NUMBER = 9000100L;
    private static final MedicalOrderType DEFAULT_MEDICAL_ORDER_TYPE = MedicalOrderType.CONSULTA;

    //Variables globales por defecto para Payment
    private static final Double DEFAULT_AMOUNT = 1000.0;
    private static final Integer DEFAULT_DISCOUNT = 0;
    private static final Long DEFAULT_AFFILIATE_ID = 10L;
    private static final Delegation DEFAULT_DELEGATION = Delegation.ALEM;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ──────────── CREATE ────────────

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("Falla si no hay usuario autenticado")
        void shouldThrowAccessDenied_whenThereIsNoAuthentication() {
            assertThatThrownBy(() -> paymentService.create(defaultPaymentDto()))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(affiliateClient, medicalOrderService, paymentRepository);
        }

        @Test
        @DisplayName("Guarda el pago con los datos del request y estado activo")
        void shouldMapRequestFields_whenCreating() {
            // Given
            authenticateAs(DEFAULT_DELEGATION, "ROLE_USER");
            stubSuccessfulCreation();

            // When
            paymentService.create(defaultPaymentDto());

            // Then
            // Verificar campos
            Payment saved = capturedPayment();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(saved.getAffiliateId()).isEqualTo(DEFAULT_AFFILIATE_ID);
                softly.assertThat(saved.getAmount()).isEqualTo(DEFAULT_AMOUNT);
                softly.assertThat(saved.getStatus()).isEqualTo(Status.ACTIVE);
                softly.assertThat(saved.getMedicalOrder()).isNotNull();
            });
        }

        @Test
        @DisplayName("Un usuario común siempre registra en su propia delegación, aunque pida otra")
        void shouldForceOwnDelegation_whenUserIsNotSuperAdmin() {
            // Given
            authenticateAs(Delegation.ELDORADO, "ROLE_USER");
            stubSuccessfulCreation();

            // When
            paymentService.create(requestForDelegation(Delegation.POSADAS_ECSALUD));

            // Then
            assertThat(capturedPayment().getDelegation()).isEqualTo(Delegation.ELDORADO);
        }

        @Test
        @DisplayName("El superadmin puede registrar en la delegación que indique")
        void shouldUseRequestedDelegation_forSuperAdmin() {
            // Given
            authenticateAs(Delegation.ELDORADO, "ROLE_SUPER_ADMIN");
            stubSuccessfulCreation();

            // When
            paymentService.create(requestForDelegation(Delegation.POSADAS_ECSALUD));

            // Then
            assertThat(capturedPayment().getDelegation()).isEqualTo(Delegation.POSADAS_ECSALUD);
        }

        @Test
        @DisplayName("El superadmin que no indica delegación registra en la suya")
        void shouldFallBackToOwnDelegation_forSuperAdminWithoutRequest() {
            // Given
            authenticateAs(Delegation.ELDORADO, "ROLE_SUPER_ADMIN");
            stubSuccessfulCreation();

            // When
            paymentService.create(requestForDelegation(null));

            // Then
            assertThat(capturedPayment().getDelegation()).isEqualTo(Delegation.ELDORADO);
        }

        @Test
        @DisplayName("Falla si el número de orden ya existe en la delegación indicada")
        void shouldPropagateConflict_whenMedicalOrderNumberAlreadyExists() {
            // Given
            authenticateAs(Delegation.POSADAS_POLICONSULTORIOS, "ROLE_SUPER_ADMIN");
            when(affiliateClient.isActive(10L)).thenReturn(true);
            when(medicalOrderService.buildAndValidate(any(), eq(Delegation.APOSTOLES)))
                    .thenThrow(new EntityConflictException(
                            "El número de orden: 9000100 ya se encuentra registrado."));

            // When / Then
            assertThatThrownBy(() -> paymentService.create(requestForDelegation(Delegation.APOSTOLES)))
                    .isInstanceOf(EntityConflictException.class)
                    .hasMessageContaining("9000100");

            // No debe quedar un pago huérfano si la orden es inválida
            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Falla si el afiliado no está activo")
        void shouldThrowConflict_whenAffiliateIsNotActive() {
            // Given: usuario autenticado + isActive(10L) -> false
            authenticateAs(DEFAULT_DELEGATION, "ROLE_USER");
            when(affiliateClient.isActive(DEFAULT_AFFILIATE_ID)).thenReturn(false);

            // When / Then
            assertThatThrownBy(() -> paymentService.create(defaultPaymentDto()))
                    .isInstanceOf(EntityConflictException.class)
                    .hasMessageContaining("El afiliado no existe");

            verifyNoInteractions(medicalOrderService, paymentRepository);
        }

        @Test
        @DisplayName("Falla si el cliente de afiliados devuelve null")
        void shouldThrowConflict_whenAffiliateLookupReturnsNull() {
            // Given
            authenticateAs(DEFAULT_DELEGATION, "ROLE_USER");
            PaymentRequestDto paymentDto = defaultPaymentDto();
            when(affiliateClient.isActive(DEFAULT_AFFILIATE_ID)).thenReturn((Boolean) null);

            // When / Then
            assertThatThrownBy(() -> paymentService.create(paymentDto))
                    .isInstanceOf(EntityConflictException.class)
                            .hasMessageContaining("El afiliado no existe");

            verifyNoInteractions(medicalOrderService, paymentRepository);
        }

        @Test
        @DisplayName("Falla si el usuario autenticado no tiene delegación asignada")
        void shouldThrowConflict_whenAuthenticatedUserHasNoDelegation() {
            // Given
            authenticateAs(null, "ROLE_USER");
            PaymentRequestDto paymentDto = requestForDelegation(null);

            // When / Then
            assertThatThrownBy(() -> paymentService.create(paymentDto))
                    .isInstanceOf(EntityConflictException.class);

            verifyNoInteractions(affiliateClient);
        }

        @Test
        @DisplayName("Calcula el monto de descuento a partir del porcentaje")
        void shouldCalculateDiscountAmount_fromPercentage() {
            // Given
            authenticateAs(DEFAULT_DELEGATION, "ROLE_USER");
            Double amount = 1000.0;
            Integer discount = 10;
            stubSuccessfulCreation();

            // When
            paymentService.create(requestWithDiscount(amount, discount));

            // Then: 1000.0 × 10% = 100.0
            assertThat(capturedPayment().getDiscountAmount()).isEqualTo(100.0, within(0.001));
        }

        @Test
        @DisplayName("El monto de descuento es cero si el descuento es cero")
        void shouldCalculateZeroDiscountAmount_whenDiscountIsZero() {
            // Given
            authenticateAs(DEFAULT_DELEGATION, "ROLE_USER");
            stubSuccessfulCreation();

            // When
            paymentService.create(requestWithDiscount(1000.0, 0));

            // Then
            assertThat(capturedPayment().getDiscountAmount()).isEqualTo(0.0, within(0.001));
        }
    }

    // ──────────── FIND BY AFFILIATE ID ────────────

    @Nested
    @DisplayName("findByAffiliateId")
    class FindByAffiliateId {

        @Test
        @DisplayName("El usuario común solo ve los pagos de su delegación")
        void shouldFilterOutOtherDelegations_whenUserIsNotSuperAdmin() {
            // Given
            authenticateAs(DEFAULT_DELEGATION, "ROLE_USER");
            // Simulamos que el repositorio devuelve una lista
            when(paymentRepository.findByAffiliateId(DEFAULT_AFFILIATE_ID))
                    .thenReturn(List.of(paymentOf(DEFAULT_DELEGATION), paymentOf(Delegation.APOSTOLES)));
            // When
            var result = paymentService.findByAffiliateId(DEFAULT_AFFILIATE_ID);

            // Then
            // De los dos pagos, solo queda el de la delegación del usuario
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDelegation()).isEqualTo(DEFAULT_DELEGATION);
        }

        @Test
        @DisplayName("El superadmin ve los pagos de todas las delegaciones")
        void shouldReturnAllDelegations_forSuperAdmin() {
            // Given
            authenticateAs(DEFAULT_DELEGATION, "ROLE_SUPER_ADMIN");
            // Simulamos que el repositorio devuelve una lista
            when(paymentRepository.findByAffiliateId(DEFAULT_AFFILIATE_ID))
                    .thenReturn(List.of(paymentOf(DEFAULT_DELEGATION), paymentOf(Delegation.APOSTOLES)));
            // When
            var result = paymentService.findByAffiliateId(DEFAULT_AFFILIATE_ID);

            // Then
            // Quedan ambos pagos
            assertThat(result).hasSize(2);
            assertThat(result)
                    .extracting(PaymentResponseDto::getDelegation)
                    .containsExactlyInAnyOrder(DEFAULT_DELEGATION, Delegation.APOSTOLES);
        }

        @Test
        @DisplayName("Devuelve lista vacía si el afiliado no tiene pagos")
        void shouldReturnEmptyList_whenAffiliateHasNoPayments() {
            // Given
            authenticateAs(DEFAULT_DELEGATION, "ROLE_USER");
            // Simulamos que el repositorio devuelve una lista vacia
            when(paymentRepository.findByAffiliateId(DEFAULT_AFFILIATE_ID))
                    .thenReturn(List.of());
            // When
            var result = paymentService.findByAffiliateId(DEFAULT_AFFILIATE_ID);

            // Then
            assertThat(result).isNotNull().isEmpty();
        }
    }

    // ──────────── FIND BY FILTERS ────────────

    @Nested
    @DisplayName("findByFilters")
    class FindByFilters {


        @Test
        @DisplayName("El usuario común consulta siempre con su propia delegación, aunque pida otra")
        void shouldForceOwnDelegation_whenUserIsNotSuperAdmin() {
            // Given
            authenticateAs(Delegation.POSADAS_ECSALUD, "ROLE_USER");

            // Filters:
            LocalDate startDate = LocalDate.of(2026, 9, 1);
            LocalDate endDate = LocalDate.of(2026, 9, 30);
            Delegation requestedDelegation = Delegation.ALEM;

            // Simulamos que el repositorio devuelve una lista vacía cuando lo llamen
            when(paymentRepository.findByFilters(any(), any(), any(), any(), any()))
                    .thenReturn(List.of());

            // When
            paymentService.findByFilters(startDate, endDate, null, requestedDelegation, null);

            // Then
            // Verificamos que el SERVICIO haya ignorado 'requestedDelegation' (ALEM)
            // y haya llamado al REPOSITORIO usando la delegación propia del usuario (POSADAS_ECSALUD)
            verify(paymentRepository).findByFilters(
                    eq(startDate),
                    eq(endDate),
                    isNull(), // status
                    eq(Delegation.POSADAS_ECSALUD),
                    isNull()  // creatorId
            );
        }

        @ParameterizedTest
        @NullSource
        @EnumSource(Delegation.class)
        @DisplayName("El superadmin consulta con la delegación que pida, o todas si no pide ninguna")
        void shouldPassRequestedDelegation_forSuperAdmin(Delegation requestedDelegation) {
            // Given
            authenticateAs(Delegation.POSADAS_ECSALUD, "ROLE_SUPER_ADMIN");

            // Filters:
            LocalDate startDate = LocalDate.of(2026, 9, 1);
            LocalDate endDate = LocalDate.of(2026, 9, 30);

            // Simulamos que el repositorio devuelve una lista vacía cuando lo llamen
            when(paymentRepository.findByFilters(any(), any(), any(), any(), any()))
                    .thenReturn(List.of());

            // When
            paymentService.findByFilters(startDate, endDate, null, requestedDelegation, null);

            // Then: al repositorio le llega la delegación pedida tal cual (null = todas)
            verify(paymentRepository).findByFilters(
                    eq(startDate),
                    eq(endDate),
                    isNull(),                // status
                    eq(requestedDelegation), // null = todas, o "ALEM"
                    isNull()                 // creatorId
            );
        }

        @Test
        @DisplayName("No consulta al servicio de afiliados si no hay pagos")
        void shouldNotCallAffiliateClient_whenNoPaymentsFound() {
            // Given
            authenticateAs(DEFAULT_DELEGATION, "ROLE_USER");

            // Filters:
            LocalDate startDate = LocalDate.of(2026, 9, 1);
            LocalDate endDate = LocalDate.of(2026, 9, 30);

            // Simulamos que el repositorio devuelve una lista vacía cuando lo llamen
            when(paymentRepository.findByFilters(any(), any(), any(), any(), any()))
                    .thenReturn(List.of());

            // When
            paymentService.findByFilters(startDate, endDate, null, null, null);

            // Then
            verifyNoInteractions(affiliateClient);
        }

        @Test
        @DisplayName("Consulta una sola vez por afiliado repetido")
        void shouldDeduplicateAffiliateIds_whenSeveralPaymentsShareAffiliate() {
            // Given
            authenticateAs(DEFAULT_DELEGATION, "ROLE_USER");

            // Filters:
            LocalDate startDate = LocalDate.of(2026, 9, 1);
            LocalDate endDate = LocalDate.of(2026, 9, 30);

            // Dos pagos distintos que comparten el mismo affiliateId
            when(paymentRepository.findByFilters(any(), any(), any(), any(), any()))
                    .thenReturn(List.of(paymentOf(DEFAULT_DELEGATION), paymentOf(DEFAULT_DELEGATION)));
            // Evitar nullPointer en lookupByIds
            when(affiliateClient.lookupByIds(any())).thenReturn(List.of());

            // When
            paymentService.findByFilters(startDate, endDate, null, null, null);

            // Then
            // El captor pesca la lista de ids que recibió el cliente de afiliados
            ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
            verify(affiliateClient).lookupByIds(captor.capture());

            // Un solo id pese a los dos pagos: el .distinct() hizo su trabajo
            // y no se hizo una consulta HTTP más grande de lo necesario
            assertThat(captor.getValue()).containsExactly(DEFAULT_AFFILIATE_ID);
        }

        @Test
        @DisplayName("Los datos del afiliado quedan en null si no se encontró")
        void shouldLeaveAffiliateFieldsNull_whenAffiliateNotFound() {
            // Given
            authenticateAs(DEFAULT_DELEGATION, "ROLE_USER");

            // Filters:
            LocalDate startDate = LocalDate.of(2026, 9, 1);
            LocalDate endDate = LocalDate.of(2026, 9, 30);

            // Un pago con afiliado asignado
            when(paymentRepository.findByFilters(any(), any(), any(), any(), any()))
                    .thenReturn(List.of(paymentOf(DEFAULT_DELEGATION)));
            // Llamada a servicio de afiliados, pero no lo devuelve
            when(affiliateClient.lookupByIds(any())).thenReturn(List.of());

            // When
            var result = paymentService.findByFilters(startDate, endDate, null, null, null);

            // Then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result).hasSize(1);
                softly.assertThat(result.get(0).getAffiliateDni()).isNull();
                softly.assertThat(result.get(0).getAffiliateFullName()).isNull();
            });
        }
    }

    // ──────────── FIND THIS MONTH ────────────

    @Nested
    @DisplayName("findThisMonth")
    class FindThisMonth {

        private final LocalDate firstDayOfMonth = YearMonth.now().atDay(1);
        private final LocalDate lastDayOfMonth = YearMonth.now().atEndOfMonth();

        @Test
        @DisplayName("Consulta desde el primer hasta el último día del mes actual")
        void shouldUseCurrentMonthRange() {
            // Given
            authenticateAs(DEFAULT_DELEGATION, "ROLE_USER");
            when(paymentRepository.findByFilters(any(), any(), any(), any(), any()))
                    .thenReturn(List.of());

            // When
            paymentService.findThisMonth(1L);

            // Then
            verify(paymentRepository).findByFilters(
                    eq(firstDayOfMonth),
                    eq(lastDayOfMonth),
                    isNull(),                 // status
                    eq(DEFAULT_DELEGATION),   // la del usuario
                    eq(1L)              // creatorId
            );
        }

        @Test
        @DisplayName("El superadmin ve los pagos del mes de todas las delegaciones")
        void shouldQueryAllDelegations_forSuperAdmin() {
            // Given
            authenticateAs(DEFAULT_DELEGATION, "ROLE_SUPER_ADMIN");
            when(paymentRepository.findByFilters(any(), any(), any(), any(), any()))
                    .thenReturn(List.of());

            // When
            paymentService.findThisMonth(null);

            // Then
            verify(paymentRepository).findByFilters(
                    eq(firstDayOfMonth),
                    eq(lastDayOfMonth),
                    isNull(),   // status
                    isNull(),   // null = todas las delegaciones
                    isNull()    // sin filtro por creador
            );
        }
    }

    // ──────────── FIND BY ID ────────────

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("Un pago de otra delegación se comporta como inexistente")
        void shouldThrowNotFound_whenPaymentBelongsToAnotherDelegation() {
            // Given
            authenticateAs(Delegation.POSADAS_ECSALUD, "ROLE_USER");
            when(paymentRepository.findById(1L))
                    .thenReturn(Optional.of(paymentOf(Delegation.ELDORADO)));

            // When / Then
            assertThatThrownBy(() -> paymentService.findById(1L))
                    .isInstanceOf(EntityNotFoundException.class);

            // No debería haber consultado al servicio de afiliados
            verifyNoInteractions(affiliateClient);
        }

        @Test
        @DisplayName("Un pago de la propia delegación se puede consultar")
        void shouldReturnPayment_whenPaymentBelongsToOwnDelegation() {
            // Given
            authenticateAs(Delegation.ELDORADO, "ROLE_USER");
            when(paymentRepository.findById(1L))
                    .thenReturn(Optional.of(paymentOf(Delegation.ELDORADO)));
            when(affiliateClient.lookupByIds(any())).thenReturn(List.of());

            // When
            var result = paymentService.findById(1L);

            // Then
            assertThat(result.getDelegation()).isEqualTo(Delegation.ELDORADO);
        }

        @Test
        @DisplayName("El superadmin puede consultar pagos de cualquier delegación")
        void shouldReturnPayment_forSuperAdmin_regardlessOfDelegation() {
            // Given
            authenticateAs(Delegation.POSADAS_ECSALUD, "ROLE_SUPER_ADMIN");
            when(paymentRepository.findById(1L))
                    .thenReturn(Optional.of(paymentOf(Delegation.ELDORADO)));
            when(affiliateClient.lookupByIds(any())).thenReturn(List.of());

            // When
            var result = paymentService.findById(1L);

            // Then
            assertThat(result.getDelegation()).isEqualTo(Delegation.ELDORADO);
        }

        @Test
        @DisplayName("Falla si el pago no existe")
        void shouldThrowNotFound_whenPaymentDoesNotExist() {
            // Given
            authenticateAs(DEFAULT_DELEGATION, "ROLE_USER");
            when(paymentRepository.findById(1L)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> paymentService.findById(1L))
                    .isInstanceOf(EntityNotFoundException.class);

            verifyNoInteractions(affiliateClient);
        }
    }

    // ──────────── FIND BY MEDICAL ORDER NUMBER ────────────

    @Nested
    @DisplayName("findByMedicalOrderNumber")
    class FindByMedicalOrderNumber {

        @Test
        @DisplayName("Falla si no existe un pago para esa orden")
        void shouldThrowNotFound_whenNoPaymentForOrderNumber() {
            // Given
            authenticateAs(DEFAULT_DELEGATION, "ROLE_USER");
            when(paymentRepository.findByMedicalOrderNumber(DEFAULT_ORDER_NUMBER)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> paymentService.findByMedicalOrderNumber(DEFAULT_ORDER_NUMBER))
                    .isInstanceOf(EntityNotFoundException.class);

            // No debería haber consultado al servicio de afiliados
            verifyNoInteractions(affiliateClient);
        }

        @Test
        @DisplayName("Un pago de otra delegación se comporta como inexistente")
        void shouldThrowNotFound_whenPaymentBelongsToAnotherDelegation() {
            // Given
            authenticateAs(Delegation.POSADAS_ECSALUD, "ROLE_USER");
            when(paymentRepository.findByMedicalOrderNumber(DEFAULT_ORDER_NUMBER)).thenReturn(Optional.of(paymentOf(Delegation.ELDORADO)));

            // When / Then
            assertThatThrownBy(() -> paymentService.findByMedicalOrderNumber(DEFAULT_ORDER_NUMBER))
                    .isInstanceOf(EntityNotFoundException.class);

            // No debería haber consultado al servicio de afiliados
            verifyNoInteractions(affiliateClient);
        }

        @Test
        @DisplayName("Devuelve el pago de la propia delegación")
        void shouldReturnPayment_whenPaymentBelongsToOwnDelegation() {
            // Given
            authenticateAs(DEFAULT_DELEGATION, "ROLE_USER");
            when(paymentRepository.findByMedicalOrderNumber(DEFAULT_ORDER_NUMBER))
                    .thenReturn(Optional.of(paymentOf(DEFAULT_DELEGATION)));
            when(affiliateClient.lookupByIds(any())).thenReturn(List.of());

            // When
            var result = paymentService.findByMedicalOrderNumber(DEFAULT_ORDER_NUMBER);

            // Then
            assertThat(result.getMedicalOrder().getNumber()).isEqualTo(DEFAULT_ORDER_NUMBER);
        }
    }

    // ──────────── UPDATE ────────────

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("Falla si el pago no existe")
        void shouldThrowNotFound_whenPaymentDoesNotExist() {
            //Given
            authenticateAs(DEFAULT_DELEGATION, "ROLE_USER");
            when(paymentRepository.findById(1L)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> paymentService.update(1L, defaultPaymentUpdateDto()))
                    .isInstanceOf(EntityNotFoundException.class);

            verifyNoInteractions(medicalOrderService);
        }

        @Test
        @DisplayName("Falla si el pago es de otra delegación")
        void shouldThrowNotFound_whenPaymentBelongsToAnotherDelegation() {
            //Given
            //Asignar diferentes delegaciones
            authenticateAs(Delegation.ALEM, "ROLE_USER");
            Payment payment = paymentOf(Delegation.APOSTOLES);
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

            // When / Then
            assertThatThrownBy(() -> paymentService.update(1L, defaultPaymentUpdateDto()))
                    .isInstanceOf(EntityNotFoundException.class);

            verifyNoInteractions(medicalOrderService);
        }

        @Test
        @DisplayName("Falla si el pago está inactivo")
        void shouldThrowConflict_whenPaymentIsInactive() {
            //Given
            authenticateAs(DEFAULT_DELEGATION, "ROLE_USER");
            Payment payment = inactivePayment();
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

            // When / Then
            assertThatThrownBy(() -> paymentService.update(1L, defaultPaymentUpdateDto()))
                    .isInstanceOf(EntityConflictException.class)
                    .hasMessageContaining("inactivo");

            verifyNoInteractions(medicalOrderService);
        }

        @Test
        @DisplayName("Un pago de exactamente 30 días todavía se puede modificar")
        void shouldAllowEdit_atTheEdgeOfTheWindow() {
            //Given
            authenticateAs(DEFAULT_DELEGATION, "ROLE_USER");
            Payment payment = paymentOf(DEFAULT_DELEGATION);
            payment.setCreatedAt(LocalDateTime.now().minusDays(30).plusMinutes(1));
            PaymentUpdateDto dto = defaultPaymentUpdateDto();
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

            // When
            paymentService.update(1L, defaultPaymentUpdateDto());

            // Then
            verify(medicalOrderService).update(payment.getMedicalOrder(), dto.getMedicalOrderDto(), DEFAULT_DELEGATION);

        }

        @Test
        @DisplayName("El usuario común no puede modificar un pago fuera de la ventana de 30 días")
        void shouldThrowConflict_whenEditWindowExpired_forRegularUser() {
            //Given
            authenticateAs(DEFAULT_DELEGATION, "ROLE_USER");
            Payment payment = paymentOverThirtyDaysAgo();
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

            // When / Then
            assertThatThrownBy(() -> paymentService.update(1L, defaultPaymentUpdateDto()))
                    .isInstanceOf(EntityConflictException.class);

            verifyNoInteractions(medicalOrderService);
        }

        @Test
        @DisplayName("El superadmin puede modificar fuera de la ventana de 30 días")
        void shouldAllowEdit_outsideWindow_forSuperAdmin() {
            // Given
            authenticateAs(DEFAULT_DELEGATION, "ROLE_SUPER_ADMIN");
            Payment payment = paymentOverThirtyDaysAgo();
            PaymentUpdateDto dto = defaultPaymentUpdateDto();
            dto.setAmount(2500.0);
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

            // When
            paymentService.update(1L, dto);

            // Then
            assertThat(payment.getAmount()).isEqualTo(2500.0);
            verify(medicalOrderService).update(payment.getMedicalOrder(), dto.getMedicalOrderDto(), DEFAULT_DELEGATION);
        }

        @Test
        @DisplayName("Un pago sin fecha de creación (dato histórico) se puede modificar")
        void shouldAllowEdit_whenCreatedAtIsNull() {
            // Given
            authenticateAs(DEFAULT_DELEGATION, "ROLE_USER");
            Payment payment = paymentOf(DEFAULT_DELEGATION);
            payment.setCreatedAt(null);
            PaymentUpdateDto dto = defaultPaymentUpdateDto();
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

            // When
            paymentService.update(1L, dto);

            // Then
            verify(medicalOrderService).update(payment.getMedicalOrder(), dto.getMedicalOrderDto(), DEFAULT_DELEGATION);
        }

        @Test
        @DisplayName("Actualiza fecha, monto y descuento dentro de la ventana")
        void shouldUpdateFields_whenInsideEditWindow() {
            // Given
            authenticateAs(DEFAULT_DELEGATION, "ROLE_USER");
            //Fields to update:
            LocalDate yesterday = LocalDate.now().minusDays(1);
            Double newAmount = 2500.0;
            Integer newDiscount = 100;

            Payment payment = paymentOf(DEFAULT_DELEGATION);
            payment.setCreatedAt(LocalDateTime.now().minusDays(10));

            PaymentUpdateDto dto = defaultPaymentUpdateDto();
            dto.setDate(yesterday);
            dto.setAmount(newAmount);
            dto.setDiscount(newDiscount);
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

            // When
            paymentService.update(1L, dto);

            // Then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(payment.getDate()).isEqualTo(yesterday);
                softly.assertThat(payment.getAmount()).isEqualTo(newAmount);
                softly.assertThat(payment.getDiscount()).isEqualTo(newDiscount);
                softly.assertThat(payment.getDiscountAmount()).isEqualTo(2500.0, within(0.001));
            });

            verify(medicalOrderService).update(payment.getMedicalOrder(), dto.getMedicalOrderDto(), DEFAULT_DELEGATION);
        }

        @Test
        @DisplayName("Valida la orden contra la delegación del pago, no la del usuario")
        void shouldValidateOrderAgainstPaymentDelegation_notUserDelegation() {
            // Given: superadmin de POSADAS_ECSALUD editando un pago de APOSTOLES
            authenticateAs(Delegation.POSADAS_ECSALUD, "ROLE_SUPER_ADMIN");
            Payment payment = paymentOf(Delegation.APOSTOLES);
            PaymentUpdateDto dto = defaultPaymentUpdateDto();
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

            // When
            paymentService.update(1L, dto);

            // Then
            verify(medicalOrderService).update(payment.getMedicalOrder(), dto.getMedicalOrderDto(), Delegation.APOSTOLES);
        }
    }

    // ──────────── CANCEL ────────────

    @Nested
    @DisplayName("cancel")
    class Cancel {

        @Test
        @DisplayName("Cancela el pago y su orden médica")
        void shouldCancelPaymentAndMedicalOrder() {
            // Given
            authenticateAs(DEFAULT_DELEGATION, "ROLE_USER");
            Payment payment = paymentOf(DEFAULT_DELEGATION);
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

            // When
            paymentService.cancel(1L);

            // Then
            assertThat(payment.getStatus()).isEqualTo(Status.INACTIVE);
            verify(medicalOrderService).cancel(payment.getMedicalOrder());
        }

        @Test
        @DisplayName("Falla si el pago no existe")
        void shouldThrowNotFound_whenPaymentDoesNotExist() {
            //Given
            authenticateAs(DEFAULT_DELEGATION, "ROLE_USER");
            when(paymentRepository.findById(1L)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> paymentService.cancel(1L))
                    .isInstanceOf(EntityNotFoundException.class);

            verifyNoInteractions(medicalOrderService);
        }

        @Test
        @DisplayName("Falla si el pago es de otra delegación")
        void shouldThrowNotFound_whenPaymentBelongsToAnotherDelegation() {
            //Given
            //Asignar diferentes delegaciones
            authenticateAs(Delegation.ALEM, "ROLE_USER");
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(paymentOf(Delegation.APOSTOLES)));

            // When / Then
            assertThatThrownBy(() -> paymentService.cancel(1L))
                    .isInstanceOf(EntityNotFoundException.class);

            verifyNoInteractions(medicalOrderService);
        }

        @Test
        @DisplayName("Falla si el pago ya está inactivo")
        void shouldThrowConflict_whenPaymentIsAlreadyInactive() {
            //Given
            authenticateAs(DEFAULT_DELEGATION, "ROLE_USER");
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(inactivePayment()));

            // When / Then
            assertThatThrownBy(() -> paymentService.cancel(1L))
                    .isInstanceOf(EntityConflictException.class)
                    .hasMessageContaining("inactivo");

            verifyNoInteractions(medicalOrderService);
        }

    }

    // ──────────── HELPERS ────────────

    // USER:
    private void authenticateAs(Delegation delegation, String role) {
        var authentication = new UsernamePasswordAuthenticationToken(
                "12345678", null, List.of(new SimpleGrantedAuthority(role)));
        authentication.setDetails(new AuthenticatedUser(1L, "Juan Pérez", delegation));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    //Entities:
    private Payment paymentOf(Delegation delegation) {
        return Payment.builder()
                .id(1L)
                .date(LocalDate.now())
                .amount(DEFAULT_AMOUNT)
                .status(Status.ACTIVE)
                .affiliateId(DEFAULT_AFFILIATE_ID)
                .delegation(delegation)
                .medicalOrder(MedicalOrder.builder().number(DEFAULT_ORDER_NUMBER).build())
                .build();
    }

    private Payment inactivePayment() {
        Payment payment = paymentOf(DEFAULT_DELEGATION);
        payment.setStatus(Status.INACTIVE);
        return payment;
    }

    private Payment paymentOverThirtyDaysAgo() {
        Payment payment = paymentOf(DEFAULT_DELEGATION);
        payment.setCreatedAt(LocalDateTime.now().minusDays(35));
        return payment;
    }

    private void stubSuccessfulCreation() {
        when(affiliateClient.isActive(DEFAULT_AFFILIATE_ID)).thenReturn(true);
        when(medicalOrderService.buildAndValidate(any(), any())).thenReturn(new MedicalOrder());
        when(paymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Payment capturedPayment() {
        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        return captor.getValue();
    }

    private PaymentRequestDto requestForDelegation(Delegation requestedDelegation) {
        PaymentRequestDto dto = defaultPaymentDto();
        dto.setDelegation(requestedDelegation);
        return dto;
    }

    private PaymentRequestDto requestWithDiscount(Double amount, Integer discount) {
        PaymentRequestDto dto = defaultPaymentDto();
        dto.setAmount(amount);
        dto.setDiscount(discount);
        return dto;
    }

    private PaymentRequestDto defaultPaymentDto(){
        return buildPaymentRequestDto(LocalDate.now(), DEFAULT_AMOUNT, DEFAULT_DISCOUNT, DEFAULT_AFFILIATE_ID, DEFAULT_DELEGATION, defaultMedicalOrderDto());
    }

    private PaymentUpdateDto defaultPaymentUpdateDto() {
        return buildPaymentUpdateDto(LocalDate.now(), DEFAULT_AMOUNT, DEFAULT_DISCOUNT, defaultMedicalOrderDto());
    }

    private MedicalOrderRequestDto defaultMedicalOrderDto(){
        return buildMedicalOrderRequestDto(DEFAULT_ORDER_NUMBER, DEFAULT_MEDICAL_ORDER_TYPE);
    }

    private PaymentRequestDto buildPaymentRequestDto(LocalDate date, Double amount, Integer discount,Long affiliateId, Delegation delegation, MedicalOrderRequestDto medicalOrderDto) {
        PaymentRequestDto dto = new PaymentRequestDto();
        dto.setDate(date);
        dto.setAmount(amount);
        dto.setDiscount(discount);
        dto.setAffiliateId(affiliateId);
        dto.setDelegation(delegation);
        dto.setMedicalOrderDto(medicalOrderDto);
        return dto;
    }

    private PaymentUpdateDto buildPaymentUpdateDto(LocalDate date, Double amount, Integer discount, MedicalOrderRequestDto medicalOrderDto) {
        PaymentUpdateDto dto = new PaymentUpdateDto();
        dto.setDate(date);
        dto.setAmount(amount);
        dto.setDiscount(discount);
        dto.setMedicalOrderDto(medicalOrderDto);
        return dto;
    }

    private MedicalOrderRequestDto buildMedicalOrderRequestDto(Long number, MedicalOrderType medicalOrderType) {
        MedicalOrderRequestDto dto = new MedicalOrderRequestDto();
        dto.setNumber(number);
        dto.setMedicalOrderType(medicalOrderType);
        return dto;
    }

}