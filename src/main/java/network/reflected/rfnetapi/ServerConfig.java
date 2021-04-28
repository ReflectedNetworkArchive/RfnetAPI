package network.reflected.rfnetapi;

import io.lettuce.core.RedisURI;

import java.io.File;

public class ServerConfig {
    private String missingMsg = "";
    private File bootConfig;

    public ServerConfig() {
        // TODO: https://bukkit.org/threads/bukkits-yaml-configuration-tutorial.42770/
        bootConfig = new File("./bootconfig.yml");
    }

    public boolean isValid() {
        // Check and make sure that the server ID field contains only contains ABCabc123
        // (other values may break some redis stuff)
        missingMsg = "Config checker not implemented yet.";
        return false;
    }

    /**
     * Returns a message about what properties are not in the config.
     * @return The message
     */
    public String whatsMissing() {
        return missingMsg;
    }

    public String getId() {
    }

    public String getArchetype() {
    }

    public RedisURI getRedisURI() {
    }

    public String getMongoURI() {
    }

    public String getMaxPlayers() {
    }
}
