<p align="center"><img src="docs/images/logo/horizontal-400x150.png" alt="picocli" height="150px"></p>

[![This fork's CI](https://github.com/wstein/picocli/actions/workflows/ci.yml/badge.svg)](https://github.com/wstein/picocli/actions/workflows/ci.yml)
[![GitHub Release](https://img.shields.io/github/release/wstein/picocli.svg)](https://github.com/wstein/picocli/releases)

> **This is [wstein](https://github.com/wstein)'s fork of [remkop/picocli](https://github.com/remkop/picocli).**
> It is used to prototype additions before (or while) they are proposed upstream.
> **For everything about picocli itself** — features, user manual, examples, articles —
> see the [upstream README](https://github.com/remkop/picocli#readme).

---

## What this fork adds

| Feature | Status | Upstream PR |
| --- | --- | --- |
| **Fish shell completion** — `AutoComplete.fish()` generates declarative `complete -c` scripts | ✅ In this fork | [remkop/picocli#2463](https://github.com/remkop/picocli/pull/2463) |
| **`picocli-spec`** — build a `CommandSpec` from a DSL or JSON without any annotated Java class | ✅ In this fork | Not yet proposed |
| **`picocli-spec-tool`** — CLI tool to preview, validate, convert, and generate completions/man pages for spec files | ✅ In this fork | Not yet proposed |
| **`helpSection` on `@Option` / `@ArgGroup`** — on-demand help sections (e.g. `--Xhelp` shows experimental flags) | ✅ In this fork | Not yet proposed |

---

## Download (this fork)

This fork publishes under `io.github.wstein:picocli` (distinct from upstream's `info.picocli:picocli`).

### Gradle

```groovy
repositories {
    maven { url 'https://wstein.github.io/picocli/maven' }
}
dependencies {
    implementation 'io.github.wstein:picocli:<version>'
}
```

### Maven

```xml
<repositories>
  <repository>
    <id>wstein-picocli</id>
    <url>https://wstein.github.io/picocli/maven</url>
  </repository>
</repositories>
<dependency>
  <groupId>io.github.wstein</groupId>
  <artifactId>picocli</artifactId>
  <version>VERSION</version>
</dependency>
```

Jars are also attached directly to [GitHub Releases](https://github.com/wstein/picocli/releases).

**Javadoc and JSON Schema** are hosted on GitHub Pages —
see the [landing page](https://wstein.github.io/picocli/) for all links.

---

## picocli-spec

[`picocli-spec`](picocli-spec/README.md) builds a picocli `CommandSpec` from a small DSL or JSON,
exports any `CommandSpec` back to JSON, and merges specs into a host CLI as subcommands —
all without an annotated Java class.

Intended use-case: give a beautiful, help-and-completion-enabled picocli front end to a
third-party tool whose own CLI isn't human-friendly.

```
// flix-0.60.0.picocli  (DSL)
command flix "The Flix Programming Language 0.60.0" {
  option --help    : boolean "prints this usage information." usageHelp
  option --version : boolean "prints the version number."    versionHelp
  option --listen  : String  "starts the socket server and listens on the given port."
  command init  "creates a new project in the current directory." { ... }
  command build "builds the current project." { ... }
  ...
}
```

```java
CommandSpec flixSpec = CommandSpecDsl.parse(Files.readString(Path.of("flix-0.60.0.picocli")));
CommandSpecMerger.merge(myUberSpec, flixSpec);
```

→ [`picocli-spec` README](picocli-spec/README.md) ·
[JSON Schema](https://wstein.github.io/picocli/spec/schema/command-spec.schema.json) ·
[Javadoc](https://wstein.github.io/picocli/apidocs/picocli-spec/)

### Gradle

```groovy
dependencies {
    implementation 'io.github.wstein:picocli-spec:<version>'
}
```

---

## picocli-spec-tool

[`picocli-spec-tool`](picocli-spec-tool/README.md) is a self-contained fat-jar CLI for working
with `.picocli` / `.json` spec files — no Java code required.

```
java -jar picocli-spec-tool-<version>-all.jar <subcommand> <spec-file>
```

| Subcommand | What it does |
| --- | --- |
| `preview` | Prints the full usage help for every command in the spec |
| `validate` | Load-only pass/fail — good for pre-commit hooks and CI |
| `completion` | Generates a bash/zsh or fish completion script |
| `manpage` | Generates AsciiDoc man pages via picocli-codegen |
| `convert` | Converts between DSL and JSON formats |

→ [`picocli-spec-tool` README](picocli-spec-tool/README.md)

---

## Upstream picocli

For everything about picocli itself — annotations, programmatic API, user manual, GraalVM,
Spring/Micronaut/Quarkus integration, community articles — see:

**[remkop/picocli on GitHub](https://github.com/remkop/picocli#readme)**
**[picocli.info](https://picocli.info)** (user manual)
**[Quick Guide](https://picocli.info/quick-guide.html)**

To use the latest **upstream** release (not this fork):

```groovy
// Gradle
implementation 'info.picocli:picocli:4.7.7'
```

```xml
<!-- Maven -->
<dependency>
  <groupId>info.picocli</groupId>
  <artifactId>picocli</artifactId>
  <version>4.7.7</version>
</dependency>
```
