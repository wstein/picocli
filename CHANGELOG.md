# Changelog

All notable changes to this fork of picocli will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [v4.9.0] - 2026-09-21

### Added

#### `picocli-spec` (New Module)
- **DSL Specification Parser & Serializer**: Define complete picocli `CommandSpec` trees using a concise, human-friendly DSL (`.picocli`) via `CommandSpecDsl.parse()` and `CommandSpecDsl.write()`.
- **JSON Specification Reader & Writer**: Parse and serialize `CommandSpec` trees from/to JSON adhering to `command-spec.schema.json` via `CommandSpecJson.read()` and `CommandSpecJson.write()`.
- **Zero Runtime Dependencies**: Embedded lightweight JSON parser (`picocli.spec.json.Json`) ensuring zero transitive dependencies beyond picocli core.
- **Spec Composition**:
  - `CommandSpecMerger.merge(CommandSpec uber, CommandSpec... imported)` attaches external specs as subcommands onto a host CLI.
  - `CommandSpecMerger.mergeAll(CommandSpec uber, Collection<CommandSpec> imported)` provides explicit collection-based batch composition.
- **Definition Reuse & Bundles**:
  - Top-level `definitions` blocks in DSL and JSON for defining reusable options and positional parameters.
  - `bundle` declarations grouping options, positionals, and argument groups, expandable into commands via `use <bundleName>`.
  - Nested bundle composition (`bundle` using an earlier `bundle`).
- **Rich Type Vocabulary (`ArgTypes`)**:
  - Built-in mappings for `String`, `boolean`, `int`, `long`, `double`, `File`, `Path`, `URI`, `URL`, `BigDecimal`, and `BigInteger`.
  - Array suffix support (e.g. `Path[]`, `File[]`, `String[]`) for multi-value arguments.
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
- **Overwrite Protection**: `picospec manpage` checks for existing `.adoc` files before writing and requires `--overwrite` to prevent accidental clobbering.

#### Core Enhancements & Features
- **On-Demand Help Sections (`helpSection`)**:
  - Added `helpSection` attribute to `@Option` and `@ArgGroup` annotations and model classes.
  - Tagged options (e.g. `--Xhelp`) act as on-demand triggers that render only their tagged section and exit.
  - Tagged options and groups are excluded from default `--help` output while remaining discoverable in shell autocompletions.
- **Fish Shell Completion**:
  - Added `AutoComplete.fish()` generating declarative `complete -c` scripts for fish shell.
  - Added `--shell=fish` option to `generate-completion` and the standalone `AutoComplete` CLI.
  - Single-quote, space, and backslash escaping tested and verified for fish syntax.

#### Documentation & Spec-Driven Development (SDD)
- Split `picocli-spec/README.md` into dedicated SDD artifacts:
  - `REQUIREMENTS.md`: Functional (FR-1–FR-7) and non-functional requirements and constraints.
  - `DESIGN.md`: EBNF grammar, architecture data flow, JSON schema reference, and design notes.
  - `README.md`: Concise quick-start guide.
- Added Architecture Decision Records under `docs/adr/`:
  - `ADR-001`: Custom Minimal JSON Parser for picocli-spec.
  - `ADR-002`: Dual DSL and JSON Format Strategy.
  - `ADR-003`: Two-Module Split (`picocli-spec` vs `picocli-spec-tool`).

### Changed
- **JDK Support Policy**: Target and build against the LTS JDK releases (8, 11, 17, 21). Removed non-blocking intermediate JDK jobs that overwhelmed CI quotas.
- **CI Dependabot Configuration**: Auto-merge restricted to `version-update:semver-patch` to prevent unreviewed breaking changes from minor version bumps.
- **Hidden Argument Groups**: Groups marked `hidden` are now flattened directly onto their parent command with `hidden = true`, avoiding empty synopsis brackets (`"[]"`) or orphaned headings.

### Fixed
- **DSL Multi-line Descriptions**: `CommandSpecDsl.parse()` splits descriptions on newline escapes (`\n`) and carriage returns into `String[]`, ensuring round-trip fidelity with `write()`.
- **Subcommand Definitions Guard**: `CommandSpecJson.read()` now explicitly rejects nested `definitions` blocks in subcommand JSON objects with a helpful error message instead of silently ignoring them.
- **Fish Completion Robustness**: Subcommand conditions and argument descriptions properly escape single quotes and backslashes in fish completion scripts.

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
