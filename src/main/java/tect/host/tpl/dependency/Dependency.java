package tect.host.tpl.dependency;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

/**
 * Runtime dependency that can be downloaded and loaded instead of being shaded
 * into the final plugin jar
 *.
 * Describes Maven coordinates, optional checksum verification, and relocations.
 * Consumer-specific logic, such as loading a driver class, belongs to the
 * component using the dependency
 *.
 * A null checksum enables trust-on-first-use verification: the first
 * download is stored and subsequent downloads are checked against it
 */
public enum Dependency {

    /*  DEPENDENCY(
            groupId,
            artifactId,
            version,
            checksum
        )
    */

    MARIADB_JAVA_CLIENT(
            "org.mariadb.jdbc",
            "mariadb-java-client",
            "3.5.9",
            "11E3BB5BBF8EF0E806AE4D6C5D5033FEDF7262CC777F0190BDE8A2F3C8E6BD8D"
    ),

    SQLITE_JDBC(
            "org.xerial",
            "sqlite-jdbc",
            "3.53.2.0",
            "DC320E4102884C135CCC30C3C6FC3FB190B750E1586A100E3ABA3BE783CF33A9"
    ),

    HIKARICP(
            "com.zaxxer",
            "HikariCP",
            "7.1.0",
            "2BE45C641B841E81DF87292E181F2C203500C5F0E9A1676F135927387F3CF40B"
    );

    private final String groupId;
    private final String artifactId;
    private final String version;
    private final String checksum;

    Dependency(String groupId, String artifactId, String version, String checksum) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.checksum = checksum;
    }

    public String artifactId() {
        return artifactId;
    }

    public String version() {
        return version;
    }

    public String checksum() {
        return checksum;
    }

    @Contract(pure = true)
    public @NonNull String fileName() {
        return artifactId + "-" + version + ".jar";
    }

    public @NonNull String mavenPath() {
        return groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/" + fileName();
    }
}