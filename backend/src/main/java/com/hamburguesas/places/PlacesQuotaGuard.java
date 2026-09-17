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
        return callType == PlacesCallType.PHOTO
            ? properties.getQuota().getMonthlyPhotoCalls()
            : properties.getQuota().getMonthlySearchCalls();
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

    private String currentMonth() {
        return YearMonth.now().toString();
    }
}
