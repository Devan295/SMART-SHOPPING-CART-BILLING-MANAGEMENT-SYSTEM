import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * DBConnection handles JDBC connectivity for smart_cart_db.
 *
 * Configuration resolution priority:
 * 1. Environment variables (DB_URL or DB_HOST/DB_PORT/DB_NAME, DB_USER, DB_PASSWORD)
 * 2. Local db.properties file (searched in working dir, classpath, parent dirs, and code source)
 * 3. Default fallback values (localhost:3306/smart_cart_db, root / 12345)
 *
 * Resilient Classpath & Password Handling:
 * Automatically locates db.properties across diverse IDE working directories
 * and dynamically loads the MySQL Connector driver from `lib/` if missing from classpath.
 */
public class DBConnection {

    private static final String DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 3306;
    private static final String DEFAULT_DATABASE = "smart_cart_db";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "12345";

    private static String jdbcUrl;
    private static String username;
    private static String password;
    private static boolean driverLoaded = false;
    private static Driver fallbackDriver = null;

    static {
        loadConfiguration();
    }

    /**
     * Loads database configuration from environment variables, db.properties, or defaults.
     */
    public static synchronized void loadConfiguration() {
        Properties props = new Properties();

        // 1. Try to locate db.properties across all likely working directories
        List<File> candidateFiles = new ArrayList<>();
        candidateFiles.add(new File("db.properties"));
        candidateFiles.add(new File("SmartShoppingCart - Devanshi", "db.properties"));
        candidateFiles.add(new File("..", "db.properties"));
        candidateFiles.add(new File("bin", "db.properties"));
        candidateFiles.add(new File("src", "db.properties"));

        try {
            File codeDir = new File(DBConnection.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (codeDir.isFile()) {
                codeDir = codeDir.getParentFile();
            }
            candidateFiles.add(new File(codeDir, "db.properties"));
            if (codeDir.getParentFile() != null) {
                candidateFiles.add(new File(codeDir.getParentFile(), "db.properties"));
            }
        } catch (Exception ignored) {
        }

        // Absolute workspace fallback
        candidateFiles.add(new File("C:\\Users\\Lenovo\\Documents\\Devanshu's VS\\SmartShoppingCart - Devanshi\\db.properties"));

        boolean propFileLoaded = false;
        for (File f : candidateFiles) {
            if (f.exists() && f.isFile()) {
                try (FileInputStream fis = new FileInputStream(f)) {
                    props.load(fis);
                    propFileLoaded = true;
                    break;
                } catch (IOException ignored) {
                }
            }
        }

        // Also check classpath if not loaded from file
        if (!propFileLoaded) {
            try (InputStream is = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
                if (is != null) {
                    props.load(is);
                    propFileLoaded = true;
                }
            } catch (Exception ignored) {
            }
        }

        // Environment variables override properties file
        String envUrl = System.getenv("DB_URL");
        String envUser = System.getenv("DB_USER");
        String envPass = System.getenv("DB_PASSWORD");
        String envHost = System.getenv("DB_HOST");
        String envPort = System.getenv("DB_PORT");
        String envDb = System.getenv("DB_NAME");

        if (envUrl != null && !envUrl.trim().isEmpty()) {
            jdbcUrl = envUrl.trim();
        } else if (props.getProperty("db.url") != null && !props.getProperty("db.url").trim().isEmpty()) {
            jdbcUrl = props.getProperty("db.url").trim();
        } else {
            String host = (envHost != null && !envHost.trim().isEmpty()) ? envHost.trim() :
                    props.getProperty("db.host", DEFAULT_HOST).trim();
            String port = (envPort != null && !envPort.trim().isEmpty()) ? envPort.trim() :
                    props.getProperty("db.port", String.valueOf(DEFAULT_PORT)).trim();
            String db = (envDb != null && !envDb.trim().isEmpty()) ? envDb.trim() :
                    props.getProperty("db.name", DEFAULT_DATABASE).trim();

            jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + db
                    + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        }

        if (envUser != null && !envUser.trim().isEmpty()) {
            username = envUser.trim();
        } else if (props.getProperty("db.user") != null && !props.getProperty("db.user").trim().isEmpty()) {
            username = props.getProperty("db.user").trim();
        } else {
            username = DEFAULT_USER;
        }

        if (envPass != null) {
            password = envPass;
        } else if (props.getProperty("db.password") != null) {
            password = props.getProperty("db.password").trim();
        } else {
            password = DEFAULT_PASSWORD;
        }

        // Try standard classpath initialization first
        try {
            Class.forName(DRIVER_CLASS);
            driverLoaded = true;
        } catch (ClassNotFoundException e) {
            // Attempt automatic dynamic recovery from lib/
            driverLoaded = tryLoadDriverDynamically();
            if (!driverLoaded) {
                System.err.println("[DBConnection] MySQL JDBC Driver not found! Ensure mysql-connector-j.jar is in the classpath/lib directory.");
            }
        }
    }

    /**
     * Dynamically finds and loads the MySQL Connector/J JAR from `lib/` or related paths.
     */
    private static synchronized boolean tryLoadDriverDynamically() {
        String[] searchDirs = new String[] {
            "lib",
            "../lib",
            ".",
            "C:/Users/Lenovo/Downloads/mysql-connector-j-26.10.0",
            "C:/Users/Lenovo/Documents/Devanshu's VS/mysql-connector-j-26.10.0",
            "C:/Users/Lenovo/Documents/Devanshu's VS/SmartShoppingCart - Devanshi/lib"
        };

        List<File> candidateJars = new ArrayList<>();
        for (String dirPath : searchDirs) {
            File dir = new File(dirPath);
            if (dir.exists()) {
                if (dir.isFile() && dir.getName().endsWith(".jar") && dir.getName().toLowerCase().contains("mysql")) {
                    candidateJars.add(dir);
                } else if (dir.isDirectory()) {
                    File[] files = dir.listFiles((d, name) -> name.endsWith(".jar") && name.toLowerCase().contains("mysql"));
                    if (files != null) {
                        for (File f : files) {
                            candidateJars.add(f);
                        }
                    }
                }
            }
        }

        for (File jar : candidateJars) {
            try {
                URL url = jar.toURI().toURL();
                URLClassLoader classLoader = new URLClassLoader(new URL[]{url}, DBConnection.class.getClassLoader());
                Class<?> driverClass = Class.forName(DRIVER_CLASS, true, classLoader);
                Driver driver = (Driver) driverClass.getDeclaredConstructor().newInstance();

                DriverShim shim = new DriverShim(driver);
                DriverManager.registerDriver(shim);
                fallbackDriver = shim;
                driverLoaded = true;
                System.out.println("[DBConnection] Successfully auto-loaded MySQL driver from: " + jar.getName());
                return true;
            } catch (Throwable t) {
                // Try next jar candidate
            }
        }
        return false;
    }

    /**
     * Gets a new JDBC Connection to MySQL.
     *
     * @return active java.sql.Connection
     * @throws SQLException with informative error message on connection failure
     */
    public static Connection getConnection() throws SQLException {
        if (!driverLoaded) {
            driverLoaded = tryLoadDriverDynamically();
            if (!driverLoaded) {
                try {
                    Class.forName(DRIVER_CLASS);
                    driverLoaded = true;
                } catch (ClassNotFoundException e) {
                    throw new SQLException("MySQL JDBC Driver ('" + DRIVER_CLASS + "') not found in classpath. " +
                            "Please ensure mysql-connector-j.jar is in the lib/ folder.", e);
                }
            }
        }

        try {
            if (fallbackDriver != null) {
                Properties info = new Properties();
                if (username != null) info.put("user", username);
                if (password != null) info.put("password", password);
                Connection conn = fallbackDriver.connect(jdbcUrl, info);
                if (conn != null) {
                    return conn;
                }
            }
            return DriverManager.getConnection(jdbcUrl, username, password);
        } catch (SQLException ex) {
            String diagnostic = interpretSqlException(ex);
            throw new SQLException(diagnostic, ex);
        }
    }

    /**
     * Translates common SQL exceptions into clear, actionable error messages for the user.
     */
    public static String interpretSqlException(SQLException ex) {
        String msg = ex.getMessage();
        int errorCode = ex.getErrorCode();

        if (errorCode == 1045 || (msg != null && msg.contains("Access denied for user"))) {
            return "Database Authentication Failed: Access denied for user '" + username +
                    "'. Please check the password in db.properties.";
        } else if (errorCode == 1049 || (msg != null && msg.contains("Unknown database"))) {
            return "Database Not Found: 'smart_cart_db' does not exist. Run database/smart_cart_db.sql first.";
        } else if (msg != null && (msg.contains("Communications link failure") || msg.contains("Connection refused"))) {
            return "Cannot Connect to MySQL Server: Ensure MySQL Server is running on port 3306 (or check host/port in db.properties).";
        }
        return "Database error [" + errorCode + "]: " + msg;
    }

    /**
     * Tests connectivity to the database.
     *
     * @return true if connection succeeded, false otherwise
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("[DBConnection Test Failed] " + e.getMessage());
            return false;
        }
    }

    /**
     * Convenience method to safely close JDBC resources without throwing exceptions.
     */
    public static void closeQuietly(AutoCloseable... closeables) {
        for (AutoCloseable c : closeables) {
            if (c != null) {
                try {
                    c.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public static String getJdbcUrl() {
        return jdbcUrl;
    }

    public static String getUsername() {
        return username;
    }

    /**
     * Wrapper shim to register dynamically loaded Driver with DriverManager across ClassLoaders.
     */
    private static class DriverShim implements Driver {
        private final Driver driver;

        public DriverShim(Driver d) {
            this.driver = d;
        }

        @Override
        public boolean acceptsURL(String u) throws SQLException {
            return this.driver.acceptsURL(u);
        }

        @Override
        public Connection connect(String u, Properties p) throws SQLException {
            return this.driver.connect(u, p);
        }

        @Override
        public int getMajorVersion() {
            return this.driver.getMajorVersion();
        }

        @Override
        public int getMinorVersion() {
            return this.driver.getMinorVersion();
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String u, Properties p) throws SQLException {
            return this.driver.getPropertyInfo(u, p);
        }

        @Override
        public boolean jdbcCompliant() {
            return this.driver.jdbcCompliant();
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            return this.driver.getParentLogger();
        }
    }

    /**
     * Standalone main method to test database connectivity independently of the GUI.
     */
    public static void main(String[] args) {
        System.out.println("==============================================");
        System.out.println("   Smart Shopping Cart - DB Connection Test   ");
        System.out.println("==============================================");
        System.out.println("Target JDBC URL : " + jdbcUrl);
        System.out.println("Target User     : " + username);

        try (Connection conn = getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("Status          : [SUCCESS] Successfully connected to MySQL database 'smart_cart_db'!");
                System.out.println("DBMS Version    : " + conn.getMetaData().getDatabaseProductVersion());
                System.out.println("Driver Version  : " + conn.getMetaData().getDriverVersion());
            } else {
                System.err.println("Status          : [FAILED] Connection returned null or was closed.");
            }
        } catch (SQLException e) {
            System.err.println("Status          : [FAILED]");
            System.err.println("Error Message   : " + e.getMessage());
        }
        System.out.println("==============================================");
    }
}
