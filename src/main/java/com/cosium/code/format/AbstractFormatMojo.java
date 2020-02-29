package com.cosium.code.format;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Created on 16/01/18.
 *
 * @author Reda.Housni-Alaoui
 */
public abstract class AbstractFormatMojo extends AbstractModulMavenGitCodeFormatMojo {

  @Parameter(property = "gcf.globPattern", required = true, defaultValue = "**/*")
  private String globPattern;

  @Override
  protected final void doExecute() throws MojoExecutionException, MojoFailureException {
    String pattern = "glob:" + globPattern;
    getLog().debug("Using pattern '" + pattern + "'");
    PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + globPattern);

    for (Path sourceDir : sourceDirs()) {
      walk(sourceDir, pathMatcher);
    }
  }

  private void walk(Path directoryToWalk, PathMatcher pathMatcher)
      throws MojoExecutionException, MojoFailureException {
    Path targetDir = targetDir();
    try {
      Files.walkFileTree(
          directoryToWalk,
          new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
              if (path.startsWith(targetDir)) {
                return FileVisitResult.CONTINUE;
              }
              if (!pathMatcher.matches(path)) {
                return FileVisitResult.CONTINUE;
              }
              try {
                process(path);
              } catch (MojoExecutionException | MojoFailureException e) {
                throw new MavenGitCodeFormatException(e);
              }
              return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
              return FileVisitResult.CONTINUE;
            }
          });

    } catch (IOException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    } catch (Exception e) {
      Throwable cause = e.getCause();
      if (cause instanceof MojoExecutionException) {
        throw (MojoExecutionException) cause;
      }
      if (cause instanceof MojoFailureException) {
        throw (MojoFailureException) cause;
      }
      throw e;
    }
  }

  protected abstract void process(Path path) throws MojoExecutionException, MojoFailureException;
}
