package com.hamburguesas.places;

import com.hamburguesas.model.PlacesApiUsage;
import com.hamburguesas.model.PlacesCallType;
import com.hamburguesas.repository.PlacesApiUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;

@Component
@RequiredArgsConstructor
public class PlacesQuotaGuard {

    private final PlacesApiUsageRepository usageRepository;
    private final PlacesProperties properties;

    @Transactional(readOnly = true)
    public boolean canCall(PlacesCallType callType) {
        return used(callType) < limitFor(callType);
    }

    @Transactional(readOnly = true)
    public int used(PlacesCallType callType) {
        return usageRepository.findByYearMonthAndCallType(currentMonth(), callType)
            .map(PlacesApiUsage::getCallCount)
            .orElse(0);
    }

    public int limitFor(PlacesCallType callType) {
        return switch (callType) {
            case SEARCH -> properties.getQuota().getMonthlySearchCalls();
            case LISTA_DE_FOTOS -> properties.getQuota().getMonthlyListaDeFotosCalls();
            case PHOTO -> properties.getQuota().getMonthlyPhotoCalls();
            case RESUMEN -> properties.getQuota().getMonthlyResumenCalls();
            case HORARIO -> properties.getQuota().getMonthlyHorarioCalls();
        };
    }

    /**
     * Recorded in its own transaction so the count survives even if the surrounding
     * sync run later fails: the call was already made and already billed.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(PlacesCallType callType) {
        PlacesApiUsage usage = usageRepository
            .findByYearMonthAndCallType(currentMonth(), callType)
            .orElseGet(() -> PlacesApiUsage.builder()
                .yearMonth(currentMonth())
                .callType(callType)
                .callCount(0)
                .build());

        usage.setCallCount(usage.getCallCount() + 1);
        usageRepository.save(usage);
    }

    /** El mes que se está contando, como "2026-10": las cuotas de Google arrancan de cero el 1. */
    public String mesEnCurso() {
        return currentMonth();
    }

    private String currentMonth() {
        return YearMonth.now().toString();
    }
}
