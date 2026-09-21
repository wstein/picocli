# Changelog

All notable changes to this fork of picocli will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [v4.9.2] - 2026-09-21

### Added
- **`mixinStandardHelpOptions` in picocli-spec**:
  - DSL support: `mixinStandardHelpOptions` directive inside command blocks.
  - JSON Schema support: `"mixinStandardHelpOptions": true` property on command objects in `command-spec.schema.json`.
  - Core integration: Automatically calls `CommandSpec#mixinStandardHelpOptions(true)` without manual `-h`/`--help` and `-V`/`--version` definitions.
  - Lossless Serialization: `CommandSpecJson.write()` and `CommandSpecDsl.write()` emit `mixinStandardHelpOptions` while suppressing duplicated synthetic standard help options in flat option lists.
- **Complete Flix CLI Spec Suite (v0.60.0 – v0.76.2)**:
  - Scanned and mapped all distinct CLI evolution points across 31 git tags in flix's history, producing targeted `.picocli` specifications and test suites:
    - `flix-0.67.0.picocli`: Added `clean` subcommand.
    - `flix-0.67.1.picocli`: Added `format` subcommand, file-argument restrictions, removal of `--args`, reduction to 10 experimental flags.
    - `flix-0.68.0.picocli`: Added `eff-check` and `eff-lock` subcommands, removed compiler `--explain` flag.
    - `flix-0.73.0.picocli`: Added `--top` compiler profiling flag.
    - `flix-0.75.2.picocli`: Added experimental `--Xnewmono` flag (11 experimental flags).
    - `flix-0.75.3.picocli`: Added `build-classes` subcommand, updated descriptions for `clean` and `--Xprint-phases`.
    - `flix-0.76.0.picocli`: Added `stat` subcommand, added `--Xverify`, removed `--Xsummary`.
  - Added CLI version range progression matrix in `picocli-spec/examples/README.md`.
- **Repository AI Agent Guidance**: Added canonical `AGENTS.md` and `.github/copilot-instructions.md` for AI workflows.

### Changed
- **Flix Examples Modernization**: Updated all flix specs to use `mixinStandardHelpOptions` and `Path[]` for positional file arguments.

---

## [v4.9.1] - 2026-09-21

### Added
- **Java 11 Target**: `picocli-spec` and `picocli-spec-tool` now target Java 11 bytecode (`sourceCompatibility = 11`, `targetCompatibility = 11`).
- **Extended `ArgTypes` Vocabulary**: Added `Path`, `URI`, `URL`, `BigDecimal`, and `BigInteger` with built-in converter mappings and JSON schema synchronization.
- **Batch Spec Merging**: Added `CommandSpecMerger.mergeAll(CommandSpec uber, Collection<CommandSpec> imported)` alongside varargs `merge()`.
- **Manpage Overwrite Guard**: `picospec manpage` checks for existing `.adoc` files before writing and requires `--overwrite` to prevent accidental loss.
- **Spec-Driven Development (SDD) Docs**:
  - Split `picocli-spec/README.md` into `REQUIREMENTS.md` (FR-1–FR-7, NFR-1–NFR-4), `DESIGN.md` (EBNF grammar, data flow, schema reference), and a concise `README.md`.
  - Added Architecture Decision Records under `docs/adr/`: `ADR-001` (Custom Minimal JSON Parser), `ADR-002` (Dual DSL & JSON Strategy), and `ADR-003` (Two-Module Split).
- **Fork-First Root README**: Overhauled repository `README.md` to highlight fork features, coordinates, and usage.

### Changed
- **JDK Support Policy**: CI matrix streamlined to LTS JDK releases (8, 11, 17, 21); dropped non-blocking intermediate JDK jobs and legacy `build-java-6-7` CI job.
- **CI Dependabot Configuration**: Auto-merge restricted to `version-update:semver-patch`.

### Fixed
- **DSL Multi-line Descriptions**: `CommandSpecDsl.parse()` splits descriptions on newline escapes (`\n`) and carriage returns into `String[]`, ensuring round-trip fidelity with `write()`.
- **Subcommand Definitions Guard**: `CommandSpecJson.read()` rejects nested `definitions` blocks on subcommand JSON objects with a helpful error.
- **Fish Completion Robustness**: Verified quoting of single quotes, backslashes, and script/subcommand names in fish completions.

---

## [v4.9.0] - 2026-09-20

### Added

#### `picocli-spec` (New Module)
- **DSL Specification Parser & Serializer**: Define complete picocli `CommandSpec` trees using a concise, human-friendly DSL (`.picocli`) via `CommandSpecDsl.parse()` and `CommandSpecDsl.write()`.
- **JSON Specification Reader & Writer**: Parse and serialize `CommandSpec` trees from/to JSON adhering to `command-spec.schema.json` via `CommandSpecJson.read()` and `CommandSpecJson.write()`.
- **Zero Runtime Dependencies**: Embedded lightweight JSON parser (`picocli.spec.json.Json`) ensuring zero transitive dependencies beyond picocli core.
- **Spec Composition**: `CommandSpecMerger.merge(CommandSpec uber, CommandSpec... imported)` attaches external specs as subcommands onto a host CLI.
- **Definition Reuse & Bundles**:
  - Top-level `definitions` blocks in DSL and JSON for defining reusable options and positional parameters.
  - `bundle` declarations grouping options, positionals, and argument groups, expandable into commands via `use <bundleName>`.
  - Nested bundle composition (`bundle` using an earlier `bundle`).
- **Base Type Vocabulary (`ArgTypes`)**:
  - Built-in mappings for `String`, `boolean`, `int`, `long`, `double`, and `File`.
  - Array suffix support (e.g. `File[]`, `String[]`) for multi-value arguments.
- **Ambiguity Validation (`SpecValidator`)**: Fails fast during parse or merge on parsing ambiguities, such as an option's `defaultValue` colliding with a sibling subcommand's name.

#### `picocli-spec-tool` (New Module)
- **Standalone `picospec` CLI**: A self-contained utility for working with spec files without writing Java code.
- **Subcommands**:
  - `preview <spec-file>`: Renders recursive usage help for a spec and all its subcommands.
  - `validate <spec-file>`: Fast load-only pass/fail validation for pre-commit hooks and CI.
  - `completion <spec-file>`: Generates bash, zsh, or fish shell completion scripts.
  - `manpage <spec-file>`: Generates AsciiDoc man pages via `picocli-codegen`.
  - `convert <spec-file>`: Losslessly converts between `.picocli` DSL and JSON formats.
- **Packaging**: Self-contained executable fat-jar (`picocli-spec-tool-<version>-all.jar`) built via Gradle Shadow plugin.

#### Core Enhancements & Features
- **On-Demand Help Sections (`helpSection`)**:
  - Added `helpSection` attribute to `@Option` and `@ArgGroup` annotations and model classes.
  - Tagged options (e.g. `--Xhelp`) act as on-demand triggers that render only their tagged section and exit.
  - Tagged options and groups are excluded from default `--help` output while remaining discoverable in shell autocompletions.
- **Fish Shell Completion**:
  - Added `AutoComplete.fish()` generating declarative `complete -c` scripts for fish shell.
  - Added `--shell=fish` option to `generate-completion` and the standalone `AutoComplete` CLI.

### Changed
- **Hidden Argument Groups**: Groups marked `hidden` are flattened directly onto their parent command with `hidden = true`, avoiding empty synopsis brackets (`"[]"`) or orphaned headings.

---

## [v4.8.0] - 2026-08-15

### Added
- Initial implementation of `picocli-spec` DSL and JSON schema reader/writer.
- Publishing of versioned JSON schemas and Javadoc to GitHub Pages.

---

## [v4.7.8] - 2026-06-01

### Added
- Initial fish shell autocompletion support (`AutoComplete.fish()`).
- Self-hosted release workflow publishing artifacts to GitHub Pages and GitHub Releases.
