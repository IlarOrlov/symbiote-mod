package com.symbiote.client;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.symbiote.SymbioteMod;
import com.symbiote.network.HotbarOwnersPayload;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

public class SymbioteModClient implements ClientModInitializer {
	private static volatile List<UUID> hotbarOwners = emptyOwners();

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(HotbarOwnersPayload.TYPE, (payload, context) -> hotbarOwners = payload.owners());

		HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, SymbioteMod.id("hotbar_owners"), new HotbarOwnerOverlay());
	}

	public static List<UUID> getHotbarOwners() {
		return hotbarOwners;
	}

	private static List<UUID> emptyOwners() {
		List<UUID> owners = new ArrayList<>(HotbarOwnersPayload.SLOT_COUNT);
		for (int i = 0; i < HotbarOwnersPayload.SLOT_COUNT; i++) {
			owners.add(HotbarOwnersPayload.NO_OWNER);
		}
		return owners;
	}
}
