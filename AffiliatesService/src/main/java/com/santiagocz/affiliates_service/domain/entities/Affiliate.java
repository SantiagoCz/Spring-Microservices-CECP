package com.santiagocz.affiliates_service.domain.entities;

import com.santiagocz.affiliates_service.domain.enums.AffiliateType;
import com.santiagocz.affiliates_service.domain.enums.RelationType;
import com.santiagocz.affiliates_service.domain.enums.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Check;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(
        name = "affiliates",
        indexes = {
                @Index(name = "idx_affiliate_primary", columnList = "primary_affiliate_id"),
                @Index(name = "idx_affiliate_type", columnList = "affiliate_type")
        }
)
@Check(constraints = """
    (affiliate_type = 'PRIMARY' AND primary_affiliate_id IS NULL AND relation IS NULL) OR
    (affiliate_type = 'DEPENDENT' AND primary_affiliate_id IS NOT NULL AND relation IS NOT NULL)
""")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"primaryAffiliate", "familyMembers"})
@EqualsAndHashCode(exclude = {"primaryAffiliate", "familyMembers"})
public class Affiliate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dni", nullable = false, unique = true)
    private String dni;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    @Enumerated(EnumType.STRING)
    @Column(name = "affiliate_type", nullable = false)
    private AffiliateType affiliateType;

    @Enumerated(EnumType.STRING)
    @Column(name = "relation")
    private RelationType relation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_affiliate_id")
    private Affiliate primaryAffiliate;

    @OneToMany(mappedBy = "primaryAffiliate", fetch = FetchType.LAZY)
    private List<Affiliate> familyMembers;

    // TODO: Auditoría
//    private Long creatorId;
//    private LocalDateTime creationDate;
//    private Long modifierId;
//    private LocalDateTime modificationDate;
//    private Long deleterId;
//    private LocalDateTime deletionDate;
}