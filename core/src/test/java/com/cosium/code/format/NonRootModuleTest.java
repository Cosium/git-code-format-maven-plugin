package com.cosium.code.format;

import io.takari.maven.testing.executor.MavenRuntime;
import io.takari.maven.testing.executor.MavenVersions;

/**
 * @author Réda Housni Alaoui
 */
@MavenVersions({"3.5.0"})
public class NonRootModuleTest extends AbstractMavenModuleTest {
  public NonRootModuleTest(MavenRuntime.MavenRuntimeBuilder mavenBuilder) throws Exception {
    super(mavenBuilder, "non-root-module", "module");
  }
}
