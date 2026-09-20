package picocli.jsonspec.tool;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ExitCode;
import picocli.jsonspec.CommandSpecDsl;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ManpageTest {

    @Rule
    public TemporaryFolder outdir = new TemporaryFolder();

    @Test
    public void generatesAnAsciiDocFileForTheRootCommandAndEverySubcommand() throws Exception {
        CommandSpec spec = CommandSpecDsl.parse(
                "command demo \"A demo tool.\" {\n" +
                "  option --verbose : boolean \"be verbose.\"\n" +
                "  command build \"Builds the thing.\" {}\n" +
                "}");

        int exitCode = Manpage.generate(spec, outdir.getRoot(), new boolean[0]);

        assertEquals(ExitCode.OK, exitCode);
        File rootPage = new File(outdir.getRoot(), "demo.adoc");
        File buildPage = new File(outdir.getRoot(), "demo-build.adoc");
        assertTrue(rootPage.exists());
        assertTrue(buildPage.exists());

        String rootContent = new String(Files.readAllBytes(rootPage.toPath()), StandardCharsets.UTF_8);
        assertTrue(rootContent.contains("A demo tool."));
        assertTrue(rootContent.contains("--verbose"));
    }
}
