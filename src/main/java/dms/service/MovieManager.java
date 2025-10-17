package dms.service;

import dms.model.Movie;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * MovieManager:
 * - In-memory list only (Phase 1/2).
 * - CRUD operations.
 * - Bulk (multi-field) update with rollback on validation failure.
 * - CSV load (id,title,director,year,duration,genre,rating).
 * - Custom action: average duration (minutes).
 *
 * Design for testability:
 * - Methods return values (boolean/Optional/OptionalDouble/etc.) instead of void.
 * - No direct Scanner usage here; CLI prompts live in the App class.
 */
public class MovieManager {
    private final List<Movie> movies = new ArrayList<>();

    // ---------- CRUD ----------
    /** Adds a movie if valid and non-duplicated. Returns true on success. */
    public boolean add(Movie m){
        if(!m.validate().isEmpty()) return false;
        if(findById(m.getMovieID()).isPresent()) return false;
        movies.add(m);
        return true;
    }

    /** Finds a movie by case-insensitive movieID. */
    public Optional<Movie> findById(String id){
        return movies.stream().filter(x->x.getMovieID().equalsIgnoreCase(id)).findFirst();
    }

    /** Returns an unmodifiable view of the internal list. */
    public List<Movie> all(){ return Collections.unmodifiableList(movies); }

    /** Removes a movie by ID; returns true if found & removed. */
    public boolean remove(String id){
        return findById(id).map(movies::remove).orElse(false);
    }

    /** Single-field update. Preferred to use updateFields for multi-field atomic updates. */
    public boolean updateField(String id, String field, String value){
        Map<String,String> map = new LinkedHashMap<>();
        map.put(field, value);
        return updateFields(id, map);
    }

    /**
     * Multi-field update with validation and rollback.
     * Supported keys: title, director, year, duration, genre, rating
     * - Parses input values defensively.
     * - On any failure, fully rolls back and returns false.
     */
    public boolean updateFields(String id, Map<String,String> fields){
        Optional<Movie> opt = findById(id);
        if (opt.isEmpty()) return false;

        Movie m = opt.get();

        // Save old state to rollback on failure
        String ot = m.getTitle();
        String od = m.getDirector();
        int    oy = m.getYear();
        double ou = m.getDurationMinutes();
        String og = m.getGenre();
        double orat= m.getRating();

        boolean ok = true;
        try {
            for (var e : fields.entrySet()) {
                String k = e.getKey().toLowerCase();
                String v = e.getValue();
                switch (k) {
                    case "title":    ok &= m.setTitle(v); break;
                    case "director": ok &= m.setDirector(v); break;
                    case "year":     ok &= m.setYear(Integer.parseInt(v.trim())); break;
                    case "duration": ok &= m.setDurationMinutes(Double.parseDouble(v.trim())); break;
                    case "genre":    ok &= m.setGenre(v); break;
                    case "rating":   ok &= m.setRating(Double.parseDouble(v.trim())); break;
                    default: ok = false; // unsupported field
                }
                if (!ok) break;
            }
        } catch (Exception ex) {
            ok = false;
        }

        if (!ok) {
            // rollback
            m.setTitle(ot);
            m.setDirector(od);
            m.setYear(oy);
            m.setDurationMinutes(ou);
            m.setGenre(og);
            m.setRating(orat);
        }
        return ok;
    }

    // ---------- Custom Action ----------
    /** Returns the average duration in minutes, or empty if no movies loaded. */
    public OptionalDouble averageDuration(){
        return movies.isEmpty()
                ? OptionalDouble.empty()
                : OptionalDouble.of(movies.stream().mapToDouble(Movie::getDurationMinutes).average().orElse(0));
    }

    // ---------- Simple ID helper ----------
    /**
     * Generates a semi-stable ID based on title letters and year.
     * If collision, appends a numeric suffix.
     */
    public String generateID(String title, int year){
        String letters = title==null? "XXX" : title.replaceAll("[^A-Za-z]","").toUpperCase();
        letters = (letters + "XXX").substring(0,3);
        String base = letters + year;
        String id = base;
        int i=1;
        while(findById(id).isPresent()) id = base + "-" + i++;
        return id;
    }

    // ---------- CSV Loader ----------
    /**
     * Loads movies from CSV file (id,title,director,year,duration,genre,rating).
     * - Defensive parsing and validation reporting.
     * - Does not throw: accumulates a report with loaded count and error messages.
     */
    public LoadReport loadCsv(String path){
        LoadReport rep = new LoadReport();
        try{
            Path p = Path.of(path);
            if(!Files.exists(p) || Files.isDirectory(p)){
                rep.errors.add("File not found: "+path);
                return rep;
            }
            try(BufferedReader br = new BufferedReader(new FileReader(p.toFile()))){
                String line; int n=1; // line counter starts at 1
                while((line=br.readLine())!=null){
                    if(line.isBlank()) { n++; continue; }
                    String[] a = line.split(",", -1);
                    if(a.length<7){
                        rep.errors.add("Bad columns at line "+n+" (expected 7)");
                        n++; continue;
                    }
                    String id=a[0].trim(), title=a[1].trim(), dir=a[2].trim(), genre=a[5].trim();
                    Integer year = tryInt(a[3]);
                    Double  dur  = tryDbl(a[4]);
                    Double  rat  = tryDbl(a[6]);

                    if(year==null || dur==null || rat==null){
                        rep.errors.add("Bad numeric values at line "+n);
                        n++; continue;
                    }

                    Movie m = new Movie(id,title,dir,year,dur,genre,rat);

                    // Extra: show validation reasons if add fails
                    List<String> valErrs = m.validate();
                    if (!valErrs.isEmpty()) {
                        rep.errors.add("Validation failed at line "+n+": " + String.join("; ", valErrs));
                        n++; continue;
                    }

                    if (add(m)) rep.loaded++;
                    else rep.errors.add("Add failed at line "+n+" (duplicate ID or invalid data).");

                    n++;
                }
            }
        }catch(Exception e){
            rep.errors.add("Unexpected: "+e.getMessage());
        }
        return rep;
    }

    // ---------- Helper types ----------
    public static class LoadReport{
        public int loaded=0;
        public final List<String> errors = new ArrayList<>();
        public String summary(){ return "Loaded="+loaded+", Errors="+errors.size(); }
        @Override public String toString(){ return summary() + (errors.isEmpty() ? "" : " " + errors.stream().collect(Collectors.joining(" | "))); }
    }

    // ---------- Parse helpers ----------
    private Integer tryInt(String s){ try{ return Integer.parseInt(s.trim()); }catch(Exception e){ return null; } }
    private Double tryDbl(String s){ try{ return Double.parseDouble(s.trim()); }catch(Exception e){ return null; } }
}
