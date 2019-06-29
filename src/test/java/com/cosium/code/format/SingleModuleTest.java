package com.cosium.code.format;

import static org.assertj.core.api.Assertions.assertThat;

import io.takari.maven.testing.executor.MavenExecution;
import io.takari.maven.testing.executor.MavenRuntime;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;

public class SingleModuleTest extends AbstractTest {

  public SingleModuleTest(MavenRuntime.MavenRuntimeBuilder mavenBuilder) throws Exception {
    super(mavenBuilder, "single-module");
  }

  @Test
  public void GIVEN_bad_formatted_files_WHEN_format_code_THEN_all_files_should_have_correct_format()
      throws Exception {
    mavenExecution()
        .withCliOptions(
            GROUP_ID + ":" + ARTIFACT_ID + ":validate-code-format", "-DglobPattern=**/*")
        .execute()
        .assertLogText("[ERROR]");

    mavenExecution()
        .withCliOptions(GROUP_ID + ":" + ARTIFACT_ID + ":format-code", "-DglobPattern=**/*")
        .execute()
        .assertErrorFreeLog();

    mavenExecution()
        .withCliOptions(
            GROUP_ID + ":" + ARTIFACT_ID + ":validate-code-format", "-DglobPattern=**/*")
        .execute()
        .assertErrorFreeLog();
  }

  @Test
  public void
      GIVEN_bad_formatted_file_WHEN_adding_and_committing_it_THEN_it_should_have_correct_format()
          throws Exception {
    mavenExecution()
        .withCliOptions(
            GROUP_ID + ":" + ARTIFACT_ID + ":validate-code-format", "-DglobPattern=**/*")
        .execute()
        .assertLogText("[ERROR]");

    mavenExecution().execute("initialize").assertErrorFreeLog();

    touch(Paths.get("src/main/java").resolve("BadFormat.java"));

    getGit().add().addFilepattern(".").call();
    getGit().commit().setMessage("Trying to commit badly formatted file").call();

    mavenExecution()
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

    mavenExecution()
        .withCliOptions(GROUP_ID + ":" + ARTIFACT_ID + ":format-code", "-DglobPattern=**/*")
        .execute();

    String newChecksum;
    try (InputStream inputStream = Files.newInputStream(generatedSourceFile)) {
      newChecksum = DigestUtils.md5Hex(inputStream);
    }

    assertThat(newChecksum).isEqualTo(oldChecksum);
  }

  private MavenExecution mavenExecution() {
    return buildMavenExecution(projectRoot());
  }
}
