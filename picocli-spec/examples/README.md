# Examples

Real-world specs, kept here (not under `src/test/resources`) so they stay easy to find and read
on GitHub as documentation in their own right. Each is sanity-checked by a corresponding test in
`src/test/java/picocli/spec/` (e.g. `Flix0600ExampleTest`, `Flix0670ExampleTest`, `Flix0762ExampleTest`).

## Flix Version Progression (v0.60.0 – v0.76.2)

Scanning git tags `v0.60.0` through `v0.76.2` in flix's repository (`Main.scala`, `Bootstrap.scala`,
`Options.scala`, `TestMain.scala`) reveals that the CLI evolved across nine distinct version ranges.
Each version range is represented by a dedicated `.picocli` spec file capturing the exact command set,
options, and behavioral rules in effect for that range:

| Version Range | Spec File | Tests | Key CLI Changes |
|---|---|---|---|
| `v0.60.0` – `v0.66.2` | `flix-0.60.0.picocli` | `Flix0600ExampleTest` | 14 subcommands, 14 experimental flags, `--args`, `--explain`, global `--listen` |
| `v0.67.0` | `flix-0.67.0.picocli` | `Flix0670ExampleTest` | Adds `clean` subcommand |
| `v0.67.1` – `v0.67.2` | `flix-0.67.1.picocli` | `Flix0671ExampleTest` | Adds `format` subcommand; restricts file arguments strictly to `check`/`doc`/`format`/`test`; drops `--args` in favor of `--`; removes 4 experimental flags (`Xfuzzer`, `Xprint-typer`, `Xchaos-monkey`, `Xiterations`) |
| `v0.68.0` – `v0.72.0` | `flix-0.68.0.picocli` | `Flix0680ExampleTest` | Adds `eff-check` and `eff-lock` subcommands; removes `--explain` |
| `v0.73.0` – `v0.75.1` | `flix-0.73.0.picocli` | `Flix0730ExampleTest` | Adds `--top` compiler profiling option to `compileOptions` |
| `v0.75.2` | `flix-0.75.2.picocli` | `Flix0752ExampleTest` | Adds experimental `--Xnewmono` flag (11 experimental flags) |
| `v0.75.3` | `flix-0.75.3.picocli` | `Flix0753ExampleTest` | Adds `build-classes` subcommand; updates `clean` description and `--Xprint-phases` description |
| `v0.76.0` – `v0.76.1` | `flix-0.76.0.picocli` | `Flix0760ExampleTest` | Adds `stat` subcommand; adds experimental `--Xverify`; removes `--Xsummary` |
| `v0.76.2` | `flix-0.76.2.picocli` | `Flix0762ExampleTest` | Adds package management commands (`install`, `remove`, `upgrade`); adds `--library` to `doc` |

## `flix-0.60.0.picocli`

A picocli-spec DSL spec for the real [flix](https://flix.dev) CLI, built from:

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
noise in `--help`, not less. The whole point of `picocli-spec` is to let a proxy curate a
better-organized command tree in front of a tool whose own CLI doesn't. So this file scopes each
option to the subcommands where it's actually meaningful, based on what the option does and what
each command needs — not on what scopt happens to permit.

That curation is a judgment call, not something derivable purely from the `--help` text. The
reasoning:

- **`--help`, `--version`**: meta-options that short-circuit normal parsing. Declared once at the
  top level (`--help` is marked `inherit` so subcommands also accept `--help` and receive shell
  completion rules, matching standard subcommand help expectations).
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
  (publishing, not resolving). The two always travel together, so they're bundled as
  `dependencyResolution`.
- **`--threads`**: only where compilation actually happens — every `dependencyResolution` command
  except `outdated` (which resolves but doesn't compile). Rather than repeat all three options,
  `compileOptions` is `dependencyResolution` plus `--threads` (bundle composition), and every
  command that needs all three just says `use compileOptions`; `outdated` uses
  `dependencyResolution` directly instead.
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
  real proxy author would plausibly make, not a fact derived fro  The 14 are wrapped
  in a `group cooperative helpSection="experimental" "The following options are experimental:%n" { ... }` once, inside
  the `xflags` bundle itself (rather than repeated in each of the four commands) — real flix's own
  `--help` has exactly that heading before this exact set of flags, so this reproduces its visual
  structure, not just its option list, while each command only needs `use xflags`. Grouping here is
  presentational, not a validation rule: `cooperative` (not `exclusive`) with picocli's default
  multiplicity (`0..1`) imposes no restriction — any subset, including none, may be used together —
  the only effect is the usage-help section heading. `helpSection="experimental"` keeps them out of default `--help`
  while keeping their options visible to shell autocompletion (since they are not marked `hidden`).
  `--Xhelp` (declared with `helpSection="experimental"`) acts as the on-demand trigger: when matched on the CLI,
  picocli's execution strategy natively intercepts it, renders only the experimental options group, and short-circuits
  execution with exit code 0. See `FlixExperimentalHelpTest` for tests exercising this behavior. `check`/`build`/`test`
  turn out to have *identical* bodies (`compileOptions` + `--explain` + `--json` + `--Xhelp` + `xflags` + `files`), so that
  whole shape is itself bundled as `devLoop`; `run` reuses `devLoop` too, adding only its own `--args`/`--entrypoint` on top.
- **`<file>...`** (modeled as a `positional files : Path[] ... arity=0..*`): attached to `check`,
  `build`, `run`, `test`, and `repl` (whose own `--help` text explicitly says "for the current
  project, or provided Flix source files") — the commands whose descriptions imply they operate
  on ad hoc source files, as opposed to `build-jar`/`build-pkg`/`doc`/etc. which operate on "the
  current project" as a whole.
- **`lsp-vscode`'s `port`**: the one case flix's own `--help` documents as command-specific
  (listed directly under `Command: lsp-vscode port`), so it's modeled as a required positional
  exactly as shown, unlike everything else which was scoped by inference.

### Format gaps that turned out not to matter for this file (once checked)

- **flix's `--` separator** (anything after a literal `--` is passed through raw to the wrapped
  program under `run`): this needs no special format support at all. picocli already treats a
  bare `--` as end-of-options by default, so an array-typed, sufficiently-arity'd positional
  captures everything after it verbatim, option-looking tokens included — verified by
  `CommandSpecDslTest.doubleDashSeparatorPassesRawTokensToAnArrayTypePositional`, not just assumed.
  `--args` (a single quoted string, per flix's own text) remains the closest *named* equivalent
  for the common case, but raw multi-token passthrough already works with no format change.
- **`mixinStandardHelpOptions`**: standard help/version options (`-h`/`--help`, `-V`/`--version`)
  are provided natively via `mixinStandardHelpOptions` without needing manual option declarations,
  and genuinely short-circuit execution.
- **Option inheritance (`ScopeType.INHERIT`)**: also resolved as a format capability (see the
  module README), but deliberately *not* used in this file: `flix`'s command tree is flat, and
  each shared option applies to an arbitrary, overlapping *subset* of siblings (the `--X*` flags:
  4 of 14 commands; `--github-token`: a different 10 of 14) — not "root + every descendant", which
  is the only shape tree inheritance can express. `definitions { ... }` + by-name references
  (`option --explain` with no `:`) is the right tool for *this* sharing pattern; inheritance is
  for a genuinely tree-shaped one, which flix doesn't have. Each reference still gets its own
  independent `OptionSpec` instance under the hood; this only removes duplication in the *source
  text*, not the resulting `CommandSpec`.

## `flix-0.76.2.picocli`

A picocli-spec DSL spec for the same real [flix](https://flix.dev) CLI, at a much later
version. Built differently from `flix-0.60.0.picocli`: no `flixw` binary was available when this
file was first written, so it was curated entirely from reading flix's actual source at git tag
[`v0.76.2`](https://github.com/flix/flix/tree/v0.76.2) —
[`Main.scala`](https://github.com/flix/flix/blob/v0.76.2/main/src/ca/uwaterloo/flix/Main.scala)
for the scopt declarations (option/command names, `--help` text, verbatim) and
[`Bootstrap.scala`](https://github.com/flix/flix/blob/v0.76.2/main/src/ca/uwaterloo/flix/api/Bootstrap.scala)
for what each command's implementation actually does with each option — necessary because scopt
only records which text is a *global* option, never which subcommands actually consult it, or
whether a subcommand even accepts file arguments at all. Reading the implementation instead of
just the option declarations surfaced real per-command differences a `--help` transcription alone
would have missed. The actual flix-0.76.2 jar was later obtained and every finding below was
confirmed by running it directly — `--help`, plus exercising `build`/`run`/`repl`/`check`/`test`/
`doc`/`format` with a loose file argument, `--version --json`, `check --json`, `--listen`
standalone, and `init --yes`/`build-pkg` — matching flix-0.60.0.picocli's own empirical bar, not
just source-inference.

### New commands since 0.60.0

`build-classes`, `clean`, `format`, `install`, `remove`, `upgrade`, `stat`, `eff-check`, `eff-lock`
were added. All of them were curated the same way as 0.60.0's commands: which of
`compileOptions`/`dependencyResolution` (see below) they need was decided by which of
`Bootstrap.bootstrap`/`Bootstrap#mkFlix` each command's `case` branch in `Main.scala` actually
calls, not by guessing from the command's name or one-line description.

flix also declares three more subcommands — `Xperf`, `Xmemory`, `Xzhegalkin` — internal
benchmarking/profiling tools for compiler developers. Unlike the `--X*` *options* (which scopt
still lists in `--help`, just under an "experimental" heading), these are declared `.hidden()` at
the *command* level: scopt never shows them in `--help` at all. This proxy doesn't expose them
either, for the same reason it never fabricates functionality the wrapped tool doesn't actually
present to its own users.

### Three genuine differences from how `flix-0.60.0.picocli` curated the equivalent options

Reading `Bootstrap.scala` rather than trusting `--help` text and command descriptions alone turned
up three real, version-specific findings (confirmed by grepping the actual dependency/option
usage across the whole cloned source tree, not just `Main.scala`, and later confirmed again by
actually running the flix-0.76.2 jar):

1. **`--no-install`'s underlying `installDeps` flag is set but never read.** It flows from the CLI
   into `CmdOpts` into `Options`, but no call in `Bootstrap.scala` (or anywhere else in the
   `main/src` tree) ever consults `options.installDeps` — the automatic-dependency-installation
   behavior it's supposed to disable isn't gated on it anywhere in this version. It's still
   declared here (flix's own `--help` still documents it, so a proxy mirrors that), but it's
   worth knowing this flag is currently a no-op if you're relying on it.
   (`build --no-install` on a fresh project still ran the full "Resolving Flix dependencies...
   Downloading..." sequence identically to `build` without the flag -- consistent with, though
   not a rigorous proof of, the grep-confirmed fact that `options.installDeps` is never read.
   The test project declared no actual dependencies, so there was nothing for the flag to
   meaningfully skip; a project with real unresolved dependencies would be a stronger check.)
2. **`--json` only ever affects `--version`'s own output.** `Main.scala` reads `cmdOpts.json`
   exactly twice: once to build `options.json`, and once directly in `printVersion(cmdOpts.json)`.
   Nothing in `Bootstrap.scala` reads `options.json`. Unlike `flix-0.60.0.picocli` (which attached
   `--json` to `check`/`build`/`run`/`test`/`outdated`, matching that version's actual behavior),
   this file keeps `--json` top-level only, paired with `--version`, since attaching it to any
   subcommand here would be documenting a feature that doesn't do anything. Confirmed empirically:
   `--version --json` produces `{"major":0,"minor":76,"revision":2}`; `check --json` on a real
   file produces byte-identical output to plain `check` (empty on success either way).
3. **File-argument support varies per command, and doesn't match every command's own `--help`
   text.** Each `case Command.X =>` branch in `Main.scala` either unconditionally rejects a
   non-empty `cmdOpts.files` ("The '...' command does not support file arguments.") or branches on
   whether it's empty to allow ad hoc file compilation. Only `check`, `doc`, `format`, and `test`
   accept files; `build`, `run`, and `repl` explicitly reject them — even though `repl`'s own
   `--help` text still reads "starts a repl for the current project, **or provided Flix source
   files**," which the code no longer honors. `flix-0.60.0.picocli` gave the shared `files`
   positional to `check`/`build`/`run`/`test`/`repl`; that would be wrong for this version, so this
   file only references it from the four commands confirmed (by the code, not the text) to accept
   it — see `Flix0762ExampleTest.onlyCheckDocFormatAndTestAcceptFileArguments`. Confirmed
   empirically against the real jar: `build Main.flix`/`run Main.flix`/`repl Main.flix` each print
   exactly `"The '<cmd>' command does not support file arguments."` and exit; `check Main.flix`,
   `test Main.flix`, `doc Main.flix`, and `format Main.flix` all run against the file with no such
   rejection.

### Everything else, briefly

- **`--entrypoint`**: still only where a runnable main matters: `build-jar`, `build-fatjar`, `run`.
- **`--yes`**: only `install` (`Bootstrap#install` calls `selectMount` with `assumeYes`) and
  `release` (confirmation prompt before publishing). Not `init` (no prompt exists in
  `Bootstrap#init`'s implementation at all) and not `build-pkg` (no confirmation logic either) —
  both would have been wrong guesses from the command names alone; `flix-0.60.0.picocli` gave
  `--yes` to `init`/`build-pkg`/`release`, which doesn't hold up for this version's `init`/
  `build-pkg` on inspection of the actual implementation. Confirmed empirically: `init --yes`
  and plain `build-pkg` (no prompt shown either way) both behave identically with or without
  the flag.
- **`--top`** (new global option, "displays a live view of where the compiler spends its time."):
  bundled into `compileOptions` alongside `--threads`, since both are only meaningful where actual
  compilation happens.
- **`--library`** (new, `doc`-only): "documents the bundled library instead of the current
  project." — scoped to `doc` alone, matching its own `.children(...)` declaration in `Main.scala`.
- **The 11 experimental `--X*` flags**: a different, mostly non-overlapping set from 0.60.0's 14
  (`Xsummary`/`Xfuzzer`/`Xprint-typer`/`Xchaos-monkey` are gone from the CLI; `Xverify`/`Xnewmono`
  are new) — same `xflags` bundle mechanism, scoped to the same four core dev-loop commands
  (`check`/`build`/`run`/`test`) for the same "keep other commands' `--help` clean" reasoning.
- **`install`/`remove`/`upgrade`**: each takes one required `package` positional with its own
  distinct description text (verbatim from `Main.scala`), so each is declared inline rather than
  through `definitions` — there's exactly one use of each, so a shared definition would buy
  nothing. All three take `--github-token` directly (not through the `dependencyResolution`
  bundle): they call `Bootstrap.install`/`remove`/`upgrade` directly with just the token, never
  `Bootstrap.bootstrap`, so `--no-install` doesn't apply to them.
