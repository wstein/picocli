package picocli.jsonspec;

import org.junit.Test;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * A {@code collection} inside {@code definitions} names a reusable bundle of already-defined
 * options/positionals; {@code use <name>} inside a command or group body expands the whole
 * bundle at that point (each member freshly cloned, same as an individual reference) instead of
 * listing every member name individually.
 */
public class CommandSpecDslCollectionTest {

    @Test
    public void useExpandsEveryOptionInTheCollection() {
        CommandSpec spec = CommandSpecDsl.parse(
                "definitions {\n" +
                "  option --Xfoo : boolean\n" +
                "  option --Xbar : boolean\n" +
                "  collection xflags {\n" +
                "    option --Xfoo\n" +
                "    option --Xbar\n" +
                "  }\n" +
                "}\n" +
                "command flix {\n" +
                "  command check {\n" +
                "    use xflags\n" +
                "  }\n" +
                "}");

        CommandSpec check = spec.subcommands().get("check").getCommandSpec();
        assertEquals(2, check.options().size());
        assertTrue(check.findOption("--Xfoo") != null);
        assertTrue(check.findOption("--Xbar") != null);
    }

    @Test
    public void useExpandsPositionalsToo() {
        CommandSpec spec = CommandSpecDsl.parse(
                "definitions {\n" +
                "  positional files : File[] arity=0..*\n" +
                "  collection inputs {\n" +
                "    positional files\n" +
                "  }\n" +
                "}\n" +
                "command flix {\n" +
                "  command build {\n" +
                "    use inputs\n" +
                "  }\n" +
                "}");

        CommandSpec build = spec.subcommands().get("build").getCommandSpec();
        assertEquals(1, build.positionalParameters().size());
    }

    @Test
    public void useCanBeCombinedWithOtherStatementsInTheSameCommand() {
        CommandSpec spec = CommandSpecDsl.parse(
                "definitions {\n" +
                "  option --Xfoo : boolean\n" +
                "  collection xflags { option --Xfoo }\n" +
                "}\n" +
                "command flix {\n" +
                "  command check {\n" +
                "    option --explain : boolean\n" +
                "    use xflags\n" +
                "    option --json : boolean\n" +
                "  }\n" +
                "}");

        CommandSpec check = spec.subcommands().get("check").getCommandSpec();
        assertEquals(3, check.options().size());
    }

    @Test
    public void useCanAppearInsideAGroup() {
        CommandSpec spec = CommandSpecDsl.parse(
                "definitions {\n" +
                "  option --Xfoo : boolean\n" +
                "  collection xflags { option --Xfoo }\n" +
                "}\n" +
                "command flix {\n" +
                "  group cooperative \"Experimental\" {\n" +
                "    use xflags\n" +
                "  }\n" +
                "}");

        assertEquals(1, spec.argGroups().size());
        assertEquals(1, spec.argGroups().get(0).args().size());
    }

    @Test
    public void twoUsesOfTheSameCollectionGetIndependentInstances() {
        CommandSpec spec = CommandSpecDsl.parse(
                "definitions {\n" +
                "  option --Xfoo : boolean\n" +
                "  collection xflags { option --Xfoo }\n" +
                "}\n" +
                "command flix {\n" +
                "  command check { use xflags }\n" +
                "  command build { use xflags }\n" +
                "}");

        assertNotSame(
                spec.subcommands().get("check").getCommandSpec().findOption("--Xfoo"),
                spec.subcommands().get("build").getCommandSpec().findOption("--Xfoo"));
    }

    @Test
    public void collectionMemberMustAlreadyBeDefined() {
        try {
            CommandSpecDsl.parse("definitions { collection xflags { option --nope } }\ncommand flix {}");
            fail("expected DslParseException");
        } catch (DslParseException expected) {
            assertTrue(expected.getMessage().contains("--nope"));
        }
    }

    @Test
    public void rejectsUseOfUndefinedCollection() {
        try {
            CommandSpecDsl.parse("command flix { use nope }");
            fail("expected DslParseException");
        } catch (DslParseException expected) {
            assertTrue(expected.getMessage().contains("nope"));
        }
    }

    @Test
    public void collectionCanBundleAGroup() {
        CommandSpec spec = CommandSpecDsl.parse(
                "definitions {\n" +
                "  option --json : boolean\n" +
                "  option --xml : boolean\n" +
                "  collection outputFormat {\n" +
                "    group exclusive \"Output format\" {\n" +
                "      option --json\n" +
                "      option --xml\n" +
                "    }\n" +
                "  }\n" +
                "}\n" +
                "command flix {\n" +
                "  command check { use outputFormat }\n" +
                "}");

        CommandSpec check = spec.subcommands().get("check").getCommandSpec();
        assertEquals(1, check.argGroups().size());
        assertTrue(check.argGroups().get(0).exclusive());
        assertEquals("Output format", check.argGroups().get(0).heading());
        assertEquals(2, check.options().size());
    }

    @Test
    public void twoUsesOfACollectionGroupGetIndependentGroupInstances() {
        CommandSpec spec = CommandSpecDsl.parse(
                "definitions {\n" +
                "  option --json : boolean\n" +
                "  collection outputFormat {\n" +
                "    group exclusive { option --json }\n" +
                "  }\n" +
                "}\n" +
                "command flix {\n" +
                "  command check { use outputFormat }\n" +
                "  command build { use outputFormat }\n" +
                "}");

        assertNotSame(
                spec.subcommands().get("check").getCommandSpec().argGroups().get(0),
                spec.subcommands().get("build").getCommandSpec().argGroups().get(0));
        assertNotSame(
                spec.subcommands().get("check").getCommandSpec().findOption("--json"),
                spec.subcommands().get("build").getCommandSpec().findOption("--json"));
    }

    @Test
    public void collectionGroupCanBeExclusiveGroupWithMutualExclusionEnforced() {
        CommandSpec spec = CommandSpecDsl.parse(
                "definitions {\n" +
                "  option --json : boolean\n" +
                "  option --xml : boolean\n" +
                "  collection outputFormat {\n" +
                "    group exclusive { option --json option --xml }\n" +
                "  }\n" +
                "}\n" +
                "command flix { command check { use outputFormat } }");
        CommandLine cmd = new CommandLine(spec);

        try {
            cmd.parseArgs("check", "--json", "--xml");
            fail("expected MutuallyExclusiveArgsException");
        } catch (CommandLine.MutuallyExclusiveArgsException expected) {
            // ok
        }
    }

    @Test
    public void collectionGroupCanBeHidden() {
        CommandSpec spec = CommandSpecDsl.parse(
                "definitions {\n" +
                "  option --Xfoo : boolean\n" +
                "  collection xflags {\n" +
                "    group cooperative hidden \"Experimental\" { option --Xfoo }\n" +
                "  }\n" +
                "}\n" +
                "command flix { command check { use xflags } }");

        CommandSpec check = spec.subcommands().get("check").getCommandSpec();
        assertTrue(check.argGroups().isEmpty());
        assertTrue(check.findOption("--Xfoo").hidden());
    }

    @Test
    public void expandedOptionsActuallyParse() {
        CommandSpec spec = CommandSpecDsl.parse(
                "definitions {\n" +
                "  option --Xfoo : boolean\n" +
                "  collection xflags { option --Xfoo }\n" +
                "}\n" +
                "command flix {\n" +
                "  command check { use xflags }\n" +
                "}");
        CommandLine cmd = new CommandLine(spec);

        CommandLine.ParseResult result = cmd.parseArgs("check", "--Xfoo");

        assertTrue(result.subcommand().matchedOptionValue("--Xfoo", Boolean.FALSE));
    }
}
