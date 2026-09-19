package picocli.jsonspec;

import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.Model.PositionalParamSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * A lightweight, human-friendly text format for describing a picocli {@link CommandSpec},
 * as an alternative to hand-writing {@link CommandSpecJson JSON}. Compiling the DSL always
 * produces the same {@code CommandSpec} model that {@link CommandSpecJson} reads and writes,
 * so a spec can be authored in this DSL and exported to JSON for other tooling, or merged
 * with other specs via {@link CommandSpecMerger}.
 *
 * <h2>Grammar (informal)</h2>
 * <pre>
 * command &lt;name&gt; ["&lt;description&gt;"] {
 *     ( option &lt;name&gt; (, &lt;name&gt;)* : &lt;type&gt; ["&lt;description&gt;"] [default=&lt;value&gt;] [required] [arity=&lt;range&gt;]
 *     | positional &lt;label&gt; : &lt;type&gt; ["&lt;description&gt;"] [default=&lt;value&gt;] [required] [arity=&lt;range&gt;]
 *     | command ... (nested, same grammar)
 *     )*
 * }
 * </pre>
 * <p>A positional parameter's bare {@code &lt;label&gt;} is wrapped as {@code "&lt;label&gt;"} to
 * form its {@linkplain PositionalParamSpec#paramLabel() paramLabel}, matching picocli's own
 * convention for annotated fields. Supported {@code &lt;type&gt;} names are the same as
 * {@link CommandSpecJson}'s (currently scalar types only). A {@code //} starts a line comment,
 * running to end of line; it is not recognized inside a quoted string.</p>
 * <p>Example:</p>
 * <pre>
 * command flix "The Flix programming language" {
 *   option -v, --verbose : boolean "Enable verbose output"
 *   command build "Compile the project" {
 *     option --release : boolean "Optimize for release"
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
        CommandSpec spec = parser.parseCommand();
        parser.expectEnd();
        return spec;
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

        CommandSpec parseCommand() {
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
                    spec.addOption(parseOption());
                } else if ("positional".equals(keyword)) {
                    spec.addPositional(parsePositional());
                } else if ("command".equals(keyword)) {
                    CommandSpec sub = parseCommand();
                    spec.addSubcommand(sub.name(), sub);
                } else {
                    throw new DslParseException("Expected 'option', 'positional', or 'command' but found '" + keyword + "'");
                }
            }
            expect(TokenKind.RBRACE, "'}'");
            return spec;
        }

        private OptionSpec parseOption() {
            expectKeyword("option");
            List<String> names = new ArrayList<String>();
            names.add(expectWord());
            while (check(TokenKind.COMMA)) {
                advance();
                names.add(expectWord());
            }
            expect(TokenKind.COLON, "':'");
            String type = expectWord();

            OptionSpec.Builder builder = OptionSpec.builder(names.toArray(new String[0])).type(ArgTypes.toClass(type));
            if (check(TokenKind.STRING)) {
                builder.description(advance().text);
            }
            while (checkWord("default") || checkWord("required") || checkWord("arity")) {
                String attr = advance().text;
                if ("required".equals(attr)) {
                    builder.required(true);
                } else {
                    expect(TokenKind.EQUALS, "'='");
                    String value = expectWordOrString();
                    if ("default".equals(attr)) { builder.defaultValue(value); } else { builder.arity(value); }
                }
            }
            return builder.build();
        }

        private PositionalParamSpec parsePositional() {
            expectKeyword("positional");
            String label = expectWord();
            expect(TokenKind.COLON, "':'");
            String type = expectWord();

            PositionalParamSpec.Builder builder = PositionalParamSpec.builder()
                    .paramLabel("<" + label + ">")
                    .type(ArgTypes.toClass(type));
            if (check(TokenKind.STRING)) {
                builder.description(advance().text);
            }
            while (checkWord("default") || checkWord("required") || checkWord("arity")) {
                String attr = advance().text;
                if ("required".equals(attr)) {
                    builder.required(true);
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
