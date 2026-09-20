# picocli-spec-tool

A picocli command line tool for working with [`picocli-spec`](../picocli-spec) DSL/JSON
spec files: preview the CLI a spec produces, generate a shell completion script or AsciiDoc man
pages for it, or just check that it loads — all without writing a line of Java.

Built the ordinary way, with picocli's own annotations (`@Command`/`@Option`/`@Parameters`).
Unlike the specs it operates on, this outer tool is not itself described by a picocli-spec
file — a spec-authoring tool has no reason to avoid the annotation processor it's built with.

## Usage

```
$ java -jar picocli-spec-tool.jar <subcommand> ...
```

| Subcommand | What it does |
|---|---|
| `preview <spec-file>` | Prints the usage help message for the spec's root command and, recursively, every subcommand — see exactly what CLI the spec produces. `--ansi=on\|off\|auto` controls color. |
| `completion <spec-file>` | Generates a bash/zsh completion script, via picocli's own `AutoComplete`. `--name` overrides the script's command name (default: the spec's own root command name); `--output <file>` writes to a file instead of stdout. |
| `manpage <spec-file>` | Generates AsciiDoc man pages for the spec and every subcommand, via picocli-codegen's `ManPageGenerator`. `--outdir <dir>` sets the output directory (default: the current directory). |
| `validate <spec-file>` | Loads the spec and reports `OK` or a one-line error — nothing is generated. Loading already runs every DSL/JSON parsing rule and `SpecValidator`'s checks, so this is just a clear pass/fail wrapper around that for a shell workflow (e.g. a pre-commit hook or CI step). |

Every subcommand accepts either a `.picocli` (DSL) or a `.json` file — the extension picks the
reader, same as `SpecLoader` does internally.

### Example

```
$ java -jar picocli-spec-tool.jar preview ../picocli-spec/examples/flix-0.60.0.picocli
Usage: flix [--help] [--version] [--listen=PARAM] [COMMAND]
The Flix Programming Language 0.60.0
      --help           prints this usage information.
      --listen=PARAM   starts the socket server and listens on the given port.
      --version        prints the version number.
Commands:
  init          creates a new project in the current directory.
  check         checks the current project for errors.
  ...

Usage: flix init [--yes]
creates a new project in the current directory.
      --yes   automatically answer yes to all prompts.

Usage: flix check [--explain] [--json] [--no-install] [--Xhelp]
                  [--github-token=PARAM] [--threads=PARAM] [<files>...]
...
```

## Why a separate module

`picocli-spec` itself has exactly one dependency, `picocli` core (`api rootProject`), matching
picocli's own zero-runtime-dependency philosophy. `manpage` needs `picocli-codegen`, a real
dependency edge this tool is happy to take on but the core spec-building library shouldn't have
to. Keeping the renderer in its own module means authoring/merging specs never drags in
`picocli-codegen` (or this tool's own transitive footprint) for consumers who only need that.

## Limitations

- `completion` only generates bash/zsh scripts (`AutoComplete.bash`); PowerShell completion isn't
  wired up.
- There's no `convert` subcommand yet (DSL ↔ JSON): `CommandSpecJson.write()` already covers
  DSL → JSON directly if you need it programmatically, but JSON → DSL would need a new DSL
  *writer* in `picocli-spec` (currently parse-only) — not yet implemented.
- No execution wiring, same as `picocli-spec` itself: this tool only renders a spec, it never
  runs the CLI it describes.

See `SpecLoaderTest`, `PreviewTest`, `CompletionTest`, `ManpageTest`, `ValidateTest`, and
`SpecToolAppTest` (the last one exercises every subcommand end-to-end through
`CommandLine#execute`, the same way a real invocation would) for runnable examples.
