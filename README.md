# AADL Language Server

This repository contains the Java/Xtext language server for the Architecture
Analysis & Design Language (AADL).

The server depends on OSATE through the pinned `osate2/` Git submodule. A
language-server revision therefore records the exact OSATE source revision used
to build and test it.

## Prerequisites

- Git
- JDK 21 or newer
- Maven 3.9 or newer

## Clone

```bash
git clone --recurse-submodules <repository-url>
cd aadl-language-server
```

For an existing clone:

```bash
git submodule update --init osate2
```

The submodule uses a normal clone from `https://github.com/osate/osate2.git`.
No Git reference repository is configured.

## Build OSATE and the language server

```bash
./scripts/build-test-release
```

The script performs two builds:

1. Build and test the pinned OSATE source, including its p2 repository.
2. Build and test the language server against that generated repository.

The language-server p2 repository is written to:

```text
releng/org.osate.aadl.ls.repository/target/repository/
```

Build provenance is written to:

```text
target/build-provenance.properties
```

## Rebuild only the language server

After OSATE has been built:

```bash
mvn clean verify -Dtycho.localArtifacts=ignore
```

## Update OSATE

Update the submodule deliberately, validate the complete build, and commit the
new gitlink:

```bash
git -C osate2 fetch origin
git -C osate2 checkout <osate-commit>
./scripts/build-test-release
git add osate2 pom.xml
```

If the selected OSATE commit changes the OSATE parent version, update the
version in the root `pom.xml` in the same commit.

## Release provenance

Test-release artifacts must retain:

- the language-server commit SHA;
- the OSATE submodule commit SHA;
- the expected OSATE gitlink SHA;
- the Maven project versions; and
- the build timestamp.

The build script rejects a dirty or mismatched OSATE submodule.
