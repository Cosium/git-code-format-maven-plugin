package com.cosium.code.format;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.regex.Pattern;

/**
 * Created on 01/11/17.
 *
 * @author Reda.Housni-Alaoui
 */
@Mojo(name = "on-pre-commit", defaultPhase = LifecyclePhase.NONE)
public class OnPreCommitMojo extends AbstractMavenGitCodeFormatMojo {

  private static final String JAVA_EXTENSION = ".java";
  private static final String COMMA = ",";

  /** The comma separated staged files list. i.e. "src/main/java/Foo.java,src/main/java/Bar.java" */
  @Parameter(property = "stagedFiles", required = true)
  private String stagedFiles;

  public void execute() throws MojoExecutionException {
    try {
      getLog().info("Executing pre-commit hooks");
      doExecute();
      getLog().info("Executed pre-commit hooks");
    } catch (Exception e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }
  }

  private void doExecute() throws GitAPIException {
    getLog().debug("Staged files are '" + stagedFiles + "'");

    Pattern.compile(COMMA)
        .splitAsStream(stagedFiles)
        .map(StringUtils::trim)
        .filter(StringUtils::isNotBlank)
        .map(this::toPath)
        .filter(this::isEligible)
        .forEach(this::format);
  }

  private Path toPath(String diffPath) {
    Path workTree = gitRepository().getWorkTree().toPath();
    return workTree.resolve(diffPath);
  }

  private boolean isEligible(Path file) {
    if (!file.toString().endsWith(JAVA_EXTENSION)) {
      getLog().debug(file + " is not a java file");
      return false;
    }
    if (!file.toAbsolutePath().toString().contains(baseDir().toAbsolutePath().toString())) {
      getLog().debug(file + " does not belong to the current project");
      return false;
    }
    return true;
  }

  private void format(Path javaFile) {
    getLog().info("Formatting '" + javaFile + "'");
    final String formattedContent;
    try (InputStream inputStream = Files.newInputStream(javaFile)) {
      formattedContent = new Formatter().formatSource(IOUtils.toString(inputStream));
    } catch (IOException | FormatterException e) {
      throw new RuntimeException(e);
    }
    try (OutputStream outputStream =
        Files.newOutputStream(javaFile, StandardOpenOption.TRUNCATE_EXISTING)) {
      IOUtils.write(formattedContent, outputStream);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    getLog().info("Formatted '" + javaFile + "'");
  }
}
