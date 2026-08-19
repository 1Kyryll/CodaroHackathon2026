package com.example.hackathoncodaro2026;

import com.example.hackathoncodaro2026.model.SportResource;
import com.example.hackathoncodaro2026.model.enums.ReservationKind;
import com.example.hackathoncodaro2026.model.enums.ResourceType;
import com.example.hackathoncodaro2026.service.PricingService;
import com.example.hackathoncodaro2026.service.impl.PricingServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class PricingServiceTests {

    private final PricingService pricingService = new PricingServiceImpl();

    @Test
    void weekendEveningTennisCostsMoreThanWeekdayMorning() {
        SportResource tennis = tennis();
        BigDecimal weekdayMorning = pricingService.quote(tennis, LocalDate.of(2026, 8, 17), LocalTime.of(10, 0), 1);
        BigDecimal weekendEvening = pricingService.quote(tennis, LocalDate.of(2026, 8, 22), LocalTime.of(18, 0), 1);
        assertThat(weekdayMorning).isEqualByComparingTo("80.00");
        assertThat(weekendEvening).isEqualByComparingTo("135.00");
        assertThat(weekendEvening).isGreaterThan(weekdayMorning);
    }

    @Test
    void twoHourQuoteSumsDaytimeAndEveningHours() {
        SportResource tennis = tennis();
        LocalDate monday = LocalDate.of(2026, 8, 17);
        BigDecimal daytime = pricingService.hourlyRate(tennis, monday, LocalTime.of(16, 0));
        BigDecimal evening = pricingService.hourlyRate(tennis, monday, LocalTime.of(17, 0));
        BigDecimal twoHours = pricingService.quote(tennis, monday, LocalTime.of(16, 0), 2);
        assertThat(daytime).isEqualByComparingTo("80.00");
        assertThat(evening).isEqualByComparingTo("108.00");
        assertThat(twoHours).isEqualByComparingTo(daytime.add(evening));
        assertThat(twoHours).isEqualByComparingTo("188.00");
    }

    @Test
    void partySizeLabelUsesPlusOnLastGroupOption() {
        SportResource tennis = tennis();
        tennis.setMinPartySize(2);
        tennis.setMaxPartySize(4);
        assertThat(tennis.partySizeLabel(2)).isEqualTo("2");
        assertThat(tennis.partySizeLabel(3)).isEqualTo("3");
        assertThat(tennis.partySizeLabel(4)).isEqualTo("4+");
        SportResource gym = new SportResource();
        gym.setType(ResourceType.GYM);
        gym.setMinPartySize(1);
        gym.setMaxPartySize(1);
        gym.setLessonMinPartySize(2);
        gym.setLessonMaxPartySize(20);
        gym.setCapacity(20);
        assertThat(gym.partySizeLabel(1)).isEqualTo("1");
        assertThat(gym.partySizeLabel(6, ReservationKind.LESSON)).isEqualTo("6");
        assertThat(gym.partySizeLabel(20, ReservationKind.LESSON)).isEqualTo("20");
    }

    private SportResource tennis() {
        SportResource resource = new SportResource();
        resource.setType(ResourceType.TENNIS);
        resource.setBaseHourlyPrice(ResourceType.TENNIS.getBaseHourlyPrice());
        resource.setMinPartySize(ResourceType.TENNIS.getMinPartySize());
        resource.setMaxPartySize(ResourceType.TENNIS.getMaxPartySize());
        return resource;
    }
}
