package picocli.spec.tool;

import picocli.AutoComplete;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;

/**
 * Generates a bash/zsh-compatible completion script for a {@link CommandSpec}, via picocli's own
 * {@link AutoComplete}. Closes the gap between what this module already delivers (a beautiful,
 * {@code --help}-enabled proxy CLI) and its originally stated goal of also delivering completion.
 */
final class Completion {

    private Completion() {
    }

    static String bashScript(CommandSpec spec, String scriptName) {
        return AutoComplete.bash(scriptName, new CommandLine(spec));
    }
}
