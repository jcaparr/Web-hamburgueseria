package com.hamburguesas.repository;

import com.hamburguesas.model.PlacesApiUsage;
import com.hamburguesas.model.PlacesCallType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlacesApiUsageRepository extends JpaRepository<PlacesApiUsage, Long> {
    Optional<PlacesApiUsage> findByYearMonthAndCallType(String yearMonth, PlacesCallType callType);
}
