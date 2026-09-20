# picocli-jsonspec

Build a picocli `CommandSpec` from JSON or from a small DSL, export any `CommandSpec` back to
JSON, and merge one or more such specs into a host ("uber") CLI as subcommands — without an
annotated Java class.

The motivating use case: give a beautiful, help-and-completion-enabled picocli front end to a
third-party tool whose own CLI is not human-friendly (arbitrary example: `flix`). Describe that
tool's commands/options as JSON (or in the DSL below), merge it into your own uber command, and
let picocli handle parsing, validation, `--help`, and shell completion. How a merged-in
subcommand is actually executed (shell out, API call, whatever) is entirely up to your own
command's `run()`/`call()` — this module only builds the combined model.

- **JSON format reference:** [`command-spec.schema.json`](src/main/resources/picocli/jsonspec/command-spec.schema.json)
  (also published at <https://wstein.github.io/picocli/jsonspec/schema/command-spec.schema.json>,
  which always tracks the latest release — pin `.../jsonspec/schema/<version>/command-spec.schema.json`,
  e.g. `.../jsonspec/schema/4.8.0/...`, for a specific release's exact schema, kept permanently)
- **Javadoc:** <https://wstein.github.io/picocli/apidocs/picocli-jsonspec/>
- **Full worked example:** [`flix.dsl`](src/test/resources/picocli/jsonspec/fixtures/flix.dsl) /
  [`flix.json`](src/test/resources/picocli/jsonspec/fixtures/flix.json), exercised end to end by
  `CommandSpecFixturesTest`

## The DSL

```
command flix "The Flix programming language" {
  option -v, --verbose : boolean "Enable verbose output"
  option -o, --output : String "Output directory" default=out
  option -t, --target : String "Compilation target" required arity=1

  command build "Compile the project" {
    option --release : boolean "Optimize for release"
    positional files : File "Input files to compile" arity=0..*
  }
  command run "Run the project" {
    positional args : String "Program arguments" arity=0..*
  }
}
```

```java
CommandSpec flixSpec = CommandSpecDsl.parse(dslText);
```

### Grammar

```
spec        := definitions? command
definitions := 'definitions' '{' ( option | positional | bundle )* '}'
bundle      := 'bundle' name '{' ( option | positional | group | use )* '}'
command     := 'command' name [string] '{' member* '}'
member      := option | positional | group | use | command
group       := 'group' ('exclusive' | 'cooperative') ['hidden'] ['multiplicity' '=' value] [string]
               '{' ( option | positional | group | use )* '}'
use         := 'use' name       // expands a bundle's members at this point
option      := 'option' name (',' name)* ( ':' type [string] attr* )?
positional  := 'positional' name ( ':' type [string] attr* )?
attr        := 'default' '=' value
             | 'required'
             | 'arity' '=' value
             | 'inherit'
             | 'hidden'
             | 'usageHelp'    // options only
             | 'versionHelp'  // options only
name, type,
value       := word            // any run of non-whitespace characters other than { } : , = "
string      := '"' ... '"'     // escapes: \" \\ \n \t
```

An `option`/`positional` with no `: type` part is a *reference* to a same-named definition from
the top-level `definitions` block (see below), not a new definition. Inside `definitions` itself,
`: type` is effectively required — there's nothing to reference yet.

Notes:

- `command`/`option`/`positional`/`bundle`/`use`/`group`/`exclusive`/`cooperative`/`hidden`/
  `multiplicity`/`default`/`required`/`arity`/`inherit`/`usageHelp`/`versionHelp` are the only
  reserved words, and only where the grammar expects one (an option/positional name may otherwise
  be any word, including one that happens to read `arity` — the parser only treats them as
  keywords at the start of a member or inside an attribute list).
- The `string` right after a name/type is that command/option/positional's `description` (a
  single line). There is currently no syntax for multiple description lines in the DSL (use JSON
  for that, via a `"description"` array).
- A positional's bare `name` (e.g. `files`) becomes its `paramLabel` wrapped in angle brackets
  (`<files>`), matching picocli's own convention for annotated fields.
- `type` is one of the names in [`ArgTypes`](src/main/java/picocli/jsonspec/ArgTypes.java)'s
  vocabulary — currently `String`, `boolean`, `int`, `long`, `double`, `File` — optionally
  suffixed with `[]` for a multi-value array type, e.g. `File[]` (see the schema's
  `$defs.type.pattern`, which is tested to stay in sync with the actual reader).
- `attr*` may appear in any order and are all optional; `required`/`inherit`/`usageHelp`/
  `versionHelp` take no value, `default=` and `arity=` do.
- `usageHelp`/`versionHelp` mark an option as picocli's built-in usage-/version-help option:
  matching it on the command line auto-prints the usage/version message and short-circuits
  execution (`CommandLine#execute` returns before running any `Runnable`/`Callable`). Positional
  parameters have no equivalent in picocli, so `positional` doesn't accept these two attributes.
- `inherit` (either kind of statement) sets picocli's `ScopeType.INHERIT`: the option/positional
  also applies to every descendant subcommand, not just the one it's declared on. Without it, an
  option/positional is local to its own command (picocli's default).
- `hidden` (either kind of statement) sets picocli's `ArgSpec#hidden()`: the option/positional is
  excluded from default usage help while remaining fully functional. A `group` can also be marked
  `hidden` as a whole — see [Grouping options](#grouping-options-group) below for why that's
  handled differently from an individually hidden option/positional.

## Reusing an option/positional across commands (`definitions`)

Real CLIs often have one option meaningful to many commands (`--json`, `--threads`, ...). Rather
than repeat its type/description/etc. in every command, describe it once in a top-level
`definitions` block and reference it by name (no `: type`) from any command:

```
definitions {
  option --json : boolean "enables json output."
  positional files : File "input source files." arity=0..*
}

command flix {
  command check {
    option --json      // reference: no ':'
    positional files   // reference: no ':'
  }
  command build {
    option --json
    option --threads : int "number of threads to use."   // still a full definition, only used here
  }
}
```

A definition and a reference can be freely mixed within one command's own list, in any order. An
option reference must name exactly one option (the target); only a *definition* (with `:`) may
declare multiple names at once. Each reference resolves to its own independent option/positional
instance — this only removes duplication from the source text, not from the resulting
`CommandSpec` (there's no shared runtime state between commands, and `CommandSpecJson.write()`
always emits fully-inlined objects regardless of how the spec was originally authored).

The JSON equivalent is a document-root `"definitions"` object; any `options[]`/`positionalParams[]`
array entry may then be either a full object (as before) or a plain string naming a
`definitions.options`/`definitions.positionalParams` entry:

```json
{
  "definitions": {
    "options": { "--json": { "names": ["--json"], "type": "boolean", "description": ["enables json output."] } },
    "positionalParams": { "files": { "paramLabel": "<files>", "type": "File", "arity": "0..*" } }
  },
  "name": "flix",
  "subcommands": [
    { "name": "check", "options": ["--json"], "positionalParams": ["files"] }
  ]
}
```

See [`flix-0.60.0.dsl`](examples/flix-0.60.0.dsl) for a realistic file built around this — 24
shared options/positionals defined once and referenced from up to 10 commands each.

## Grouping options (`group`)

A `group` declares a picocli `ArgGroupSpec` — a set of options/positionals validated as a unit,
either mutually exclusive or cooperative, with an optional multiplicity and heading:

```
command flix {
  group exclusive "Output format" {
    option --json : boolean
    option --xml : boolean
  }
  group cooperative multiplicity=1 {
    option --user : String required
    option --password : String required
  }
}
```

`exclusive` (picocli's own default) means at most one of the group's own args may be matched
together; `cooperative` (`exclusive: false` in JSON) means they may all be matched together, and
is typically combined with `multiplicity=1` to validate a set that must be given together or not
at all. `multiplicity` uses the same range syntax as `arity` (`Range.valueOf`); `"1"` makes the
group itself required. A group's `option`/`positional` entries accept the same definition-or-
reference syntax as a command's own, and a group may nest further `group`s as subgroups.

A group's args are automatically added to the enclosing command's own options/positionals —
picocli's `CommandSpec#addArgGroup` does this — so `spec.findOption("--json")` finds it exactly
as if it had been declared directly on the command; only the *validation rule* (exclusive/
cooperative/multiplicity) is different. In JSON, a command's `"argGroups"` array holds the same
shape recursively (`exclusive`, `multiplicity`, `heading`, `hidden`, `options`, `positionalParams`,
`use`, `subgroups`), and `CommandSpecJson.write()` correctly omits a grouped arg from the command's
own flat `"options"`/`"positionalParams"` arrays to avoid emitting it twice.

### A hidden group

```
command flix {
  command check {
    group cooperative hidden "The following options are experimental:%n" {
      option --Xfuzzer : boolean
      option --Xsummary : boolean
    }
  }
}
```

A `hidden` group is never actually built as a real `ArgGroupSpec` — its members are flattened
directly into the enclosing command/group instead, each with its own `hidden` forced true.
This was an empirical finding, not a design preference: a group whose every member is hidden
otherwise still leaves visible artifacts in usage help — an orphaned heading, and, regardless of
heading, a stray empty `"[]"` in the synopsis line for the group itself — so hiding is done by
never building the group at all, rather than by hiding a real one. `exclusive`/`multiplicity` are
moot on a hidden group: no mutual-exclusion/multiplicity validation applies to former members of a
group that no longer exists as such. Individually hidden options/positionals (the `hidden` attr
from the grammar above) and a hidden group compose the same way in JSON — see the field reference
below.

## Reusable bundles of options/positionals (`bundle`/`use`)

`definitions` (above) removes duplication for a *single* option/positional referenced individually.
When the same *set* of several options (optionally wrapped in a group) is reused across many
commands, name the whole set once as a `bundle` inside `definitions`, then pull in the whole thing
with one `use <name>` wherever it's needed — in a command body, in a group body, or in another
bundle's own body:

```
definitions {
  option --github-token : String "API key to use for GitHub dependency resolution."
  option --no-install : boolean "disables automatic installation of dependencies."
  option --threads : int "number of threads to use for compilation."

  bundle commonOptions {
    option --github-token
    option --no-install
    option --threads
  }

  option --Xfuzzer : boolean "[experimental] enables compiler fuzzing."
  option --Xsummary : boolean "[experimental] prints a summary of the compiled modules."

  bundle xflags {
    group cooperative hidden "The following options are experimental:%n" {
      option --Xfuzzer
      option --Xsummary
    }
  }
}

command flix {
  command check {
    use commonOptions
    use xflags
  }
  command build {
    use commonOptions   // independent OptionSpec instances -- not shared with check's
  }
}
```

Each `use` expands to fresh, independent option/positional/group instances (same as an individual
reference) — two commands `use`-ing the same bundle never share runtime state. `use` can be
combined with a command's own `option`/`positional`/`group` statements in any order, and a bundle
may itself `use` an earlier bundle (bundle composition), as `xflags` above could in turn be
`use`d by a larger bundle. Every name a bundle's own `option`/`positional` lines reference must
already be defined earlier in `definitions` — a bundle can't declare a brand-new option inline.

The JSON equivalent is a `"bundles"` object inside the document-root `"definitions"`, each entry
shaped `{ "options": [...], "positionalParams": [...], "groups": [...], "use": [...] }` (all four
optional; `"options"`/`"positionalParams"` are always plain name/label strings here, `"groups"` is
an array of ordinary argGroup objects), and a `"use"` array accepted alongside `"options"`/
`"positionalParams"`/`"argGroups"` on any command or argGroup object:

```json
{
  "definitions": {
    "options": {
      "--github-token": { "names": ["--github-token"], "type": "String" },
      "--no-install": { "names": ["--no-install"], "type": "boolean" },
      "--threads": { "names": ["--threads"], "type": "int" }
    },
    "bundles": {
      "commonOptions": { "options": ["--github-token", "--no-install", "--threads"] }
    }
  },
  "name": "flix",
  "subcommands": [
    { "name": "check", "use": ["commonOptions"] },
    { "name": "build", "use": ["commonOptions"] }
  ]
}
```

See [`flix-0.60.0.dsl`](examples/flix-0.60.0.dsl) for the real `xflags` bundle (14 experimental
flags plus their hidden group heading) reused by `check`/`build`/`run`/`test` with a single
`use xflags` each.

## JSON

The DSL compiles to the same model `CommandSpecJson` reads and writes, so you can go either
direction:

```java
String json = CommandSpecJson.write(flixSpec);      // e.g. for docs, a web UI, other tooling
CommandSpec sameSpec = CommandSpecJson.read(json);   // read it back
```

`CommandSpecJson.write()` of the DSL example above produces exactly this (also checked into
[`flix.json`](src/test/resources/picocli/jsonspec/fixtures/flix.json), enforced by
`CommandSpecFixturesTest`):

```json
{
  "name": "flix",
  "description": ["The Flix programming language"],
  "options": [
    { "names": ["-v", "--verbose"], "type": "boolean", "description": ["Enable verbose output"], "arity": "0" },
    { "names": ["-o", "--output"], "type": "String", "description": ["Output directory"], "defaultValue": "out", "arity": "1" },
    { "names": ["-t", "--target"], "type": "String", "description": ["Compilation target"], "required": true, "arity": "1" }
  ],
  "subcommands": [
    {
      "name": "build",
      "description": ["Compile the project"],
      "options": [
        { "names": ["--release"], "type": "boolean", "description": ["Optimize for release"], "arity": "0" }
      ],
      "positionalParams": [
        { "paramLabel": "<files>", "type": "File", "description": ["Input files to compile"], "arity": "0..*" }
      ]
    },
    {
      "name": "run",
      "description": ["Run the project"],
      "positionalParams": [
        { "paramLabel": "<args>", "type": "String", "description": ["Program arguments"], "arity": "0..*" }
      ]
    }
  ]
}
```

### Field reference

The formal, versioned reference is the JSON Schema (linked above); this table is a quick summary.

**Command object** (the document root, and every entry in `subcommands`):

| Field | Type | Required | Notes |
|---|---|---|---|
| `name` | string | only inside `subcommands` | Root may omit it (keeps picocli's own placeholder name). |
| `description` | string[] | no | One entry per usage-help line. |
| `options` | (option \| string)[] | no | A string entry is a reference into `definitions.options` (root only, see below). |
| `positionalParams` | (positionalParam \| string)[] | no | A string entry is a reference into `definitions.positionalParams`. |
| `argGroups` | argGroup[] | no | See [Grouping options](#grouping-options-group). |
| `use` | string[] | no | Names of `definitions.bundles` entries to expand into this command's own options/positionalParams/argGroups. See [Reusable bundles](#reusable-bundles-of-optionspositionals-bundleuse). |
| `subcommands` | command[] | no | Recursive; each entry requires `name`. |
| `definitions` | object | no | **Document root only.** `{ "options": {name: option}, "positionalParams": {label: positionalParam}, "bundles": {name: bundle} }` — named templates any command's `options`/`positionalParams`/`use` can reference by (string) name instead of inlining. |

**Option object** (`options[]`, or a value in `definitions.options`):

| Field | Type | Required | Notes |
|---|---|---|---|
| `names` | string[] | **yes**, ≥1 | e.g. `["-v", "--verbose"]`. |
| `type` | string | no | One of the `ArgTypes` names, optionally suffixed `[]` (e.g. `File[]`) for a multi-value array — required whenever `arity` allows more than one value. |
| `description` | string[] | no | |
| `defaultValue` | string | no | See the "not required if it has a default" note below. |
| `required` | boolean | no, default `false` | An option with a `defaultValue` is reported as not required by picocli regardless of this flag (`ArgSpec#required()`'s documented "#261" behavior) — don't set both expecting `required` to win. |
| `arity` | string | no | e.g. `"0"`, `"1"`, `"0..1"`, `"1..*"`, `"0..*"`; parsed by `Range.valueOf(String)`. |
| `usageHelp` | boolean | no, default `false` | Options only. Marks picocli's built-in usage-help option (auto-prints and short-circuits execution). |
| `versionHelp` | boolean | no, default `false` | Options only. Marks picocli's built-in version-help option. |
| `hidden` | boolean | no, default `false` | Excludes this option from default usage help while it remains fully functional. |
| `scope` | string | no, default `"local"` | `"inherit"` makes this option also apply to every descendant subcommand (picocli's `ScopeType.INHERIT`), not just the command it's declared on. |

**Positional param object** (`positionalParams[]`, or a value in `definitions.positionalParams`):
same fields as an option (`scope`/`hidden` included) except `names` is replaced by an optional
`paramLabel` (string, defaults to picocli's own `"PARAM"`), and there's no `usageHelp`/
`versionHelp` (options only).

**ArgGroup object** (`argGroups[]`, or an entry in a bundle's `groups[]`):

| Field | Type | Required | Notes |
|---|---|---|---|
| `exclusive` | boolean | no, default `true` | `true`: at most `multiplicity` of this group's own args may be matched together. `false` (`cooperative` in the DSL): they may all be matched together — typically paired with `multiplicity: "1"` to require the whole set together or not at all. |
| `multiplicity` | string | no, default `"0..1"` | Same range syntax as `arity`. `"1"` makes the group itself required. |
| `heading` | string | no | Usage-help section heading. |
| `hidden` | boolean | no, default `false` | Never actually built as an `ArgGroupSpec`: all members (and subgroups' members, recursively) are flattened into the enclosing command/group instead, each forced `hidden: true`. See [A hidden group](#a-hidden-group) above for why. |
| `options` | (option \| string)[] | no | Same definition-or-reference syntax as a command's own. |
| `positionalParams` | (positionalParam \| string)[] | no | Same definition-or-reference syntax as a command's own. |
| `use` | string[] | no | Names of `definitions.bundles` entries to expand into this group's own options/positionalParams/subgroups. |
| `subgroups` | argGroup[] | no | Nested groups. |

**Bundle object** (a value in `definitions.bundles`):

| Field | Type | Required | Notes |
|---|---|---|---|
| `options` | string[] | no | Names of options already in `definitions.options` — always a plain name reference here, never an inline object. |
| `positionalParams` | string[] | no | Labels already in `definitions.positionalParams` — always a plain label reference here. |
| `groups` | argGroup[] | no | Groups declared inline within this bundle; materialized fresh (with fresh option/positional instances) each time the bundle is `use`d. |
| `use` | string[] | no | Names of other `definitions.bundles` entries to fold into this one — must already appear earlier in the same `bundles` map. |

## Merging into a host CLI

```java
CommandSpec uber = new CommandLine(new MyUberCommand()).getCommandSpec(); // your own annotated/programmatic CLI
CommandSpecMerger.merge(uber, flixSpec, otherToolSpec);                   // attaches each by its own name()

new CommandLine(uber).execute(args); // "myuber --config x flix build --release" now parses
```

`merge` rejects an imported spec with no name, and rejects a name collision with an existing
subcommand.

## Current limitations

- Base types are `String`, `boolean`, `int`, `long`, `double`, `File` (`ArgTypes`), each usable
  as a multi-value array by suffixing `[]` (e.g. `File[]`) — required whenever `arity` allows more
  than one value, since picocli can only bind multiple matches to an array or `Collection` type.
  `Collection`/`Map` types (which need an explicit auxiliary type due to generics erasure) aren't
  supported yet; arrays cover the same need without that extra complexity.
- No `@Mixin`-equivalent for reusing a whole pre-built fragment of a command's structure across
  *files*; `definitions`/references (above) cover reuse of individual options/positionals within
  one file.
- No execution wiring is provided or assumed: a merged-in subcommand has no `run()`/`call()`
  of its own. Attach one via the host command's own dispatch logic (e.g. an `IExecutionStrategy`
  that recognizes commands originating from an imported spec and shells out accordingly).
- An option's `defaultValue` that's textually identical to a sibling subcommand's name is
  rejected by picocli when it applies that default — unconditionally, on every invocation, even
  with zero arguments given (confirmed empirically; not a bug in this module, picocli's parser
  being conservative about a token that looks like a subcommand name). `read()`/`parse()`/`merge()`
  all catch this automatically via `SpecValidator` and fail fast with a clear message, rather than
  letting it surface as a confusing runtime exception. (An earlier version of this note also
  warned that an unbounded-arity positional sibling to subcommands could "swallow" a subcommand
  name; that turned out to be wrong on direct empirical testing — picocli correctly dispatches
  into the subcommand regardless. Declared here for the record, not as a caveat to design around.)

See the test classes (`CommandSpecDslTest`, `CommandSpecJsonTest`, `CommandSpecMergerTest`,
`CommandSpecFixturesTest`, `CommandSpecSchemaTest`, `CommandSpecDslDefinitionsTest`,
`CommandSpecJsonDefinitionsTest`, `CommandSpecDslArgGroupTest`, `CommandSpecJsonArgGroupTest`,
`CommandSpecDslHiddenGroupTest`, `CommandSpecJsonHiddenGroupAndBundleTest`,
`CommandSpecDslBundleTest`, `ArgTypesTest`, `SpecValidatorTest`, `FlixExampleTest`) for more
complete, runnable examples.
