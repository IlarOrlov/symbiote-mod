package com.symbiote.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class SymbioteNetworking {
	private SymbioteNetworking() {
	}

	public static void register() {
		PayloadTypeRegistry.clientboundPlay().register(HotbarOwnersPayload.TYPE, HotbarOwnersPayload.CODEC);
	}
}
