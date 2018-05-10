package com.songoda.epicspawners.listeners;

import com.songoda.arconix.plugin.Arconix;
import com.songoda.epicspawners.EpicSpawners;
import com.songoda.epicspawners.spawners.object.Spawner;
import com.songoda.epicspawners.spawners.object.SpawnerData;
import com.songoda.epicspawners.spawners.object.SpawnerStack;
import com.songoda.epicspawners.utils.Debugger;
import com.songoda.epicspawners.utils.Methods;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by songoda on 2/25/2017.
 */
public class EntityListeners implements Listener {

    private final EpicSpawners instance;

    public EntityListeners(EpicSpawners instance) {
        this.instance = instance;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlow(EntityExplodeEvent e) {
        try {
            if (!e.isCancelled()) {
                List<Block> destroyed = e.blockList();
                Iterator<Block> it = destroyed.iterator();
                List<Block> toCancel = new ArrayList<>();
                while (it.hasNext()) {
                    Block b = it.next();
                    if (b.getType() != Material.MOB_SPAWNER) continue;

                    Location spawnLocation = b.getLocation();

                    if (EpicSpawners.getInstance().getConfig().getBoolean("Main.Prevent Spawners From Exploding"))
                        toCancel.add(b);
                    else if (e.getEntity() instanceof Creeper && EpicSpawners.getInstance().getConfig().getBoolean("Spawner Drops.Drop On Creeper Explosion")
                            || e.getEntity() instanceof TNTPrimed && EpicSpawners.getInstance().getConfig().getBoolean("Spawner Drops.Drop On TNT Explosion")) {

                        Spawner spawner = instance.getSpawnerManager().getSpawnerFromWorld(b.getLocation());
                        boolean canDrop = true;

                        String chance = "";
                        if (e.getEntity() instanceof Creeper && EpicSpawners.getInstance().getConfig().getBoolean("Spawner Drops.Drop On Creeper Explosion"))
                            chance = EpicSpawners.getInstance().getConfig().getString("Spawner Drops.Chance On TNT Explosion");
                        else if (e.getEntity() instanceof TNTPrimed && EpicSpawners.getInstance().getConfig().getBoolean("Spawner Drops.Drop On TNT Explosion"))
                            chance = EpicSpawners.getInstance().getConfig().getString("Spawner Drops.Chance On Creeper Explosion");
                        int ch = Integer.parseInt(chance.replace("%", ""));
                        double rand = Math.random() * 100;
                        if (rand - ch < 0 || ch == 100) {
                            for (SpawnerStack stack : spawner.getSpawnerStacks()) {
                                ItemStack item = instance.getApi().newSpawnerItem(stack.getSpawnerData().getName(), stack.getStackSize(), 1);
                                spawnLocation.getWorld().dropItemNaturally(spawnLocation.clone().add(.5, 0, .5), item);
                            }
                            instance.getSpawnerManager().removeSpawnerFromWorld(spawnLocation);
                            instance.getHologramHandler().despawn(spawnLocation.getBlock());
                            instance.getApi().removeDisplayItem(spawner);
                        }
                    }
                    EpicSpawners.getInstance().getHologramHandler().processChange(b);
                    Location nloc = spawnLocation.clone();
                    nloc.add(.5, -.4, .5);
                    List<Entity> near = (List<Entity>) nloc.getWorld().getNearbyEntities(nloc, 8, 8, 8);
                    for (Entity ee : near) {
                        if (ee.getLocation().getX() == nloc.getX() && ee.getLocation().getY() == nloc.getY() && ee.getLocation().getZ() == nloc.getZ()) {
                            ee.remove();
                        }
                    }

                }

                for (Block block : toCancel) {
                    e.blockList().remove(block);
                }

            }
        } catch (Exception ex) {
            Debugger.runReport(ex);
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        try {
            EpicSpawners instance = EpicSpawners.getInstance();
            if (event.getEntity().getKiller() == null && !(event.getEntity().getKiller() instanceof Player)) return;
            Player player = event.getEntity().getKiller();
            if (event.getEntity().hasMetadata("ES")) {
                SpawnerData spawnerData = instance.getSpawnerManager().getSpawnerType(event.getEntity().getMetadata("ES").get(0).asString());
                for (ItemStack itemStack : spawnerData.getItemDrops()) {
                    Location location = event.getEntity().getLocation();
                    location.getWorld().dropItemNaturally(location, itemStack);
                }

            }
            if (!instance.getSpawnManager().isNaturalSpawn(event.getEntity().getUniqueId()) && !instance.getConfig().getBoolean("Spawner Drops.Count Unnatural Kills Towards Spawner Drop"))
                return;

            int amt = instance.getPlayerActionManager().getPlayerAction(player).addKilledEntity(event.getEntityType());
            int goal = instance.getConfig().getInt("Spawner Drops.Kills Needed for Drop");

            int customGoal = instance.getSpawnerManager().getSpawnerType(Methods.getType(event.getEntityType())).getKillGoal();
            if (customGoal != 0) goal = customGoal;

            String type = Methods.getType(event.getEntity().getType());

            if (instance.getConfig().getInt("Spawner Drops.Alert Every X Before Drop") != 0
                    && amt % instance.getConfig().getInt("Spawner Drops.Alert Every X Before Drop") == 0
                    && amt != goal) {
                Arconix.pl().getApi().packetLibrary.getActionBarManager().sendActionBar(player, instance.getLocale().getMessage("event.goal.alert", goal - amt, type));
            }

            if (amt % goal == 0) {
                ItemStack item = instance.getApi().newSpawnerItem(type, 1, 1);
                event.getEntity().getLocation().getWorld().dropItemNaturally(event.getEntity().getLocation(), item);
                Arconix.pl().getApi().packetLibrary.getActionBarManager().sendActionBar(player, instance.getLocale().getMessage("event.goal.reached", type));
            }

        } catch (Exception ex) {
            Debugger.runReport(ex);
        }
    }
}
