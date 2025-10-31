package com.nba.standings.util;

import com.nba.standings.exception.InvalidDateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Month;

/**
 * Utility component for NBA season date operations.
 * Provides methods to determine season boundaries and validate dates.
 */
@Component
public class SeasonDateUtility {
    
    private static final Logger logger = LoggerFactory.getLogger(SeasonDateUtility.class);
    
    /**
     * Determine the season start date based on a given date.
     * NBA season starts on October 1st.
     * If the given date is before October, the season started in the previous year.
     * 
     * @param date the reference date
     * @return the season start date (October 1st of the appropriate year)
     */
    public LocalDate determineSeasonStart(LocalDate date) {
        int year = date.getYear();
        
        // If the date is before October (months 1-9), the season started in the previous year
        if (date.getMonth().getValue() < Month.OCTOBER.getValue()) {
            year = year - 1;
        }
        
        return LocalDate.of(year, Month.OCTOBER, 1);
    }
    
    /**
     * Validate that the date is within the current NBA season.
     * NBA season starts on October 1st and ends around June of the following year.
     * 
     * @param date the date to validate
     * @throws InvalidDateException if the date is before the current season start or in the future
     */
    public void validateDateWithinCurrentSeason(LocalDate date) {
        logger.info("SEASON_UTIL: Validating date: {}", date);
        LocalDate today = LocalDate.now();
        logger.info("SEASON_UTIL: Today's date: {}", today);
        
        // Date cannot be in the future
        if (date.isAfter(today)) {
            logger.error("SEASON_UTIL: Date {} is after today {}", date, today);
            throw new InvalidDateException("Date cannot be in the future");
        }
        logger.info("SEASON_UTIL: Date is not in the future - OK");
        
        // Determine the current season start date
        LocalDate seasonStart = determineSeasonStart(today);
        logger.info("SEASON_UTIL: Current season start date: {}", seasonStart);
        
        // Date must be on or after the season start
        if (date.isBefore(seasonStart)) {
            logger.error("SEASON_UTIL: Date {} is before season start {}", date, seasonStart);
            throw new InvalidDateException("Date must be within the current NBA season (on or after " + seasonStart + ")");
        }
        logger.info("SEASON_UTIL: Date is within current season - OK");
    }
}
