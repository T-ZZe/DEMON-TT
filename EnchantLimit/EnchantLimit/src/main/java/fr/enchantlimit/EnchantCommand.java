package fr.enchantlimit;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class EnchantCommand implements CommandExecutor {

    private final GuiManager gui;

    public EnchantCommand(GuiManager gui) {
        this.gui = gui;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(Component.text("Commande reservee aux joueurs.", NamedTextColor.RED));
            return true;
        }
        if (!p.hasPermission("enchantlimit.admin")) {
            p.sendMessage(Component.text("Tu n'as pas la permission.", NamedTextColor.RED));
            return true;
        }
        gui.openMain(p);
        return true;
    }
}
