package picocli.spec.tool;

import picocli.CommandLine.Model.CommandSpec;
import picocli.spec.CommandSpecDsl;
import picocli.spec.CommandSpecJson;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Loads a {@link CommandSpec} from a picocli-spec file, choosing the DSL parser or the JSON
 * reader based on the file's extension ({@code .picocli}/{@code .dsl} vs {@code .json}). Shared by
 * every subcommand of this tool, since all of them start from "here is a spec file" the same way.
 */
final class SpecLoader {

    private SpecLoader() {
    }

    static CommandSpec load(File specFile) throws IOException {
        String text = new String(Files.readAllBytes(specFile.toPath()), StandardCharsets.UTF_8);
        String name = specFile.getName();
        if (name.endsWith(".json")) {
            return CommandSpecJson.read(text);
        }
        if (name.endsWith(".picocli") || name.endsWith(".dsl")) {
            return CommandSpecDsl.parse(text);
        }
        throw new IllegalArgumentException(
                "Unrecognized spec file extension: \"" + name + "\" (expected \".picocli\" or \".json\")");
    }
}
