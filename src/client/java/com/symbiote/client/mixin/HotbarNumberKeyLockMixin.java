package com.symbiote.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.symbiote.client.SymbioteModClient;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;

/**
 * Pressing a hotbar number key (1-9) for a slot locked to another online
 * player should do nothing, same as scrolling onto it
 * ({@link HotbarScrollLockMixin}) - the local player simply can't select
 * that slot.
 */
@Mixin(Minecraft.class)
public abstract class HotbarNumberKeyLockMixin {
	@Redirect(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;setSelectedSlot(I)V"))
	private void symbiote$blockLockedNumberKey(final Inventory inventory, final int slot) {
		if (!SymbioteModClient.isLockedToSomeoneElse(slot)) {
			inventory.setSelectedSlot(slot);
		}
	}
}
