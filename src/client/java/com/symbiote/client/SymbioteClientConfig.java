package com.symbiote.client;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.symbiote.SymbioteMod;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Purely local, per-client settings - never sent to or read from the server.
 * Currently just the low-health screen tint toggle: whether a warning is
 * shown is each player's own business, unlike everything in
 * {@link com.symbiote.SymbioteConfig}, which has to be identical for
 * everyone since it changes actual shared gameplay behavior.
 */
public final class SymbioteClientConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static volatile SymbioteClientConfig instance = new SymbioteClientConfig();
	private static Path configPath;

	public boolean lowHealthWarningEnabled = true;

	public static SymbioteClientConfig get() {
		return instance;
	}

	public static synchronized void load() {
		configPath = FabricLoader.getInstance().getConfigDir().resolve("symbiote-client.json");
		if (Files.exists(configPath)) {
			try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
				SymbioteClientConfig loaded = GSON.fromJson(reader, SymbioteClientConfig.class);
				if (loaded != null) {
					instance = loaded;
				}
			} catch (IOException | RuntimeException e) {
				SymbioteMod.LOGGER.warn("Failed to read config/symbiote-client.json, using defaults", e);
			}
		}
		save();
	}

	public static synchronized void save() {
		if (configPath == null) {
			configPath = FabricLoader.getInstance().getConfigDir().resolve("symbiote-client.json");
		}
		try {
			Files.createDirectories(configPath.getParent());
			Path tmp = configPath.resolveSibling("symbiote-client.json.tmp");
			try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
				GSON.toJson(instance, writer);
			}
			Files.move(tmp, configPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			SymbioteMod.LOGGER.warn("Failed to write config/symbiote-client.json", e);
		}
	}

	public static synchronized void setLowHealthWarningEnabled(final boolean enabled) {
		instance.lowHealthWarningEnabled = enabled;
		save();
	}
}
