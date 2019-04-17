package com.cosium.code.format;

import io.takari.maven.testing.executor.MavenRuntime;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class SingleModuleTest extends AbstractTest {

  public SingleModuleTest(MavenRuntime.MavenRuntimeBuilder mavenBuilder) throws Exception {
    super(mavenBuilder, "single-module");
  }

  @Test
  public void GIVEN_bad_formatted_files_WHEN_format_code_THEN_all_files_should_have_correct_format()
      throws Exception {
    buildMavenExecution()
        .withCliOptions(
            GROUP_ID + ":" + ARTIFACT_ID + ":validate-code-format", "-DglobPattern=**/*")
        .execute()
        .assertLogText("[ERROR]");

    buildMavenExecution()
        .withCliOptions(GROUP_ID + ":" + ARTIFACT_ID + ":format-code", "-DglobPattern=**/*")
        .execute()
        .assertErrorFreeLog();

    buildMavenExecution()
        .withCliOptions(
            GROUP_ID + ":" + ARTIFACT_ID + ":validate-code-format", "-DglobPattern=**/*")
        .execute()
        .assertErrorFreeLog();
  }

  @Test
  public void
      GIVEN_bad_formatted_file_WHEN_adding_and_committing_it_THEN_it_should_have_correct_format()
          throws Exception {
    buildMavenExecution()
        .withCliOptions(
            GROUP_ID + ":" + ARTIFACT_ID + ":validate-code-format", "-DglobPattern=**/*")
        .execute()
        .assertLogText("[ERROR]");

    buildMavenExecution().execute("initialize").assertErrorFreeLog();

    touch(Paths.get("src/main/java").resolve("BadFormat.java"));

    getGit().add().addFilepattern(".").call();
    getGit().commit().setMessage("Trying to commit badly formatted file").call();

    buildMavenExecution()
        .withCliOptions(
            GROUP_ID + ":" + ARTIFACT_ID + ":validate-code-format", "-DglobPattern=**/*")
        .execute()
        .assertErrorFreeLog();
  }

  @Test
  public void
      GIVEN_bad_formatted_generated_file_WHEN_formatting_THEN_generated_file_should_be_skipped()
          throws Exception {
    Path generatedSourceFile =
        resolveRelativelyToProjectRoot(
            Paths.get("target/generated-sources").resolve("GeneratedBadFormat.java"));
    String oldChecksum;
    try (InputStream inputStream = Files.newInputStream(generatedSourceFile)) {
      oldChecksum = DigestUtils.md5Hex(inputStream);
    }

    buildMavenExecution()
        .withCliOptions(GROUP_ID + ":" + ARTIFACT_ID + ":format-code", "-DglobPattern=**/*")
        .execute();

    String newChecksum;
    try (InputStream inputStream = Files.newInputStream(generatedSourceFile)) {
      newChecksum = DigestUtils.md5Hex(inputStream);
    }

    assertThat(newChecksum).isEqualTo(oldChecksum);
  }
}
