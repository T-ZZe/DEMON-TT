package fr.enchantlimit;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class GuiManager implements Listener {

    public enum Category { ITEMS, ARMOR }

    public static class Holder implements InventoryHolder {
        final Map<Integer, Consumer<InventoryClickEvent>> actions = new HashMap<>();
        Inventory inventory;

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }

    /** Emplacements du centre d'un coffre 54 : 4 lignes de 7. */
    private static final int[] CONTENT = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43};

    private static final String[] ORDER = {
            "HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS",
            "SWORD", "SPEAR", "PICKAXE", "AXE", "SHOVEL", "HOE"};

    private final LimitManager limits;
    private final Map<Tier, List<Material>> armorByTier = new EnumMap<>(Tier.class);
    private final Map<Tier, List<Material>> itemsByTier = new EnumMap<>(Tier.class);
    private final List<Enchantment> allEnchants = new ArrayList<>();

    public GuiManager(LimitManager limits) {
        this.limits = limits;
        for (Enchantment e : RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT)) {
            allEnchants.add(e);
        }
        allEnchants.sort(Comparator.comparing(e -> e.getKey().getKey()));

        for (Material m : Material.values()) {
            if (m.isLegacy() || !m.isItem() || m.isAir() || m == Material.ENCHANTED_BOOK) continue;
            if (!isEnchantable(m)) continue;
            var target = Tier.isArmor(m) ? armorByTier : itemsByTier;
            target.computeIfAbsent(Tier.of(m), k -> new ArrayList<>()).add(m);
        }
        Comparator<Material> cmp = Comparator.<Material>comparingInt(GuiManager::rank)
                .thenComparingInt(Material::ordinal);
        armorByTier.values().forEach(l -> l.sort(cmp));
        itemsByTier.values().forEach(l -> l.sort(cmp));
    }

    private static int rank(Material m) {
        String n = m.name();
        for (int i = 0; i < ORDER.length; i++) {
            if (n.endsWith("_" + ORDER[i])) return i;
        }
        return 100;
    }

    private boolean isEnchantable(Material m) {
        ItemStack s = new ItemStack(m);
        for (Enchantment e : allEnchants) {
            if (e.equals(Enchantment.BINDING_CURSE) || e.equals(Enchantment.VANISHING_CURSE)) continue;
            if (e.canEnchantItem(s)) return true;
        }
        return false;
    }

    private List<Enchantment> enchantsFor(Material m) {
        ItemStack s = new ItemStack(m);
        List<Enchantment> list = new ArrayList<>();
        for (Enchantment e : allEnchants) {
            if (e.canEnchantItem(s)) list.add(e);
        }
        return list;
    }

    // ------------------------------------------------------------------ helpers

    private static Component line(String text, TextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    private static Component bold(String text, TextColor color) {
        return Component.text(text, color, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false);
    }

    private static Component title(String text, TextColor color) {
        return Component.text(text, color, TextDecoration.BOLD);
    }

    private ItemStack icon(Material mat, Component name, List<Component> lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(name);
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.values());
        it.setItemMeta(meta);
        return it;
    }

    private ItemStack pane(Material mat) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(Component.empty());
        meta.setHideTooltip(true);
        it.setItemMeta(meta);
        return it;
    }

    private void fill(Inventory inv, Material paneMat) {
        ItemStack p = pane(paneMat);
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, p);
        }
    }

    private Holder newHolder() {
        return new Holder();
    }

    private Inventory create(Holder h, int size, Component title) {
        Inventory inv = Bukkit.createInventory(h, size, title);
        h.inventory = inv;
        return inv;
    }

    private void put(Inventory inv, Holder h, int slot, ItemStack it, Consumer<InventoryClickEvent> action) {
        inv.setItem(slot, it);
        if (action != null) h.actions.put(slot, action);
    }

    private ItemStack backButton() {
        return icon(Material.ARROW, bold("◀ Retour", NamedTextColor.RED), List.of());
    }

    private int sizeFor(int n) {
        return n <= 9 ? 27 : 54;
    }

    /** Une ligne centree pour 9 elements ou moins, sinon la grille du centre. */
    private int[] slotsFor(int n) {
        if (n <= 9) {
            int start = 9 + (9 - n) / 2;
            int[] s = new int[n];
            for (int i = 0; i < n; i++) s[i] = start + i;
            return s;
        }
        int[] s = new int[Math.min(n, CONTENT.length)];
        System.arraycopy(CONTENT, 0, s, 0, s.length);
        return s;
    }

    private String plural(int n, String word) {
        return n + " " + word + (n > 1 ? "s" : "");
    }

    // ------------------------------------------------------------------ menu principal

    public void openMain(Player p) {
        Holder h = newHolder();
        Inventory inv = create(h, 27, title("✦ Enchantements ✦", NamedTextColor.LIGHT_PURPLE));

        put(inv, h, 11, icon(Material.NETHERITE_PICKAXE, bold("Items", NamedTextColor.AQUA), List.of(
                line("Outils, armes et objets spéciaux", NamedTextColor.GRAY),
                line("Limite les niveaux d'enchantement.", NamedTextColor.GRAY),
                Component.empty(),
                line("▶ Clique pour ouvrir", NamedTextColor.YELLOW))),
                e -> openTiers(p, Category.ITEMS));

        put(inv, h, 13, icon(Material.NETHERITE_CHESTPLATE, bold("Armures", NamedTextColor.LIGHT_PURPLE), List.of(
                line("Cuir, fer, diamant, netherite...", NamedTextColor.GRAY),
                line("Limite les niveaux d'enchantement.", NamedTextColor.GRAY),
                Component.empty(),
                line("▶ Clique pour ouvrir", NamedTextColor.YELLOW))),
                e -> openTiers(p, Category.ARMOR));

        put(inv, h, 15, icon(Material.ANVIL, bold("Limites de pièces", NamedTextColor.GOLD), List.of(
                line("Nombre de pièces d'une même armure", NamedTextColor.GRAY),
                line("portées en même temps.", NamedTextColor.GRAY),
                line("Ex : 2 pièces en netherite max.", NamedTextColor.DARK_GRAY),
                Component.empty(),
                line("▶ Clique pour ouvrir", NamedTextColor.YELLOW))),
                e -> openQuotas(p));

        fill(inv, Material.PURPLE_STAINED_GLASS_PANE);
        p.openInventory(inv);
    }

    // ------------------------------------------------------------------ familles

    private Map<Tier, List<Material>> mapOf(Category c) {
        return c == Category.ARMOR ? armorByTier : itemsByTier;
    }

    private int totalRestrictions(List<Material> list) {
        int n = 0;
        for (Material m : list) n += limits.countRestrictions(m);
        return n;
    }

    public void openTiers(Player p, Category cat) {
        Map<Tier, List<Material>> map = mapOf(cat);
        List<Tier> tiers = new ArrayList<>(map.keySet());
        boolean armor = cat == Category.ARMOR;

        Holder h = newHolder();
        Inventory inv = create(h, sizeFor(tiers.size()), armor
                ? title("✦ Armures ✦", NamedTextColor.LIGHT_PURPLE)
                : title("✦ Items ✦", NamedTextColor.AQUA));

        int[] slots = slotsFor(tiers.size());
        for (int i = 0; i < slots.length; i++) {
            Tier t = tiers.get(i);
            List<Material> list = map.get(t);
            int r = totalRestrictions(list);

            List<Component> lore = new ArrayList<>();
            lore.add(line(plural(list.size(), "équipement"), NamedTextColor.GRAY));
            lore.add(line(r == 0 ? "Aucune restriction" : plural(r, "restriction"),
                    r == 0 ? NamedTextColor.GREEN : NamedTextColor.RED));
            if (armor && Tier.ARMOR.contains(t)) {
                int pl = limits.getPieceLimit(t);
                lore.add(line(pl >= LimitManager.NO_PIECE_LIMIT
                                ? "Pièces portées : illimité"
                                : "Pièces portées : " + pl + " max",
                        pl >= LimitManager.NO_PIECE_LIMIT ? NamedTextColor.GREEN : NamedTextColor.GOLD));
            }
            lore.add(Component.empty());
            lore.add(line("▶ Clique pour ouvrir", NamedTextColor.YELLOW));

            put(inv, h, slots[i], icon(t.icon, bold(t.label, t.color), lore), e -> openPieces(p, cat, t));
        }
        put(inv, h, inv.getSize() - 5, backButton(), e -> openMain(p));
        fill(inv, armor ? Material.LIGHT_BLUE_STAINED_GLASS_PANE : Material.ORANGE_STAINED_GLASS_PANE);
        p.openInventory(inv);
    }

    // ------------------------------------------------------------------ pieces d'une famille

    public void openPieces(Player p, Category cat, Tier tier) {
        List<Material> list = mapOf(cat).getOrDefault(tier, List.of());
        Holder h = newHolder();
        Inventory inv = create(h, sizeFor(list.size()),
                title("✦ " + tier.label + " ✦", tier.color));

        int[] slots = slotsFor(list.size());
        for (int i = 0; i < slots.length; i++) {
            Material m = list.get(i);
            int n = limits.countRestrictions(m);
            ItemStack it = icon(m, Component.translatable(m.translationKey(), tier.color)
                            .decoration(TextDecoration.ITALIC, false),
                    List.of(
                            line(n == 0 ? "Aucune restriction" : plural(n, "restriction"),
                                    n == 0 ? NamedTextColor.GREEN : NamedTextColor.RED),
                            Component.empty(),
                            line("▶ Clique pour modifier", NamedTextColor.YELLOW)));
            put(inv, h, slots[i], it, e -> openEnchants(p, cat, tier, m));
        }
        put(inv, h, inv.getSize() - 5, backButton(), e -> openTiers(p, cat));
        fill(inv, cat == Category.ARMOR ? Material.LIGHT_BLUE_STAINED_GLASS_PANE : Material.ORANGE_STAINED_GLASS_PANE);
        p.openInventory(inv);
    }

    // ------------------------------------------------------------------ enchantements

    public void openEnchants(Player p, Category cat, Tier tier, Material mat) {
        Holder h = newHolder();
        Inventory inv = create(h, 54, Component.translatable(mat.translationKey(), tier.color)
                .decoration(TextDecoration.BOLD, true));

        int n = limits.countRestrictions(mat);
        inv.setItem(4, icon(mat, Component.translatable(mat.translationKey(), tier.color)
                        .decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true),
                List.of(line(n == 0 ? "Aucune restriction" : plural(n, "restriction"),
                        n == 0 ? NamedTextColor.GREEN : NamedTextColor.RED))));

        List<Enchantment> list = enchantsFor(mat);
        for (int i = 0; i < list.size() && i < CONTENT.length; i++) {
            Enchantment ench = list.get(i);
            int slot = CONTENT[i];
            put(inv, h, slot, book(mat, ench), e -> {
                int max = ench.getMaxLevel();
                int cur = limits.getEffective(mat, ench);
                int next;
                if (e.getClick().isShiftClick()) next = max;
                else if (e.getClick().isRightClick()) next = cur + 1 > max ? 0 : cur + 1;
                else next = cur - 1 < 0 ? max : cur - 1; // 4 -> 3 -> 2 -> 1 -> 0 -> 4
                limits.setLimit(mat, ench, next);
                inv.setItem(slot, book(mat, ench));
            });
        }

        put(inv, h, 45, backButton(), e -> openPieces(p, cat, tier));
        put(inv, h, 53, icon(Material.TNT, bold("Tout réinitialiser", NamedTextColor.RED), List.of(
                line("Remet les valeurs vanilla", NamedTextColor.GRAY),
                line("pour cet équipement.", NamedTextColor.GRAY))),
                e -> {
                    limits.reset(mat);
                    openEnchants(p, cat, tier, mat);
                });

        fill(inv, Material.BLACK_STAINED_GLASS_PANE);
        p.openInventory(inv);
    }

    private ItemStack book(Material mat, Enchantment e) {
        int max = e.getMaxLevel();
        int cur = limits.getEffective(mat, e);
        ItemStack it = new ItemStack(cur == 0 ? Material.BARRIER : Material.ENCHANTED_BOOK);
        it.setAmount(Math.max(1, cur));
        ItemMeta meta = it.getItemMeta();

        Component name = cur == 0
                ? Component.translatable(e.translationKey(), NamedTextColor.RED)
                : e.displayName(cur);
        meta.displayName(name.decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(line("Maximum autorisé : " + (cur == 0 ? "INTERDIT" : cur),
                cur == 0 ? NamedTextColor.RED : (cur < max ? NamedTextColor.GOLD : NamedTextColor.GREEN)));
        lore.add(line("Maximum vanilla : " + max, NamedTextColor.DARK_GRAY));
        lore.add(Component.empty());
        lore.add(line("Clic gauche : baisser (boucle)", NamedTextColor.GRAY));
        lore.add(line("Clic droit : monter", NamedTextColor.GRAY));
        lore.add(line("Shift + clic : vanilla", NamedTextColor.GRAY));
        meta.lore(lore);
        if (cur < max) meta.setEnchantmentGlintOverride(true);
        meta.addItemFlags(ItemFlag.values());
        it.setItemMeta(meta);
        return it;
    }

    // ------------------------------------------------------------------ limites de pieces

    public void openQuotas(Player p) {
        List<Tier> tiers = new ArrayList<>();
        for (Tier t : armorByTier.keySet()) {
            if (Tier.ARMOR.contains(t)) tiers.add(t);
        }

        Holder h = newHolder();
        Inventory inv = create(h, sizeFor(tiers.size()), title("✦ Limites de pièces ✦", NamedTextColor.GOLD));

        int[] slots = slotsFor(tiers.size());
        for (int i = 0; i < slots.length; i++) {
            Tier t = tiers.get(i);
            int slot = slots[i];
            put(inv, h, slot, quotaItem(t), e -> {
                int cur = limits.getPieceLimit(t);
                int next;
                if (e.getClick().isShiftClick()) next = LimitManager.NO_PIECE_LIMIT;
                else if (e.getClick().isRightClick()) next = cur + 1 > LimitManager.NO_PIECE_LIMIT ? 0 : cur + 1;
                else next = cur - 1 < 0 ? LimitManager.NO_PIECE_LIMIT : cur - 1; // 4 -> 3 -> 2 -> 1 -> 0 -> 4
                limits.setPieceLimit(t, next);
                inv.setItem(slot, quotaItem(t));
            });
        }
        put(inv, h, inv.getSize() - 5, backButton(), e -> openMain(p));
        fill(inv, Material.RED_STAINED_GLASS_PANE);
        p.openInventory(inv);
    }

    private ItemStack quotaItem(Tier t) {
        int cur = limits.getPieceLimit(t);
        boolean unlimited = cur >= LimitManager.NO_PIECE_LIMIT;

        List<Component> lore = new ArrayList<>();
        if (unlimited) lore.add(line("Pièces portées : illimité (4)", NamedTextColor.GREEN));
        else if (cur == 0) lore.add(line("Pièces portées : INTERDIT", NamedTextColor.RED));
        else lore.add(line("Pièces portées : " + cur + " max", NamedTextColor.GOLD));
        lore.add(Component.empty());
        lore.add(line("Clic gauche : baisser (boucle)", NamedTextColor.GRAY));
        lore.add(line("Clic droit : monter", NamedTextColor.GRAY));
        lore.add(line("Shift + clic : illimité", NamedTextColor.GRAY));

        ItemStack it = icon(cur == 0 ? Material.BARRIER : t.icon, bold(t.label, t.color), lore);
        it.setAmount(Math.max(1, cur));
        if (!unlimited) {
            ItemMeta meta = it.getItemMeta();
            meta.setEnchantmentGlintOverride(true);
            it.setItemMeta(meta);
        }
        return it;
    }

    // ------------------------------------------------------------------ events

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof Holder) e.setCancelled(true);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof Holder h)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() != top) return;
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!p.hasPermission("enchantlimit.admin")) {
            p.closeInventory();
            return;
        }
        Consumer<InventoryClickEvent> action = h.actions.get(e.getSlot());
        if (action != null) {
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
            action.accept(e);
        }
    }
}
