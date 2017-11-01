package com.cosium.code.format;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Created on 01/11/17.
 *
 * @author Reda.Housni-Alaoui
 */
@Mojo(name = "on-pre-commit", defaultPhase = LifecyclePhase.NONE)
public class OnPreCommitMojo extends AbstractMavenGitCodeFormatMojo {

  private static final String JAVA_EXTENSION = ".java";

  public void execute() throws MojoExecutionException {
    try {
      getLog().info("Executing pre-commit hooks");
      doExecute();
      getLog().info("Executing pre-commit hooks");
    } catch (Exception e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }
  }

  private void doExecute() throws GitAPIException {
    getLog().debug("Formatting source code");
    git()
        .diff()
        .setCached(true)
        .setShowNameAndStatusOnly(true)
        .call()
        .stream()
        .map(DiffEntry::getNewPath)
        .map(this::toPath)
        .filter(this::isFormattable)
        .forEach(this::format);
  }

  private Path toPath(String diffPath) {
    Path workTree = git().getRepository().getWorkTree().toPath();
    return workTree.resolve(diffPath);
  }

  private boolean isFormattable(Path file) {
    if (!file.getFileName().endsWith(JAVA_EXTENSION)) {
      return false;
    }
    if (!file.toAbsolutePath().toString().contains(baseDir().toAbsolutePath().toString())) {
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
