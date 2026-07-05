package dev.zm.disguise.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public interface Menu {
    Inventory getInventory();

    void open(Player player);
}
