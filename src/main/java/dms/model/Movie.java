package dms.model;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;

/**
 * Movie entity for Phase 1/2.
 * - Keeps data only in memory (no DB, no GUI).
 * - Includes validation so bad data never enters the system.
 * - Year is validated in the range [1888, currentYear+1].
 * - Setters return boolean to be easily unit-testable (avoid void + hidden state).
 */
public class Movie {
    private String movieID;          // unique (not title)
    private String title;
    private String director;
    private int year;                // 1888..(currentYear+1)
    private double durationMinutes;  // > 0
    private String genre;
    private double rating;           // 1.0..10.0

    public Movie(String movieID, String title, String director, int year,
                 double durationMinutes, String genre, double rating) {
        this.movieID = movieID;
        this.title = title;
        this.director = director;
        this.year = year;
        this.durationMinutes = durationMinutes;
        this.genre = genre;
        this.rating = rating;
    }

    /** Returns a list of validation errors (empty list means valid). */
    public List<String> validate() {
        List<String> errs = new ArrayList<>();
        if (isBlank(movieID)) errs.add("movieID required");
        if (isBlank(title)) errs.add("title required");
        if (isBlank(director)) errs.add("director required");
        if (year < 1888) errs.add("year must be >= 1888");
        if (year > maxAllowedYear()) errs.add("year must be <= " + maxAllowedYear());
        if (durationMinutes <= 0) errs.add("duration must be > 0");
        if (isBlank(genre)) errs.add("genre required");
        if (rating < 1.0 || rating > 10.0) errs.add("rating must be 1.0..10.0");
        return errs;
    }

    // ---------- Setters return boolean for easy testing (avoid void) ----------
    public boolean setTitle(String v){
        if(isBlank(v)) return false;
        title = v.trim();
        return true;
    }
    public boolean setDirector(String v){
        if(isBlank(v)) return false;
        director = v.trim();
        return true;
    }
    public boolean setYear(int v){
        if (v < 1888 || v > maxAllowedYear()) return false;
        year = v;
        return true;
    }
    public boolean setDurationMinutes(double v){
        if(v <= 0) return false;
        durationMinutes = v;
        return true;
    }
    public boolean setGenre(String v){
        if(isBlank(v)) return false;
        genre = v.trim();
        return true;
    }
    public boolean setRating(double v){
        if(v < 1.0 || v > 10.0) return false;
        rating = v;
        return true;
    }

    // ---------- Getters ----------
    public String getMovieID(){ return movieID; }
    public String getTitle(){ return title; }
    public String getDirector(){ return director; }
    public int getYear(){ return year; }
    public double getDurationMinutes(){ return durationMinutes; }
    public String getGenre(){ return genre; }
    public double getRating(){ return rating; }

    @Override public String toString(){
        return String.format("%s | %s | %s | %d | %.1f min | %s | %.1f",
                movieID,title,director,year,durationMinutes,genre,rating);
    }

    // ---------- Helpers ----------
    private boolean isBlank(String s){ return s==null || s.trim().isEmpty(); }

    /** Upper bound for movie year: current year + 1 (allows early releases). */
    private static int maxAllowedYear() {
        return Year.now().getValue() + 1;
    }
}
