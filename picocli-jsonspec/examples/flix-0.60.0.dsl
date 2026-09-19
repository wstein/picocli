// Realistic picocli-jsonspec DSL for the real flix CLI (v0.60.0), based on:
//   fish❯ ./flixw -- --help
// and structural analysis of flix's own scopt-based parser (Main.scala), which declares most
// options as global (parsed before any command, not restricted to it). scopt itself does not
// scope these to specific commands -- the assignment below is this proxy's own curation, picking
// only the options that are actually meaningful for each command, based on what each option does
// and what each command needs. See picocli-jsonspec/examples/README.md for the reasoning behind
// each judgment call.
//
// Known gaps in the current DSL/JSON format (not modeled here): no --/passthrough-args separator,
// no usageHelp/versionHelp option kind (--help/--version below are plain booleans, not wired to
// short-circuit parsing the way picocli's own built-in help options do), and no option inheritance
// (so --help/--version are declared once at the top level only, matching scopt's own behavior of
// parsing global options before the command rather than after it).

command flix "The Flix Programming Language 0.60.0" {
  option --help : boolean "prints this usage information."
  option --version : boolean "prints the version number."
  option --listen : int "starts the socket server and listens on the given port."

  command init "creates a new project in the current directory." {
    option --yes : boolean "automatically answer yes to all prompts."
  }

  command check "checks the current project for errors." {
    option --explain : boolean "provides suggestions on how to solve a problem."
    option --json : boolean "enables json output."
    option --github-token : String "API key to use for GitHub dependency resolution."
    option --no-install : boolean "disables automatic installation of dependencies."
    option --threads : int "number of threads to use for compilation."
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
    positional files : File "input Flix source code files, Flix packages, and Java archives." arity=0..*
  }

  command build "builds (i.e. compiles) the current project." {
    option --explain : boolean "provides suggestions on how to solve a problem."
    option --json : boolean "enables json output."
    option --github-token : String "API key to use for GitHub dependency resolution."
    option --no-install : boolean "disables automatic installation of dependencies."
    option --threads : int "number of threads to use for compilation."
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
    positional files : File "input Flix source code files, Flix packages, and Java archives." arity=0..*
  }

  command build-jar "builds a jar-file from the current project." {
    option --entrypoint : String "specifies the main entry point."
    option --github-token : String "API key to use for GitHub dependency resolution."
    option --no-install : boolean "disables automatic installation of dependencies."
    option --threads : int "number of threads to use for compilation."
  }

  command build-fatjar "builds a fatjar-file from the current project." {
    option --entrypoint : String "specifies the main entry point."
    option --github-token : String "API key to use for GitHub dependency resolution."
    option --no-install : boolean "disables automatic installation of dependencies."
    option --threads : int "number of threads to use for compilation."
  }

  command build-pkg "builds a fpkg-file from the current project." {
    option --github-token : String "API key to use for GitHub dependency resolution."
    option --no-install : boolean "disables automatic installation of dependencies."
    option --threads : int "number of threads to use for compilation."
    option --yes : boolean "automatically answer yes to all prompts."
  }

  command doc "generates API documentation." {
    option --github-token : String "API key to use for GitHub dependency resolution."
    option --no-install : boolean "disables automatic installation of dependencies."
    option --threads : int "number of threads to use for compilation."
  }

  command run "runs main for the current project." {
    option --args : String "arguments passed to main. Must be a single quoted string."
    option --entrypoint : String "specifies the main entry point."
    option --explain : boolean "provides suggestions on how to solve a problem."
    option --json : boolean "enables json output."
    option --github-token : String "API key to use for GitHub dependency resolution."
    option --no-install : boolean "disables automatic installation of dependencies."
    option --threads : int "number of threads to use for compilation."
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
    positional files : File "input Flix source code files, Flix packages, and Java archives." arity=0..*
  }

  command test "runs the tests for the current project." {
    option --explain : boolean "provides suggestions on how to solve a problem."
    option --json : boolean "enables json output."
    option --github-token : String "API key to use for GitHub dependency resolution."
    option --no-install : boolean "disables automatic installation of dependencies."
    option --threads : int "number of threads to use for compilation."
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
    positional files : File "input Flix source code files, Flix packages, and Java archives." arity=0..*
  }

  command repl "starts a repl for the current project, or provided Flix source files." {
    option --github-token : String "API key to use for GitHub dependency resolution."
    option --no-install : boolean "disables automatic installation of dependencies."
    option --threads : int "number of threads to use for compilation."
    positional files : File "input Flix source code files, Flix packages, and Java archives." arity=0..*
  }

  command lsp "starts the Plain-LSP server." {}

  command lsp-vscode "starts the VSCode-LSP server and listens on the given port." {
    positional port : int "the port number to listen on." required
  }

  command release "releases a new version to GitHub." {
    option --yes : boolean "automatically answer yes to all prompts."
  }

  command outdated "shows dependencies which have newer versions available." {
    option --json : boolean "enables json output."
    option --github-token : String "API key to use for GitHub dependency resolution."
    option --no-install : boolean "disables automatic installation of dependencies."
  }
}
