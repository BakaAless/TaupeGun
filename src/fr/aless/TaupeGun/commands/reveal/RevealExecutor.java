package fr.aless.TaupeGun.commands.reveal;

import fr.aless.TaupeGun.game.PlayerTaupe;
import fr.aless.TaupeGun.game.TaupeGunManager;
import fr.aless.TaupeGun.plugin.TaupeGunPlugin;
import fr.aless.TaupeGun.utils.Message;
import net.minecraft.server.v1_15_R1.PacketPlayOutNamedSoundEffect;
import net.minecraft.server.v1_15_R1.SoundCategory;
import net.minecraft.server.v1_15_R1.SoundEffects;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class RevealExecutor implements CommandExecutor {

    private final TaupeGunPlugin main;

    public RevealExecutor() {
        this.main = TaupeGunPlugin.getInstance();
        this.main.getCommand("reveal").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return false;
        }
        final Player player = (Player) sender;
        final PlayerTaupe playerTaupe = PlayerTaupe.getPlayerTaupe(player);

        if (!this.main.getTaupeGunManager().getState().equals(TaupeGunManager.State.STARTED)) {
            Message.create(this.main.getFileManager().getPrefixError("start")).sendMessage(player);
            return false;
        }
        if (playerTaupe == null) {
            Message.create(this.main.getFileManager().getPrefixError("inGame")).sendMessage(player);
            return false;
        }
        if (!playerTaupe.isTaupe()) {
            Message.create(this.main.getFileManager().getPrefixError("taupe")).sendMessage(player);
            return false;
        }
        if (playerTaupe.isReveal()) {
            Message.create(this.main.getFileManager().getPrefixError("reveal")).sendMessage(player);
            return false;
        }
        player.getLocation().getWorld().dropItem(player.getLocation(), new ItemStack(Material.GOLDEN_APPLE)).setPickupDelay(0);
        playerTaupe.setReveal(true);
        if (playerTaupe.getTeam().getPlayers().size() == 0) {
            playerTaupe.getTeam().destroy();
        }
        playerTaupe.setTeam(playerTaupe.getTaupe());
        playerTaupe.getTaupe().show(playerTaupe.getPlayer());
        for (final Player pls : this.main.getServer().getOnlinePlayers()) {
            final PacketPlayOutNamedSoundEffect sound = new PacketPlayOutNamedSoundEffect(SoundEffects.ENTITY_GHAST_HURT, SoundCategory.BLOCKS, pls.getLocation().getX(), pls.getLocation().getY(), pls.getLocation().getZ(), 1, 1);
            ((CraftPlayer) pls).getHandle().playerConnection.sendPacket(sound);
        }
        Message.create(this.main.getFileManager().getPrefixWarn("taupes.reveal"))
                .broadcast();
        return true;
    }
}
