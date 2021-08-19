package com.reflectednetwork.rfnetapi.cclibcompat;

import com.reflectednetwork.rfnetapi.WorldPluginInterface;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import top.cavecraft.cclib.ICCLib.IGameUtils;
import top.cavecraft.cclib.ICCLib.ITickReq;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static com.reflectednetwork.rfnetapi.GetReflectedAPIKt.getReflectedAPI;
import static org.bukkit.Bukkit.getServer;

/**
 * This class exists only for backwards compatibility.
 */
@Deprecated
@SuppressWarnings("ALL")
public class GameUtils implements IGameUtils, Listener {
    private final Component returnToLobbyName = Component.text("Return to Lobby").color(NamedTextColor.RED);

    int ticksToStart = 0;
    int ticksPassed = 0;
    int maxPlayers = 0;
    final int teleportToArenaTicks = 60;
    List<ITickReq> tickRequirements;
    Runnable startGame;
    Runnable teleport;
    public boolean started = false;

    private final List<Material> interactBlockList = Arrays.asList(
            Material.ACACIA_DOOR,
            Material.BIRCH_DOOR,
            Material.DARK_OAK_DOOR,
            Material.IRON_DOOR,
            Material.JUNGLE_DOOR,
            Material.SPRUCE_DOOR,
            Material.OAK_TRAPDOOR,
            Material.OAK_DOOR,
            Material.IRON_TRAPDOOR,
            Material.LEVER,
            Material.STONE_BUTTON,
            Material.OAK_BUTTON
    );

    public GameUtils(List<ITickReq> tickRequirements) {
        this.tickRequirements = tickRequirements;
        this.startGame = () -> {};
        this.teleport = () -> {};

        for (ITickReq tickReq : tickRequirements) {
            if (((TickReq)tickReq).getNumberOfTicks() > ticksToStart) {
                ticksToStart = ((TickReq)tickReq).getNumberOfTicks();
            }

            if (((TickReq)tickReq).getNumberOfPlayers() > maxPlayers) {
                maxPlayers = ((TickReq)tickReq).getNumberOfPlayers();
            }
        }

        Bukkit.getServer().getScheduler().runTaskTimer(WorldPluginInterface.INSTANCE.getPlugin(), () -> {

        }, 0, 1);
    }

    public GameUtils(List<ITickReq> tickRequirements, Runnable startGame, Runnable teleport) {
        this.tickRequirements = tickRequirements;
        this.startGame = startGame;
        this.teleport = teleport;

        for (ITickReq tickReq : tickRequirements) {
            if (((TickReq)tickReq).getNumberOfTicks() > ticksToStart) {
                ticksToStart = ((TickReq)tickReq).getNumberOfTicks();
            }

            if (((TickReq)tickReq).getNumberOfPlayers() > maxPlayers) {
                maxPlayers = ((TickReq)tickReq).getNumberOfPlayers();
            }
        }

        Bukkit.getServer().getScheduler().runTaskTimer(WorldPluginInterface.INSTANCE.getPlugin(), this::tick, 0, 1);
    }

    public void tick() {
        if (!started) {
            preStart();
        }
    }

    public void preStart() {
        Collection<? extends Player> players = Bukkit.getServer().getOnlinePlayers();
        if (players.size() > 1) {
            for (ITickReq tickReq : tickRequirements) {
                if (players.size() == ((TickReq)tickReq).getNumberOfPlayers() && ticksPassed < ticksToStart - ((TickReq)tickReq).getNumberOfTicks()) {
                    ticksPassed = ticksToStart - ((TickReq)tickReq).getNumberOfTicks();
                }
                if (ticksPassed == ticksToStart - ((TickReq)tickReq).getNumberOfTicks()) {
                    String message = ChatColor.GOLD + "Game starts regardless of player count in";
                    String time = " " + ChatColor.BLUE + ((ticksToStart - ticksPassed)/20) + "s";
                    Bukkit.getServer().broadcastMessage(message + time);
                    for (Player player : players) {
                        player.playSound(player.getLocation(), Sound.BLOCK_TRIPWIRE_CLICK_ON, 1, 1);
                    }
                }
            }

            if (ticksPassed == ticksToStart - teleportToArenaTicks) {
                teleport.run();
                getReflectedAPI().setAvailable(false);
                clearGameBox(getReflectedAPI().getLoadedMap());
                Bukkit.getServer().getScheduler().runTaskLater(WorldPluginInterface.INSTANCE.getPlugin(), () -> {
                    Bukkit.getServer().broadcastMessage(ChatColor.GREEN + "Go!");
                    for (Player player : players) {
                        player.resetTitle();
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1.5f);
                    }
                    startGame.run();
                    started = true;
                }, teleportToArenaTicks);
            }

            if (ticksPassed >= ticksToStart - teleportToArenaTicks && ticksPassed % 20 == 0 && ticksToStart - ticksPassed != 0) {
                for (Player player : players) {
                    player.sendTitle("" + ChatColor.RED + ((ticksToStart - ticksPassed)/20), "");
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 0.75f);
                }
            }

            ticksPassed++;
        } else if (ticksPassed != 0) {
            ticksPassed = 0;
        }
    }

    @EventHandler
    public void leave(PlayerQuitEvent event) {
        if (getServer().getOnlinePlayers().size() <= 1 && started) {
            getReflectedAPI().restart();
        }
    }

    @EventHandler
    public void hunger(FoodLevelChangeEvent event) {
        event.setFoodLevel(20);
    }

    @EventHandler
    public void playerInteract(PlayerInteractEvent event) {
        if (!started) {
            kickIfLobbyTP(event.getItem(), event.getPlayer());
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void playerJoin(PlayerJoinEvent event) {
        event.setJoinMessage(ChatColor.GOLD + event.getPlayer().getName() + " joined. " + ChatColor.BLUE + "(" + getServer().getOnlinePlayers().size() + "/" + maxPlayers + ")");
        setupFreshPlayer(event.getPlayer());
    }

    @Override
    public void blockInteract(PlayerInteractEvent event) {
        try {
            if (event.getClickedBlock() == null) return;
            if (interactBlockList.contains(event.getClickedBlock().getType())) {
                event.setCancelled(true);
            }
        } catch (Exception e) {
            e.printStackTrace();System.out.println(e.getMessage());
        }
    }

    @Override
    public void awardGems(Player player, GemAmount amount) {

    }

    @Override
    public void setupFreshPlayer(Player player) {
        try {
            player.setGameMode(GameMode.SURVIVAL);
            player.getInventory().clear();
            player.teleport(new Location(getReflectedAPI().getLoadedMap(), 0, 100, 0));

            player.getInventory().setItem(8, createNamedItem(Material.TNT, 1, returnToLobbyName)); //Slot 8 is the far right hotbar slot
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 999999, 1, true));
        } catch (Exception e) {
            e.printStackTrace();System.out.println(e.getMessage());
        }
    }

    @Deprecated
    private ItemStack createNamedItem(Material mat, int count, Component name) {
        ItemStack item = new ItemStack(mat, count);
        item.editMeta(it -> it.displayName(name.decoration(TextDecoration.ITALIC, false)));
        return item;
    }

    @Override
    public void kickIfLobbyTP(ItemStack item, Player player) {
        try {
            if (item != null && player != null && Objects.requireNonNull(item.getItemMeta().displayName()).contains(returnToLobbyName)) {
                getReflectedAPI().sendPlayer(player, "lobby");
            }
        } catch (Exception e) {
            e.printStackTrace();System.out.println(e.getMessage());
        }
    }

    @Override
    public void winnerEffect(Player winner) {
        try {
            winner.setGameMode(GameMode.CREATIVE);
            winner.setFlying(true);
            Bukkit.getServer().getScheduler().runTaskTimer(WorldPluginInterface.INSTANCE.getPlugin(), () -> {
                Firework firework = (Firework) winner.getWorld().spawnEntity(winner.getLocation(), EntityType.FIREWORK);
                FireworkMeta fireworkMeta = firework.getFireworkMeta();
                fireworkMeta.setPower(1);
                if (Math.random() > 0.5) {
                    fireworkMeta.addEffect(FireworkEffect.builder().with(FireworkEffect.Type.STAR).flicker(true).withColor(Color.ORANGE).build());
                } else {
                    fireworkMeta.addEffect(FireworkEffect.builder().with(FireworkEffect.Type.CREEPER).flicker(true).withColor(Color.GREEN).build());
                }
                firework.setFireworkMeta(fireworkMeta);
            }, 0, 10);
        } catch (Exception e) {
            e.printStackTrace();System.out.println(e.getMessage());
        }
    }

    @Override
    public void deathEffect(Player player) {
        try {
            if (player.getGameMode() == GameMode.SURVIVAL) {
                player.teleport(new Location(player.getWorld(), 0, 65, 0));
                player.setGameMode(GameMode.SPECTATOR);
                Bukkit.getServer().broadcastMessage(ChatColor.GOLD + player.getDisplayName() + ChatColor.WHITE + " died. " + ChatColor.GOLD + "FINAL KILL");
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BAT_DEATH, 8, 1);
                player.hidePlayer(player);
            }
        } catch (Exception e) {
            e.printStackTrace();System.out.println(e.getMessage());
        }
    }

    @Override
    public void deathMessage(Player player) {
        try {
            if (player.getGameMode() == GameMode.SURVIVAL) {
                Bukkit.getServer().broadcastMessage(ChatColor.GOLD + player.getDisplayName() + ChatColor.WHITE + " died.");
                player.sendTitle(ChatColor.RED + "You died.", "");
                Bukkit.getServer().getScheduler().runTaskLater(WorldPluginInterface.INSTANCE.getPlugin(), player::resetTitle, 20 * 2);
                player.teleport(new Location(player.getWorld(), 0, 65, 0));
                player.setGameMode(GameMode.SPECTATOR);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BAT_DEATH, 8, 1);
            }
        } catch (Exception e) {
            e.printStackTrace();System.out.println(e.getMessage());
        }
    }

    @Override
    public void clearGameBox(World world) {
        try {
            if (world != null) {
                for (int x = -15; x < 15; x++) {
                    for (int y = 122; y > 96; y--) {
                        for (int z = -15; z < 15; z++) {
                            Block block = world.getBlockAt(x, y, z);
                            if (block != null) {
                                block.setType(Material.AIR);
                            }
                        }
                    }
                }

                for (Item item : world.getEntitiesByClass(Item.class)) {
                    if (item != null && item.getLocation().getY() >= 96) {
                        item.remove();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();System.out.println(e.getMessage());
        }
    }
}

