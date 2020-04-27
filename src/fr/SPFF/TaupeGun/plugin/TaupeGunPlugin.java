package fr.SPFF.TaupeGun.plugin;

import fr.SPFF.TaupeGun.commands.Executor;
import fr.SPFF.TaupeGun.game.TaupeGunManager;
import fr.SPFF.TaupeGun.listeners.Listening;
import fr.SPFF.TaupeGun.utils.FileManager;
import org.bukkit.ChatColor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.logging.Level;

public class TaupeGunPlugin extends JavaPlugin {

    private static TaupeGunPlugin instance;

    private TaupeGunManager taupeGunManager;

    private FileManager fileManager;

    @Override
    public void onEnable() {
        super.onEnable();
        TaupeGunPlugin.instance = this;
        try {
            this.fileManager = new FileManager("data");
        } catch (IOException | InvalidConfigurationException e) {
            this.getLogger().log(Level.SEVERE, ChatColor.translateAlternateColorCodes('&', "§c§lTaupe Gun §4§l» §cError while retrieve creating files ."), e);
        }
        if(this.getServer().getWorlds().parallelStream().noneMatch(world -> world.getName().equalsIgnoreCase(this.fileManager.getFile("data").getString("world")))){
            this.getLogger().log(Level.SEVERE, ChatColor.translateAlternateColorCodes('&', "§c§lTaupe Gun §4§l» §c§lUnknown world in data.yml"));
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.getLogger().info("\n" +
                "___________                              ________              \n" +
                "\\__    ___/____   __ ________   ____    /  _____/ __ __  ____  \n" +
                "  |    |  \\__  \\ |  |  \\____ \\_/ __ \\  /   \\  ___|  |  \\/    \\ \n" +
                "  |    |   / __ \\|  |  /  |_> >  ___/  \\    \\_\\  \\  |  /   |  \\\n" +
                "  |____|  (____  /____/|   __/ \\___  >  \\______  /____/|___|  /\n" +
                "               \\/      |__|        \\/          \\/           \\/ \n");

        new Executor();
        new Listening();

        this.taupeGunManager = new TaupeGunManager();
    }

    public static TaupeGunPlugin getInstance() {
        return TaupeGunPlugin.instance;
    }

    public FileManager getFileManager(){
        return this.fileManager;
    }

    public TaupeGunManager getTaupeGunManager() {
        return this.taupeGunManager;
    }
}
