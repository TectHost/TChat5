package tect.host.tpl.dependency;

import org.jspecify.annotations.NonNull;

/** Maven repositories used to resolve a Dependency, checked in order */
public enum DependencyRepository {

    MAVEN_CENTRAL("https://repo1.maven.org/maven2/");

    private final String baseUrl;

    DependencyRepository(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public @NonNull String resolveUrl(@NonNull Dependency dependency) {
        return baseUrl + dependency.mavenPath();
    }
}