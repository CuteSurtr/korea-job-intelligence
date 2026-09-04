package com.kji.support;

/**
 * The PostgreSQL the integration tests run against.
 *
 * <p>By default a throwaway container is started, which requires a Docker daemon. Point
 * {@code KJI_TEST_DATABASE_URL} at an already-running PostgreSQL to run the same suite
 * without Docker:
 *
 * <pre>
 * KJI_TEST_DATABASE_URL=jdbc:postgresql://localhost:5432/kji_test ./gradlew test
 * </pre>
 *
 * <p>The suite truncates between tests rather than recreating the schema, so an external
 * database must be one you are willing to have emptied. Use a database kept for testing,
 * never the one the application runs against.
 */
public final class TestDatabase {

    static final String URL_PROPERTY = "kji.test.datasource.url";
    static final String USERNAME_PROPERTY = "kji.test.datasource.username";
    static final String PASSWORD_PROPERTY = "kji.test.datasource.password";

    private static final String DEFAULT_CREDENTIAL = "kji";

    private static volatile TestDatabase instance;

    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final boolean containerBacked;

    private TestDatabase(String jdbcUrl, String username, String password, boolean containerBacked) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.containerBacked = containerBacked;
    }

    public static TestDatabase instance() {
        TestDatabase resolved = instance;
        if (resolved == null) {
            synchronized (TestDatabase.class) {
                resolved = instance;
                if (resolved == null) {
                    resolved = resolve();
                    instance = resolved;
                }
            }
        }
        return resolved;
    }

    private static TestDatabase resolve() {
        String url = setting(URL_PROPERTY, "KJI_TEST_DATABASE_URL");
        if (url == null) {
            return Container.start();
        }
        return new TestDatabase(url,
                orDefault(setting(USERNAME_PROPERTY, "KJI_TEST_DATABASE_USERNAME"), DEFAULT_CREDENTIAL),
                orDefault(setting(PASSWORD_PROPERTY, "KJI_TEST_DATABASE_PASSWORD"), DEFAULT_CREDENTIAL),
                false);
    }

    private static String setting(String systemProperty, String environmentVariable) {
        String value = System.getProperty(systemProperty);
        if (value == null) {
            value = System.getenv(environmentVariable);
        }
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String orDefault(String value, String fallback) {
        return value == null ? fallback : value;
    }

    public String jdbcUrl() {
        return jdbcUrl;
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }

    /** True when this run started its own container, false when an external database was given. */
    public boolean containerBacked() {
        return containerBacked;
    }

    /**
     * Holds the Testcontainers reference in a separate class so that the container classes are
     * only loaded when a container is actually wanted. Without that, a run against an external
     * database would still need Testcontainers to initialize, and it fails without Docker.
     */
    private static final class Container {

        private static TestDatabase start() {
            org.testcontainers.containers.PostgreSQLContainer<?> postgres =
                    new org.testcontainers.containers.PostgreSQLContainer<>("postgres:16-alpine")
                            .withDatabaseName("kji")
                            .withUsername(DEFAULT_CREDENTIAL)
                            .withPassword(DEFAULT_CREDENTIAL);
            try {
                postgres.start();
            } catch (RuntimeException cause) {
                throw new IllegalStateException(
                        "The integration tests need PostgreSQL. Docker was not usable, so no "
                                + "container could be started. Either start Docker, or point the "
                                + "suite at a PostgreSQL you already run:\n"
                                + "  KJI_TEST_DATABASE_URL=jdbc:postgresql://localhost:5432/kji_test "
                                + "./gradlew test\n"
                                + "That database is truncated between tests, so use one kept for "
                                + "testing.", cause);
            }
            return new TestDatabase(postgres.getJdbcUrl(), postgres.getUsername(),
                    postgres.getPassword(), true);
        }
    }
}
