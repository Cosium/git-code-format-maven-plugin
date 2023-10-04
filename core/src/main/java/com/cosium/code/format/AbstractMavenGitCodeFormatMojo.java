package com.cosium.code.format;

import com.cosium.code.format.formatter.CodeFormatterConfigurationFactory;
import com.cosium.code.format.formatter.CodeFormatters;
import com.cosium.code.format_spi.CodeFormatter;
import com.cosium.code.format_spi.CodeFormatterFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

/**
 * @author Réda Housni Alaoui
 */
public abstract class AbstractMavenGitCodeFormatMojo extends AbstractMojo {

  protected static final String HOOKS_DIR = "hooks";

  @Parameter(readonly = true, defaultValue = "${project}")
  private MavenProject currentProject;

  @Parameter(defaultValue = "${project.build.sourceEncoding}")
  private String sourceEncoding;

  @Parameter private Map<String, String> formatterOptions;

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

  protected final CodeFormatters collectCodeFormatters() {
    return new CodeFormatters(createCodeFormatters());
  }

  private List<CodeFormatter> createCodeFormatters() {

    List<CodeFormatterFactory> formatterFactories = new ArrayList<>();
    ServiceLoader.load(CodeFormatterFactory.class).forEach(formatterFactories::add);
    if (formatterFactories.isEmpty()) {
      throw new IllegalStateException(
          "No "
              + CodeFormatter.class
              + " instance found. You probably forgot to declare a code formatter as a plugin's dependency. You can use https://github.com/Cosium/git-code-format-maven-plugin#automatic-code-format-and-validation-activation as an example.");
    }

    CodeFormatterConfigurationFactory formatterConfigurationFactory =
        new CodeFormatterConfigurationFactory(
            Optional.ofNullable(formatterOptions).orElseGet(Collections::emptyMap));

    return formatterFactories.stream()
        .map(
            codeFormatterFactory ->
                codeFormatterFactory.build(
                    formatterConfigurationFactory.build(codeFormatterFactory.configurationId()),
                    sourceEncoding))
        .collect(Collectors.toList());
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
