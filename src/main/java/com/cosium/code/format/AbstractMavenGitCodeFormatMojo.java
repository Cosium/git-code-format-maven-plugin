package com.cosium.code.format;

import com.cosium.code.format.formatter.CodeFormatter;
import com.cosium.code.format.formatter.CompositeCodeFormatter;
import com.cosium.code.format.formatter.JavaFormatter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Created on 01/11/17.
 *
 * @author Reda.Housni-Alaoui
 */
public abstract class AbstractMavenGitCodeFormatMojo extends AbstractMojo {

  @Parameter(readonly = true, defaultValue = "${project}")
  private MavenProject currentProject;

  private final CodeFormatter codeFormatter = new CompositeCodeFormatter(new JavaFormatter(this::getLog));

  protected Repository gitRepository() {
    Repository gitRepository;
    try {
      gitRepository = new FileRepositoryBuilder().findGitDir(currentProject.getBasedir()).build();
    } catch (IOException e) {
      throw new RuntimeException(
          "Could not find the git repository. Run 'git init' if you did not.", e);
    }
    return gitRepository;
  }

  protected Path baseDir() {
    return currentProject.getBasedir().toPath();
  }

  protected String artifactId() {
    return currentProject.getArtifactId();
  }

  protected CodeFormatter codeFormatter(){
    return codeFormatter;
  }
}
