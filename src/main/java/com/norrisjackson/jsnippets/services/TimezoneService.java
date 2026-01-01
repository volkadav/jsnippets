package com.norrisjackson.jsnippets.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class TimezoneService {

    /**
     * Converts a UTC Instant to the user's timezone and formats it
     * @param utcInstant The instant in UTC (from database)
     * @param userTimezone The user's preferred timezone (IANA ID like "America/New_York")
     * @param pattern The formatting pattern (e.g., "MMM dd, yyyy HH:mm")
     * @return Formatted date string in user's timezone
     */
    public String formatDateInUserTimezone(Instant utcInstant, String userTimezone, String pattern) {
        if (utcInstant == null) {
            return "";
        }
        
        try {
            // Get the user's timezone, fallback to UTC if invalid
            ZoneId zoneId;
            try {
                zoneId = ZoneId.of(userTimezone != null ? userTimezone : "UTC");
            } catch (Exception e) {
                log.warn("Invalid timezone '{}', falling back to UTC", userTimezone);
                zoneId = ZoneId.of("UTC");
            }
            
            // Convert to user's timezone
            ZonedDateTime zonedDateTime = utcInstant.atZone(zoneId);

            // Format using the provided pattern
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            return zonedDateTime.format(formatter);
            
        } catch (Exception e) {
            log.error("Error formatting instant {} with timezone {} and pattern {}",
                     utcInstant, userTimezone, pattern, e);
            // Fallback to simple string representation
            return utcInstant.toString();
        }
    }

    /**
     * Converts a UTC Instant to the user's timezone and formats it with a default pattern
     * @param utcInstant The instant in UTC (from database)
     * @param userTimezone The user's preferred timezone
     * @return Formatted date string like "Dec 01, 2024 3:30 PM"
     */
    public String formatDateInUserTimezone(Instant utcInstant, String userTimezone) {
        return formatDateInUserTimezone(utcInstant, userTimezone, "MMM dd, yyyy h:mm a");
    }

    /**
     * Gets the timezone display name for the user
     * @param userTimezone The user's timezone ID
     * @return Human readable timezone name with offset
     */
    public String getTimezoneDisplayName(String userTimezone) {
        if (userTimezone == null || userTimezone.trim().isEmpty()) {
            return "UTC";
        }
        
        try {
            ZoneId zoneId = ZoneId.of(userTimezone);
            ZonedDateTime now = ZonedDateTime.now(zoneId);
            String offset = now.format(DateTimeFormatter.ofPattern("xxx"));
            return userTimezone + " (UTC" + offset + ")";
        } catch (Exception e) {
            log.warn("Error getting display name for timezone '{}'", userTimezone, e);
            return userTimezone;
        }
    }
}