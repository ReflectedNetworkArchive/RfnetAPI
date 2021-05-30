package network.reflected.rfnetapi;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import static org.bukkit.Bukkit.getLogger;

public class ServerConfig {
    private String missingMsg = "";
    private boolean valid = true;
    private boolean finishedLoad;
    private File bootConfigFile;
    private YamlConfiguration bootConfig;

    public ServerConfig() {
        bootConfigFile = new File("./bootconfig.yml");

        if(!bootConfigFile.exists()){
            //noinspection ResultOfMethodCallIgnored
            bootConfigFile.getParentFile().mkdirs();
            copy(getClass().getClassLoader().getResourceAsStream("bootconfig.yml"), bootConfigFile);
        }

        if (getClass().getClassLoader().getResourceAsStream("bootconfig.yml") == null) {
            getLogger().warning("Template bootconfig.yml in jar is missing. (Something is about to fail)");
        }

        if (bootConfigFile == null) {
            getLogger().warning("Failed to create bootconfig.yml file. (Something is about to fail)");
        }

        try {
            bootConfig = YamlConfiguration.loadConfiguration(bootConfigFile);
        } catch (Exception e) {
            missingMsg = "Error loading config file.";
            e.printStackTrace();
        }

        if (getArchetype().equals("REPLACE-THIS")) {
            missingMsg = "You need to fill out bootconfig.yaml in your server's root directory.";
            valid = false;
        }

        finishedLoad = true;
    }

    public boolean isValid() {
        // Check and make sure that the server ID field contains only contains ABCabc123
        // (other values may break some redis stuff)
        return valid && finishedLoad;
    }

    public String whatsMissing() {
        return missingMsg;
    }

    public String getId() {
        return bootConfig.getString("server-id");
    }

    public String getArchetype() {
        return bootConfig.getString("archetype");
    }

    public String getRedisURI() {
        return bootConfig.getString("redis-lettuce-uri");
    }

    public String getMongoURI() {
        return bootConfig.getString("mongo-uri");
    }

    public String getConnectionStrategy() {
        return bootConfig.getString("player-connection-strategy");
    }

    public boolean isGlobalChatEnabled() {
        return bootConfig.getBoolean("global-chat");
    }

    public boolean isRandomizedMapsEnabled() {
        return bootConfig.getBoolean("randomize-maps");
    }

    public List<String> getMaps() {
        return bootConfig.getStringList("maps");
    }

    public String getAddress() {
        return bootConfig.getString("connection-string");
    }

    // Stolen from bukkit forums if it breaks then idk
    private void copy(InputStream in, File file) {
        try {
            OutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while((len=in.read(buf))>0){
                out.write(buf,0,len);
            }
            out.close();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
