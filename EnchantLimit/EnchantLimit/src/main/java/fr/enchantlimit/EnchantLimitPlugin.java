package fr.enchantlimit;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class EnchantLimitPlugin extends JavaPlugin {

    private LimitManager limits;
    private GuiManager gui;

    @Override
    public void onEnable() {
        limits = new LimitManager(this);
        limits.load();
        gui = new GuiManager(limits);
        LimitListener listener = new LimitListener(this, limits);

        Bukkit.getPluginManager().registerEvents(gui, this);
        Bukkit.getPluginManager().registerEvents(listener, this);

        var cmd = getCommand("enchantement");
        if (cmd != null) cmd.setExecutor(new EnchantCommand(gui));

        // Controle regulier : niveaux d'enchantement + nombre de pieces portees
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                Inventory inv = p.getInventory();
                for (int i = 0; i < inv.getSize(); i++) {
                    ItemStack it = inv.getItem(i);
                    if (it != null && limits.clamp(it)) inv.setItem(i, it);
                }
                listener.enforce(p, null);
            }
        }, 40L, 40L);
    }

    @Override
    public void onDisable() {
        if (limits != null) limits.save();
    }
}
