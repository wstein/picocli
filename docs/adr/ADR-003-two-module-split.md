# ADR-003: Two-Module Split (picocli-spec vs picocli-spec-tool)

## Status
Accepted

## Context
The `picocli-spec` feature family provides two main capabilities:
1. Core parsing, model generation, and composition (`CommandSpecDsl`, `CommandSpecJson`, `CommandSpecMerger`, `SpecValidator`).
2. CLI developer tooling: previewing CLI usage help, generating shell completions, generating AsciiDoc man pages, and converting formats.

Generating AsciiDoc man pages depends on `picocli-codegen` (`ManPageGenerator`). If `picocli-spec` bundled man page generation directly, all applications embedding `picocli-spec` at runtime would transitively depend on `picocli-codegen` and its build requirements.

## Decision
Split functionality into two separate Gradle sub-modules:
- **`picocli-spec`**: Zero-dependency runtime library (depends only on `picocli` core). Contains DSL/JSON parsing and serialization, `ArgTypes`, merger, and validator.
- **`picocli-spec-tool`**: Standalone CLI utility (`picospec`). Depends on `picocli-spec`, `picocli-codegen`, and `picocli`. Published as a shadow fat-jar (`picocli-spec-tool-<version>-all.jar`) containing all dependencies for direct command-line execution (`picospec preview|completion|manpage|validate|convert`).

## Consequences

### Positive
- Strict isolation of dependencies: applications embedding spec models remain lightweight and zero-dependency.
- Build tooling (man page generation, shadow jar packaging) remains isolated to the developer tool.
- Clear separation between the specification engine and operator commands.

### Negative
- Users needing man page generation in their build pipelines must invoke the CLI tool or depend on `picocli-spec-tool` rather than `picocli-spec` alone.
