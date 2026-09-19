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

## The DSL

```
command flix "The Flix programming language" {
  option -v, --verbose : boolean "Enable verbose output"

  command build "Compile the project" {
    option --release : boolean "Optimize for release"
  }
  command run "Run the project" {
    positional args : String "Program arguments" arity=0..*
  }
}
```

```java
CommandSpec flixSpec = CommandSpecDsl.parse(dslText);
```

A positional's bare label (`args`) becomes its `paramLabel` (`<args>`), matching picocli's own
convention for annotated fields. Option/positional attributes: `default=<value>`, `required`,
`arity=<range>`, all optional and in any order after the type/description.

## JSON

The DSL compiles to the same model `CommandSpecJson` reads and writes, so you can go either
direction:

```java
String json = CommandSpecJson.write(flixSpec);      // e.g. for docs, a web UI, other tooling
CommandSpec sameSpec = CommandSpecJson.read(json);   // read it back
```

JSON shape (a `CommandSpecJson.write()` output looks like this):

```json
{
  "name": "flix",
  "description": ["The Flix programming language"],
  "options": [
    { "names": ["-v", "--verbose"], "type": "boolean", "arity": "0" }
  ],
  "subcommands": [
    { "name": "build", "options": [ { "names": ["--release"], "type": "boolean", "arity": "0" } ] }
  ]
}
```

## Merging into a host CLI

```java
CommandSpec uber = new CommandLine(new MyUberCommand()).getCommandSpec(); // your own annotated/programmatic CLI
CommandSpecMerger.merge(uber, flixSpec, otherToolSpec);                   // attaches each by its own name()

new CommandLine(uber).execute(args); // "myuber --config x flix build --release" now parses
```

`merge` rejects an imported spec with no name, and rejects a name collision with an existing
subcommand.

## Current limitations

- Argument types are scalar only: `String`, `boolean`, `int`, `long`, `double`, `File`
  (`ArgTypes`). Array/`Collection`-typed options and positionals aren't supported yet.
- No `ArgGroup`/mixin support yet — flat options and positional params per command level.
- No execution wiring is provided or assumed: a merged-in subcommand has no `run()`/`call()`
  of its own. Attach one via the host command's own dispatch logic (e.g. an `IExecutionStrategy`
  that recognizes commands originating from an imported spec and shells out accordingly).

See the test classes (`CommandSpecDslTest`, `CommandSpecJsonTest`, `CommandSpecMergerTest`) for
more complete, runnable examples.
