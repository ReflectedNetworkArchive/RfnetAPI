package network.reflected.rfnetapi;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import network.reflected.rfnetapi.chestgui.GUIProvider;
import network.reflected.rfnetapi.commands.CommandRegistry;
import network.reflected.rfnetapi.purchases.PurchaseAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class ReflectedAPI {
    @Getter private final RfnetAPI plugin;

    private static final List<APIGetCallback> callbacks = new ArrayList<>();
    static void checkCallbacks() {
        ReflectedAPI api = get();
        if (api != null) {
            for (APIGetCallback callback : callbacks) {
                Bukkit.getScheduler().runTask(api.plugin, () -> callback.run(api));
            }
            callbacks.clear();
        }
    }

    /**
     * Returns an instance of the API
     * May be null if called before this plugin loads,
     * so depend on this plugin in your plugin.yml.
     * @see ReflectedAPI#get(network.reflected.rfnetapi.ReflectedAPI.APIGetCallback)
     *
     * @return An instance of this class
     */
    @Nullable
    public static ReflectedAPI get() {
        @Nullable Plugin plugin = Bukkit.getPluginManager().getPlugin("RfnetAPI");
        if (plugin instanceof RfnetAPI) {
            return ((RfnetAPI) plugin).getApi();
        } else {
            return null;
        }
    }

    /**
     * Gives an instance of this class via a callback
     * so that your plugin doesn't have to depend on RfnetAPI.
     *
     * @param lambda The callback to run when the plugin has loaded
     */
    public static void get(APIGetCallback lambda) {
        ReflectedAPI api = get();
        if (api != null) {
            lambda.run(api);
            checkCallbacks();
        } else {
            callbacks.add(lambda);
        }
    }

    @Getter private final GUIProvider guiProvider = new GUIProvider();
    @Getter private final PurchaseAPI purchaseAPI = new PurchaseAPI();
    @Getter private final CommandRegistry commandProvider = new CommandRegistry();

    public String getLoadedMap() {
        return plugin.getLoadedMapName();
    }

    public boolean isMinigameWorldSupportEnabled() {
        return plugin.isMinigameWorld();
    }

    public void setAvailable(boolean available) {
        plugin.getDatabase().setAvailable(available);
    }

    public void restart() {
        plugin.restart();
    }

    public void sendPlayer(Player player, String archetype) {
        plugin.sendPlayer(player, archetype);
    }

    public interface APIGetCallback {
        void run(@NotNull ReflectedAPI api);
    }
}
