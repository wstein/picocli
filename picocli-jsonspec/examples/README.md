# Examples

Real-world specs, kept here (not under `src/test/resources`) so they stay easy to find and read
on GitHub as documentation in their own right. Each is still sanity-checked by a test in
`src/test/java/picocli/jsonspec/` (see `FlixExampleTest`).

## `flix-0.60.0.picocli`

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
  real proxy author would plausibly make, not a fact derived from flix's source. The 14 are wrapped
  in a `group cooperative hidden "The following options are experimental:%n" { ... }` once, inside
  the `xflags` bundle itself (rather than repeated in each of the four commands) — real flix's own
  `--help` has exactly that heading before this exact set of flags, so this reproduces its visual
  structure, not just its option list, while each command only needs `use xflags`. Grouping here is
  presentational, not a validation rule: `cooperative` (not `exclusive`) with picocli's default
  multiplicity (`0..1`) imposes no restriction — any subset, including none, may be used together —
  the only effect is the usage-help section heading (and, as a side effect, the synopsis line
  brackets the 14 as a visual unit too). `hidden` keeps them out of default `--help`/synopsis
  entirely; `--Xhelp` (this proxy's own addition, paired with the hidden group) is meant to reveal
  them on demand — a declarative spec can only mark it as a plain boolean option, not wire up
  *what happens* when it's matched; see `FlixExperimentalHelpTest` for a host CLI's own dispatch
  logic doing exactly that (printing the matched command's hidden `--X*` options via picocli's
  `Help.optionListExcludingGroups`). `check`/`build`/`test` turn out to have *identical* bodies (`compileOptions`
  + `--explain` + `--json` + `--Xhelp` + `xflags` + `files`), so that whole shape is itself bundled
  as `devLoop`; `run` reuses `devLoop` too, adding only its own `--args`/`--entrypoint` on top.
- **`<file>...`** (modeled as a `positional files : File ... arity=0..*`): attached to `check`,
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
- **`usageHelp`/`versionHelp`**: resolved — see the module README. `--help`/`--version` above are
  now marked `usageHelp`/`versionHelp` and genuinely short-circuit execution.
- **Option inheritance (`ScopeType.INHERIT`)**: also resolved as a format capability (see the
  module README), but deliberately *not* used in this file: `flix`'s command tree is flat, and
  each shared option applies to an arbitrary, overlapping *subset* of siblings (the `--X*` flags:
  4 of 14 commands; `--github-token`: a different 10 of 14) — not "root + every descendant", which
  is the only shape tree inheritance can express. `definitions { ... }` + by-name references
  (`option --explain` with no `:`) is the right tool for *this* sharing pattern; inheritance is
  for a genuinely tree-shaped one, which flix doesn't have. Each reference still gets its own
  independent `OptionSpec` instance under the hood; this only removes duplication in the *source
  text*, not the resulting `CommandSpec`.
