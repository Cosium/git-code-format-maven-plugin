package com.cosium.code.format.maven;

import static org.assertj.core.api.Assertions.assertThat;

import com.cosium.code.format.TestingLog;
import com.cosium.code.format.executable.CommandRunException;
import com.cosium.code.format.executable.CommandRunner;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Réda Housni Alaoui
 */
public class MavenExecutablesTest {

  /** Holds no maven wrapper, leaving the maven installation as the only candidate. */
  @TempDir private Path repository;

  private Map<String, String> systemProperties;
  private TestingCommandRunner commandRunner;
  private MavenExecutables tested;

  @BeforeEach
  public void beforeEach() {
    systemProperties = new HashMap<>();
    commandRunner = new TestingCommandRunner();
    tested =
        new MavenExecutables(
            TestingLog::new, repository, repository, systemProperties::get, commandRunner);
  }

  @Test
  public void testMavenHomeExecutable() {
    systemProperties.put("maven.home", "/opt/maven");
    Path expectedPath = Paths.get("/opt/maven/bin/mvn");
    commandRunner.validExecutables.add(expectedPath.toString());
    Path path = tested.select(false, false);
    assertThat(path).isEqualTo(expectedPath);
  }

  @Test
  public void testMavenHomeDebugExecutable() {
    systemProperties.put("maven.home", "/opt/maven");
    Path expectedPath = Paths.get("/opt/maven/bin/mvnDebug");
    commandRunner.validExecutables.add(expectedPath.toString());
    Path path = tested.select(true, false);
    assertThat(path).isEqualTo(expectedPath);
  }

  @Test
  public void testMavenPathExecutableFallback() {
    systemProperties.put("maven.home", "/opt/maven");
    commandRunner.validExecutables.add("mvn");
    Path path = tested.select(false, false);
    assertThat(path).isEqualTo(Paths.get("mvn"));
  }

  @Test
  public void testMavenPathDebugExecutableFallback() {
    systemProperties.put("maven.home", "/opt/maven");
    commandRunner.validExecutables.add("mvnDebug");
    Path path = tested.select(true, false);
    assertThat(path).isEqualTo(Paths.get("mvnDebug"));
  }

  private static class TestingCommandRunner implements CommandRunner {

    final Set<String> validExecutables = new HashSet<>();

    @Override
    public String run(Path workingDir, Map<String, String> environment, String... command) {
      if (validExecutables.contains(command[0])) {
        return null;
      }
      throw new CommandRunException(1, "");
    }
  }
}
