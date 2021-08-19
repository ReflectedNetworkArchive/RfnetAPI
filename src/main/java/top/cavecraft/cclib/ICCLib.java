package top.cavecraft.cclib;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Scoreboard;

import java.util.List;

/**
 * This file is provided for backwards compatibility. Please do not interact with it in your plugins.
 */

@SuppressWarnings("DeprecatedIsStillUsed")
@Deprecated
public interface ICCLib {
    /**
     * This function internally enables the automatic deletion of the spawn box, joining and spectating features, the
     * initial start game timer, and last but not least it instantiates GameUtils which it then returns or can be
     * accessed via {@link ICCLib#getGameUtils()}.
     *
     * @param tickRequirements
     * A list of ITickReq, representing all possible numbers of players and whether or not the game can start at that
     * amount as well as the time until the start when that number is reached.
     *
     * @return
     * Returns an {@link IGameUtils} instance, since enabling game features probably means you want to use them.
     */
    @Deprecated
    IGameUtils enableGameFeatures(List<ITickReq> tickRequirements);

    /**
     * This function internally enables the automatic deletion of the spawn box, joining and spectating features, the
     * initial start game timer, and last but not least it instantiates GameUtils which it then returns or can be
     * accessed via {@link ICCLib#getGameUtils()}.
     *
     * @param tickRequirements
     * A list of ITickReq, representing all possible numbers of players and whether or not the game can start at that
     * amount as well as the time until the start when that number is reached.
     *
     * @param startGame
     * Run when the game starts, and the timer is completely up.
     *
     * @param teleport
     * Run before the countown, usually when players are teleported to the arena / map.
     */
    void enableGameFeatures(List<ITickReq> tickRequirements, Runnable startGame, Runnable teleport);

    /**
     * This function is useful if you would rather not store the result from {@link ICCLib#enableGameFeatures(List)} or
     * you can't access it's result from where it was originally called.
     *
     * @return
     * Returns an {@link IGameUtils} instance, or null if you haven't called {@link ICCLib#enableGameFeatures(List)}.
     */
    IGameUtils getGameUtils();

    /**
     * Constructs a TickReq object (extends {@link ITickReq}) for use in {@link ICCLib#enableGameFeatures(List)}.
     *
     * @param numberOfPlayers
     * The number of players that this TickReq refers to.
     *
     * @param seconds
     * The number of seconds until the game starts when numberOfPlayers is reached. If numberOfPlayers is reached and
     * the timer has more waiting time than this number, then the timer will be decreased to this number.
     *
     * @param canStart
     * Whether or not the game can start at this numberOfPlayers.
     *
     * @return
     * Returns the newly constructed TickReq.
     */
    ITickReq createTickReq(int numberOfPlayers, int seconds, boolean canStart);

    /**
     * Creates a named item. There is a long-form way built into bukkit, but this just shortens it.
     *
     * @param material
     * The {@link Material} of the item.
     *
     * @param amount
     * The number of items in the {@link ItemStack}.
     *
     * @param name
     * The name of the item. (Italic like an anvil, add {@link ChatColor#RESET} at the beginning to fix)
     *
     * @return
     * Returns the new {@link ItemStack} that has been constructed.
     */
    ItemStack createNamedItem(Material material, int amount, String name);

    /**
     * Offsets a {@link Location} by the given coordinates. There is also a long-form way in bukkit, this is shorthand.
     *
     * @param original
     * The original {@link Location}, what is being transformed or offset. Also called the preimage in geometry terms.
     *
     * @param xoff
     * The x-offset.
     *
     * @param yoff
     * The y-offset.
     *
     * @param zoff
     * The z-offset.
     *
     * @return
     * Returns the transformed {@link Location}.
     */
    Location offset(Location original, double xoff, double yoff, double zoff);

    /**
     * Constructs an action bar message (yes there is a way to do this in bukkit, but not in 1.8)
     *
     * @param message
     * The text of the message. Supports {@link ChatColor}.
     *
     * @return
     * Returns an {@link IActionBarMessage} which can be sent to players.
     */
    IActionBarMessage createActionBarMessage(String message);

    /**
     * Stops the server and runs ./restart.sh to restart it correctly. Also marks the server as unavailable.
     */
    void restartServer();

    /**
     * Gets the scoreboard manager. Null if called before the plugin is enabled.
     *
     * @return
     * Scoreboard Manager
     */
    IScoreboardManager getScoreboardManager();

    /**
     * Deprecated, does nothing.
     */
    @Deprecated
    void awardGems(Player player, IGameUtils.GemAmount amount);

    /**
     * Deprecated, does nothing.
     */
    @Deprecated
    double getGems(Player player);

    /**
     * Deprecated, does nothing.
     */
    @Deprecated
    void decreaseGems(Player player, double decrease);

    /**
     * Creates a new {@link IChildScoreboard}.
     *
     * @return
     * The child scoreboard.
     */
    IChildScoreboard createChildScoreboard();

    /**
     * Ew, it has manager in the name ... but don't worry clean code fanatics! That name is correct, because this class
     * actually manages creation and deletion of scoreboards for all players on the server.
     */
    interface IScoreboardManager {
        /**
         * This adds a line to all scoreboards rather than just one player.
         *
         * @param tag
         * The tag of the line, so you can access it later with {@link IScoreboardManager#setDynamicOfAll(String, String)}
         *
         * @param text
         * The text of the line you are adding. This cannot be changed and each line must be unique or they are turned
         * into one line. To fix this for blank lines, use {@link IScoreboardManager#addBlankLineToAll()}
         */
        void addLineToAll(String tag, String text);

        /**
         * This sets the title of all scoreboards.
         *
         * @param title
         * The title to set to.
         */
        void setTitleOfAll(String title);

        /**
         * Sets the dynamic text of a line on all scoreboards.
         *
         * @param tag
         * The line's tag.
         *
         * @param text
         * The dynamic text that is being set.
         */
        void setDynamicOfAll(String tag, String text);

        /**
         * Adds a blank line to all scoreboards.
         */
        void addBlankLineToAll();

        /**
         * Clears all scoreboards
         */
        void clearAll();

        /**
         * Gets the scoreboard for a certain player. Will be null if they are not online.
         *
         * @param player
         * The player to get the scoreboard for.
         *
         * @return
         * Returns the scoreboard.
         */
        IPlayerScoreboard getScoreboardFor(Player player);
    }

    //TODO comment this thing
    interface IPlayerScoreboard {
        void setTitle(String title);

        void addLine(String tag, String text);

        void addBlankLine();

        void setDynamic(String tag, String text);

        void clear();

        /**
         * Similar to {@link ICCLib#createChildScoreboard()} except it is automatically added to this scoreboard.
         *
         * @return
         * The child scoreboard.
         */
        IChildScoreboard createChild();

        void addChild(IChildScoreboard child);

        Scoreboard getHandle();
    }

    interface IChildScoreboard {
        void setTitle(String title);

        void addLine(String tag, String text);

        void addBlankLine();

        void setDynamic(String tag, String text);
    }

    /**
     * GameUtils is a utility class with many functions constantly used in minigames.
     */
    interface IGameUtils {
        /**
         * Enum representing different amounts of gems. Used in {@link IGameUtils#awardGems(Player, GemAmount)}
         */
        enum GemAmount {
            SMALL,
            MEDIUM,
            LARGE,
            EXTRA_LARGE
        }

        /**
         * Blocks interaction with interactive blocks (trapdoors, levers, etc.) without fully blocking all interaction.
         *
         * @param event
         * The event from an event handler.
         */
        void blockInteract(PlayerInteractEvent event);

        /**
         * Awards a player gems using the mongo database.
         *
         * @param player
         * The player to award.
         *
         * @param amount
         * The amount to award.
         */
        void awardGems(Player player, GemAmount amount);

        /**
         * Gives a player night vision, resets their position, and gives them the "Return to lobby" item. Usually this
         * function would be run every time a player joins.
         *
         * @param player
         * The player to "set up".
         */
        void setupFreshPlayer(Player player);

        /**
         * Kicks a player if they are holding the "Return to lobby" item.
         *
         * @param item
         * The {@link ItemStack} they are holding
         *
         * @param player
         * The player to kick if it is the return to lobby item.
         */
        void kickIfLobbyTP(ItemStack item, Player player);

        /**
         * Fireworks!
         *
         * @param winner
         * The player to launch the fireworks from.
         */
        void winnerEffect(Player winner);

        /**
         * Prints a custom death message and plays a sound, but also makes the player a spectator. This is for a final
         * kill, when they cannot respawn.
         *
         * @param player
         * The player that died.
         */
        void deathEffect(Player player);

        /**
         * Prints a custom death message and plays a sound. This is for when they can respawn.
         *
         * @param player
         * The player that died.
         */
        void deathMessage(Player player);

        /**
         * Clears the game box. This happens automatically for the default world after the timer finishes.
         *
         * @param world
         * The world to clear the box from
         */
        void clearGameBox(World world);
    }

    /**
     * This interface is explained in {@link ICCLib#createTickReq(int, int, boolean)} and it has no comments because it
     * is for internal use.
     */
    interface ITickReq {}

    /**
     * An action bar message for 1.8
     */
    interface IActionBarMessage {
        /**
         * Sends the message to a player.
         *
         * @param player
         * The player to send the action bar message to.
         */
        void sendTo(Player player);
    }
}
