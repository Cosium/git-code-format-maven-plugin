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
  private static final String BIN_MVN = "bin/mvn";

  public Path getMavenExecutable() {
    Path mavenHome = Paths.get(System.getProperty(MAVEN_HOME_PROP));
    Path executable = mavenHome.resolve(BIN_MVN);
    if (!Files.exists(executable)) {
      throw new MavenGitCodeFormatException(
          "Could not find maven executable. " + executable + " does not exist.");
    }
    return executable;
  }
}
