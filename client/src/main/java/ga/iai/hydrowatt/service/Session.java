package ga.iai.hydrowatt.service;

/** Détient les jetons JWT de la session courante (en mémoire uniquement). */
public class Session {
    private static String accessToken;
    private static String refreshToken;
    private static String username;
    private static boolean admin;

    public static void set(String access, String refresh, String user) {
        accessToken = access;
        refreshToken = refresh;
        username = user;
    }

    public static void setAdmin(boolean isAdmin) { admin = isAdmin; }
    public static boolean isAdmin() { return admin; }

    public static String getAccessToken() { return accessToken; }
    public static String getRefreshToken() { return refreshToken; }
    public static String getUsername() { return username; }

    public static void updateAccessToken(String access) { accessToken = access; }

    public static void clear() {
        accessToken = null;
        refreshToken = null;
        username = null;
        admin = false;
    }

    public static boolean isAuthenticated() { return accessToken != null; }
}
