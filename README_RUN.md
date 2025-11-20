# Running the University Management System

## Prerequisites

1. **Java JDK 17 or higher** (you have Java 24 installed ✓)
2. **JavaFX SDK** - Download from https://openjfx.io/

## Quick Start

### Option 1: Using IntelliJ IDEA (Recommended)

1. Open the project in IntelliJ IDEA
2. Ensure JavaFX is configured in Project Structure → Libraries
3. Right-click on `src/edu/facilities/Main.java`
4. Select "Run 'Main.main()'"

### Option 2: Using Command Line

1. **Download JavaFX SDK:**
   - Go to https://openjfx.io/
   - Download JavaFX SDK for your platform (Windows)
   - Extract to `C:\javafx-sdk-21\` (or update the path in `run.bat`)

2. **Run the application:**
   ```batch
   run.bat
   ```

### Option 3: Manual Compilation and Run

```batch
REM Set JavaFX path (adjust as needed)
set JAVAFX_PATH=C:\javafx-sdk-21\lib

REM Compile
javac --module-path "%JAVAFX_PATH%" --add-modules javafx.controls,javafx.fxml -d out -sourcepath src src/edu/facilities/Main.java src/edu/facilities/ui/*.java src/edu/facilities/model/*.java src/edu/facilities/service/*.java

REM Copy resources
xcopy /Y src\main\resources\fxml\*.fxml out\fxml\

REM Run
java --module-path "%JAVAFX_PATH%" --add-modules javafx.controls,javafx.fxml -cp "out;src/main/resources" edu.facilities.Main
```

## Project Structure

```
src/
├── edu/facilities/
│   ├── Main.java              (Application entry point)
│   ├── model/                 (Data models)
│   ├── service/               (Business logic)
│   └── ui/                    (Controllers)
├── main/resources/fxml/       (FXML UI files)
└── module-info.java          (Module descriptor)
```

## Troubleshooting

- **"Module javafx.controls not found"**: JavaFX SDK is not installed or path is incorrect
- **"FXML file not found"**: Ensure FXML files are in `src/main/resources/fxml/`
- **Compilation errors**: Check that all Java files are in the correct package structure

