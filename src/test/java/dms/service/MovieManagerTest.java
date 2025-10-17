// File: src/test/java/dms/service/MovieManagerTest.java
package dms.service;

import dms.model.Movie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MovieManager covering:
 * - Opening/Loading a CSV file (positive and negative)
 * - Adding objects (Create)
 * - Removing objects (Delete)
 * - Updating objects (Update) with validation and rollback
 * - Custom action (average duration)
 *
 * These tests simulate database-like operations in memory, aligned with Phase 2 requirements.
 */
public class MovieManagerTest {

    @TempDir
    Path tempDir;

    // ---------- Open File (CSV) ----------
    @Test
    void loadCsv_success_readsValidRows() throws IOException {
        // Arrange: create a temp CSV with valid rows
        String csv = String.join("\n",
                "AVA2015,Avatar,James Cameron,2009,162,Science Fiction,8.0",
                "INT2010,Inception,Christopher Nolan,2010,148,Science Fiction,9.0",
                "GOD1972,The Godfather,Francis Ford Coppola,1972,175,Crime,9.2"
        );
        Path file = tempDir.resolve("movies_ok.csv");
        Files.writeString(file, csv);

        MovieManager mgr = new MovieManager();

        // Act
        MovieManager.LoadReport rep = mgr.loadCsv(file.toString());

        // Assert
        assertEquals(3, rep.loaded, "Expected to load 3 valid rows");
        assertEquals(0, rep.errors.size(), "No errors expected for well-formed CSV");
        assertEquals(3, mgr.all().size(), "Manager should contain 3 movies");
    }

    @Test
    void loadCsv_handlesBadNumbers_andContinues() throws IOException {
        // Arrange: mix of valid/invalid lines (bad year/duration/rating)
        String csv = String.join("\n",
                "OKK2000,Ok Movie,Dir,2000,100,Drama,7.5",
                "BADYEAR,BadY,Dir,abcd,120,Drama,8.0", // bad year
                "BADDUR,BadD,Dir,2001,xx,Drama,8.0",   // bad duration
                "BADRAT,BadR,Dir,2002,110,Drama,xx",   // bad rating
                "DUP2005,DuplicateID,Dir,2005,95,Action,7.0",
                "DUP2005,DuplicateIDAgain,Dir,2006,100,Action,8.0" // duplicate ID
        );
        Path file = tempDir.resolve("movies_mixed.csv");
        Files.writeString(file, csv);

        MovieManager mgr = new MovieManager();

        // Act
        MovieManager.LoadReport rep = mgr.loadCsv(file.toString());

        // Assert: only the first and the first duplicate row's ID should load once (duplicate rejected)
        assertEquals(2, rep.loaded, "Expected to load 2 rows: OKK2000 and DUP2005 once");
        assertTrue(rep.errors.size() >= 3, "Expect several errors for bad numerics and duplicate");
        assertEquals(2, mgr.all().size(), "Manager should contain 2 valid movies");
    }

    @Test
    void loadCsv_fileNotFound_returnsErrorAndZeroLoaded() {
        MovieManager mgr = new MovieManager();
        MovieManager.LoadReport rep = mgr.loadCsv("not_exists_123.csv");
        assertEquals(0, rep.loaded);
        assertFalse(rep.errors.isEmpty(), "Should report 'file not found' error");
    }

    // ---------- Create (Add) ----------
    @Test
    void addMovie_success_whenValidAndUnique() {
        MovieManager mgr = new MovieManager();
        Movie m = new Movie("ID1", "Inception", "Christopher Nolan", 2010, 148, "Science Fiction", 9.0);
        assertTrue(mgr.add(m), "Should add a valid, unique movie");
        assertEquals(1, mgr.all().size());
    }

    @Test
    void addMovie_rejectsDuplicateId() {
        MovieManager mgr = new MovieManager();
        Movie a = new Movie("ID1", "A", "Dir", 2000, 100, "Drama", 7.0);
        Movie b = new Movie("ID1", "B", "Dir", 2001, 100, "Drama", 7.5);
        assertTrue(mgr.add(a));
        assertFalse(mgr.add(b), "Duplicate movieID should be rejected");
        assertEquals(1, mgr.all().size());
    }

    // ---------- Delete (Remove) ----------
    @Test
    void removeMovie_nonExisting_returnsFalse_andDoesNotCrash() {
        MovieManager mgr = new MovieManager();
        assertFalse(mgr.remove("NOPE"), "Removing unknown ID should return false without exceptions");
    }

    @Test
    void removeMovie_existing_returnsTrue_andRemoves() {
        MovieManager mgr = new MovieManager();
        Movie m = new Movie("ID2", "Matrix", "Wachowski", 1999, 136, "Science Fiction", 8.7);
        assertTrue(mgr.add(m));
        assertTrue(mgr.remove("ID2"));
        assertTrue(mgr.all().isEmpty(), "List should be empty after removal");
    }

    // ---------- Update ----------
    @Test
    void updateFields_success_forMultipleValidChanges() {
        MovieManager mgr = new MovieManager();
        Movie m = new Movie("U1", "Old", "Dir", 2000, 100, "Drama", 7.0);
        assertTrue(mgr.add(m));

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("title", "New Title");
        fields.put("duration", "123.5");
        fields.put("rating", "8.4");

        assertTrue(mgr.updateFields("U1", fields), "Update should succeed for valid fields");
        Movie updated = mgr.findById("U1").orElseThrow();
        assertEquals("New Title", updated.getTitle());
        assertEquals(123.5, updated.getDurationMinutes(), 0.0001);
        assertEquals(8.4, updated.getRating(), 0.0001);
    }

    @Test
    void updateFields_invalidYear_rollsBackAndReturnsFalse() {
        MovieManager mgr = new MovieManager();
        Movie m = new Movie("U2", "Title", "Dir", 2000, 100, "Drama", 7.0);
        assertTrue(mgr.add(m));

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("title", "ShouldNotStick");
        fields.put("year", "3040"); // invalid per validation

        assertFalse(mgr.updateFields("U2", fields), "Update should fail due to invalid year");
        Movie after = mgr.findById("U2").orElseThrow();

        // Verify rollback: none of the fields should have changed
        assertEquals("Title", after.getTitle(), "Title change must be rolled back");
        assertEquals(2000, after.getYear(), "Year must remain original after rollback");
    }

    // ---------- Custom Action (Average) ----------
    @Test
    void averageDuration_empty_returnsEmptyOptional() {
        MovieManager mgr = new MovieManager();
        OptionalDouble avg = mgr.averageDuration();
        assertTrue(avg.isEmpty(), "Average should be empty when no movies exist");
    }

    @Test
    void averageDuration_returnsCorrectValue() {
        MovieManager mgr = new MovieManager();
        assertTrue(mgr.add(new Movie("A", "T1", "D1", 2000, 100, "G", 7.0)));
        assertTrue(mgr.add(new Movie("B", "T2", "D2", 2001, 200, "G", 8.0)));
        OptionalDouble avg = mgr.averageDuration();
        assertTrue(avg.isPresent());
        assertEquals(150.0, avg.getAsDouble(), 0.0001, "Average of 100 and 200 should be 150.0");
    }
}
