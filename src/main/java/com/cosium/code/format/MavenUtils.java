package com.cosium.code.format;

import org.apache.maven.plugin.logging.Log;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * Created on 02/11/17.
 *
 * @author Reda.Housni-Alaoui
 */
public class MavenUtils {

    private static final String MAVEN_HOME_PROP = "maven.home";
    private static final String BIN_MVN = "bin/mvn";

    private final Supplier<Log> log;

    public MavenUtils(Supplier<Log> log) {
        requireNonNull(log);
        this.log = log;
    }

    public Path getMavenExecutable() {
        Path mavenHome = Paths.get(System.getProperty(MAVEN_HOME_PROP));
        Path executable = mavenHome.resolve(BIN_MVN);
        if (!Files.exists(executable)) {
            throw new RuntimeException(
                    "Could not find maven executable. " + executable + " does not exist.");
        }
        return executable;
    }
}
