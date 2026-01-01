package com.norrisjackson.jsnippets.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TimezoneServiceTest {

    private TimezoneService timezoneService;
    private Instant testInstant;

    @BeforeEach
    void setUp() {
        timezoneService = new TimezoneService();
        
        // Create a fixed test instant: 2024-12-01 15:30:00 UTC
        LocalDateTime utcDateTime = LocalDateTime.of(2024, 12, 1, 15, 30, 0);
        ZonedDateTime utcZonedDateTime = utcDateTime.atZone(ZoneId.of("UTC"));
        testInstant = utcZonedDateTime.toInstant();
    }

    @Test
    void formatDateInUserTimezone_WithUTCTimezone_ReturnsCorrectFormat() {
        // When
        String result = timezoneService.formatDateInUserTimezone(testInstant, "UTC", "MMM dd, yyyy h:mm a");

        // Then
        assertThat(result).isEqualTo("Dec 01, 2024 3:30 PM");
    }

    @Test
    void formatDateInUserTimezone_WithNewYorkTimezone_ReturnsCorrectFormat() {
        // When (UTC-5 in December, so 15:30 UTC = 10:30 EST)
        String result = timezoneService.formatDateInUserTimezone(testInstant, "America/New_York", "MMM dd, yyyy h:mm a");

        // Then
        assertThat(result).isEqualTo("Dec 01, 2024 10:30 AM");
    }

    @Test
    void formatDateInUserTimezone_WithLondonTimezone_ReturnsCorrectFormat() {
        // When (UTC+0 in December, so same time)
        String result = timezoneService.formatDateInUserTimezone(testInstant, "Europe/London", "MMM dd, yyyy h:mm a");

        // Then
        assertThat(result).isEqualTo("Dec 01, 2024 3:30 PM");
    }

    @Test
    void formatDateInUserTimezone_WithTokyoTimezone_ReturnsCorrectFormat() {
        // When (UTC+9, so 15:30 UTC = 00:30 next day)
        String result = timezoneService.formatDateInUserTimezone(testInstant, "Asia/Tokyo", "MMM dd, yyyy h:mm a");

        // Then
        assertThat(result).isEqualTo("Dec 02, 2024 12:30 AM");
    }

    @Test
    void formatDateInUserTimezone_WithDefaultPattern_ReturnsDefaultFormat() {
        // When
        String result = timezoneService.formatDateInUserTimezone(testInstant, "UTC");

        // Then
        assertThat(result).isEqualTo("Dec 01, 2024 3:30 PM");
    }

    @Test
    void formatDateInUserTimezone_WithNullDate_ReturnsEmptyString() {
        // When
        String result = timezoneService.formatDateInUserTimezone((Instant) null, "UTC", "MMM dd, yyyy");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void formatDateInUserTimezone_WithInvalidTimezone_FallsBackToUTC() {
        // When
        String result = timezoneService.formatDateInUserTimezone(testInstant, "Invalid/Timezone", "MMM dd, yyyy h:mm a");

        // Then
        assertThat(result).isEqualTo("Dec 01, 2024 3:30 PM"); // UTC time
    }

    @Test
    void formatDateInUserTimezone_WithNullTimezone_FallsBackToUTC() {
        // When
        String result = timezoneService.formatDateInUserTimezone(testInstant, null, "MMM dd, yyyy h:mm a");

        // Then
        assertThat(result).isEqualTo("Dec 01, 2024 3:30 PM"); // UTC time
    }

    @Test
    void getTimezoneDisplayName_WithValidTimezone_ReturnsDisplayName() {
        // When
        String result = timezoneService.getTimezoneDisplayName("America/New_York");

        // Then
        assertThat(result).matches("America/New_York \\(UTC[+-]\\d{2}:\\d{2}\\)");
    }

    @Test
    void getTimezoneDisplayName_WithUTC_ReturnsDisplayName() {
        // When
        String result = timezoneService.getTimezoneDisplayName("UTC");

        // Then
        assertThat(result).isEqualTo("UTC (UTC+00:00)");
    }

    @Test
    void getTimezoneDisplayName_WithNullTimezone_ReturnsUTC() {
        // When
        String result = timezoneService.getTimezoneDisplayName(null);

        // Then
        assertThat(result).isEqualTo("UTC");
    }

    @Test
    void getTimezoneDisplayName_WithEmptyTimezone_ReturnsUTC() {
        // When
        String result = timezoneService.getTimezoneDisplayName("");

        // Then
        assertThat(result).isEqualTo("UTC");
    }

    @Test
    void getTimezoneDisplayName_WithInvalidTimezone_ReturnsOriginalString() {
        // When
        String result = timezoneService.getTimezoneDisplayName("Invalid/Timezone");

        // Then
        assertThat(result).isEqualTo("Invalid/Timezone");
    }
}