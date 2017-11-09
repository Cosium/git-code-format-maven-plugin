[![Maven Central Latest](https://img.shields.io/maven-central/v/com.cosium.code/maven-git-code-format.svg)](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.cosium.code%22%20AND%20a%3A%22maven-git-code-format%22)

# Maven Git Code Format

A maven plugin that automatically deploys [google-java-format](https://github.com/google/google-java-format) code formatter as a `pre-commit` git hook.  
The hook will format staged java files.

### How to use it

Drop it in your maven build :

```xml
<plugin>
  <groupId>com.cosium.code</groupId>
  <artifactId>maven-git-code-format</artifactId>
  <version>1.13</version>
  <executions>
    <execution>
      <goals>
        <goal>install-hooks</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

### How it works

On the `initialize` maven phase, `git-code-format:install-hooks` installs a git `pre-commit` hook that looks like this :
```bash
#!/bin/bash
/usr/share/apache-maven-3.5.0/bin/mvn git-code-format:on-pre-commit
```

On `pre-commit` git phase, the hook triggers the `git-code-format:on-pre-commit` which formats the code of the modified java files using `google-java-format`. 