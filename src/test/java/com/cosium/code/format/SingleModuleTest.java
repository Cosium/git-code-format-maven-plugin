package com.cosium.code.format;

import io.takari.maven.testing.executor.MavenRuntime;
import org.junit.Test;

import java.nio.file.Paths;

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
      GIVEN_bad_formatted_file_WHEN_adding_then_committing_it_THEN_it_should_have_correct_format()
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

  @Test(expected = AssertionError.class)
  public void GIVEN_bad_formatted_file_WHEN_committing_it_THEN_it_should_have_correct_format()
      throws Exception {
    buildMavenExecution()
        .withCliOptions(
            GROUP_ID + ":" + ARTIFACT_ID + ":validate-code-format", "-DglobPattern=**/*")
        .execute()
        .assertLogText("[ERROR]");

    buildMavenExecution().execute("initialize").assertErrorFreeLog();

    touch(Paths.get("src/main/java").resolve("BadFormat.java"));

    getGit().commit().setAll(true).setMessage("Trying to commit badly formatted file").call();

    buildMavenExecution()
        .withCliOptions(
            GROUP_ID + ":" + ARTIFACT_ID + ":validate-code-format", "-DglobPattern=**/*")
        .execute()
        .assertErrorFreeLog();
  }
}
