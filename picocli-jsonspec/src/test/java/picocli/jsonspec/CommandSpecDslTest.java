package picocli.jsonspec;

import org.junit.Test;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.Model.PositionalParamSpec;
import picocli.CommandLine.ParseResult;

import java.io.File;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CommandSpecDslTest {

    @Test
    public void parsesNameAndDescription() {
        CommandSpec spec = CommandSpecDsl.parse("command flix \"The Flix programming language\" {}");
        assertEquals("flix", spec.name());
        assertArrayEquals(new String[] {"The Flix programming language"}, spec.usageMessage().description());
    }

    @Test
    public void parsesCommandWithoutDescription() {
        CommandSpec spec = CommandSpecDsl.parse("command flix {}");
        assertEquals("flix", spec.name());
    }

    @Test
    public void parsesOptionsWithNamesTypeDescriptionAndAttributes() {
        CommandSpec spec = CommandSpecDsl.parse(
                "command flix {\n" +
                "  option -v, --verbose : boolean \"Enable verbose output\"\n" +
                "  option -o, --output : String \"Output directory\" default=build\n" +
                "  option -t, --target : String required arity=1\n" +
                "}");

        assertEquals(3, spec.options().size());

        OptionSpec verbose = spec.findOption("--verbose");
        assertArrayEquals(new String[] {"-v", "--verbose"}, verbose.names());
        assertEquals(boolean.class, verbose.type());
        assertArrayEquals(new String[] {"Enable verbose output"}, verbose.description());
        assertFalse(verbose.required());

        OptionSpec output = spec.findOption("--output");
        assertEquals(String.class, output.type());
        assertEquals("build", output.defaultValue());
        assertFalse(output.required());

        OptionSpec target = spec.findOption("--target");
        assertTrue(target.required());
        assertEquals("1", target.arity().toString());
    }

    @Test
    public void parsesPositionalParams() {
        CommandSpec spec = CommandSpecDsl.parse(
                "command flix {\n" +
                "  positional files : File \"Input files\" arity=0..*\n" +
                "}");

        assertEquals(1, spec.positionalParameters().size());
        PositionalParamSpec files = spec.positionalParameters().get(0);
        assertEquals("<files>", files.paramLabel());
        assertEquals(File.class, files.type());
        assertArrayEquals(new String[] {"Input files"}, files.description());
        assertEquals("0..*", files.arity().toString());
    }

    @Test
    public void parsesNestedSubcommands() {
        CommandSpec spec = CommandSpecDsl.parse(
                "command flix {\n" +
                "  command build \"Compile the project\" {\n" +
                "    option --release : boolean\n" +
                "  }\n" +
                "  command run {\n" +
                "    positional args : String arity=0..*\n" +
                "  }\n" +
                "}");

        assertEquals(2, spec.subcommands().size());
        CommandSpec build = spec.subcommands().get("build").getCommandSpec();
        assertEquals("build", build.name());
        assertArrayEquals(new String[] {"Compile the project"}, build.usageMessage().description());
        assertEquals("--release", build.options().get(0).longestName());

        CommandSpec run = spec.subcommands().get("run").getCommandSpec();
        assertEquals(1, run.positionalParameters().size());
    }

    @Test
    public void builtCommandLineActuallyParsesArguments() {
        CommandSpec spec = CommandSpecDsl.parse(
                "command flix {\n" +
                "  option -v, --verbose : boolean\n" +
                "  command build {\n" +
                "    option --release : boolean\n" +
                "  }\n" +
                "}");

        CommandLine cmd = new CommandLine(spec);
        ParseResult result = cmd.parseArgs("-v", "build", "--release");

        assertTrue(result.matchedOptionValue("--verbose", Boolean.FALSE));
        assertTrue(result.hasSubcommand());
        assertTrue(result.subcommand().matchedOptionValue("--release", Boolean.FALSE));
    }

    @Test
    public void rejectsUnknownType() {
        try {
            CommandSpecDsl.parse("command flix { option -x : Frobnicator }");
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Frobnicator"));
        }
    }

    @Test
    public void skipsLineComments() {
        CommandSpec spec = CommandSpecDsl.parse(
                "// a leading comment\n" +
                "command flix { // trailing comment after the opening brace\n" +
                "  // a comment on its own line\n" +
                "  option -v, --verbose : boolean // trailing comment after a declaration\n" +
                "}\n" +
                "// a trailing comment at the very end");

        assertEquals("flix", spec.name());
        assertEquals(1, spec.options().size());
        assertEquals("--verbose", spec.options().get(0).longestName());
    }

    @Test
    public void lineCommentDoesNotAffectSlashesInsideAString() {
        CommandSpec spec = CommandSpecDsl.parse(
                "command flix \"see https://example.org/docs for more\" {}");

        assertArrayEquals(new String[] {"see https://example.org/docs for more"}, spec.usageMessage().description());
    }

    @Test
    public void rejectsMissingBrace() {
        try {
            CommandSpecDsl.parse("command flix { option -v : boolean");
            fail("expected DslParseException");
        } catch (DslParseException expected) {
            // ok
        }
    }

    @Test
    public void rejectsGarbageAtTopLevel() {
        try {
            CommandSpecDsl.parse("not a command");
            fail("expected DslParseException");
        } catch (DslParseException expected) {
            // ok
        }
    }
}
