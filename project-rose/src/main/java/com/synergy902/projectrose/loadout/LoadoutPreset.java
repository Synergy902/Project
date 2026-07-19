package com.synergy902.projectrose.loadout;

import com.synergy902.projectrose.compat.CuriosCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.List;

public final class LoadoutPreset {
    public static final int MAIN_INVENTORY_SIZE = 36;
    public static final int ARMOR_SIZE = 4;
    public static final int OFFHAND_SIZE = 1;

    private final String name;
    private final List<CompoundTag> mainInventory;
    private final List<CompoundTag> armor;
    private final List<CompoundTag> offhand;
    private final ListTag curiosInventory;

    private LoadoutPreset(
            String name,
            List<CompoundTag> mainInventory,
            List<CompoundTag> armor,
            List<CompoundTag> offhand,
            ListTag curiosInventory
    ) {
        this.name = name;
        this.mainInventory = copyTags(mainInventory, MAIN_INVENTORY_SIZE);
        this.armor = copyTags(armor, ARMOR_SIZE);
        this.offhand = copyTags(offhand, OFFHAND_SIZE);
        this.curiosInventory = curiosInventory.copy();
    }

    public String name() {
        return name;
    }

    public static LoadoutPreset capture(ServerPlayer player, String name) {
        return new LoadoutPreset(
                sanitizeName(name),
                serializeStacks(player.getInventory().items),
                serializeStacks(player.getInventory().armor),
                serializeStacks(player.getInventory().offhand),
                captureCurios(player)
        );
    }

    public void apply(ServerPlayer player) {
        player.getInventory().clearContent();
        applyStacks(mainInventory, player.getInventory().items);
        applyStacks(armor, player.getInventory().armor);
        applyStacks(offhand, player.getInventory().offhand);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        player.inventoryMenu.broadcastChanges();
        restoreCurios(player, curiosInventory);
    }

    public ItemStack icon() {
        for (CompoundTag tag : mainInventory) {
            ItemStack stack = deserialize(tag);
            if (!stack.isEmpty()) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("name", name);
        tag.put("mainInventory", toListTag(mainInventory));
        tag.put("armor", toListTag(armor));
        tag.put("offhand", toListTag(offhand));
        tag.put("curiosInventory", curiosInventory.copy());
        return tag;
    }

    public static LoadoutPreset load(CompoundTag tag) {
        return new LoadoutPreset(
                sanitizeName(tag.getString("name")),
                fromListTag(tag.getList("mainInventory", Tag.TAG_COMPOUND)),
                fromListTag(tag.getList("armor", Tag.TAG_COMPOUND)),
                fromListTag(tag.getList("offhand", Tag.TAG_COMPOUND)),
                tag.getList("curiosInventory", Tag.TAG_COMPOUND)
        );
    }

    public static LoadoutPreset fromSerializedSlots(
            String name,
            List<CompoundTag> mainInventory,
            List<CompoundTag> armor,
            List<CompoundTag> offhand,
            ListTag curiosInventory
    ) {
        return new LoadoutPreset(name, mainInventory, armor, offhand, curiosInventory);
    }

    public static LoadoutPreset fromEditor(ServerPlayer player, String name, SimpleContainer container) {
        List<CompoundTag> main = new ArrayList<>(MAIN_INVENTORY_SIZE);
        List<CompoundTag> armor = new ArrayList<>(ARMOR_SIZE);
        List<CompoundTag> offhand = new ArrayList<>(OFFHAND_SIZE);
        for (int index = 0; index < MAIN_INVENTORY_SIZE; index++) {
            main.add(serialize(container.getItem(index)));
        }
        for (int index = 0; index < ARMOR_SIZE; index++) {
            armor.add(serialize(container.getItem(MAIN_INVENTORY_SIZE + index)));
        }
        offhand.add(serialize(container.getItem(MAIN_INVENTORY_SIZE + ARMOR_SIZE)));
        return new LoadoutPreset(name, main, armor, offhand, captureCurios(player));
    }

    public void copyInto(SimpleContainer container) {
        for (int index = 0; index < MAIN_INVENTORY_SIZE; index++) {
            container.setItem(index, deserialize(mainInventory.get(index)));
        }
        for (int index = 0; index < ARMOR_SIZE; index++) {
            container.setItem(MAIN_INVENTORY_SIZE + index, deserialize(armor.get(index)));
        }
        container.setItem(MAIN_INVENTORY_SIZE + ARMOR_SIZE, deserialize(offhand.get(0)));
        container.setChanged();
    }

    public List<CompoundTag> mainInventoryTags() {
        return copyTags(mainInventory, MAIN_INVENTORY_SIZE);
    }

    public List<CompoundTag> armorTags() {
        return copyTags(armor, ARMOR_SIZE);
    }

    public List<CompoundTag> offhandTags() {
        return copyTags(offhand, OFFHAND_SIZE);
    }

    public ListTag curiosInventory() {
        return curiosInventory.copy();
    }

    public static CompoundTag serializeStack(ItemStack stack) {
        return serialize(stack);
    }

    public static ItemStack deserializeStack(CompoundTag tag) {
        return deserialize(tag);
    }

    private static String sanitizeName(String input) {
        String value = input == null ? "" : input.strip();
        if (value.isEmpty()) {
            return "Unnamed Class";
        }
        return value.length() > 32 ? value.substring(0, 32) : value;
    }

    private static List<CompoundTag> serializeStacks(List<ItemStack> stacks) {
        List<CompoundTag> tags = new ArrayList<>(stacks.size());
        for (ItemStack stack : stacks) {
            tags.add(serialize(stack));
        }
        return tags;
    }

    private static CompoundTag serialize(ItemStack stack) {
        return stack.isEmpty() ? new CompoundTag() : stack.save(new CompoundTag());
    }

    private static ItemStack deserialize(CompoundTag tag) {
        return tag.isEmpty() ? ItemStack.EMPTY : ItemStack.of(tag.copy());
    }

    private static void applyStacks(List<CompoundTag> source, List<ItemStack> destination) {
        int limit = Math.min(source.size(), destination.size());
        for (int index = 0; index < limit; index++) {
            destination.set(index, deserialize(source.get(index)));
        }
    }

    private static List<CompoundTag> copyTags(List<CompoundTag> source, int size) {
        List<CompoundTag> result = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            result.add(index < source.size() ? source.get(index).copy() : new CompoundTag());
        }
        return result;
    }

    private static ListTag toListTag(List<CompoundTag> source) {
        ListTag list = new ListTag();
        source.forEach(tag -> list.add(tag.copy()));
        return list;
    }

    private static List<CompoundTag> fromListTag(ListTag source) {
        List<CompoundTag> tags = new ArrayList<>(source.size());
        for (int index = 0; index < source.size(); index++) {
            tags.add(source.getCompound(index).copy());
        }
        return tags;
    }

    private static ListTag captureCurios(ServerPlayer player) {
        return ModList.get().isLoaded("curios") ? CuriosCompat.capture(player) : new ListTag();
    }

    private static void restoreCurios(ServerPlayer player, ListTag savedInventory) {
        if (ModList.get().isLoaded("curios")) {
            CuriosCompat.restore(player, savedInventory);
        }
    }
}
