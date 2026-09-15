package com.cosium.code.format;

import io.takari.maven.testing.executor.MavenRuntime;
import io.takari.maven.testing.executor.MavenVersions;

@MavenVersions({"3.5.0"})
public class SingleModuleTest extends AbstractMavenModuleTest {

  public SingleModuleTest(MavenRuntime.MavenRuntimeBuilder mavenBuilder) throws Exception {
    super(mavenBuilder, "single-module", "");
  }
}
