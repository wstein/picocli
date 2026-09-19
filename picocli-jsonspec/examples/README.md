# Examples

Real-world specs, kept here (not under `src/test/resources`) so they stay easy to find and read
on GitHub as documentation in their own right. Each is still sanity-checked by a test in
`src/test/java/picocli/jsonspec/` (see `FlixExampleTest`).

## `flix-0.60.0.dsl`

A picocli-jsonspec DSL spec for the real [flix](https://flix.dev) CLI, built from:

```
$ ./flixw -- --help
```

output for flix v0.60.0, plus structural analysis of flix's own command-line parser
([`Main.scala`](https://github.com/flix/flix/blob/master/main/src/ca/uwaterloo/flix/Main.scala),
which uses [scopt](https://github.com/scopt/scopt)).

### Why this isn't just a flat transcription of `--help`

flix's `--help` output lists nearly all options in one flat block at the bottom, because scopt
declares them as *global* — parsed before the command is even chosen, not scoped to it. Every one
of them is technically legal with every command. A proxy CLI that mechanically mirrored that
structure (every option attached to every subcommand) would be a *worse* CLI than flix's own: more
noise in `--help`, not less. The whole point of `picocli-jsonspec` is to let a proxy curate a
better-organized command tree in front of a tool whose own CLI doesn't. So this file scopes each
option to the subcommands where it's actually meaningful, based on what the option does and what
each command needs — not on what scopt happens to permit.

That curation is a judgment call, not something derivable purely from the `--help` text. The
reasoning:

- **`--help`, `--version`**: meta-options that short-circuit normal parsing. Declared once at the
  top level only, matching scopt's own structure (declared outside any command's `.children()`,
  so they must precede the command name in the real tool too — `flix --help build` doesn't work
  in flix any more than it would here).
- **`--listen`**: also top level, with no subcommand at all — confirmed empirically
  (`./flixw --listen 8099` starts the WebSocket server standalone, printing
  `WebSocket server listening on: ws://localhost:8099` with no command verb involved). This
  was originally guessed as a `repl`-scoped option in an earlier draft of this file; that guess
  was wrong, which is exactly why it's worth verifying judgment calls like this against actual
  usage rather than trusting inference from `--help` text or source structure alone.
- **`--github-token`, `--no-install`**: govern dependency resolution. Attached to every command
  that needs the project's dependency graph resolved: `check`, `build`, `build-jar`,
  `build-fatjar`, `build-pkg`, `doc`, `run`, `test`, `repl`, and `outdated` (which must talk to
  GitHub to check for newer versions). Not `init` (nothing to resolve yet) or `release`
  (publishing, not resolving).
- **`--threads`**: only where compilation actually happens: `check`, `build`, `build-jar`,
  `build-fatjar`, `build-pkg`, `doc`, `run`, `test`.
- **`--explain`**: only where user-facing compiler diagnostics can occur: `check`, `build`, `run`,
  `test`.
- **`--json`**: only on commands with output worth machine-consuming: `check`, `build`, `run`,
  `test`, `outdated`.
- **`--entrypoint`**: only where a specific entry point matters for producing/running an
  executable: `build-jar`, `build-fatjar`, `run`.
- **`--args`**: only `run` — it's specifically "arguments passed to main".
- **`--yes`**: only where a prompt is plausible: `init`, `build-pkg`, `release`.
- **The 14 `--Xbenchmark-*`/`--Xlib`/`--Xprint-*`/`--Xsummary`/`--Xfuzzer`/`--Xsubeffecting`/
  `--Xchaos-monkey`/`--Xiterations`/`--Xno-deprecated` experimental compiler flags**: attached
  only to the four core dev-loop commands that actually invoke the compiler pipeline end to end
  — `check`, `build`, `run`, `test` — and deliberately *not* to `build-jar`/`build-fatjar`/
  `build-pkg`/`doc`/`repl`, to keep those commands' own `--help` clean. That's a curation choice a
  real proxy author would plausibly make, not a fact derived from flix's source.
- **`<file>...`** (modeled as a `positional files : File ... arity=0..*`): attached to `check`,
  `build`, `run`, `test`, and `repl` (whose own `--help` text explicitly says "for the current
  project, or provided Flix source files") — the commands whose descriptions imply they operate
  on ad hoc source files, as opposed to `build-jar`/`build-pkg`/`doc`/etc. which operate on "the
  current project" as a whole.
- **`lsp-vscode`'s `port`**: the one case flix's own `--help` documents as command-specific
  (listed directly under `Command: lsp-vscode port`), so it's modeled as a required positional
  exactly as shown, unlike everything else which was scoped by inference.

### Known gaps this file doesn't attempt to model

- flix splits its own CLI args from the wrapped program's args with a literal `--` separator
  (anything after `--` is passed through raw to the program under `run`). picocli-jsonspec has no
  concept of this; `--args` (a single quoted string, per flix's own text) is the closest
  equivalent captured here.
- picocli's built-in `usageHelp`/`versionHelp` option behavior (auto-printing help/version and
  exiting) isn't part of the DSL/JSON vocabulary yet — `--help`/`--version` here are plain
  booleans, not wired to short-circuit parsing.
- No option inheritance (`ScopeType.INHERIT`) in the DSL/JSON format yet, which is why options
  that are copy-pasted across `check`/`build`/`run`/`test` above are genuinely duplicated in the
  file rather than declared once and shared.
