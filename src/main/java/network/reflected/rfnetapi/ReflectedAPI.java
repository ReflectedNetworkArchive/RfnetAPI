package network.reflected.rfnetapi;

import lombok.Getter;
import network.reflected.rfnetapi.chestgui.GUIProvider;
import network.reflected.rfnetapi.purchases.PurchaseAPI;
import org.bukkit.Bukkit;

public class ReflectedAPI {
    @Getter private static final GUIProvider guiProvider = new GUIProvider();
    @Getter private static final PurchaseAPI purchaseAPI = new PurchaseAPI();
    @Getter private static final CommandRegistry commandProvider = new CommandRegistry();
    @Getter private static String mapName;

    static void setMapName(String mapname) {
        mapName = mapname;
    }

    @Deprecated()
    public static RfnetAPI getPlugin() {
        return (RfnetAPI) Bukkit.getPluginManager().getPlugin("RfnetAPI");
    }
}
