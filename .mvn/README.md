# Maven project root marker

This directory marks the repository root for Maven.

The root `pom.xml` resolves the OSATE p2 repository at:

```text
${maven.multiModuleProjectDirectory}/osate2/releng/org.osate.build.repository/target/repository/
```

Keeping this marker makes that path stable when Maven is invoked with `-f` or
from a subdirectory.
