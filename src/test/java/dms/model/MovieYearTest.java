// File: src/test/java/dms/model/MovieYearTest.java
package dms.model;

import org.junit.jupiter.api.Test;

import java.time.Year;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests focused on validating the movie year rules:
 * - Lower bound: 1888
 * - Upper bound: currentYear + 1 (allows early releases)
 * Includes positive and negative cases for both constructor+validate() and setter behavior.
 */
public class MovieYearTest {

    @Test
    void year_rejectsFarFuture_like3040_viaValidateAndSetter() {
        int tooHigh = 3040;
        Movie m = new Movie("ID", "Title", "Director", tooHigh, 120.0, "Genre", 8.0);

        // validate() should contain at least one error (upper bound)
        List<String> errs = m.validate();
        assertFalse(errs.isEmpty(), "Validation should fail for year far in the future");
        assertTrue(errs.stream().anyMatch(e -> e.contains("year must be <=")),
                "Validation errors should mention the upper bound");

        // setter should reject too-high year
        assertFalse(m.setYear(tooHigh), "Setter must reject a year far beyond the upper limit");
    }

    @Test
    void year_rejectsTooLow_below1888_viaValidateAndSetter() {
        int tooLow = 1700;
        Movie m = new Movie("ID", "Title", "Director", tooLow, 120.0, "Genre", 8.0);

        List<String> errs = m.validate();
        assertFalse(errs.isEmpty(), "Validation should fail for year below 1888");
        assertTrue(errs.stream().anyMatch(e -> e.contains("year must be >=")),
                "Validation errors should mention the lower bound");

        assertFalse(m.setYear(tooLow), "Setter must reject a year below 1888");
    }

    @Test
    void year_acceptsCurrentPlusOne() {
        int ok = Year.now().getValue() + 1;
        Movie m = new Movie("ID", "Title", "Director", ok, 120.0, "Genre", 8.0);

        assertTrue(m.validate().isEmpty(), "Validation should pass for currentYear+1");
        assertTrue(m.setYear(ok), "Setter should accept currentYear+1");
        assertEquals(ok, m.getYear());
    }

    @Test
    void year_acceptsLowerBound1888() {
        Movie m = new Movie("ID", "Title", "Director", 1888, 120.0, "Genre", 8.0);
        assertTrue(m.validate().isEmpty(), "Validation should pass for 1888 (lower bound)");
        assertTrue(m.setYear(1888), "Setter should accept 1888");
        assertEquals(1888, m.getYear());
    }
}
