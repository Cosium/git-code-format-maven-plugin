package com.cosium.code.format.utils;

import com.cosium.code.format.MavenGitCodeFormatException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created on 02/11/17.
 *
 * @author Reda.Housni-Alaoui
 */
public class MavenUtils {

  private static final String MAVEN_HOME_PROP = "maven.home";

  public Path getMavenExecutable(boolean debug) {
    Path mavenHome = Paths.get(System.getProperty(MAVEN_HOME_PROP));
    Path bin = mavenHome.resolve("bin");
    Path executable;
    if (!debug) {
      executable = bin.resolve("mvn");
    } else {
      executable = bin.resolve("mvnDebug");
    }
    if (!Files.exists(executable)) {
      throw new MavenGitCodeFormatException(
          "Could not find maven executable. " + executable + " does not exist.");
    }
    return executable;
  }
}
