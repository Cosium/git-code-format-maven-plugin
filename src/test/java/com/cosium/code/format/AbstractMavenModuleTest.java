package com.cosium.code.format;

import static org.assertj.core.api.Assertions.assertThat;

import io.takari.maven.testing.executor.MavenExecution;
import io.takari.maven.testing.executor.MavenRuntime;
import java.nio.file.Paths;
import org.junit.Test;

/** @author Réda Housni Alaoui */
public abstract class AbstractMavenModuleTest extends AbstractTest {

  private final String badFormatJava;
  private final String mavenModuleDirectory;

  public AbstractMavenModuleTest(
      MavenRuntime.MavenRuntimeBuilder mavenBuilder,
      String projectRootDirectoryName,
      String mavenModuleDirectory)
      throws Exception {
    super(mavenBuilder, projectRootDirectoryName);
    this.mavenModuleDirectory = mavenModuleDirectory;
    this.badFormatJava =
        Paths.get(mavenModuleDirectory).resolve("src/main/java/BadFormat.java").toString();
  }

  @Test
  public void GIVEN_bad_formatted_files_WHEN_format_code_THEN_all_files_should_have_correct_format()
      throws Exception {
    mavenExecution()
        .withCliOptions(goalCliOption("validate-code-format"))
        .execute()
        .assertLogText("is not correctly formatted");

    mavenExecution().withCliOptions(goalCliOption("format-code")).execute().assertErrorFreeLog();

    mavenExecution()
        .withCliOptions(goalCliOption("validate-code-format"))
        .execute()
        .assertErrorFreeLog();

    assertMatchExpected(badFormatJava);
  }

  @Test
  public void
      GIVEN_bad_formatted_file_WHEN_adding_and_committing_it_THEN_it_should_have_correct_format()
          throws Exception {
    mavenExecution()
        .withCliOptions(goalCliOption("validate-code-format"))
        .execute()
        .assertLogText("is not correctly formatted");

    mavenExecution().execute("initialize").assertErrorFreeLog();

    touch(badFormatJava);

    jGit().add().addFilepattern(".").call();
    jGit()
        .commit()
        .setCommitter(gitIdentity())
        .setAuthor(gitIdentity())
        .setMessage("Trying to commit badly formatted file")
        .call();

    mavenExecution()
        .withCliOptions(goalCliOption("validate-code-format"))
        .execute()
        .assertErrorFreeLog();

    assertMatchExpected(badFormatJava);
  }

  @Test
  public void
      GIVEN_bad_formatted_generated_file_WHEN_formatting_THEN_generated_file_should_be_skipped()
          throws Exception {
    String generatedSourceFile =
        Paths.get(mavenModuleDirectory)
            .resolve("target/generated-sources/GeneratedBadFormat.java")
            .toString();
    String oldChecksum = sha1(generatedSourceFile);

    mavenExecution().withCliOptions(goalCliOption("format-code")).execute();

    String newChecksum = sha1(generatedSourceFile);
    assertThat(newChecksum).isEqualTo(oldChecksum);
  }

  @Test
  public void
      GIVEN_bad_formatted_files_WHEN_format_code_with_aosp_enabled_THEN_all_files_should_be_formatted_according_to_aosp()
          throws Exception {
    mavenExecution()
        .withCliOptions(goalCliOption("validate-code-format"), "-Daosp=true")
        .execute()
        .assertLogText("is not correctly formatted");

    mavenExecution()
        .withCliOptions(goalCliOption("format-code"), "-Daosp=true")
        .execute()
        .assertErrorFreeLog();

    mavenExecution()
        .withCliOptions(goalCliOption("validate-code-format"), "-Daosp=true")
        .execute()
        .assertErrorFreeLog();

    mavenExecution()
        .withCliOptions(goalCliOption("validate-code-format"))
        .execute()
        .assertLogText("is not correctly formatted");
  }

  @Test
  public void GIVEN_bad_formatted_file_WHEN_committing_all_THEN_it_should_have_correct_format()
      throws Exception {
    mavenExecution()
        .withCliOptions(goalCliOption("validate-code-format"))
        .execute()
        .assertLogText("is not correctly formatted");

    mavenExecution().execute("initialize").assertErrorFreeLog();

    touch(badFormatJava);

    // When using CommitCommand 'all' option, JGit trigger the precommit hooks before adding the files to staging.
    // See https://bugs.eclipse.org/bugs/show_bug.cgi?id=577333
    // Until it is fixed, we perform 'all' option staging operation before calling commit manually.
    jGit().add().addFilepattern(".").setUpdate(true).call();
    jGit()
        .commit()
        .setCommitter(gitIdentity())
        .setAuthor(gitIdentity())
        .setMessage("Trying to commit badly formatted file")
        .call();

    mavenExecution()
        .withCliOptions(goalCliOption("validate-code-format"))
        .execute()
        .assertErrorFreeLog();

    assertMatchExpected(badFormatJava);
  }

  private MavenExecution mavenExecution() {
    return buildMavenExecution(projectRoot().resolve(mavenModuleDirectory));
  }
}
