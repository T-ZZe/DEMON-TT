package fr.enchantlimit;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;

import java.util.EnumSet;
import java.util.Set;

/** Famille d'equipement (materiau). L'ordre de l'enum = ordre d'affichage. */
public enum Tier {
    LEATHER("Cuir", Material.LEATHER, TextColor.color(0xA0652D), "LEATHER_"),
    CHAINMAIL("Mailles", Material.CHAINMAIL_CHESTPLATE, NamedTextColor.GRAY, "CHAINMAIL_"),
    WOOD("Bois", Material.OAK_PLANKS, TextColor.color(0xBA8C50), "WOODEN_"),
    STONE("Pierre", Material.COBBLESTONE, NamedTextColor.GRAY, "STONE_"),
    COPPER("Cuivre", Material.COPPER_INGOT, TextColor.color(0xE77C56), "COPPER_"),
    IRON("Fer", Material.IRON_INGOT, NamedTextColor.WHITE, "IRON_"),
    GOLD("Or", Material.GOLD_INGOT, NamedTextColor.YELLOW, "GOLDEN_"),
    DIAMOND("Diamant", Material.DIAMOND, NamedTextColor.AQUA, "DIAMOND_"),
    NETHERITE("Netherite", Material.NETHERITE_INGOT, TextColor.color(0x9C8FB5), "NETHERITE_"),
    TURTLE("Tortue", Material.TURTLE_SCUTE, NamedTextColor.GREEN, "TURTLE_"),
    SPECIAL("Spécial", Material.NETHER_STAR, NamedTextColor.LIGHT_PURPLE, "");

    /** Familles pour lesquelles on peut limiter le nombre de pieces portees. */
    public static final Set<Tier> ARMOR = EnumSet.of(LEATHER, CHAINMAIL, COPPER, IRON, GOLD, DIAMOND, NETHERITE, TURTLE);

    public final String label;
    public final Material icon;
    public final TextColor color;
    private final String prefix;

    Tier(String label, Material icon, TextColor color, String prefix) {
        this.label = label;
        this.icon = icon;
        this.color = color;
        this.prefix = prefix;
    }

    public static Tier of(Material m) {
        String n = m.name();
        for (Tier t : values()) {
            if (!t.prefix.isEmpty() && n.startsWith(t.prefix)) return t;
        }
        return SPECIAL;
    }

    public static boolean isArmor(Material m) {
        String n = m.name();
        return n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE")
                || n.endsWith("_LEGGINGS") || n.endsWith("_BOOTS");
    }
}
