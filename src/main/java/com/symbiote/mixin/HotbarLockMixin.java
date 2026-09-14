package com.symbiote.mixin;

import java.util.List;
import java.util.UUID;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.symbiote.HotbarOwnership;
import com.symbiote.SymbioteConfig;
import com.symbiote.Team;
import com.symbiote.TeamManager;
import com.symbiote.network.HotbarOwnersPayload;

import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Keeps a hotbar slot locked to {@link HotbarOwnership} untouchable by
 * anyone else, in two layers:
 *
 * <ol>
 *   <li>If the click is directly on a locked slot - hovering it and pressing
 *   Q (drop), F (swap hands), a number key, or clicking it outright - the
 *   whole click is cancelled before vanilla does anything. This has to
 *   happen <em>before</em>, not after: dropping spawns an item entity in the
 *   world and swap-hands writes into the offhand slot, neither of which a
 *   same-tick revert could undo without leaving a duplicated item behind.</li>
 *   <li>As a backstop for indirect landings vanilla picks internally - e.g.
 *   shift-clicking a stack in from a chest, where the destination hotbar
 *   slot isn't a parameter we can check up front - the 9 hotbar slots and
 *   the cursor are snapshotted before the click and restored if a slot
 *   locked to someone else ends up changed anyway.</li>
 * </ol>
 */
@Mixin(AbstractContainerMenu.class)
public abstract class HotbarLockMixin {
	@Shadow
	@Final
	public NonNullList<Slot> slots;

	@Shadow
	public abstract ItemStack getCarried();

	@Shadow
	public abstract void setCarried(ItemStack stack);

	@Shadow
	public abstract void broadcastFullState();

	@Unique
	private ItemStack[] symbiote$before;

	@Unique
	private ItemStack symbiote$beforeCarried;

	@Unique
	private UUID symbiote$clicker;

	@Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
	private void symbiote$capture(final int slotId, final int button, final ContainerInput input, final Player player, final CallbackInfo ci) {
		this.symbiote$before = null;

		if (!(player instanceof ServerPlayer serverPlayer) || !SymbioteConfig.get().enableHotbarOwnership) {
			return;
		}

		List<UUID> owners = HotbarOwnership.currentOwnersFor(serverPlayer);

		int hoveredHotbarSlot = this.symbiote$hotbarIndexOf(slotId, serverPlayer);
		boolean touchesLockedSlotDirectly = this.symbiote$isLockedToSomeoneElse(hoveredHotbarSlot, owners, serverPlayer)
			|| (input == ContainerInput.SWAP && this.symbiote$isLockedToSomeoneElse(button, owners, serverPlayer));

		if (touchesLockedSlotDirectly) {
			ci.cancel();
			return;
		}

		this.symbiote$clicker = serverPlayer.getUUID();

		Team team = TeamManager.teamOf(serverPlayer);
		ItemStack[] snapshot = new ItemStack[HotbarOwnersPayload.SLOT_COUNT];
		for (int i = 0; i < snapshot.length; i++) {
			snapshot[i] = team.items.get(i).copy();
		}
		this.symbiote$before = snapshot;
		this.symbiote$beforeCarried = this.getCarried().copy();
	}

	@Inject(method = "clicked", at = @At("RETURN"))
	private void symbiote$revert(final int slotId, final int button, final ContainerInput input, final Player player, final CallbackInfo ci) {
		ItemStack[] before = this.symbiote$before;
		this.symbiote$before = null;
		if (before == null || !(player instanceof ServerPlayer serverPlayer)) {
			return;
		}

		List<UUID> owners = HotbarOwnership.currentOwnersFor(serverPlayer);
		Team team = TeamManager.teamOf(serverPlayer);

		boolean violated = false;
		for (int i = 0; i < before.length; i++) {
			if (this.symbiote$isLockedToSomeoneElse(i, owners, serverPlayer) && !ItemStack.matches(before[i], team.items.get(i))) {
				violated = true;
				break;
			}
		}

		if (!violated) {
			return;
		}

		for (int i = 0; i < before.length; i++) {
			team.items.set(i, before[i]);
		}
		this.setCarried(this.symbiote$beforeCarried);
		this.broadcastFullState();
	}

	/** Maps a menu-local slot id to the shared hotbar index (0-8) it refers to, or -1 if it isn't one. */
	@Unique
	private int symbiote$hotbarIndexOf(final int slotId, final ServerPlayer player) {
		if (slotId < 0 || slotId >= this.slots.size()) {
			return -1;
		}
		Slot slot = this.slots.get(slotId);
		if (slot.container != player.getInventory()) {
			return -1;
		}
		int index = slot.getContainerSlot();
		return (index >= 0 && index < HotbarOwnersPayload.SLOT_COUNT) ? index : -1;
	}

	@Unique
	private boolean symbiote$isLockedToSomeoneElse(final int hotbarIndex, final List<UUID> owners, final ServerPlayer player) {
		if (hotbarIndex < 0 || hotbarIndex >= HotbarOwnersPayload.SLOT_COUNT) {
			return false;
		}
		UUID owner = owners.get(hotbarIndex);
		return !owner.equals(HotbarOwnersPayload.NO_OWNER) && !owner.equals(player.getUUID());
	}
}
