# AGENTS.md

This file provides guidance to AI agents when working with code in this repository.

## Overview

This is the AADL Language Server based on OSATE 2.19.0, implementing the Language Server Protocol (LSP) for the Architecture Analysis & Design Language (AADL). It provides language services for VSCode and other LSP-compatible editors.

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

## Building

**You cannot build individual modules of the language server.** The Tycho build
resolves the whole reactor together — always build from the aggregator root, not
from a single module directory.

### Maven Build

Run the Maven launch configuration `aadl.ls.releng.launch` or execute:

```bash
cd releng/aadl.ls.releng
mvn clean verify -f ../.. -Dtycho.localArtifacts=ignore
```

The build produces a p2 repository with all required plugin JARs in:
`releng/org.osate.aadl.ls.repository/target/repository/plugins/`

**`.mvn` marker is load-bearing.** `pom.xml` resolves the OSATE p2 repository via
`file://${maven.multiModuleProjectDirectory}/../osate2/...`, which must point at the
sibling `osate2` checkout. A `.mvn/` directory at the `aadl-language-server` root pins
`maven.multiModuleProjectDirectory` to that root for every invocation style (`-f
osate2-server/pom.xml` from the repo root, `mvn` from inside a module, etc.). Without
it, `-f osate2-server/pom.xml` resolves the repository to the nonexistent
`aadl-language-server/osate2` and the build fails with "No repository found at ...".
Do not delete `aadl-language-server/.mvn/`.

### Prerequisites

- OSATE development environment (see osate.org)
- Import the setup file: `aadl-ls.setup`
- Java 21+
- Maven with Tycho support

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

2. **Client Side** - Edit VSCode extension:
   - Add command to `package.json`
   - Implement handler in `extension.ts`

**Note**: OSATE analyses may need modification to work without Eclipse workbench (e.g., use EMF `UriConverter` instead of Eclipse `IFile` for file I/O).

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

## Common Tasks

**Rebuild after code changes**: Run `aadl.ls.releng.launch` or `mvn clean verify` from root

**Test with VSCode client**: Build server, copy plugins to `vscode-client/server/aadl/lib`, package extension

**Add annex support**: Follow pattern in `ErrorModelLsSetup.java` and `ErrorModelLsRuntimeModule.java`

**Debug scope resolution issues**: Set breakpoints in `Aadl2LsGlobalScopeProvider.getScope()`

**Debug command execution**: Set breakpoints in `CommandService.execute()`
