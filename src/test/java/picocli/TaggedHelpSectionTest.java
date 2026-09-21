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

        @CommandLine.Parameters(index = "0", arity = "0..1", description = "Standard file argument.")
        String stdFile;

        @CommandLine.Parameters(index = "1", arity = "0..1", helpSection = "experimental", description = "Experimental file argument.")
        String expFile;

        public void run() {}
    }

    @Test
    public void testModelAttributes() {
        ArgGroupSpec group = ArgGroupSpec.builder().addArg(OptionSpec.builder("-a").build()).helpSection("experimental").build();
        assertEquals("experimental", group.helpSection());

        OptionSpec option = OptionSpec.builder("--Xhelp").helpSection("experimental").build();
        assertEquals("experimental", option.helpSection());

        CommandLine.Model.PositionalParamSpec param = CommandLine.Model.PositionalParamSpec.builder().paramLabel("<exp>").helpSection("experimental").build();
        assertEquals("experimental", param.helpSection());

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

        assertEquals("", cmd.getCommandSpec().positionalParameters().get(0).helpSection());
        assertEquals("experimental", cmd.getCommandSpec().positionalParameters().get(1).helpSection());

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

        // Positional parameters in standard help
        assertTrue(usage.contains("Standard file argument."));
        assertFalse("Standard help should exclude experimental positional parameter", usage.contains("Experimental file argument."));

        // Auto-generated notice in standard help
        assertTrue("Standard help should include notice pointing to trigger option",
                usage.contains("Run 'mycmd --Xhelp' to view experimental options and commands."));
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

        // Experimental positional parameter rendered on demand
        assertTrue("Experimental help should contain expFile", usage.contains("Experimental file argument."));
        assertFalse("Experimental help should exclude standard positional parameter", usage.contains("Standard file argument."));

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

        java.util.Optional<String> sectionFromPos = spec.findHelpSectionForPositional("<expFile>");
        assertTrue(sectionFromPos.isPresent());
        assertEquals("experimental", sectionFromPos.get());

        java.util.Optional<String> sectionFromStdPos = spec.findHelpSectionForPositional("<stdFile>");
        assertFalse(sectionFromStdPos.isPresent());

        StringWriter sw = new StringWriter();
        cmd.printHelpSection("experimental", new PrintWriter(sw));
        String rendered = sw.toString();
        assertTrue(rendered.contains("Experimental Options:"));
        assertTrue(rendered.contains("exp-sub"));
    }

    @Command(name = "custom-section",
            helpSections = {
                    @CommandLine.HelpSection(name = "preview",
                            heading = "Preview Features:%n",
                            description = { "These features are subject to change." },
                            emptyMessage = "No preview features available.")
            })
    static class CustomSectionCmd implements Runnable {
        @Option(names = "--foo", helpSection = "preview", description = "A preview option.")
        String foo;

        public void run() {}
    }

    @Test
    public void testHelpSectionSpecCustomHeadingAndLooseOptions() {
        CommandLine cmd = new CommandLine(new CustomSectionCmd());
        CommandLine.Model.HelpSectionSpec sectionSpec = cmd.getCommandSpec().helpSectionSpec("preview");
        assertNotNull(sectionSpec);
        assertEquals("Preview Features:%n", sectionSpec.heading());
        assertEquals("No preview features available.", sectionSpec.emptyMessage());
        assertEquals(1, sectionSpec.description().length);
        assertEquals("These features are subject to change.", sectionSpec.description()[0]);

        StringWriter sw = new StringWriter();
        cmd.printHelpSection("preview", new PrintWriter(sw));
        String rendered = sw.toString();
        assertTrue(rendered.contains("Preview Features:"));
        assertTrue(rendered.contains("These features are subject to change."));
        assertTrue(rendered.contains("--foo"));
        assertTrue(rendered.contains("A preview option."));
    }

    @Command(name = "empty-section",
            helpSections = {
                    @CommandLine.HelpSection(name = "experimental", emptyMessage = "No experimental options for this command.")
            })
    static class EmptySectionCmd implements Runnable {
        @Option(names = "--standard", description = "Standard.")
        String std;

        public void run() {}
    }

    @Test
    public void testHelpSectionSpecEmptyFallbackMessage() {
        CommandLine cmd = new CommandLine(new EmptySectionCmd());
        String rendered = cmd.getHelp().renderHelpSection("experimental");
        assertTrue(rendered.contains("No experimental options for this command."));

        // Non-configured empty section returns empty string
        String unconfigured = cmd.getHelp().renderHelpSection("nonexistent");
        assertEquals("", unconfigured);
    }

    @Test
    public void testHelpSectionSpecProgrammaticApi() {
        CommandLine.Model.CommandSpec spec = CommandLine.Model.CommandSpec.create();
        CommandLine.Model.HelpSectionSpec sectionSpec = CommandLine.Model.HelpSectionSpec.builder("perf")
                .heading("Performance Tuning:%n")
                .description("Tuning knobs.")
                .emptyMessage("No performance tuning options.")
                .build();
        spec.addHelpSectionSpec(sectionSpec);

        assertEquals(sectionSpec, spec.helpSectionSpec("perf"));
        assertTrue(spec.helpSections().contains("perf"));

        CommandLine cmd = new CommandLine(spec);
        assertEquals(String.format("No performance tuning options.%n"), cmd.getHelp().renderHelpSection("perf"));
    }

    @Command(name = "custom-notice",
            helpSections = {
                    @CommandLine.HelpSection(name = "experimental",
                            notice = "%nExperimental options omitted. Run 'custom-notice -X' to inspect.%n")
            })
    static class CustomNoticeCmd implements Runnable {
        @Option(names = "-X", usageHelp = true, helpSection = "experimental", description = "Show experimental.")
        boolean xhelp;

        @Option(names = "--foo", helpSection = "experimental", description = "Experimental foo.")
        boolean foo;

        public void run() {}
    }

    @Test
    public void testHelpSectionsNoticeCustomAndDisabled() {
        CommandLine cmd = new CommandLine(new CustomNoticeCmd());
        String usage = cmd.getUsageMessage();
        assertTrue(usage.contains("Experimental options omitted. Run 'custom-notice -X' to inspect."));

        // Disable notice via usageMessage()
        cmd.getCommandSpec().usageMessage().showHelpSectionsNotice(false);
        String usageNoNotice = cmd.getUsageMessage();
        assertFalse(usageNoNotice.contains("Experimental options omitted."));
    }
}
