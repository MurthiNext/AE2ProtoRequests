package com.murthinext.ae2pr.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.bladecoder.ink.compiler.Compiler;
import com.bladecoder.ink.compiler.IFileHandler;
import com.bladecoder.ink.runtime.Error.ErrorType;
import com.bladecoder.ink.runtime.SimpleJson;

public final class InkStoryCompiler {

    private static final Pattern LINE_ID = Pattern.compile("#\\s*id:\\s*([^#\\]\\r\\n]+)");

    private InkStoryCompiler() {
    }

    public static void main(String[] args) throws Exception {
        Path source = Path.of(args[0]).toAbsolutePath().normalize();
        Path output = Path.of(args[1]);
        Path nodes = Path.of(args[2]);
        List<Path> languages;
        try (var directories = Files.list(source)) {
            languages = directories.filter(Files::isDirectory).sorted().toList();
        }
        if (languages.isEmpty()) {
            throw new IllegalArgumentException("至少需要一个剧情语言目录");
        }
        Set<Path> targets = new HashSet<>();
        for (Path language : languages) {
            targets.add(compile(language, output, nodes));
        }
        try (var generated = Files.walk(output.resolve("assets/ae2pr/story"))) {
            for (Path file : generated.filter(path -> path.getFileName().toString().equals("main.ink.json"))
                    .filter(path -> !targets.contains(path)).toList()) {
                Files.delete(file);
            }
        }
    }

    private static Path compile(Path language, Path output, Path nodes) throws Exception {
        Set<String> ids = new HashSet<>();
        try (var sources = Files.walk(language)) {
            for (Path file : sources.filter(path -> path.toString().endsWith(".ink")).sorted().toList()) {
                var matcher = LINE_ID.matcher(Files.readString(file));
                while (matcher.find()) {
                    String id = matcher.group(1).strip();
                    if (id.length() > 256 || !id.matches("[A-Za-z0-9_./-]+")) {
                        throw new IllegalArgumentException("剧情 ID 格式无效：" + file + " / " + id);
                    }
                    if (!ids.add(id)) {
                        throw new IllegalArgumentException("剧情 ID 重复：" + file + " / " + id);
                    }
                }
            }
        }
        Compiler.Options options = new Compiler.Options();
        options.sourceFilename = "main.ink";
        options.countAllVisits = true;
        options.errorHandler = (message, type) -> {
            if (type == ErrorType.Error) {
                throw new IllegalArgumentException(message);
            }
            System.err.println(message);
        };
        options.fileHandler = new IFileHandler() {
            @Override
            public String resolveInkFilename(String includeName) {
                Path file = language.resolve(includeName).normalize();
                if (!file.startsWith(language)) {
                    throw new IllegalArgumentException("INCLUDE 超出当前语言目录：" + includeName);
                }
                return file.toString();
            }

            @Override
            public String loadInkFileContents(String filename) throws IOException {
                return Files.readString(Path.of(filename), StandardCharsets.UTF_8);
            }
        };
        var program = new Compiler(Files.readString(language.resolve("main.ink")), options).compile();
        if (program == null) {
            throw new IllegalStateException("剧情编译失败：" + language);
        }
        try (var definitions = Files.walk(nodes)) {
            for (Path file : definitions.filter(path -> path.toString().endsWith(".json")).sorted().toList()) {
                Object entry = SimpleJson.textToDictionary(Files.readString(file)).get("entry");
                if (!(entry instanceof String knot) || !program.hasFunction(knot)) {
                    throw new IllegalArgumentException("剧情入口不存在：" + file + " / " + language.getFileName());
                }
            }
        }
        Path target = output.resolve("assets/ae2pr/story/" + language.getFileName() + "/main.ink.json");
        Files.createDirectories(target.getParent());
        Files.writeString(target, program.toJson(), StandardCharsets.UTF_8);
        System.out.println("已编译剧情：" + language.getFileName());
        return target;
    }
}
