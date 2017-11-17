package com.cosium.code.format;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Created on 07/11/17.
 *
 * @author Reda.Housni-Alaoui
 */
@Mojo(name = "format-code", defaultPhase = LifecyclePhase.NONE)
public class FormatCodeMojo extends AbstractModulMavenGitCodeFormatMojo {

  @Parameter(property = "globPattern", required = true)
  private String globPattern;

  @Override
  protected void doExecute() throws MojoExecutionException, MojoFailureException {
    String pattern = "glob:" + globPattern;
    getLog().debug("Using pattern '" + pattern + "'");
    PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + globPattern);

    try {
      Files.walkFileTree(
          baseDir(),
          new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs)
                throws IOException {
              if (pathMatcher.matches(path)) {
                codeFormatter().format(path);
              }
              return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
              return FileVisitResult.CONTINUE;
            }
          });
    } catch (IOException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }
  }
}
