package com.synergy902.projectrose.menu;

import com.synergy902.projectrose.data.RoseSavedData;
import com.synergy902.projectrose.loadout.LoadoutPreset;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class AdminLoadoutMenu extends AbstractContainerMenu {
    public static final int LOADOUT_SLOT_COUNT = 41;
    private static final int PLAYER_INVENTORY_START = LOADOUT_SLOT_COUNT;

    private final SimpleContainer editorInventory = new SimpleContainer(LOADOUT_SLOT_COUNT);
    private final int loadoutIndex;

    public AdminLoadoutMenu(int containerId, Inventory playerInventory, FriendlyByteBuf data) {
        this(containerId, playerInventory, data.readVarInt());
    }

    public AdminLoadoutMenu(int containerId, Inventory playerInventory, int loadoutIndex) {
        super(RoseMenus.ADMIN_LOADOUT.get(), containerId);
        this.loadoutIndex = Math.max(0, Math.min(4, loadoutIndex));

        if (playerInventory.player instanceof ServerPlayer serverPlayer) {
            RoseSavedData.get(serverPlayer.server).loadout(this.loadoutIndex)
                    .ifPresent(preset -> preset.copyInto(editorInventory));
        }

        addEditorSlots();
        addPlayerInventory(playerInventory);
    }

    public int loadoutIndex() {
        return loadoutIndex;
    }

    public void importPlayerInventory(ServerPlayer player) {
        for (int index = 0; index < LoadoutPreset.MAIN_INVENTORY_SIZE; index++) {
            editorInventory.setItem(index, player.getInventory().items.get(index).copy());
        }
        for (int index = 0; index < LoadoutPreset.ARMOR_SIZE; index++) {
            editorInventory.setItem(LoadoutPreset.MAIN_INVENTORY_SIZE + index,
                    player.getInventory().armor.get(index).copy());
        }
        editorInventory.setItem(
                LoadoutPreset.MAIN_INVENTORY_SIZE + LoadoutPreset.ARMOR_SIZE,
                player.getInventory().offhand.get(0).copy()
        );
        editorInventory.setChanged();
        broadcastChanges();
    }

    public void clearEditor() {
        editorInventory.clearContent();
        editorInventory.setChanged();
        broadcastChanges();
    }

    public void save(ServerPlayer player, String name) {
        RoseSavedData.get(player.server).setLoadout(
                loadoutIndex,
                LoadoutPreset.fromEditor(player, name, editorInventory)
        );
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < LOADOUT_SLOT_COUNT) {
            Slot slot = slots.get(slotId);
            ItemStack carried = getCarried();
            ItemStack current = slot.getItem();
            if (clickType == ClickType.THROW) {
                slot.set(ItemStack.EMPTY);
            } else if (clickType == ClickType.SWAP && button >= 0 && button < 9) {
                slot.set(player.getInventory().getItem(button).copy());
            } else if (clickType == ClickType.CLONE && !current.isEmpty()) {
                ItemStack copied = current.copy();
                copied.setCount(copied.getMaxStackSize());
                slot.set(copied);
            } else if (clickType == ClickType.PICKUP && carried.isEmpty()) {
                if (button == 1 && !current.isEmpty() && current.getCount() > 1) {
                    ItemStack reduced = current.copy();
                    reduced.shrink(1);
                    slot.set(reduced);
                } else {
                    slot.set(ItemStack.EMPTY);
                }
            } else if (clickType == ClickType.PICKUP && !carried.isEmpty()) {
                if (button == 1 && ItemStack.isSameItemSameTags(current, carried)
                        && current.getCount() < current.getMaxStackSize()) {
                    ItemStack increased = current.copy();
                    increased.grow(1);
                    slot.set(increased);
                } else {
                    ItemStack copied = carried.copy();
                    if (button == 1) {
                        copied.setCount(1);
                    }
                    slot.set(copied);
                }
            }
            editorInventory.setChanged();
            broadcastChanges();
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < PLAYER_INVENTORY_START || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        ItemStack source = slots.get(index).getItem();
        if (source.isEmpty()) {
            return ItemStack.EMPTY;
        }
        for (int editorIndex = 0; editorIndex < LOADOUT_SLOT_COUNT; editorIndex++) {
            if (editorInventory.getItem(editorIndex).isEmpty()) {
                editorInventory.setItem(editorIndex, source.copy());
                editorInventory.setChanged();
                broadcastChanges();
                break;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.hasPermissions(2);
    }

    private void addEditorSlots() {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int index = 9 + row * 9 + column;
                addSlot(new GhostLoadoutSlot(editorInventory, index, 8 + column * 18, 18 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new GhostLoadoutSlot(editorInventory, column, 8 + column * 18, 76));
        }
        for (int armorIndex = 0; armorIndex < 4; armorIndex++) {
            addSlot(new GhostLoadoutSlot(
                    editorInventory,
                    LoadoutPreset.MAIN_INVENTORY_SIZE + armorIndex,
                    174,
                    18 + armorIndex * 18
            ));
        }
        addSlot(new GhostLoadoutSlot(
                editorInventory,
                LoadoutPreset.MAIN_INVENTORY_SIZE + LoadoutPreset.ARMOR_SIZE,
                192,
                18
        ));
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 112 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 170));
        }
    }
}
