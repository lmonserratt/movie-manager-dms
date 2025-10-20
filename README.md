# 🎬 Movie Manager DMS  
**Phase 2 — Testing & Build Automation (JUnit 5 + Maven)**  

A console-based **Data Management System (DMS)** built in Java to manage movie records.  
This project demonstrates object-oriented design, data validation, CRUD operations, file loading (CSV), and automated testing with JUnit 5.  

---

## 📖 Features

✅ Add, remove, and update movies  
✅ Load movie data from a CSV file  
✅ Validate input (year, duration, rating, etc.)  
✅ Calculate the **average duration** of all movies  
✅ Fully tested with **JUnit 5**  
✅ Built and packaged with **Maven**  

---

## 🏗️ Project Structure

movie-manager-dms/
├── pom.xml # Maven project file
├── src/
│ ├── main/
│ │ ├── java/dms/app/ # Main CLI application
│ │ ├── java/dms/model/ # Movie entity class
│ │ ├── java/dms/service/ # MovieManager logic (CRUD, CSV, validation)
│ │ └── resources/ # CSV sample data
│ └── test/
│ ├── java/dms/model/ # Year validation unit tests
│ └── java/dms/service/ # CRUD and CSV tests
└── target/
└── movie-manager-dms-1.0.0.jar # Executable JAR file

---

## ⚙️ How to Run (Maven Required)

### 🧩 Option 1 — Run the program from source

```bash
mvn clean package
java -cp target/classes dms.app.Main

🧱 Option 2 — Run the packaged JAR
java -jar target/movie-manager-dms-1.0.0.jar


💡 When prompted for a CSV file, simply press ENTER to auto-load movies_sample.csv.

🧪 Run All Tests

This project uses JUnit 5 for automated testing.

mvn test


You should see:

Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

🗂️ Sample CSV File (movies_sample.csv)

AVA2015,Avatar,James Cameron,2009,162,Science Fiction,8.0
INT2010,Inception,Christopher Nolan,2010,148,Science Fiction,9.0
GOD1972,The Godfather,Francis Ford Coppola,1972,175,Crime,9.2
...

📦 Build Output

After building successfully with Maven, the final files are located in:

target/movie-manager-dms-1.0.0.jar


You can double-click this JAR or run it via terminal.

🎥 Demo Video

Watch the full project demo here:
👉 [YouTube Video Presentation](https://youtu.be/09_nvZWtt8I)

👨‍💻 Author

Luis Augusto Monserratt Alvarado
Valencia College — COP 3330C / Software Development

GitHub: @lmonserratt

Email: lmonserrattalvara@mail.valenciacollege.edu

🏁 Version

v1.0.0 — Phase 2 (Testing & JAR Packaging)

Added JUnit 5 unit tests

Implemented year validation (<= currentYear + 1)

Fixed exit confirmation (“y” or “yes”)

Packaged Maven project into an executable JAR
