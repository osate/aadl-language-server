# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

This is the AADL Language Server based on OSATE 2.19.0, implementing the Language Server Protocol (LSP) for the Architecture Analysis & Design Language (AADL). It provides language services for VSCode and other LSP-compatible editors.

**Technology Stack**: Java 21, Maven/Tycho, Xtext, Eclipse LSP4J, Google Guice

## Project Structure

The project follows Eclipse plugin architecture with Maven/Tycho build:

- **org.osate.aadl.ls/** - Main launcher bundle with entry points
  - Source root: `src/org/osate/aadl/ls/`
  - `Aadl2ServerLauncher.java` - Socket-based server launcher (for testing)
  - `RunAadl2Server.java` - Standard stdio-based launcher (production)

- **plugins/org.osate.aadl.ls.core/** - Core language server implementation
  - Source root: `src/org/osate/aadl/ls/core/`
  - `AadlServerModule.java` (package `...core`) - Custom ServerModule with multi-root workspace support; kept out of `internal` because it is the bound server module
  - Everything below lives in the `...core.internal` subpackage:
    - `AadlLanguageServerPlugin.java` - Plugin activator and server initialization
    - `AadlServer.java` - Eclipse application entry point
    - `Aadl2LsSetup.java` - Xtext language setup with Guice dependency injection
    - `Aadl2LsRuntimeModule.java` - Runtime bindings (e.g., global scope provider)
    - `Aadl2LsIdeModule.java` - IDE service bindings (commands, symbol mapper)
    - `Aadl2LsGlobalScopeProvider.java` - Custom scope provider for language server mode
    - `Aadl2LsProjectDescriptionFactory.java` - Reads project dependencies from `.project` XML files so cross-project references work without an Eclipse workspace
    - `CommandService.java` - Custom LSP commands (instantiate, analyze latency)
    - `AadlSymbolNameProvider.java` - Document symbol naming (non-qualified names)
    - `ErrorModelLsSetup.java` and `ErrorModelLsRuntimeModule.java` - Error Model annex support

- **releng/org.osate.aadl.ls.repository/** - Eclipse p2 repository packaging
- **releng/aadl.ls.releng/** - Build configuration and launch files

## Building

### Maven Build

Run the Maven launch configuration `aadl.ls.releng.launch` or execute:

```bash
cd releng/aadl.ls.releng
mvn clean verify -f ../.. -Dtycho.localArtifacts=ignore
```

The build produces a p2 repository with all required plugin JARs in:
`releng/org.osate.aadl.ls.repository/target/repository/plugins/`

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
   - Useful for testing with socket-based clients

### Debug Mode

Set VM argument `-Daadl.ls.debug=true` to enable:
- Log files in workspace `.metadata/.out-*.log` and `.error-*.log`
- Logback configuration from `plugins/org.osate.aadl.ls.core/logback.xml`

## Architecture

### Language Server Initialization

The server uses Xtext's language server framework with custom setup:

1. **Annex Processing**: `EcorePlugin.ExtensionProcessor.process(null)` loads plugin metadata for annexes
2. **Dependency Injection**: Guice modules combine runtime and IDE modules
3. **Global Scope**: Custom `Aadl2LsGlobalScopeProvider` loads contributed AADL property sets on-demand

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

- **aadl.instantiate** - Instantiates component implementation to `.aaxl` file
  - Args: URI of AADL file, component implementation name
  - Uses `InstantiateModel.instantiate(ComponentImplementation)`

- **aadl.analyze.latency** - Runs flow latency analysis on instance model
  - Args: URI of instance (`.aaxl`) file
  - Uses `FlowLatencyAnalysisSwitch`

## Adding New LSP Commands

To add a new command (e.g., for analysis):

1. **Server Side** - Edit `CommandService.java`:
   - Add command name to `initialize()` list
   - Add command handler in `execute()` method
   - Access model via `ILanguageServerAccess.doRead()`

2. **Client Side** - Edit VSCode extension:
   - Add command to `package.json`
   - Implement handler in `extension.ts`

**Note**: OSATE analyses may need modification to work without Eclipse workbench (e.g., use EMF `UriConverter` instead of Eclipse `IFile` for file I/O).

## Dependency Injection Customization

Xtext services are customized via Guice in `Aadl2LsIdeModule`:

- `bindIExecutableCommandService()` → `CommandService`
- `bindDocumentSymbolNameProvider()` → `AadlSymbolNameProvider`

Runtime bindings in `Aadl2LsRuntimeModule`:

- `bindIGlobalScopeProvider()` → `Aadl2LsGlobalScopeProvider`
- `bindIEClassGlobalScopeProvider()` → `Aadl2LsGlobalScopeProvider`

## Important Files

- **MANIFEST.MF** files - Define OSGi bundle dependencies
- **plugin.xml** - Registers Eclipse application extension point
- **pom.xml** files - Maven/Tycho build configuration
- **build.properties** - Defines files to include in plugin JARs
- **mkjar.xml** - Ant script for manual JAR creation (alternative to Maven)

## Common Tasks

**Rebuild after code changes**: Run `aadl.ls.releng.launch` or `mvn clean verify` from root

**Test with VSCode client**: Build server, copy plugins to `vscode-client/server/aadl/lib`, package extension

**Add annex support**: Follow pattern in `ErrorModelLsSetup.java` and `ErrorModelLsRuntimeModule.java`

**Debug scope resolution issues**: Set breakpoints in `Aadl2LsGlobalScopeProvider.getScope()`

**Debug command execution**: Set breakpoints in `CommandService.execute()`
