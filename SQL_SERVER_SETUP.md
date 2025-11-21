# SQL Server Connection Setup - Quick Fix Guide

## ✅ Issues Fixed

1. **Removed `integratedSecurity=true`** - This was causing "No suitable driver" because it requires `sqljdbc_auth.dll`
2. **Switched to SQL Authentication** - Now uses username/password (simpler, no DLL needed)
3. **Fixed driver dependency** - Removed `runtime` scope, updated to `12.6.1.jre17` (compatible with Java 21)
4. **Added module requirement** - `requires com.microsoft.sqlserver.jdbc;` in module-info.java
5. **Improved error messages** - Better diagnostics for connection failures

## 🔧 Connection String Format

**Current (SQL Authentication):**
```
jdbc:sqlserver://localhost:1433;databaseName=agile;encrypt=true;trustServerCertificate=true;loginTimeout=30;
```

**For your hostname (DESKTOP-U4EMGMM):**
```
jdbc:sqlserver://DESKTOP-U4EMGMM:1433;databaseName=agile;encrypt=true;trustServerCertificate=true;loginTimeout=30;
```

## 🚀 Quick Setup Steps

### 1. Set Environment Variables (Optional - defaults to localhost/sa)

**PowerShell:**
```powershell
$env:MSSQL_HOST = "DESKTOP-U4EMGMM"
$env:MSSQL_PORT = "1433"
$env:MSSQL_DB = "agile"
$env:MSSQL_USER = "sa"              # or your SQL login username
$env:MSSQL_PASSWORD = "YourPassword" # your SQL login password
```

**Or set permanently:**
```powershell
setx MSSQL_HOST "DESKTOP-U4EMGMM"
setx MSSQL_PORT "1433"
setx MSSQL_DB "agile"
setx MSSQL_USER "sa"
setx MSSQL_PASSWORD "YourPassword"
```

### 2. Ensure SQL Server Login Exists

**In SSMS, run:**
```sql
-- If using 'sa', ensure it's enabled:
ALTER LOGIN sa ENABLE;
ALTER LOGIN sa WITH PASSWORD = 'YourPassword';

-- Or create a new SQL login:
CREATE LOGIN agile_user WITH PASSWORD = 'agile123';
USE agile;
CREATE USER agile_user FOR LOGIN agile_user;
ALTER ROLE db_owner ADD MEMBER agile_user;
```

### 3. Verify SQL Server Authentication Mode

**In SSMS:**
1. Right-click server → Properties → Security
2. Ensure "SQL Server and Windows Authentication mode" is selected
3. Restart SQL Server service if you changed it

### 4. Rebuild and Test

```bash
mvn clean compile
mvn javafx:run
```

## 🔍 Troubleshooting

### "No suitable driver" Error
- ✅ **FIXED**: Removed `integratedSecurity=true` and `runtime` scope
- ✅ **FIXED**: Added module requirement
- Run `mvn clean install` to refresh dependencies

### "Login failed" Error
- Check username/password are correct
- Ensure SQL Authentication is enabled (not just Windows Auth)
- Verify login exists: `SELECT name FROM sys.sql_logins;`

### "Connection refused" Error
- Verify SQL Server is running: `netstat -an | findstr 1433`
- Check firewall allows port 1433
- Try `localhost` instead of hostname if DNS issues

### SSL/Certificate Errors
- ✅ **FIXED**: Using `encrypt=true;trustServerCertificate=true;`
- This accepts self-signed certificates

## 📝 Connection String Options

**For default instance:**
```
jdbc:sqlserver://DESKTOP-U4EMGMM:1433;databaseName=agile;encrypt=true;trustServerCertificate=true;
```

**For named instance (e.g., SQLEXPRESS):**
```
jdbc:sqlserver://DESKTOP-U4EMGMM\SQLEXPRESS:1433;databaseName=agile;encrypt=true;trustServerCertificate=true;
```

**With custom port:**
```
jdbc:sqlserver://DESKTOP-U4EMGMM:1434;databaseName=agile;encrypt=true;trustServerCertificate=true;
```

## ✅ Test Connection

Add this to your `Main.java` temporarily:
```java
import edu.facilities.service.DatabaseConnection;

// In main method:
if (DatabaseConnection.testConnection()) {
    System.out.println("Database ready!");
} else {
    System.err.println("Database connection failed!");
}
```

