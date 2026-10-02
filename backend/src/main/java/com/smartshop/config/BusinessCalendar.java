package com.smartshop.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Date du jour dans le fuseau de la boutique : les echeances sont des dates locales (Tunisie).
 */
@Component
public class BusinessCalendar {

    private final Clock clock;
    private final ZoneId zone;

    public BusinessCalendar(Clock clock, @Value("${smartshop.business-zone:Africa/Tunis}") ZoneId zone) {
        this.clock = clock;
        this.zone = zone;
    }

    public LocalDate today() {
        return LocalDate.now(clock.withZone(zone));
    }

    /** Date locale (Tunisie) d'un instant. */
    public LocalDate dateOf(Instant instant) {
        return LocalDate.ofInstant(instant, zone);
    }
}
