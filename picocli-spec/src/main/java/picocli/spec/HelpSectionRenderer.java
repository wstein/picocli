package picocli.spec;

import picocli.CommandLine;
import picocli.CommandLine.Help;
import picocli.CommandLine.IHelpSectionRenderer;
import picocli.CommandLine.Model.ArgGroupSpec;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.Model.PositionalParamSpec;
import picocli.CommandLine.Model.UsageMessageSpec;
import picocli.CommandLine.ParseResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages tagged help sections (such as experimental or internal option groups) that are excluded
 * from standard {@code --help} usage messages, included in shell autocompletion (because their
 * options remain {@code hidden = false}), and rendered on demand when a matching trigger option
 * (e.g. {@code --Xhelp}) is parsed.
 */
public final class HelpSectionRenderer {

    public static final String PREFIX = "helpSection:";

    private HelpSectionRenderer() {}

    /** Returns the helpSection name for the given group, or {@code null} if untagged. */
    public static String getHelpSection(ArgGroupSpec group) {
        if (group == null) { return null; }
        String key = group.headingKey();
        if (key != null && key.startsWith(PREFIX)) {
            return key.substring(PREFIX.length());
        }
        return null;
    }

    /** Returns the helpSection name for the given option, or {@code null} if untagged. */
    public static String getHelpSection(OptionSpec option) {
        if (option == null) { return null; }
        String key = option.descriptionKey();
        if (key != null && key.startsWith(PREFIX)) {
            return key.substring(PREFIX.length());
        }
        return null;
    }

    /**
     * Inspects the command's parse result to determine if any matched option is an on-demand help
     * trigger for a tagged help section. Returns the section name, or {@code null} for standard help.
     */
    public static String findActiveHelpSection(Help help) {
        if (help == null || help.commandSpec() == null) {
            return null;
        }
        CommandLine cmd = help.commandSpec().commandLine();
        if (cmd == null) {
            return null;
        }
        ParseResult pr = cmd.getParseResult();
        if (pr == null) {
            return null;
        }
        for (OptionSpec opt : pr.matchedOptions()) {
            String section = getHelpSection(opt);
            if (section != null) {
                return section;
            }
        }
        return null;
    }

    /**
     * Renders the specified tagged help section for the given command help.
     */
    public static String renderSection(CommandLine cmd, String sectionName) {
        return renderSection(cmd.getHelp(), sectionName);
    }

    /**
     * Renders the specified tagged help section for the given {@link Help} instance.
     */
    public static String renderSection(Help help, String sectionName) {
        if (help == null || sectionName == null) {
            return "";
        }
        Set<ArgSpec> done = new HashSet<ArgSpec>();
        StringBuilder sb = new StringBuilder();

        for (ArgGroupSpec group : help.commandSpec().argGroups()) {
            if (!sectionName.equals(getHelpSection(group))) {
                continue;
            }
            List<OptionSpec> groupOptions = new ArrayList<OptionSpec>(group.allOptionsNested());
            Comparator<OptionSpec> optionSort = help.createDefaultOptionSort();
            if (optionSort != null) {
                Collections.sort(groupOptions, optionSort);
            }
            groupOptions.removeAll(done);
            done.addAll(groupOptions);

            List<PositionalParamSpec> groupPositionals = new ArrayList<PositionalParamSpec>(group.allPositionalParametersNested());
            groupPositionals.removeAll(done);
            done.addAll(groupPositionals);

            Help.Layout groupLayout = help.createDefaultLayout();
            groupLayout.addPositionalParameters(groupPositionals, help.parameterLabelRenderer());
            groupLayout.addOptions(groupOptions, help.parameterLabelRenderer());

            if (group.heading() != null) {
                sb.append(help.createHeading(group.heading()));
            }
            sb.append(groupLayout);
        }

        // Loose options tagged with sectionName that are not in an ArgGroup
        List<OptionSpec> looseOptions = new ArrayList<OptionSpec>();
        for (OptionSpec opt : help.commandSpec().options()) {
            if (opt.group() == null && !opt.usageHelp() && !done.contains(opt) && sectionName.equals(getHelpSection(opt))) {
                looseOptions.add(opt);
            }
        }
        if (!looseOptions.isEmpty()) {
            Comparator<OptionSpec> optionSort = help.createDefaultOptionSort();
            if (optionSort != null) {
                Collections.sort(looseOptions, optionSort);
            }
            Help.Layout layout = help.createDefaultLayout();
            layout.addOptions(looseOptions, help.parameterLabelRenderer());
            sb.append(layout);
        }

        return sb.toString();
    }

    /**
     * Installs tagged help section filtering and rendering on {@code spec} and all its subcommands.
     */
    public static void install(CommandSpec spec) {
        if (spec == null) {
            return;
        }
        for (CommandLine sub : spec.subcommands().values()) {
            install(sub.getCommandSpec());
        }

        Set<String> sectionNames = new LinkedHashSet<String>();
        for (ArgGroupSpec g : spec.argGroups()) {
            String s = getHelpSection(g);
            if (s != null) {
                sectionNames.add(s);
            }
        }
        for (OptionSpec opt : spec.options()) {
            String s = getHelpSection(opt);
            if (s != null) {
                sectionNames.add(s);
            }
        }

        if (sectionNames.isEmpty()) {
            return;
        }

        final CommandSpec cleanSpec = createCleanSpec(spec);
        UsageMessageSpec usageMessage = spec.usageMessage();
        Map<String, IHelpSectionRenderer> sectionMap = usageMessage.sectionMap();

        for (final String key : new ArrayList<String>(usageMessage.sectionKeys())) {
            final IHelpSectionRenderer original = sectionMap.get(key);
            sectionMap.put(key, new IHelpSectionRenderer() {
                public String render(Help help) {
                    String activeSection = findActiveHelpSection(help);
                    if (activeSection != null) {
                        return ""; // Suppress standard sections during on-demand help
                    }
                    if (UsageMessageSpec.SECTION_KEY_OPTION_LIST.equals(key)) {
                        Help cleanHelp = new Help(cleanSpec, help.colorScheme());
                        return cleanHelp.optionList();
                    }
                    if (UsageMessageSpec.SECTION_KEY_OPTION_LIST_HEADING.equals(key)) {
                        Help cleanHelp = new Help(cleanSpec, help.colorScheme());
                        return cleanHelp.optionListHeading();
                    }
                    if (UsageMessageSpec.SECTION_KEY_SYNOPSIS.equals(key)) {
                        Help cleanHelp = new Help(cleanSpec, help.colorScheme());
                        return cleanHelp.synopsis(help.synopsisHeadingLength());
                    }
                    return original != null ? original.render(help) : "";
                }
            });
        }

        for (final String sectionName : sectionNames) {
            String sectionKey = PREFIX + sectionName;
            sectionMap.put(sectionKey, new IHelpSectionRenderer() {
                public String render(Help help) {
                    String activeSection = findActiveHelpSection(help);
                    if (sectionName.equals(activeSection)) {
                        return renderSection(help, sectionName);
                    }
                    return "";
                }
            });
            List<String> keys = new ArrayList<String>(usageMessage.sectionKeys());
            if (!keys.contains(sectionKey)) {
                keys.add(sectionKey);
                usageMessage.sectionKeys(keys);
            }
        }
    }

    private static CommandSpec createCleanSpec(CommandSpec spec) {
        CommandSpec clean = CommandSpec.create();
        clean.name(spec.name());
        clean.aliases(spec.aliases());
        clean.usageMessage().customSynopsis(spec.usageMessage().customSynopsis());
        clean.usageMessage().abbreviateSynopsis(spec.usageMessage().abbreviateSynopsis());
        clean.usageMessage().synopsisHeading(spec.usageMessage().synopsisHeading());
        clean.usageMessage().synopsisSubcommandLabel(spec.usageMessage().synopsisSubcommandLabel());
        clean.usageMessage().optionListHeading(spec.usageMessage().optionListHeading());
        clean.usageMessage().sortOptions(spec.usageMessage().sortOptions());
        clean.usageMessage().sortSynopsis(spec.usageMessage().sortSynopsis());
        clean.parser().posixClusteredShortOptionsAllowed(spec.parser().posixClusteredShortOptionsAllowed());

        for (PositionalParamSpec p : spec.positionalParameters()) {
            clean.addPositional(p);
        }

        Set<OptionSpec> taggedOptions = new HashSet<OptionSpec>();
        for (ArgGroupSpec g : spec.argGroups()) {
            if (getHelpSection(g) != null) {
                taggedOptions.addAll(g.allOptionsNested());
            } else {
                clean.addArgGroup(g);
            }
        }

        for (OptionSpec opt : spec.options()) {
            if (opt.group() == null && !taggedOptions.contains(opt)) {
                clean.addOption(opt);
            }
        }

        for (Map.Entry<String, CommandLine> sub : spec.subcommands().entrySet()) {
            clean.addSubcommand(sub.getKey(), sub.getValue().getCommandSpec());
        }

        new CommandLine(clean);
        return clean;
    }
}
