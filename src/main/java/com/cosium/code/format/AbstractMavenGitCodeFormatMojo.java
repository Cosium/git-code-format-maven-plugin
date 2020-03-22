package com.cosium.code.format;

import com.cosium.code.format.formatter.CodeFormatter;
import com.cosium.code.format.formatter.CodeFormatters;
import com.cosium.code.format.formatter.GoogleJavaFormatter;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;

/**
 * Created on 01/11/17.
 *
 * @author Reda.Housni-Alaoui
 */
public abstract class AbstractMavenGitCodeFormatMojo extends AbstractMojo {

  protected static final String HOOKS_DIR = "hooks";

  private final Supplier<List<CodeFormatter>> codeFormatters;

  @Parameter(readonly = true, defaultValue = "${project}")
  private MavenProject currentProject;

  @Parameter private MavenGoogleJavaFormatOptions googleJavaFormatOptions;

  @Parameter(defaultValue = "${project.build.sourceEncoding}")
  private String sourceEncoding;

  public AbstractMavenGitCodeFormatMojo() {
    codeFormatters =
        () ->
            Collections.singletonList(
                new GoogleJavaFormatter(
                    ofNullable(googleJavaFormatOptions)
                        .orElseGet(MavenGoogleJavaFormatOptions::new)
                        .toFormatterOptions(),
                    sourceEncoding));
  }

  protected final Repository gitRepository() {
    Repository gitRepository;
    try {
      FileRepositoryBuilder repositoryBuilder =
          new FileRepositoryBuilder().findGitDir(currentProject.getBasedir());
      String gitIndexFileEnvVariable = System.getenv("GIT_INDEX_FILE");
      if (StringUtils.isNotBlank(gitIndexFileEnvVariable)) {
        repositoryBuilder = repositoryBuilder.setIndexFile(new File(gitIndexFileEnvVariable));
      }
      gitRepository = repositoryBuilder.build();
    } catch (IOException e) {
      throw new MavenGitCodeFormatException(
          "Could not find the git repository. Run 'git init' if you did not.", e);
    }
    return gitRepository;
  }

  protected final Path baseDir() {
    return currentProject.getBasedir().toPath();
  }

  protected final Path pomFile() {
    return currentProject.getFile().toPath();
  }

  protected final List<Path> sourceDirs() {
    return Stream.of(
            currentProject.getCompileSourceRoots(), currentProject.getTestCompileSourceRoots())
        .flatMap(Collection::stream)
        .map(Paths::get)
        .collect(Collectors.toList());
  }

  protected final Path targetDir() {
    return Paths.get(currentProject.getBuild().getDirectory());
  }

  protected final String artifactId() {
    return currentProject.getArtifactId();
  }

  protected final CodeFormatters codeFormatters() {
    return new CodeFormatters(codeFormatters.get());
  }

  protected final boolean isExecutionRoot() {
    return currentProject.isExecutionRoot();
  }

  /**
   * Get or creates the git hooks directory
   *
   * @return The git hooks directory
   */
  protected final Path getOrCreateHooksDirectory() {
    Path hooksDirectory = gitRepository().getDirectory().toPath().resolve(HOOKS_DIR);
    if (!Files.exists(hooksDirectory)) {
      getLog().debug("Creating directory " + hooksDirectory);
      try {
        Files.createDirectories(hooksDirectory);
      } catch (IOException e) {
        throw new MavenGitCodeFormatException(e);
      }
    } else {
      getLog().debug(hooksDirectory + " already exists");
    }
    return hooksDirectory;
  }

  protected final Path gitBaseDir() {
    return gitRepository().getDirectory().getParentFile().toPath();
  }
}
