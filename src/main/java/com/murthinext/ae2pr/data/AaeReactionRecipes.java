package com.murthinext.ae2pr.data;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.util.GsonHelper;

/**
 * 数据生成阶段直接读取模组文件中的反应仓配方，不加载这些模组。
 */
public final class AaeReactionRecipes {

    private static final String JARS_PROPERTY = "ae2pr.recipeSources";
    private static final List<String> RECIPE_DIRS = List.of("data/advanced_ae/recipes/", "data/advanced_ae/recipe/");
    private static final List<String> MOD_META_FILES = List.of("META-INF/mods.toml", "META-INF/neoforge.mods.toml");
    private static final Pattern MOD_ID = Pattern.compile("modId\\s*=\\s*\"([^\"]+)\"");
    private static final String REACTION_TYPE = "advanced_ae:reaction";
    private static final String CONDITIONAL_TYPE = "forge:conditional";
    private static final String MOD_LOADED_CONDITION = "forge:mod_loaded";

    private AaeReactionRecipes() {
    }

    public record Reaction(String source, String path, int energy, List<JsonObject> inputs, @Nullable JsonObject fluid,
                           JsonObject output, Set<String> conditions) {}

    public static List<Reaction> readAll() {
        List<Reaction> reactions = new ArrayList<>();
        for (Path jarPath : jarPaths()) {
            try (JarFile jar = new JarFile(jarPath.toFile())) {
                String source = readSource(jar, jarPath);
                for (Enumeration<JarEntry> entries = jar.entries(); entries.hasMoreElements();) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (!name.endsWith(".json") || RECIPE_DIRS.stream().noneMatch(name::startsWith)) {
                        continue;
                    }
                    try (Reader reader = new InputStreamReader(jar.getInputStream(entry), StandardCharsets.UTF_8)) {
                        collect(JsonParser.parseReader(reader).getAsJsonObject(), source, name, reactions);
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to read recipes from " + jarPath, e);
            }
        }
        reactions.sort(Comparator.comparing(Reaction::source).thenComparing(Reaction::path));
        return reactions;
    }

    private static List<Path> jarPaths() {
        String value = System.getProperty(JARS_PROPERTY);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing -D" + JARS_PROPERTY +
                                            ", run datagen through the runData Gradle task");
        }
        List<Path> paths = new ArrayList<>();
        for (String part : value.split(Pattern.quote(File.pathSeparator))) {
            if (part.isBlank()) {
                continue;
            }
            Path path = Path.of(part);
            if (!Files.isRegularFile(path)) {
                throw new IllegalStateException("Recipe source not found: " + path);
            }
            paths.add(path);
        }
        return paths;
    }

    private static String readSource(JarFile jar, Path jarPath) throws IOException {
        for (String metaFile : MOD_META_FILES) {
            JarEntry entry = jar.getJarEntry(metaFile);
            if (entry == null) {
                continue;
            }
            String meta;
            try (Reader reader = new InputStreamReader(jar.getInputStream(entry), StandardCharsets.UTF_8)) {
                StringBuilder builder = new StringBuilder();
                char[] buffer = new char[4096];
                for (int read = reader.read(buffer); read > 0; read = reader.read(buffer)) {
                    builder.append(buffer, 0, read);
                }
                meta = builder.toString();
            }
            Matcher matcher = MOD_ID.matcher(meta);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        throw new IllegalStateException("No mod id declared in " + jarPath);
    }

    private static void collect(JsonObject json, String source, String path, List<Reaction> reactions) {
        String type = GsonHelper.getAsString(json, "type", "");
        if (REACTION_TYPE.equals(type)) {
            reactions.add(create(json, source, path, Set.of()));
            return;
        }
        if (!CONDITIONAL_TYPE.equals(type)) {
            return;
        }
        for (JsonElement element : GsonHelper.getAsJsonArray(json, "recipes", new JsonArray())) {
            JsonObject wrapper = element.getAsJsonObject();
            JsonObject recipe = GsonHelper.getAsJsonObject(wrapper, "recipe");
            if (REACTION_TYPE.equals(GsonHelper.getAsString(recipe, "type", ""))) {
                reactions.add(create(recipe, source, path, readConditions(wrapper)));
            }
        }
    }

    private static Reaction create(JsonObject json, String source, String path, Set<String> conditions) {
        List<JsonObject> inputs = new ArrayList<>();
        for (JsonElement element : GsonHelper.getAsJsonArray(json, "input_items", new JsonArray())) {
            inputs.add(element.getAsJsonObject());
        }
        return new Reaction(source, path, GsonHelper.getAsInt(json, "energy"), inputs,
                            json.has("fluid") ? GsonHelper.getAsJsonObject(json, "fluid") : null,
                            GsonHelper.getAsJsonObject(json, "output"), conditions);
    }

    private static Set<String> readConditions(JsonObject wrapper) {
        Set<String> conditions = new LinkedHashSet<>();
        for (JsonElement element : GsonHelper.getAsJsonArray(wrapper, "conditions", new JsonArray())) {
            JsonObject condition = element.getAsJsonObject();
            if (MOD_LOADED_CONDITION.equals(GsonHelper.getAsString(condition, "type", ""))) {
                conditions.add(GsonHelper.getAsString(condition, "modid"));
            }
        }
        return conditions;
    }
}
