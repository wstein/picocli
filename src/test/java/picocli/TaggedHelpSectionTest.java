package picocli;

import org.junit.Test;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.ArgGroupSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.Option;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.*;

public class TaggedHelpSectionTest {

    @Command(name = "mycmd", description = "Test command with tagged help groups.",
            subcommands = { MyCmd.StdSub.class, MyCmd.ExpSub.class })
    static class MyCmd implements Runnable {
        @Option(names = "--help", usageHelp = true, description = "Show this help message and exit.")
        boolean help;

        @Option(names = "--Xhelp", usageHelp = true, helpSection = "experimental",
                description = "Show experimental options.")
        boolean xhelp;

        @Option(names = "--standard", description = "A standard option.")
        String standard;

        @ArgGroup(helpSection = "experimental", heading = "Experimental Options:%n")
        ExperimentalGroup exp;

        static class ExperimentalGroup {
            @Option(names = "--Xalpha", description = "Alpha experimental flag.")
            boolean alpha;

            @Option(names = "--Xbeta", description = "Beta experimental option.")
            String beta;
        }

        @Command(name = "std-sub", description = "Standard subcommand.")
        static class StdSub implements Runnable {
            public void run() {}
        }

        @Command(name = "exp-sub", helpSection = "experimental", description = "Experimental subcommand.")
        static class ExpSub implements Runnable {
            public void run() {}
        }

        public void run() {}
    }

    @Test
    public void testModelAttributes() {
        ArgGroupSpec group = ArgGroupSpec.builder().addArg(OptionSpec.builder("-a").build()).helpSection("experimental").build();
        assertEquals("experimental", group.helpSection());

        OptionSpec option = OptionSpec.builder("--Xhelp").helpSection("experimental").build();
        assertEquals("experimental", option.helpSection());

        CommandLine.Model.CommandSpec cmdSpec = CommandLine.Model.CommandSpec.create().helpSection("experimental");
        assertEquals("experimental", cmdSpec.helpSection());
    }

    @Test
    public void testAnnotationBinding() {
        CommandLine cmd = new CommandLine(new MyCmd());
        OptionSpec xhelp = cmd.getCommandSpec().findOption("--Xhelp");
        assertNotNull(xhelp);
        assertEquals("experimental", xhelp.helpSection());
        assertTrue(xhelp.usageHelp());

        assertFalse(cmd.getCommandSpec().argGroups().isEmpty());
        ArgGroupSpec expGroup = cmd.getCommandSpec().argGroups().get(0);
        assertEquals("experimental", expGroup.helpSection());

        CommandLine expSub = cmd.getSubcommands().get("exp-sub");
        assertNotNull(expSub);
        assertEquals("experimental", expSub.getCommandSpec().helpSection());

        CommandLine stdSub = cmd.getSubcommands().get("std-sub");
        assertNotNull(stdSub);
        assertEquals("", stdSub.getCommandSpec().helpSection());
    }

    @Test
    public void testStandardHelpExcludesExperimentalGroup() {
        CommandLine cmd = new CommandLine(new MyCmd());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int exitCode = cmd.execute("--help");
        assertEquals(0, exitCode);

        String usage = sw.toString();
        // Visible in standard help
        assertTrue(usage.contains("--standard"));
        assertTrue(usage.contains("--help"));
        assertTrue(usage.contains("--Xhelp"));

        // Experimental group hidden from standard help
        assertFalse(usage.contains("Experimental Options:"));
        assertFalse(usage.contains("--Xalpha"));
        assertFalse(usage.contains("--Xbeta"));

        // Subcommands in standard help
        assertTrue(usage.contains("std-sub"));
        assertFalse("Standard help should exclude experimental subcommand", usage.contains("exp-sub"));
    }

    @Test
    public void testExperimentalHelpRendersExperimentalSection() {
        CommandLine cmd = new CommandLine(new MyCmd());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int exitCode = cmd.execute("--Xhelp");
        assertEquals(0, exitCode);

        String usage = sw.toString();
        // Experimental group rendered on demand
        assertTrue(usage.contains("Experimental Options:"));
        assertTrue(usage.contains("--Xalpha"));
        assertTrue(usage.contains("--Xbeta"));

        // Experimental subcommand rendered on demand
        assertTrue("Experimental help should contain exp-sub", usage.contains("exp-sub"));
        assertFalse("Experimental help should not contain std-sub", usage.contains("std-sub"));

        // Standard options omitted from experimental help
        assertFalse(usage.contains("--standard"));
        assertFalse(usage.contains("Show this help message and exit."));
    }

    @Test
    public void testAutocompletionIncludesExperimentalOptions() {
        CommandLine cmd = new CommandLine(new MyCmd());

        String bash = AutoComplete.bash("mycmd", cmd);
        assertTrue("Bash should suggest --Xalpha", bash.contains("--Xalpha"));
        assertTrue("Bash should suggest --Xbeta", bash.contains("--Xbeta"));

        String fish = AutoComplete.fish("mycmd", cmd);
        assertTrue("Fish should suggest Xalpha", fish.contains("Xalpha"));
        assertTrue("Fish should suggest Xbeta", fish.contains("Xbeta"));
    }

    @Test
    public void testHelpSectionDiscoveryAndLookupApi() {
        CommandLine cmd = new CommandLine(new MyCmd());
        CommandLine.Model.CommandSpec spec = cmd.getCommandSpec();

        Set<String> sections = spec.helpSections();
        assertTrue("helpSections should contain 'experimental'", sections.contains("experimental"));

        java.util.Optional<OptionSpec> trigger = spec.findHelpSectionTrigger("experimental");
        assertTrue(trigger.isPresent());
        assertEquals("--Xhelp", trigger.get().longestName());

        java.util.Optional<OptionSpec> missingTrigger = spec.findHelpSectionTrigger("nonexistent");
        assertFalse(missingTrigger.isPresent());

        java.util.Optional<String> sectionFromFlag = spec.findHelpSectionForOption("--Xhelp");
        assertTrue(sectionFromFlag.isPresent());
        assertEquals("experimental", sectionFromFlag.get());

        java.util.Optional<String> sectionFromGroupOpt = spec.findHelpSectionForOption("--Xalpha");
        assertTrue(sectionFromGroupOpt.isPresent());
        assertEquals("experimental", sectionFromGroupOpt.get());

        java.util.Optional<String> sectionFromStd = spec.findHelpSectionForOption("--standard");
        assertFalse(sectionFromStd.isPresent());

        StringWriter sw = new StringWriter();
        cmd.printHelpSection("experimental", new PrintWriter(sw));
        String rendered = sw.toString();
        assertTrue(rendered.contains("Experimental Options:"));
        assertTrue(rendered.contains("exp-sub"));
    }
}
