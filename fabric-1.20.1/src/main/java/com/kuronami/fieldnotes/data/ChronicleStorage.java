package com.kuronami.fieldnotes.data;

import com.kuronami.fieldnotes.FieldNotes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.GsonBuilder;
import com.google.gson.Gson;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player chronicle persistence (server-side).
 *
 * <p>Each player gets one JSON file under {@code <world>/data/fieldnotes/<uuid>.json}.
 * Entries are appended on advancement grant, persisted on world save. Read on demand.
 *
 * <p>Single-process model: we hold the in-memory list keyed by player UUID and flush
 * to disk after each append (cheap; a chronicle of N entries is small JSON).
 */
public final class ChronicleStorage {

    /** Folder under world dir to store per-player JSON files. */
    private static final String FOLDER_NAME = "fieldnotes";

    private static final Codec<List<ChronicleEntry>> LIST_CODEC = ChronicleEntry.CODEC.listOf();

    /** Loaded chronicles by player UUID, scoped to current server lifecycle. */
    private static final Map<UUID, List<ChronicleEntry>> CACHE = new ConcurrentHashMap<>();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ChronicleStorage() {}

    /** Resolve {@code <world>/data/fieldnotes/} for the running server. Returns null if no path. */
    private static Path folder(MinecraftServer server) {
        Path worldData = server.getWorldPath(LevelResource.ROOT).resolve("data").resolve(FOLDER_NAME);
        try {
            Files.createDirectories(worldData);
        } catch (IOException e) {
            FieldNotes.LOGGER.error("Failed to create chronicle folder: {}", worldData, e);
            return null;
        }
        return worldData;
    }

    private static Path file(MinecraftServer server, UUID uuid) {
        Path f = folder(server);
        return f == null ? null : f.resolve(uuid + ".json");
    }

    /** Get the chronicle for a player, loading from disk on first access. */
    public static List<ChronicleEntry> get(MinecraftServer server, UUID uuid) {
        return CACHE.computeIfAbsent(uuid, key -> load(server, key));
    }

    /** Convenience overload accepting a {@link ServerPlayer}. */
    public static List<ChronicleEntry> get(ServerPlayer player) {
        return get(player.server, player.getUUID());
    }

    /** Append a new entry for the given player and persist immediately. */
    public static void append(ServerPlayer player, ChronicleEntry entry) {
        UUID uuid = player.getUUID();
        List<ChronicleEntry> list = get(player.server, uuid);
        synchronized (list) {
            list.add(entry);
        }
        save(player.server, uuid);
        FieldNotes.LOGGER.debug("Chronicle appended for {}: {}", player.getName().getString(), entry.advancementId());
    }

    /** Load entries from disk. Empty list if file missing or unparseable. */
    private static List<ChronicleEntry> load(MinecraftServer server, UUID uuid) {
        Path path = file(server, uuid);
        if (path == null || !Files.exists(path)) {
            return Collections.synchronizedList(new ArrayList<>());
        }
        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            JsonElement root = JsonParser.parseString(json);
            List<ChronicleEntry> list = LIST_CODEC.parse(JsonOps.INSTANCE, root)
                    .resultOrPartial(err -> FieldNotes.LOGGER.warn("Chronicle parse error for {}: {}", uuid, err))
                    .orElseGet(ArrayList::new);
            return Collections.synchronizedList(new ArrayList<>(list));
        } catch (IOException e) {
            FieldNotes.LOGGER.warn("Failed to read chronicle {}: {}", path, e.toString());
            return Collections.synchronizedList(new ArrayList<>());
        }
    }

    /** Persist a player's chronicle to disk. Called after each append. */
    private static void save(MinecraftServer server, UUID uuid) {
        Path path = file(server, uuid);
        if (path == null) return;
        List<ChronicleEntry> list = CACHE.getOrDefault(uuid, List.of());
        List<ChronicleEntry> snapshot;
        synchronized (list) {
            snapshot = new ArrayList<>(list);
        }
        try {
            JsonElement json = LIST_CODEC.encodeStart(JsonOps.INSTANCE, snapshot)
                    .getOrThrow(false, msg -> { throw new RuntimeException("encode chronicle: " + msg); });
            Files.writeString(path, GSON.toJson(json), StandardCharsets.UTF_8);
        } catch (Exception e) {
            FieldNotes.LOGGER.warn("Failed to write chronicle {}: {}", path, e.toString());
        }
    }

    /** Drop in-memory cache (call on server shutdown). */
    public static void clearCache() {
        CACHE.clear();
    }
}
