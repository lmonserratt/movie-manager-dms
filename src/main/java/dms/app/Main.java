package dms.app;

import dms.model.Movie;
import dms.service.MovieManager;

import java.time.Year;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Scanner;

/**
 * Main class for Phase 1/2 of the Movie Manager DMS.
 *
 * Features:
 *  - Menu-based CLI
 *  - CRUD operations
 *  - Safe input validation (never crashes on bad input)
 *  - Custom action (average duration)
 *  - Load CSV by file path OR PASTE lines
 *  - Auto-detect default CSV (movies_sample.csv) from multiple relative locations
 *  - Update multiple fields in one step
 *
 * Phase-2 readiness:
 *  - Input collection remains here (UI), logic in MovieManager (service layer).
 *  - Methods return values to support unit testing.
 *  - Exit confirmation accepts 'y' or 'yes'.
 *  - Year prompts reflect [1888..currentYear+1].
 */
public class Main { // Only static method allowed
    public static void main(String[] args){
        new App().run();
    }
}

/** Handles all program flow and user interaction (CLI). */
class App {
    private final Scanner in = new Scanner(System.in);
    private final MovieManager mgr = new MovieManager();
    private boolean running = true;

    /** Main loop. */
    public void run(){
        println("=== Movie Manager DMS (Phase 1/2) ===");
        while(running){
            menu();
            String op = ask("Choose (1-7): ");
            switch(op){
                case "1": loadFile(); break;
                case "2": showAll(); break;
                case "3": createManual(); break;
                case "4": removeById(); break;
                case "5": updateMultipleFields(); break;
                case "6": showAverage(); break;
                case "7": if (confirmExit()) running = false; break; // <-- FIX: exit on 'y'/'yes'
                default: println("Invalid option. Try again.");
            }
        }
        println("Bye!");
    }

    /** Prints the main menu. */
    private void menu(){
        println("");
        println("1) Load CSV file");
        println("2) Display all movies");
        println("3) Create (manual add)");
        println("4) Remove by MovieID");
        println("5) Update multiple fields");
        println("6) Custom: Average duration");
        println("7) Exit");
    }

    /**
     * Loads movies from CSV.
     * - If user presses ENTER, auto-detects 'movies_sample.csv' from several common relative locations.
     * - If user types 'PASTE', allows pasting CSV lines (finish with empty line).
     * CSV format: id,title,director,year,duration,genre,rating
     */
    private void loadFile(){
        String path = ask("CSV path OR type PASTE to paste lines (press ENTER for default 'movies_sample.csv'): ").trim();

        // --- Auto-detect default CSV if user pressed ENTER ---
        if (path.isEmpty()) {
            String[] guesses = {
                    // same folder and typical project root
                    "movies_sample.csv",
                    "src/movies_sample.csv",
                    // one level up (e.g., running from a subfolder)
                    "../movies_sample.csv",
                    "../src/movies_sample.csv",
                    // two levels up (e.g., running jar from out/artifacts)
                    "../../movies_sample.csv",
                    "../../src/movies_sample.csv"
            };
            for (String g : guesses) {
                java.nio.file.Path p = java.nio.file.Path.of(g);
                try {
                    if (java.nio.file.Files.exists(p) && !java.nio.file.Files.isDirectory(p)) {
                        path = g;
                        println("Auto-detected file: " + path);
                        break;
                    }
                } catch (Exception ignored) {}
            }
            if (path.isEmpty()) {
                println("No default CSV found. Please type a valid path or use PASTE mode.");
                return;
            }
        }

        // --- PASTE mode (user pastes lines directly) ---
        if (path.equalsIgnoreCase("PASTE")) {
            println("Paste CSV lines now (id,title,director,year,duration,genre,rating).");
            println("Press ENTER twice to finish.");
            StringBuilder sb = new StringBuilder();
            while (true) {
                String line = ask("");
                if (line.isEmpty()) break; // blank line ends input
                sb.append(line).append("\n");
            }
            try {
                java.nio.file.Path tmp = java.nio.file.Files.createTempFile("movies_paste_", ".csv");
                java.nio.file.Files.writeString(tmp, sb.toString());
                var rep = mgr.loadCsv(tmp.toString());
                println(rep.summary());
                if(!rep.errors.isEmpty()){
                    println("Errors:");
                    rep.errors.forEach(e -> println(" - " + e));
                }
                java.nio.file.Files.deleteIfExists(tmp); // cleanup temp file
            } catch (Exception e) {
                println("Unexpected error: " + e.getMessage());
            }
            return; // done
        }

        // --- Normal path mode ---
        var rep = mgr.loadCsv(path);
        println(rep.summary());
        if(!rep.errors.isEmpty()){
            println("Errors:");
            rep.errors.forEach(e -> println(" - " + e));
        }
    }

    /** Prints all movies. */
    private void showAll(){
        var all = mgr.all();
        if(all.isEmpty()) println("(no movies)");
        else all.forEach(m -> println(m.toString()));
    }

    /** Creates a new movie via CLI with validation. */
    private void createManual(){
        String title = askNonEmpty("Title: ");
        String director = askNonEmpty("Director: ");
        int year = askInt("Year (1888.." + maxYear() + "): ", 1888, maxYear());
        double dur = askDouble("Duration minutes (>0): ", 0.000001, Double.MAX_VALUE);
        String genre = askNonEmpty("Genre: ");
        double rating = askDouble("Rating (1..10): ", 1.0, 10.0);

        String id = mgr.generateID(title, year);
        Movie m = new Movie(id, title, director, year, dur, genre, rating);
        println(mgr.add(m) ? "Added: " + m : "Add failed (validation or duplicate).");
    }

    /** Removes a movie by MovieID. */
    private void removeById(){
        String id = askNonEmpty("MovieID to remove: ");
        println(mgr.remove(id) ? "Deleted." : "Not found.");
    }

    /**
     * Updates MULTIPLE fields in a single step.
     * User can skip any field by pressing ENTER.
     */
    private void updateMultipleFields(){
        String id = askNonEmpty("MovieID to update: ");
        if (mgr.findById(id).isEmpty()) {
            println("Not found.");
            return;
        }

        println("Leave empty to skip a field. Available: title | director | year | duration | genre | rating");

        Map<String,String> fields = new LinkedHashMap<>();
        String t  = ask("New Title: ");
        String d  = ask("New Director: ");
        String y  = ask("New Year (1888.." + maxYear() + "): ");
        String du = ask("New Duration minutes (>0): ");
        String g  = ask("New Genre: ");
        String r  = ask("New Rating (1..10): ");

        if(!t.isBlank())  fields.put("title", t);
        if(!d.isBlank())  fields.put("director", d);
        if(!y.isBlank())  fields.put("year", y);
        if(!du.isBlank()) fields.put("duration", du);
        if(!g.isBlank())  fields.put("genre", g);
        if(!r.isBlank())  fields.put("rating", r);

        boolean ok = mgr.updateFields(id, fields);
        println(ok ? "Updated." : "Update failed (validation error).");
        if (ok) mgr.findById(id).ifPresent(m -> println(m.toString()));
    }

    /** Shows average duration or a friendly message if empty. */
    private void showAverage(){
        OptionalDouble avg = mgr.averageDuration();
        println(avg.isPresent() ? String.format("Average duration: %.2f min", avg.getAsDouble())
                : "No movies in the system.");
    }

    // ---------- Safe input helpers ----------
    private String ask(String prompt){
        System.out.print(prompt);
        String s = in.nextLine();
        return (s == null) ? "" : s.trim();
    }
    private String askNonEmpty(String prompt){
        String s;
        do { s = ask(prompt); if(s.isEmpty()) println("Value required."); } while(s.isEmpty());
        return s;
    }
    private int askInt(String prompt, int min, int max){
        while(true){
            try{
                int v = Integer.parseInt(ask(prompt));
                if(v<min || v>max) throw new IllegalArgumentException();
                return v;
            }catch(Exception e){ println("Invalid integer. Try again."); }
        }
    }
    private double askDouble(String prompt, double min, double max){
        while(true){
            try{
                double v = Double.parseDouble(ask(prompt));
                if(v<min || v>max) throw new IllegalArgumentException();
                return v;
            }catch(Exception e){ println("Invalid number. Try again."); }
        }
    }

    /** Exit confirmation accepts 'y' or 'yes' (case-insensitive). */
    private boolean confirmExit(){
        String s = ask("Exit? (y/n): ").trim();
        return s.equalsIgnoreCase("y") || s.equalsIgnoreCase("yes");
        // If you'd like to accept anything starting with 'y': return s.toLowerCase().startsWith("y");
    }

    /** Upper bound for year prompts: current year + 1 (allows early releases). */
    private int maxYear(){
        return Year.now().getValue() + 1;
    }

    private void println(String s){ System.out.println(s); }
}
