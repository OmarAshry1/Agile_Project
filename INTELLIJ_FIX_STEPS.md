# IntelliJ - Apply SQL Server Connection Fixes

## ✅ Your files ARE already fixed! Here's how to see them in IntelliJ:

### Step 1: Reload Maven Project
1. In IntelliJ, open the **Maven** tool window (View → Tool Windows → Maven)
2. Click the **Reload All Maven Projects** button (circular arrow icon)
3. Wait for dependencies to download

### Step 2: Refresh Project Files
1. Right-click on your project root folder
2. Select **Synchronize 'projectagile'**
3. Or press `Ctrl + Alt + Y` (Windows/Linux) or `Cmd + Option + Y` (Mac)

### Step 3: Invalidate Caches (if still not working)
1. Go to **File → Invalidate Caches...**
2. Check all boxes
3. Click **Invalidate and Restart**
4. IntelliJ will restart and reload everything

---

## 📋 Manual Verification Checklist

### 1. Check `DatabaseConnection.java` (Line 40 should look like this):
```java
connection = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
```
**NOT:**
```java
connection = DriverManager.getConnection(DB_URL);  // ❌ WRONG
```

### 2. Check Connection URL (Line 21-23 should have NO `integratedSecurity`):
```java
private static final String DB_URL = String.format(
    "jdbc:sqlserver://%s:%s;databaseName=%s;encrypt=true;trustServerCertificate=true;loginTimeout=30;",
    HOST, PORT, DATABASE
);
```

### 3. Check `pom.xml` (Line 35-39):
```xml
<dependency>
    <groupId>com.microsoft.sqlserver</groupId>
    <artifactId>mssql-jdbc</artifactId>
    <version>12.4.2.jre11</version>
</dependency>
```
**NO `<scope>runtime</scope>` should be present!**

### 4. Check `module-info.java` (Line 5):
```java
requires com.microsoft.sqlserver.jdbc;
```

---

## 🔧 If Changes Still Don't Appear - Manual Copy/Paste

### Replace `DatabaseConnection.java` getConnection() method:

**FIND THIS (OLD - WRONG):**
```java
public static Connection getConnection() throws SQLException {
    if (connection == null || connection.isClosed()) {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            connection = DriverManager.getConnection(DB_URL);  // ❌ MISSING USERNAME/PASSWORD
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQL Server JDBC Driver not found", e);
        }
    }
    return connection;
}
```

**REPLACE WITH THIS (NEW - CORRECT):**
```java
public static synchronized Connection getConnection() throws SQLException {
    if (connection == null || connection.isClosed()) {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            // ✅ Pass username and password as separate parameters
            connection = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
            System.out.println("✓ Connected to SQL Server: " + HOST + ":" + PORT + "/" + DATABASE);
        } catch (ClassNotFoundException e) {
            throw new SQLException(
                "SQL Server JDBC Driver not found. Ensure mssql-jdbc is in classpath.", e);
        } catch (SQLException e) {
            System.err.println("Connection URL: " + DB_URL);
            System.err.println("Host: " + HOST + ", Port: " + PORT + ", Database: " + DATABASE);
            throw new SQLException(
                "Failed to connect to SQL Server. Check: 1) Server is running, 2) Port is open, 3) Credentials are correct. Error: " + e.getMessage(), e);
        }
    }
    return connection;
}
```

### Replace Connection URL (if it has `integratedSecurity`):

**FIND THIS (OLD - WRONG):**
```java
private static final String DB_URL = "jdbc:sqlserver://localhost:1433;databaseName=agile;encrypt=true;trustServerCertificate=true;integratedSecurity=true;";
```

**REPLACE WITH THIS (NEW - CORRECT):**
```java
private static final String DB_URL = String.format(
    "jdbc:sqlserver://%s:%s;databaseName=%s;encrypt=true;trustServerCertificate=true;loginTimeout=30;",
    HOST, PORT, DATABASE
);
```

### Add Username/Password Constants (if missing):

**ADD THESE LINES after the DATABASE constant:**
```java
private static final String USERNAME = getEnvOrDefault("MSSQL_USER", "sa");
private static final String PASSWORD = getEnvOrDefault("MSSQL_PASSWORD", "Password123");
```

### Add Helper Method (if missing):

**ADD THIS METHOD at the end of the class:**
```java
private static String getEnvOrDefault(String key, String defaultValue) {
    String value = System.getenv(key);
    return (value != null && !value.isBlank()) ? value.trim() : defaultValue;
}
```

---

## ✅ Quick Test After Changes

1. **Rebuild Project:** `Build → Rebuild Project`
2. **Run:** Right-click `Main.java` → `Run 'Main'`
3. **Check Console:** Should see "✓ Connected to SQL Server" message

---

## 🚨 Common IntelliJ Issues

### Issue: "File is read-only"
- Right-click file → Properties → Uncheck "Read-only"

### Issue: "Cannot resolve symbol 'SQLServerDriver'"
- Maven → Reload All Projects
- File → Invalidate Caches → Restart

### Issue: Changes not saving
- File → Save All (`Ctrl + S`)
- Check if file has unsaved changes indicator (red dot)

---

## 📝 Current File Status (What Should Be There)

✅ **DatabaseConnection.java** - Already fixed (SQL Auth, no integratedSecurity)  
✅ **pom.xml** - Driver dependency correct  
✅ **module-info.java** - Module requirement added  
✅ **Database.java** - Also fixed (backup class)

**All files are correct! Just refresh IntelliJ to see them.**

