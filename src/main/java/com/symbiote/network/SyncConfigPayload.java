package com.symbiote.network;

import com.symbiote.SymbioteConfig;
import com.symbiote.SymbioteMod;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Sent from server to every client on join, and again whenever an op or the
 * singleplayer host changes the settings via {@link UpdateConfigPayload}, so
 * every client's settings screen and hotbar-lock rendering agree with the
 * server's actual behavior.
 */
public record SyncConfigPayload(
	boolean syncCraftingGrid,
	boolean syncArmor,
	boolean syncOffhand,
	boolean enableHotbarOwnership,
	SymbioteConfig.HotbarOwnershipMode hotbarOwnershipMode
) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SyncConfigPayload> TYPE = new CustomPacketPayload.Type<>(SymbioteMod.id("sync_config"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SyncConfigPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.BOOL, SyncConfigPayload::syncCraftingGrid,
		ByteBufCodecs.BOOL, SyncConfigPayload::syncArmor,
		ByteBufCodecs.BOOL, SyncConfigPayload::syncOffhand,
		ByteBufCodecs.BOOL, SyncConfigPayload::enableHotbarOwnership,
		ByteBufCodecs.idMapper(
			id -> SymbioteConfig.HotbarOwnershipMode.values()[id],
			SymbioteConfig.HotbarOwnershipMode::ordinal
		),
		SyncConfigPayload::hotbarOwnershipMode,
		SyncConfigPayload::new
	);

	public static SyncConfigPayload fromConfig(final SymbioteConfig config) {
		return new SyncConfigPayload(
			config.syncCraftingGrid,
			config.syncArmor,
			config.syncOffhand,
			config.enableHotbarOwnership,
			config.hotbarOwnershipMode
		);
	}

	public SymbioteConfig toConfig() {
		SymbioteConfig config = new SymbioteConfig();
		config.syncCraftingGrid = this.syncCraftingGrid;
		config.syncArmor = this.syncArmor;
		config.syncOffhand = this.syncOffhand;
		config.enableHotbarOwnership = this.enableHotbarOwnership;
		config.hotbarOwnershipMode = this.hotbarOwnershipMode;
		return config;
	}

	@Override
	public CustomPacketPayload.Type<SyncConfigPayload> type() {
		return TYPE;
	}
}
