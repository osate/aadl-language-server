# AGENTS.md

This file provides guidance to AI agents when working with code in this repository.

## Overview

This is the standalone AADL Language Server repository, implementing the
Language Server Protocol (LSP) for the Architecture Analysis & Design Language
(AADL). It provides language services for VS Code and other LSP-compatible
editors. The current build is pinned to OSATE 2.19.0 through a Git submodule.

**Technology Stack**: Java 21, Maven/Tycho, Xtext, Eclipse LSP4J, Google Guice

## Project Structure

The project follows Eclipse plugin architecture with Maven/Tycho build:

- **org.osate.aadl.ls/** - The language server bundle. Source root: `src/org/osate/aadl/ls/`.
  - Top-level package `org.osate.aadl.ls` - launchers and the bound server module:
    - `Aadl2ServerLauncher.java` - Socket-based server launcher (for testing)
    - `RunAadl2Server.java` - Standard stdio-based launcher (production)
    - `AadlServerModule.java` - Custom `ServerModule` with multi-root workspace support
  - `setup/` - Xtext/Guice wiring:
    - `Aadl2LsSetup.java`, `Aadl2LsRuntimeModule.java`, `Aadl2LsIdeModule.java`
    - `ErrorModelLsSetup.java`, `ErrorModelLsRuntimeModule.java` - Error Model annex support
  - `scoping/` - Standalone-mode scope/project plumbing:
    - `Aadl2LsGlobalScopeProvider.java` - Custom scope provider for language server mode
    - `Aadl2LsProjectDescriptionFactory.java` - Reads project dependencies from `.project` XML files so cross-project references work without an Eclipse workspace
  - `commands/` - Custom LSP `workspace/executeCommand` handlers:
    - `CommandService.java` - Dispatcher, registers commands and routes by name
    - `Command.java` - Command interface
    - `InstantiateCommand.java`, `AnalyzeLatencyCommand.java` - Per-command logic
    - `CommandUtil.java` - Shared helpers (argument parsing, URI path cleanup)
  - `services/` - LSP feature contributions bound in `Aadl2LsIdeModule`:
    - `AadlHoverService.java` - Hover content (HTML→Markdown documentation)
    - `AadlSymbolNameProvider.java` - Document symbol naming (non-qualified names)
    - `AadlLanguageServerExtension.java` - custom JSON-RPC requests for synchronous CLI builds and read-only access to plugin-contributed AADL sources

- **plugins/org.osate.aadl.ls.tests/** - JUnit test fragment of `org.osate.aadl.ls`
- **releng/org.osate.aadl.ls.repository/** - Eclipse p2 repository packaging
- **releng/aadl.ls.releng/** - Build configuration and launch files
- **osate2/** - Pinned `osate/osate2` Git submodule and source of the parent POM,
  target platform, bundles, and generated OSATE p2 repository
- **scripts/build-test-release** - Reproducible two-phase OSATE and language-server build
- **.github/workflows/test-release.yml** - Manual and `test-*` tag test-release workflow

## Dependency on OSATE

The `osate2/` directory is a Git submodule for
`https://github.com/osate/osate2.git`. The committed gitlink is the authoritative
OSATE dependency. Do not make release builds follow a branch dynamically.

Initialize it after cloning:

```bash
git submodule update --init osate2
```

Do not configure a Git reference repository for the submodule.

Treat the submodule as a separate repository:

- For ordinary language-server work, do not modify or advance `osate2/`.
- Before making an explicitly requested OSATE change, read `osate2/AGENTS.md`.
- Commit OSATE source changes in the OSATE repository, not in this repository.
  This repository records only the resulting OSATE commit through its gitlink.
- A dirty or mismatched submodule is a release-build failure.
- When updating the gitlink, use an exact reviewed commit. If its OSATE parent
  version differs, update the root `pom.xml` parent version in the same change.

## Building

**You cannot build individual modules of the language server.** The Tycho build
resolves the whole reactor together — always build from the aggregator root, not
from a single module directory.

### Prerequisites

- Git with the `osate2/` submodule initialized
- JDK 21+
- Maven 3.9+
- Network and Maven/p2 cache access for a clean online build

### Complete test-release build

Build OSATE first and then build the language server:

```bash
./scripts/build-test-release
```

This is intentionally a two-phase build. The language-server Tycho reactor
cannot resolve the OSATE p2 repository until the OSATE reactor has finished
creating it. The script:

1. verifies that `osate2/` is clean and matches the committed gitlink;
2. runs the complete OSATE reactor, including tests and p2/product assembly;
3. runs the language-server reactor with `clean verify`;
4. rejects unexpected duplicate bundle versions, allowing only the required
   two `org.antlr.runtime` versions; and
5. writes `target/build-provenance.properties`.

Do not claim the test-release build passed until both Maven invocations finish
successfully.

### Language-server-only rebuild

The OSATE p2 repository must already exist under the submodule. Run the Maven
launch configuration `aadl.ls.releng.launch` or execute:

```bash
mvn clean verify -Dtycho.localArtifacts=ignore
```

The build produces a p2 repository with all required plugin JARs in:
`releng/org.osate.aadl.ls.repository/target/repository/plugins/`

**`.mvn` marker is load-bearing.** It pins
`${maven.multiModuleProjectDirectory}` to this repository root so `pom.xml`
resolves the generated OSATE p2 repository under `osate2/`. Do not delete it.

### Test validation

- Use `verify`, not only `test`; Tycho may compile tests without executing them
  during the `test` lifecycle.
- The test bundle intentionally selects `org.osate.aadl.ls.tests.AllTests` so
  each test runs once.
- Inspect
  `plugins/org.osate.aadl.ls.tests/target/surefire-reports/TEST-org.osate.aadl.ls.tests.AllTests.xml`
  and require a nonzero `tests` count with zero failures and errors.
- Platform-specific tests may be skipped on unsupported operating systems;
  report those skips explicitly.
- Linux CI runs the complete build under `xvfb-run` because OSATE contains
  Eclipse test infrastructure that may require a display.
- If the user explicitly requests offline Maven validation, add `-o` to the
  applicable Maven invocations and do not silently fall back to network access.

## Running and Testing

### Running in Eclipse

Two launch configurations are available in `org.osate.aadl.ls/.launch/`:

1. **RunAadl2Server.launch** - Stdio-based server (for VSCode integration)
   - Main class: `org.osate.aadl.ls.RunAadl2Server`
   - Uses Maven classpath provider

2. **Aadl2ServerLauncher.launch** - Socket-based server (for debugging)
   - Main class: `org.osate.aadl.ls.Aadl2ServerLauncher`
   - Args: `-host localhost -port 6215 -trace`
   - Useful for debugging the socket transport with an external LSP client

There is no repository-level Oomph setup file. Import the language-server
projects and the required OSATE projects from the initialized submodule into a
Java 21 Eclipse/OSATE development workspace.

### Debug Mode

Set VM argument `-Daadl.ls.debug=true` to enable:
- Log files in workspace `.metadata/.out-*.log` and `.error-*.log`

## Architecture

### Language Server Initialization

The server uses Xtext's language server framework with custom setup:

1. **Annex Processing**: `EcorePlugin.ExtensionProcessor.process(null)` loads plugin metadata for annexes
2. **Dependency Injection**: Guice modules combine runtime and IDE modules
3. **Global Scope**: Custom `Aadl2LsGlobalScopeProvider` loads contributed AADL property sets on-demand

### AADL is case-insensitive

AADL identifiers (package names, classifier names, feature names, etc.) are
case-insensitive per the AADL standard. Any code that compares an AADL identifier
to a user-supplied string — for example matching a `<package>::<classifier>`
argument against the model — must use case-insensitive comparison
(`String.equalsIgnoreCase`, or normalize both sides). See `CommandService.java`
for an example in the `aadl.instantiate` lookup.

### Key Differences from OSATE

- **No Eclipse Workbench**: Runs standalone without UI components
- **Custom Global Scope**: Uses `LoadOnDemandResourceDescriptions` instead of Eclipse container mechanism
- **Multi-Root Workspace Support**: VSCode workspace can contain multiple AADL projects with cross-project references
- **Contributed Resources**: Pre-declared AADL property sets loaded via `PluginSupportUtil.getContributedAadl()`

### Multi-Root Workspace Configuration

The server supports VSCode multi-root workspaces via Xtext's `MultiProjectWorkspaceConfigFactory`:

- **Server Module**: `AadlServerModule.java` binds `IMultiRootWorkspaceConfigFactory` to `MultiProjectWorkspaceConfigFactory`
- **Client Support**: VSCode extension sends workspace folder information in `initializationOptions`
- **Dynamic Updates**: Client handles `workspace/didChangeWorkspaceFolders` notifications

**Benefits**:
- Multiple AADL projects in a single VSCode workspace
- Cross-project references and dependencies (similar to OSATE Eclipse projects)
- Each workspace folder can contain AADL project structures
- Backward compatible: single-folder workspaces continue to work

**Usage**: Create a `.code-workspace` file with multiple AADL project folders, or use VSCode's "Add Folder to Workspace" command.

### Custom LSP Commands

Implemented in `CommandService.java`:

- **aadl.instantiate** - Instantiates component implementation to `.aaxl2` file
  - Args: URI of AADL file, component implementation name
  - Uses `InstantiateModel.instantiate(ComponentImplementation)`

- **aadl.analyze.latency** - Runs flow latency analysis on instance model
  - Args: URI of instance (`.aaxl2`) file and five optional analysis-setting booleans
  - Uses `FlowLatencyAnalysisSwitch`

- **aadl.analyze.busLoad** - Runs bus load analysis on instance model
  - Args: URI of instance (`.aaxl2`) file
  - Uses `NewBusLoadAnalysis`

- **aadl.analyze.reachability** - Runs SOM mode reachability analysis on instance model
  - Args: URI of instance (`.aaxl2`) file and optional report-format booleans
  - Uses `ReachabilityAnalyzer`

## Adding New LSP Commands

To add a new command (e.g., for analysis):

1. **Server Side** - in `commands/`:
   - Add a class implementing `Command`. Put the LSP command name in a `NAME` constant
     and run the work inside `ILanguageServerAccess.doRead(...)` so it sees the live
     index. Reuse `CommandUtil` for argument parsing and URI cleanup.
   - Register the new command in `CommandService`'s constructor with `register(...)`.

2. **Client Side** - Update each client repository that exposes the command.
   The client changes are released independently from this repository.

**Note**: OSATE analyses may need modification to work without Eclipse workbench (e.g., use EMF `UriConverter` instead of Eclipse `IFile` for file I/O).

Add or update tests in `plugins/org.osate.aadl.ls.tests/` for every changed
server behavior. Prefer LSP-level coverage for protocol-visible behavior and
unit tests for isolated helpers. Store AADL fixtures under the test bundle's
`test-models/` tree rather than embedding substantial models in Java strings.

## Dependency Injection Customization

Xtext services are customized via Guice in `Aadl2LsIdeModule`:

- `bindIExecutableCommandService()` → `CommandService`
- `bindDocumentSymbolNameProvider()` → `AadlSymbolNameProvider`
- `bindHoverService()` → `AadlHoverService`
- `bindILanguageServerExtension()` → `AadlLanguageServerExtension`

Runtime bindings in `Aadl2LsRuntimeModule`:

- `bindIGlobalScopeProvider()` → `Aadl2LsGlobalScopeProvider`
- `bindIEClassGlobalScopeProvider()` → `Aadl2LsGlobalScopeProvider`

## Important Files

- **MANIFEST.MF** files - Define OSGi bundle dependencies
- **pom.xml** files - Maven/Tycho build configuration
- **build.properties** - Defines files to include in plugin JARs
- **.gitmodules** - Defines the public OSATE submodule URL
- **scripts/build-test-release** - Defines the authoritative test-release build and checks
- **target/build-provenance.properties** - Generated record of language-server and OSATE revisions

## Change and Commit Hygiene

- Preserve the full repository-standard copyright and license headers.
- Keep changes to the language server, OSATE gitlink, and release tooling
  independently reviewable when practical.
- Before committing, run `git diff --check`, inspect the staged diff, and verify
  both the superproject and submodule status.
- When asked to commit, use a concise subject, a blank line, and a descriptive
  body explaining what changed and why. Use a subject-only commit only when
  explicitly requested.
- Do not push, create a release, or move the OSATE gitlink unless requested.

## Common Tasks

**Rebuild after code changes**: Run `aadl.ls.releng.launch` or `mvn clean verify` from root

**Create a test-release build**: Run `./scripts/build-test-release`

**Check submodule state**: Run `git submodule status` and `git -C osate2 status --short --branch`

**Update OSATE intentionally**: Check out the reviewed OSATE commit in
`osate2/`, update the root parent version if required, run the complete
test-release build, and stage the `osate2` gitlink

**Add annex support**: Follow pattern in `ErrorModelLsSetup.java` and `ErrorModelLsRuntimeModule.java`

**Debug scope resolution issues**: Set breakpoints in `Aadl2LsGlobalScopeProvider.getScope()`

**Debug command execution**: Set breakpoints in `CommandService.execute()`
