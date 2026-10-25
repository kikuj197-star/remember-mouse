package com.remembermouse;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Identifies server-side SkyBlock GUI controls without treating stored items as controls. */
public final class SkyBlockUiDetector {
    private static final Pattern NUMBERED_TRAP_TITLE = Pattern.compile("^trap\\s*#?\\s*\\d+.*$");
    private static final List<String> PAGE_OR_NAVIGATION_NAMES = List.of(
        "next page", "previous page", "prev page", "first page", "last page",
        "go to page", "jump to page", "go back", "back to", "close menu",
        "close", "return to"
    );
    private static final List<String> NPC_SHOP_TITLES = List.of(
        "shop", "merchant", "trades", "商店", "交易"
    );
    private static final List<String> NPC_ACTIONS = List.of(
        "click to buy", "click to purchase", "click to sell", "click to trade",
        "buyback", "sold items",
        "点击购买", "点击出售", "点击交易", "回购"
    );
    private static final List<String> NPC_SELL_HOPPER_MARKERS = List.of(
        "sell item", "sell items", "sell instantly", "sell your items", "npc sell",
        "click to sell", "shift-click to sell",
        "出售物品", "出售道具", "点击出售"
    );
    private static final List<String> UI_CLICK_ACTIONS = List.of(
        "click to open", "click to view", "click to select", "click to browse",
        "click to go", "click to return", "click to close", "click to manage",
        "click to switch", "click to confirm", "click to continue", "click to claim",
        "click to collect", "click to equip", "click to warp", "click to travel"
    );
    private static final List<String> ITEM_STORAGE_TITLES = List.of(
        "backpack", "ender chest", "personal vault", "accessory bag",
        "potion bag", "fishing bag", "quiver"
    );
    private static final List<String> MIDDLE_CLICK_INCOMPATIBLE_TITLES = List.of(
        "the hex", "anvil", "reforge", "enchant item", "drill anvil",
        "runic pedestal", "rune removal", "salvage item", "upgrade item",
        "item upgrades", "convert to dungeon item", "offer pets", "exp sharing",
        "attribute fusion", "item fusion", "loadout", "sack",
        "heart of the mountain slot", "heart of the forest slot", "hotm slot", "hotf slot"
    );

    private SkyBlockUiDetector() {}

    public static boolean isUiSlot(AbstractContainerScreen<?> screen, Slot slot) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || slot == null || !slot.hasItem() || !isInSkyBlock(minecraft)) {
            return false;
        }

        ItemStack stack = slot.getItem();
        String screenTitle = normalize(screen.getTitle());

        // Storage assigns distinct server actions to left and right clicks and
        // does not accept CLONE/middle clicks for opening pages or backpacks.
        if (screenTitle.equals("storage") || screenTitle.startsWith("storage ")) return false;

        // Refilling these containers moves real fuel/bait. Their left click is
        // not a menu action and must retain its original PICKUP semantics.
        if (isFuelOrBaitTransferScreen(screenTitle)) return false;

        // Transaction/upgrade screens use the original left-click type as part
        // of their protocol and do not accept CLONE clicks.
        if (containsAny(screenTitle, MIDDLE_CLICK_INCOMPATIBLE_TITLES)) return false;

        // NPC shops intentionally accept middle clicks for both shop offers
        // and the player's inventory. This prevents a normal PICKUP click from
        // briefly carrying an item while buying or selling. A named/lored sell
        // hopper also identifies shops whose title is just the NPC's name.
        if (isNpcShop(screen)) return true;

        // The SkyBlock Menu nether star is a server-owned UI control despite
        // occupying a player inventory slot.
        if (isSkyBlockMenuEntry(stack)) return true;

        // Outside an identified NPC shop, player inventory/hotbar slots are
        // real items and must retain their original click behavior.
        if (slot.container == minecraft.player.getInventory()) return false;

        return hasDirectUiMarker(stack, screenTitle);
    }

    private static boolean isInSkyBlock(Minecraft minecraft) {
        if (minecraft.level == null) return false;
        Objective sidebar = minecraft.level.getScoreboard().getDisplayObjective(DisplaySlot.SIDEBAR);
        return sidebar != null && normalize(sidebar.getDisplayName()).contains("skyblock");
    }

    private static boolean isFuelOrBaitTransferScreen(String title) {
        return title.contains("inferno minion")
            || title.contains("vermin trap")
            || title.contains("pest trap")
            || NUMBERED_TRAP_TITLE.matcher(title).matches();
    }

    private static boolean isNpcShop(AbstractContainerScreen<?> screen) {
        if (containsAny(normalize(screen.getTitle()), NPC_SHOP_TITLES)) return true;

        Minecraft minecraft = Minecraft.getInstance();
        for (Slot candidate : screen.getMenu().slots) {
            if (minecraft.player != null && candidate.container == minecraft.player.getInventory()) continue;
            if (!candidate.hasItem()) continue;
            ItemStack candidateStack = candidate.getItem();
            if (isNpcSellHopper(candidateStack) || hasNpcAction(candidateStack)) return true;
        }
        return false;
    }

    private static boolean isNpcSellHopper(ItemStack stack) {
        if (!stack.is(Items.HOPPER)) return false;
        if (containsAny(normalize(stack.getHoverName()), NPC_SELL_HOPPER_MARKERS)) return true;

        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return false;
        for (Component component : lore.lines()) {
            if (containsAny(normalize(component), NPC_SELL_HOPPER_MARKERS)) return true;
        }
        return false;
    }

    private static boolean hasDirectUiMarker(ItemStack stack, String screenTitle) {
        String name = normalize(stack.getHoverName());
        if (isNavigationName(name)) return true;

        String itemName = stack.getItem().toString().toLowerCase(Locale.ROOT);
        Component customName = stack.get(DataComponents.CUSTOM_NAME);
        if (customName != null && normalize(customName).isBlank()
            && itemName.contains("stained_glass_pane")) return true;

        // In backpacks and Ender Chest pages, an item with a real SkyBlock ID
        // is stored content. Keep its original click even if its ability lore
        // happens to mention clicking.
        if (containsAny(screenTitle, ITEM_STORAGE_TITLES) && !skyBlockId(stack).isBlank()) return false;

        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore != null) {
            for (Component component : lore.lines()) {
                String line = normalize(component);
                if (isUiClickInstruction(line) || containsAny(line, NPC_ACTIONS)) return true;
            }
        }
        return false;
    }

    private static boolean hasNpcAction(ItemStack stack) {
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return false;
        for (Component component : lore.lines()) {
            if (containsAny(normalize(component), NPC_ACTIONS)) return true;
        }
        return false;
    }

    private static boolean isUiClickInstruction(String line) {
        // Only left clicks are rewritten, so a right-click-only instruction is
        // not evidence that the left click is a GUI action.
        if (line.contains("right-click") || line.contains("right click") || line.startsWith("右键")) {
            return false;
        }
        return containsAny(line, UI_CLICK_ACTIONS)
            || (line.startsWith("click to ") && !line.contains("shift-click"))
            || line.startsWith("点击")
            || line.startsWith("左键点击");
    }

    private static boolean isNavigationName(String name) {
        return name.equals("back") || containsAny(name, PAGE_OR_NAVIGATION_NAMES);
    }

    private static boolean isSkyBlockMenuEntry(ItemStack stack) {
        if (skyBlockId(stack).equalsIgnoreCase("SKYBLOCK_MENU")) return true;

        Identifier itemModel = stack.get(DataComponents.ITEM_MODEL);
        if (itemModel != null && itemModel.toString().equalsIgnoreCase("skyblock:skyblock_menu")) return true;

        String name = normalize(stack.getHoverName());
        String itemName = stack.getItem().toString().toLowerCase(Locale.ROOT);
        return itemName.contains("nether_star")
            && (name.equals("skyblock menu") || name.startsWith("skyblock menu ("))
            && hasLore(stack, "click to open");
    }

    private static String skyBlockId(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return "";

        CompoundTag root = customData.copyTag();
        String id = root.getStringOr("id", "");
        if (id.isBlank() && root.contains("ExtraAttributes")) {
            id = root.getCompoundOrEmpty("ExtraAttributes").getStringOr("id", "");
        }
        return id.trim();
    }

    private static boolean hasLore(ItemStack stack, String marker) {
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return false;
        for (Component component : lore.lines()) {
            if (normalize(component).contains(marker)) return true;
        }
        return false;
    }

    private static String normalize(Component component) {
        return component.getString()
            .toLowerCase(Locale.ROOT)
            .replace('\u00a0', ' ')
            .replaceAll("\\s+", " ")
            .trim();
    }

    private static boolean containsAny(String text, List<String> markers) {
        for (String marker : markers) {
            if (text.contains(marker)) return true;
        }
        return false;
    }
}
