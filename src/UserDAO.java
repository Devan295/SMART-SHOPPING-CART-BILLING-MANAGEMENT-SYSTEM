import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * UserDAO handles authentication and user lookup from the `users` table.
 *
 * NOTE ON SECURITY:
 * For this academic prototype, credentials are validated using plaintext comparisons
 * to maintain 100% baseline compatibility with the provided schema and sample seed records.
 * In production environments, passwords must be securely hashed using salted algorithms
 * such as BCrypt or Argon2, and never stored or queried in plaintext.
 */
public class UserDAO {

    /**
     * Authenticates a user against the `users` table using a PreparedStatement.
     *
     * @param username the username provided at login
     * @param password the password provided at login
     * @return User object if authentication is successful, null if invalid credentials
     * @throws SQLException if a database connectivity or query error occurs
     */
    public User authenticate(String username, String password) throws SQLException {
        if (username == null || password == null) {
            return null;
        }

        String sql = "SELECT user_id, username FROM users WHERE username = ? AND password = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username.trim());
            stmt.setString(2, password.trim());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int userId = rs.getInt("user_id");
                    String uname = rs.getString("username");
                    return new User(userId, uname);
                }
            }
        }
        return null;
    }

    /**
     * User model representing an authenticated user.
     */
    public static class User {
        private final int userId;
        private final String username;

        public User(int userId, String username) {
            this.userId = userId;
            this.username = username;
        }

        public int getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }

        @Override
        public String toString() {
            return "User{id=" + userId + ", username='" + username + "'}";
        }
    }
}

