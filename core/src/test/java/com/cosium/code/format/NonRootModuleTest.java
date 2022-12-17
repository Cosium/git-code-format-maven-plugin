package com.cosium.code.format;

import io.takari.maven.testing.executor.MavenRuntime;

/**
 * @author Réda Housni Alaoui
 */
public class NonRootModuleTest extends AbstractMavenModuleTest {
  public NonRootModuleTest(MavenRuntime.MavenRuntimeBuilder mavenBuilder) throws Exception {
    super(mavenBuilder, "non-root-module", "module");
  }
}
