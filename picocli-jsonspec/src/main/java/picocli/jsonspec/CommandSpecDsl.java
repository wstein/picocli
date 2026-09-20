package picocli.jsonspec;

import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.Model.PositionalParamSpec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A lightweight, human-friendly text format for describing a picocli {@link CommandSpec},
 * as an alternative to hand-writing {@link CommandSpecJson JSON}. Compiling the DSL always
 * produces the same {@code CommandSpec} model that {@link CommandSpecJson} reads and writes,
 * so a spec can be authored in this DSL and exported to JSON for other tooling, or merged
 * with other specs via {@link CommandSpecMerger}.
 *
 * <h2>Grammar (informal)</h2>
 * <pre>
 * [ definitions {
 *     ( option &lt;name&gt; (, &lt;name&gt;)* : &lt;type&gt; ["&lt;description&gt;"] [default=&lt;value&gt;] [required] [arity=&lt;range&gt;]
 *     | positional &lt;label&gt; : &lt;type&gt; ["&lt;description&gt;"] [default=&lt;value&gt;] [required] [arity=&lt;range&gt;]
 *     )*
 * } ]
 * command &lt;name&gt; ["&lt;description&gt;"] {
 *     ( option &lt;name&gt; (, &lt;name&gt;)* ( : &lt;type&gt; ["&lt;description&gt;"] [default=&lt;value&gt;] [required] [arity=&lt;range&gt;] )?
 *     | positional &lt;label&gt; ( : &lt;type&gt; ["&lt;description&gt;"] [default=&lt;value&gt;] [required] [arity=&lt;range&gt;] )?
 *     | command ... (nested, same grammar)
 *     )*
 * }
 * </pre>
 * <p>A positional parameter's bare {@code &lt;label&gt;} is wrapped as {@code "&lt;label&gt;"} to
 * form its {@linkplain PositionalParamSpec#paramLabel() paramLabel}, matching picocli's own
 * convention for annotated fields. Supported {@code &lt;type&gt;} names are the same as
 * {@link CommandSpecJson}'s (currently scalar types only). A {@code //} starts a line comment,
 * running to end of line; it is not recognized inside a quoted string.</p>
 * <p>An {@code option}/{@code positional} statement inside a {@code command} body that omits the
 * {@code : <type>} part (and everything after it) is a <em>reference</em> to a same-named option
 * or positional param declared once in the optional top-level {@code definitions} block, instead
 * of a new definition -- the DSL's answer to many commands sharing the same option, without
 * repeating its type/description/etc. verbatim in each one. A definition and a reference can be
 * freely mixed within one command's own option/positional list. An option reference must name
 * exactly one option (the one being referenced); a multi-name list only makes sense when
 * defining (with a {@code :}), not referencing.</p>
 * <p>Example:</p>
 * <pre>
 * definitions {
 *   option --release : boolean "Optimize for release"
 * }
 * command flix "The Flix programming language" {
 *   option -v, --verbose : boolean "Enable verbose output"
 *   command build "Compile the project" {
 *     option --release
 *   }
 * }
 * </pre>
 */
public final class CommandSpecDsl {

    private CommandSpecDsl() {}

    /** Parses the given DSL text into a {@link CommandSpec} (with any nested subcommands). */
    public static CommandSpec parse(String dsl) {
        List<Token> tokens = new Lexer(dsl).tokenize();
        Parser parser = new Parser(tokens);
        Definitions definitions = parser.parseDefinitionsBlockIfPresent();
        CommandSpec spec = parser.parseCommand(definitions);
        parser.expectEnd();
        return spec;
    }

    /**
     * The optional top-level {@code definitions} block: named option/positional-param templates
     * that a command body can reference by name (an {@code option}/{@code positional} statement
     * with no {@code :}) instead of repeating the full definition. Options are keyed by every
     * name they were declared with; positional params are keyed by their bare (unwrapped) label.
     * Resolving a reference clones the stored definition via picocli's own
     * {@code OptionSpec.builder(original)}/{@code PositionalParamSpec.builder(original)}, since
     * a single {@code ArgSpec} instance can't belong to more than one {@code CommandSpec}.
     */
    private static final class Definitions {
        static final Definitions EMPTY = new Definitions(
                Collections.<String, OptionSpec>emptyMap(), Collections.<String, PositionalParamSpec>emptyMap());

        final Map<String, OptionSpec> options;
        final Map<String, PositionalParamSpec> positionalParams;

        Definitions(Map<String, OptionSpec> options, Map<String, PositionalParamSpec> positionalParams) {
            this.options = options;
            this.positionalParams = positionalParams;
        }

        OptionSpec resolveOption(String name) {
            OptionSpec def = options.get(name);
            if (def == null) {
                throw new DslParseException("Reference to undefined option \"" + name + "\": not declared in the definitions block");
            }
            return OptionSpec.builder(def).build();
        }

        PositionalParamSpec resolvePositional(String label) {
            PositionalParamSpec def = positionalParams.get(label);
            if (def == null) {
                throw new DslParseException("Reference to undefined positional parameter \"" + label + "\": not declared in the definitions block");
            }
            return PositionalParamSpec.builder(def).build();
        }
    }

    // ---- lexer ----

    private enum TokenKind { LBRACE, RBRACE, COLON, COMMA, EQUALS, STRING, WORD, EOF }

    private static final class Token {
        final TokenKind kind;
        final String text;
        Token(TokenKind kind, String text) { this.kind = kind; this.text = text; }
    }

    private static final class Lexer {
        private final String text;
        private int pos;

        Lexer(String text) { this.text = text; }

        List<Token> tokenize() {
            List<Token> tokens = new ArrayList<Token>();
            Token token;
            do {
                token = next();
                tokens.add(token);
            } while (token.kind != TokenKind.EOF);
            return tokens;
        }

        private Token next() {
            skipInsignificant();
            if (pos >= text.length()) { return new Token(TokenKind.EOF, ""); }
            char c = text.charAt(pos);
            switch (c) {
                case '{': pos++; return new Token(TokenKind.LBRACE, "{");
                case '}': pos++; return new Token(TokenKind.RBRACE, "}");
                case ':': pos++; return new Token(TokenKind.COLON, ":");
                case ',': pos++; return new Token(TokenKind.COMMA, ",");
                case '=': pos++; return new Token(TokenKind.EQUALS, "=");
                case '"': return readString();
                default:  return readWord();
            }
        }

        /** Skips whitespace and {@code //} line comments (to end of line), repeating until neither remains. */
        private void skipInsignificant() {
            while (true) {
                while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) { pos++; }
                if (pos + 1 < text.length() && text.charAt(pos) == '/' && text.charAt(pos + 1) == '/') {
                    while (pos < text.length() && text.charAt(pos) != '\n') { pos++; }
                } else {
                    break;
                }
            }
        }

        private Token readString() {
            int start = pos;
            pos++; // opening quote
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (pos >= text.length()) {
                    throw new DslParseException("Unterminated string starting at position " + start);
                }
                char c = text.charAt(pos++);
                if (c == '"') { break; }
                if (c == '\\' && pos < text.length()) {
                    char escaped = text.charAt(pos++);
                    switch (escaped) {
                        case '"':  sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case 'n':  sb.append('\n'); break;
                        case 't':  sb.append('\t'); break;
                        default:   sb.append(escaped);
                    }
                } else {
                    sb.append(c);
                }
            }
            return new Token(TokenKind.STRING, sb.toString());
        }

        private Token readWord() {
            int start = pos;
            while (pos < text.length() && !Character.isWhitespace(text.charAt(pos)) && "{}:,=\"".indexOf(text.charAt(pos)) < 0) {
                pos++;
            }
            return new Token(TokenKind.WORD, text.substring(start, pos));
        }
    }

    // ---- parser ----

    private static final class Parser {
        private final List<Token> tokens;
        private int pos;

        Parser(List<Token> tokens) { this.tokens = tokens; }

        private Token current() { return tokens.get(pos); }
        private boolean check(TokenKind kind) { return current().kind == kind; }
        private boolean checkWord(String text) { return check(TokenKind.WORD) && current().text.equals(text); }
        private Token advance() { Token t = current(); if (t.kind != TokenKind.EOF) { pos++; } return t; }

        void expectEnd() {
            if (!check(TokenKind.EOF)) {
                throw new DslParseException("Unexpected trailing content: '" + current().text + "'");
            }
        }

        private void expect(TokenKind kind, String expectedDescription) {
            if (!check(kind)) {
                throw new DslParseException("Expected " + expectedDescription + " but found '" + current().text + "'");
            }
            advance();
        }

        private void expectKeyword(String keyword) {
            if (!checkWord(keyword)) {
                throw new DslParseException("Expected '" + keyword + "' but found '" + current().text + "'");
            }
            advance();
        }

        private String expectWord() {
            if (!check(TokenKind.WORD)) {
                throw new DslParseException("Expected a name but found '" + current().text + "'");
            }
            return advance().text;
        }

        private String expectWordOrString() {
            if (check(TokenKind.WORD) || check(TokenKind.STRING)) { return advance().text; }
            throw new DslParseException("Expected a value but found '" + current().text + "'");
        }

        /** Parses the optional top-level {@code definitions { ... } } block, or returns {@link Definitions#EMPTY} if absent. */
        Definitions parseDefinitionsBlockIfPresent() {
            if (!checkWord("definitions")) { return Definitions.EMPTY; }
            advance();
            expect(TokenKind.LBRACE, "'{'");
            Map<String, OptionSpec> options = new LinkedHashMap<String, OptionSpec>();
            Map<String, PositionalParamSpec> positionalParams = new LinkedHashMap<String, PositionalParamSpec>();
            while (!check(TokenKind.RBRACE)) {
                if (checkWord("option")) {
                    OptionSpec option = parseOption(Definitions.EMPTY);
                    for (String optionName : option.names()) { options.put(optionName, option); }
                } else if (checkWord("positional")) {
                    PositionalParamSpec positional = parsePositional(Definitions.EMPTY);
                    positionalParams.put(unwrapParamLabel(positional.paramLabel()), positional);
                } else {
                    throw new DslParseException("Expected 'option' or 'positional' but found '" + current().text + "'");
                }
            }
            expect(TokenKind.RBRACE, "'}'");
            return new Definitions(options, positionalParams);
        }

        private String unwrapParamLabel(String paramLabel) {
            return paramLabel.startsWith("<") && paramLabel.endsWith(">")
                    ? paramLabel.substring(1, paramLabel.length() - 1)
                    : paramLabel;
        }

        CommandSpec parseCommand(Definitions definitions) {
            expectKeyword("command");
            String name = expectWord();
            CommandSpec spec = CommandSpec.create().name(name);
            if (check(TokenKind.STRING)) {
                spec.usageMessage().description(advance().text);
            }
            expect(TokenKind.LBRACE, "'{'");
            while (!check(TokenKind.RBRACE)) {
                if (!check(TokenKind.WORD)) {
                    throw new DslParseException("Expected 'option', 'positional', or 'command' but found '" + current().text + "'");
                }
                String keyword = current().text;
                if ("option".equals(keyword)) {
                    spec.addOption(parseOption(definitions));
                } else if ("positional".equals(keyword)) {
                    spec.addPositional(parsePositional(definitions));
                } else if ("command".equals(keyword)) {
                    CommandSpec sub = parseCommand(definitions);
                    spec.addSubcommand(sub.name(), sub);
                } else {
                    throw new DslParseException("Expected 'option', 'positional', or 'command' but found '" + keyword + "'");
                }
            }
            expect(TokenKind.RBRACE, "'}'");
            return spec;
        }

        private OptionSpec parseOption(Definitions definitions) {
            expectKeyword("option");
            List<String> names = new ArrayList<String>();
            names.add(expectWord());
            while (check(TokenKind.COMMA)) {
                advance();
                names.add(expectWord());
            }
            if (!check(TokenKind.COLON)) {
                if (names.size() != 1) {
                    throw new DslParseException("An option reference (no ':') must name exactly one option, got: " + names);
                }
                return definitions.resolveOption(names.get(0));
            }
            advance(); // ':'
            String type = expectWord();

            OptionSpec.Builder builder = OptionSpec.builder(names.toArray(new String[0])).type(ArgTypes.toClass(type));
            if (check(TokenKind.STRING)) {
                builder.description(advance().text);
            }
            while (checkWord("default") || checkWord("required") || checkWord("arity")
                    || checkWord("usageHelp") || checkWord("versionHelp") || checkWord("inherit")) {
                String attr = advance().text;
                if ("required".equals(attr)) {
                    builder.required(true);
                } else if ("usageHelp".equals(attr)) {
                    builder.usageHelp(true);
                } else if ("versionHelp".equals(attr)) {
                    builder.versionHelp(true);
                } else if ("inherit".equals(attr)) {
                    builder.scopeType(picocli.CommandLine.ScopeType.INHERIT);
                } else {
                    expect(TokenKind.EQUALS, "'='");
                    String value = expectWordOrString();
                    if ("default".equals(attr)) { builder.defaultValue(value); } else { builder.arity(value); }
                }
            }
            return builder.build();
        }

        private PositionalParamSpec parsePositional(Definitions definitions) {
            expectKeyword("positional");
            String label = expectWord();
            if (!check(TokenKind.COLON)) {
                return definitions.resolvePositional(label);
            }
            advance(); // ':'
            String type = expectWord();

            PositionalParamSpec.Builder builder = PositionalParamSpec.builder()
                    .paramLabel("<" + label + ">")
                    .type(ArgTypes.toClass(type));
            if (check(TokenKind.STRING)) {
                builder.description(advance().text);
            }
            while (checkWord("default") || checkWord("required") || checkWord("arity") || checkWord("inherit")) {
                String attr = advance().text;
                if ("required".equals(attr)) {
                    builder.required(true);
                } else if ("inherit".equals(attr)) {
                    builder.scopeType(picocli.CommandLine.ScopeType.INHERIT);
                } else {
                    expect(TokenKind.EQUALS, "'='");
                    String value = expectWordOrString();
                    if ("default".equals(attr)) { builder.defaultValue(value); } else { builder.arity(value); }
                }
            }
            return builder.build();
        }
    }
}
