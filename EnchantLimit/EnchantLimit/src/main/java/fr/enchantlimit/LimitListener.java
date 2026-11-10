package fr.enchantlimit;

import io.papermc.paper.event.player.PlayerArmorChangeEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class LimitListener implements Listener {

    private static final List<EquipmentSlot> SLOTS =
            List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    private final EnchantLimitPlugin plugin;
    private final LimitManager limits;

    public LimitListener(EnchantLimitPlugin plugin, LimitManager limits) {
        this.plugin = plugin;
        this.limits = limits;
    }

    // ------------------------------------------------------------ niveaux

    @EventHandler
    public void onEnchant(EnchantItemEvent e) {
        Iterator<Map.Entry<Enchantment, Integer>> it = e.getEnchantsToAdd().entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            Integer lim = limits.getLimit(e.getItem().getType(), entry.getKey());
            if (lim != null && entry.getValue() > lim) {
                if (lim <= 0) it.remove();
                else entry.setValue(lim);
            }
        }
    }

    @EventHandler
    public void onAnvil(PrepareAnvilEvent e) {
        ItemStack result = e.getResult();
        if (result != null && limits.clamp(result)) e.setResult(result);
    }

    /** Amelioration diamant -> netherite : on redescend aux limites de la version netherite. */
    @EventHandler
    public void onSmithing(PrepareSmithingEvent e) {
        ItemStack result = e.getResult();
        if (result != null && limits.clamp(result)) e.setResult(result);
    }

    // ------------------------------------------------------------ pieces portees

    @EventHandler
    public void onArmorChange(PlayerArmorChangeEvent e) {
        Player p = e.getPlayer();
        EquipmentSlot slot = switch (e.getSlot()) {
            case HEAD -> EquipmentSlot.HEAD;
            case CHEST -> EquipmentSlot.CHEST;
            case LEGS -> EquipmentSlot.LEGS;
            case FEET -> EquipmentSlot.FEET;
            default -> null;
        };
        Bukkit.getScheduler().runTask(plugin, () -> enforce(p, slot));
    }

    /** Retire les pieces en trop (preferred = piece qui vient d'etre equipee). */
    public void enforce(Player p, EquipmentSlot preferred) {
        PlayerInventory inv = p.getInventory();
        for (Tier tier : Tier.ARMOR) {
            int limit = limits.getPieceLimit(tier);
            if (limit >= LimitManager.NO_PIECE_LIMIT) continue;

            List<EquipmentSlot> worn = new ArrayList<>();
            for (EquipmentSlot s : SLOTS) {
                ItemStack it = inv.getItem(s);
                if (it != null && !it.getType().isAir()
                        && Tier.isArmor(it.getType()) && Tier.of(it.getType()) == tier) {
                    worn.add(s);
                }
            }
            int excess = worn.size() - limit;
            if (excess <= 0) continue;

            worn.sort(Comparator.comparingInt(s -> s == preferred ? 0 : 1 + (3 - SLOTS.indexOf(s))));
            for (int i = 0; i < excess; i++) {
                EquipmentSlot s = worn.get(i);
                ItemStack it = inv.getItem(s);
                inv.setItem(s, null);
                inv.addItem(it).values().forEach(
                        left -> p.getWorld().dropItemNaturally(p.getLocation(), left));
            }
            p.sendMessage(Component.text("Tu ne peux porter que " + limit + " pièce(s) en "
                    + tier.label + " à la fois.", NamedTextColor.RED));
        }
    }
}
