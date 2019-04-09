[![Maven Central Latest](https://img.shields.io/maven-central/v/com.cosium.code/maven-git-code-format.svg)](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.cosium.code%22%20AND%20a%3A%22maven-git-code-format%22)
[![Build Status](https://travis-ci.org/Cosium/maven-git-code-format.svg?branch=master)](https://travis-ci.org/Cosium/maven-git-code-format)

# Maven Git Code Format

A maven plugin that automatically deploys [google-java-format](https://github.com/google/google-java-format) code formatter as a `pre-commit` git hook.  
On commit, the hook will automatically format staged java files.

### Automatic code format activation

Add this to your maven project **root** pom.xml :

```xml
<build>
  <plugins>
    <plugin>
      <groupId>com.cosium.code</groupId>
      <artifactId>maven-git-code-format</artifactId>
      <version>${maven-git-code-format.version}</version>
      <executions>
        <execution>
          <goals>
            <goal>install-hooks</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

### Manual code formatting

```console
mvn git-code-format:format-code -DglobPattern=**/*
```

### Manual code format validation

```console
mvn git-code-format:validate-code-format -DglobPattern=**/*
```

### How the hook works

On the `initialize` maven phase, `git-code-format:install-hooks` installs a git `pre-commit` hook that looks like this :
```bash
#!/bin/bash
/usr/share/apache-maven-3.5.0/bin/mvn git-code-format:on-pre-commit
```

On `pre-commit` git phase, the hook triggers the `git-code-format:on-pre-commit` which formats the code of the modified java files using `google-java-format`. 