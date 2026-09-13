package com.symbiote.client;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.symbiote.SymbioteConfig;
import com.symbiote.SymbioteMod;
import com.symbiote.network.HotbarOwnersPayload;
import com.symbiote.network.SyncConfigPayload;
import com.symbiote.network.UpdateConfigPayload;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public class SymbioteModClient implements ClientModInitializer {
	private static volatile List<UUID> hotbarOwners = emptyOwners();
	private static volatile SymbioteConfig lastKnownConfig = new SymbioteConfig();

	private static KeyMapping openSettingsKey;

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(HotbarOwnersPayload.TYPE, (payload, context) -> hotbarOwners = payload.owners());
		ClientPlayNetworking.registerGlobalReceiver(SyncConfigPayload.TYPE, (payload, context) -> lastKnownConfig = payload.toConfig());

		HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, SymbioteMod.id("hotbar_owners"), new HotbarOwnerOverlay());

		KeyMapping.Category category = KeyMapping.Category.register(SymbioteMod.id("main"));
		openSettingsKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.symbiote.open_settings",
			InputConstants.Type.KEYSYM,
			InputConstants.UNKNOWN.getValue(),
			category
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openSettingsKey.consumeClick()) {
				if (client.gui.screen() == null) {
					client.setScreenAndShow(new SymbioteOptionsScreen(null, lastKnownConfig));
				}
			}
		});
	}

	public static List<UUID> getHotbarOwners() {
		return hotbarOwners;
	}

	public static SymbioteConfig getLastKnownConfig() {
		return lastKnownConfig;
	}

	public static void sendConfigUpdate(final SymbioteConfig config) {
		ClientPlayNetworking.send(UpdateConfigPayload.fromConfig(config));
	}

	/** Whether {@code slot} is currently locked to some other online player (not us, not unowned). */
	public static boolean isLockedToSomeoneElse(final int slot) {
		if (slot < 0 || slot >= hotbarOwners.size()) {
			return false;
		}
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null) {
			return false;
		}
		UUID owner = hotbarOwners.get(slot);
		return !owner.equals(HotbarOwnersPayload.NO_OWNER) && !owner.equals(minecraft.player.getUUID());
	}

	private static List<UUID> emptyOwners() {
		List<UUID> owners = new ArrayList<>(HotbarOwnersPayload.SLOT_COUNT);
		for (int i = 0; i < HotbarOwnersPayload.SLOT_COUNT; i++) {
			owners.add(HotbarOwnersPayload.NO_OWNER);
		}
		return owners;
	}
}
