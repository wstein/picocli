// Realistic picocli-jsonspec DSL for the real flix CLI (v0.60.0), based on:
// ./flixw -- --help
// and structural analysis of flix's own scopt-based parser (Main.scala), which declares most
// options as global (parsed before any command, not restricted to it). scopt itself does not
// scope these to specific commands -- the assignment below is this proxy's own curation, picking
// only the options that are actually meaningful for each command, based on what each option does
// and what each command needs. See picocli-jsonspec/examples/README.md for the reasoning behind
// each judgment call.
//
// --help/--version are marked usageHelp/versionHelp below and genuinely short-circuit execution.
// Shared options/positionals are declared once in definitions { ... } and referenced by name
// (option --explain, no ':') rather than via inheritance: flix's command tree is flat and each
// shared option applies to an arbitrary, overlapping subset of siblings, which inheritance can't
// express -- see picocli-jsonspec/examples/README.md for why, and for the one remaining format
// gap this file doesn't need (flix's -- passthrough separator needs no special format support at
// all; picocli already handles it natively for an array-typed positional).

definitions {
  option --explain : boolean "provides suggestions on how to solve a problem."
  option --json : boolean "enables json output."
  option --github-token : String "API key to use for GitHub dependency resolution."
  option --no-install : boolean "disables automatic installation of dependencies."
  option --threads : int "number of threads to use for compilation."
  option --entrypoint : String "specifies the main entry point."
  option --yes : boolean "automatically answer yes to all prompts."

  // Not part of real flix's own --help; this proxy's own addition, paired with hiding the
  // --X* flags below. Its *behavior* -- actually printing them -- can't be expressed in a
  // declarative spec; see FlixExperimentalHelp for the runtime side of this.
  option --Xhelp : boolean "shows the experimental options for this command."

  option --Xbenchmark-code-size : boolean "[experimental] benchmarks the size of the generated JVM files."
  option --Xbenchmark-incremental : boolean "[experimental] benchmarks the performance of each compiler phase in incremental mode."
  option --Xbenchmark-phases : boolean "[experimental] benchmarks the performance of each compiler phase."
  option --Xbenchmark-frontend : boolean "[experimental] benchmarks the performance of the frontend."
  option --Xbenchmark-throughput : boolean "[experimental] benchmarks the performance of the entire compiler."
  option --Xlib : String "[experimental] controls the amount of std. lib. to include (nix, min, all)."
  option --Xno-deprecated : boolean "[experimental] disables deprecated features."
  option --Xprint-phases : boolean "[experimental] prints the ASTs after the each phase."
  option --Xsummary : boolean "[experimental] prints a summary of the compiled modules."
  option --Xfuzzer : boolean "[experimental] enables compiler fuzzing."
  option --Xprint-typer : String "[experimental] writes constraints to dot files."
  option --Xsubeffecting : String "[experimental] enables sub-effecting in select places"
  option --Xchaos-monkey : boolean "[experimental] introduces randomness."
  option --Xiterations : int "[experimental] sets the maximum number of constraint resolution iterations during typechecking"

  // Bundles the 14 flags above so each of check/build/run/test can pull them all in with one
  // "use xflags" instead of listing every name.
  bundle xflags {
    option --Xbenchmark-code-size
    option --Xbenchmark-incremental
    option --Xbenchmark-phases
    option --Xbenchmark-frontend
    option --Xbenchmark-throughput
    option --Xlib
    option --Xno-deprecated
    option --Xprint-phases
    option --Xsummary
    option --Xfuzzer
    option --Xprint-typer
    option --Xsubeffecting
    option --Xchaos-monkey
    option --Xiterations
  }

  positional files : File[] "input Flix source code files, Flix packages, and Java archives." arity=0..*
}

command flix "The Flix Programming Language 0.60.0" {
  option --help : boolean "prints this usage information." usageHelp
  option --version : boolean "prints the version number." versionHelp
  option --listen : int "starts the socket server and listens on the given port."

  command init "creates a new project in the current directory." {
    option --yes
  }

  command check "checks the current project for errors." {
    option --explain
    option --json
    option --github-token
    option --no-install
    option --threads
    option --Xhelp
    group cooperative hidden "The following options are experimental:%n" {
      use xflags
    }
    positional files
  }

  command build "builds (i.e. compiles) the current project." {
    option --explain
    option --json
    option --github-token
    option --no-install
    option --threads
    option --Xhelp
    group cooperative hidden "The following options are experimental:%n" {
      use xflags
    }
    positional files
  }

  command build-jar "builds a jar-file from the current project." {
    option --entrypoint
    option --github-token
    option --no-install
    option --threads
  }

  command build-fatjar "builds a fatjar-file from the current project." {
    option --entrypoint
    option --github-token
    option --no-install
    option --threads
  }

  command build-pkg "builds a fpkg-file from the current project." {
    option --github-token
    option --no-install
    option --threads
    option --yes
  }

  command doc "generates API documentation." {
    option --github-token
    option --no-install
    option --threads
  }

  command run "runs main for the current project." {
    option --args : String "arguments passed to main. Must be a single quoted string."
    option --entrypoint
    option --explain
    option --json
    option --github-token
    option --no-install
    option --threads
    option --Xhelp
    group cooperative hidden "The following options are experimental:%n" {
      use xflags
    }
    positional files
  }

  command test "runs the tests for the current project." {
    option --explain
    option --json
    option --github-token
    option --no-install
    option --threads
    option --Xhelp
    group cooperative hidden "The following options are experimental:%n" {
      use xflags
    }
    positional files
  }

  command repl "starts a repl for the current project, or provided Flix source files." {
    option --github-token
    option --no-install
    option --threads
    positional files
  }

  command lsp "starts the Plain-LSP server." {}

  command lsp-vscode "starts the VSCode-LSP server and listens on the given port." {
    positional port : int "the port number to listen on." required
  }

  command release "releases a new version to GitHub." {
    option --yes
  }

  command outdated "shows dependencies which have newer versions available." {
    option --json
    option --github-token
    option --no-install
  }
}
