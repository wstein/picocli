package picocli.spec.tool;

import org.junit.Test;
import picocli.CommandLine.Model.CommandSpec;
import picocli.spec.CommandSpecDsl;

import static org.junit.Assert.assertTrue;

public class CompletionTest {

    @Test
    public void generatesABashScriptMentioningEveryOptionAndSubcommand() throws Exception {
        CommandSpec spec = CommandSpecDsl.parse(
                "command demo \"A demo tool.\" {\n" +
                "  option --verbose : boolean \"be verbose.\"\n" +
                "  command build \"Builds the thing.\" {\n" +
                "    option --release : boolean \"optimize for release.\"\n" +
                "  }\n" +
                "}");

        String script = Completion.bashScript(spec, "demo");

        assertTrue(script.contains("#!/usr/bin/env bash"));
        assertTrue(script.contains("--verbose"));
        assertTrue(script.contains("--release"));
        assertTrue(script.contains("build"));
    }
}
