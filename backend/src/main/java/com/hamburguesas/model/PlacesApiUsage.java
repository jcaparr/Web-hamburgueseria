package com.hamburguesas.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Our own counter of Google Places calls, so the sync job stops itself before it can
 * reach the paid tier instead of relying only on the quota configured in Google Cloud.
 */
@Entity
@Table(
    name = "places_api_usage",
    uniqueConstraints = @UniqueConstraint(columnNames = {"year_month", "call_type"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlacesApiUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Format: 2026-09 */
    @Column(name = "year_month", nullable = false, length = 7)
    private String yearMonth;

    @Enumerated(EnumType.STRING)
    @Column(name = "call_type", nullable = false, length = 20)
    private PlacesCallType callType;

    @Column(name = "call_count", nullable = false)
    private int callCount;
}
