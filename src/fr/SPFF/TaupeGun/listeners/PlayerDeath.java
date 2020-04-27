package fr.SPFF.TaupeGun.listeners;

import fr.SPFF.TaupeGun.game.PlayerTaupe;
import fr.SPFF.TaupeGun.utils.Message;
import net.minecraft.server.v1_15_R1.PacketPlayOutNamedSoundEffect;
import net.minecraft.server.v1_15_R1.SoundCategory;
import net.minecraft.server.v1_15_R1.SoundEffects;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;

class PlayerDeath {

    private final Listening listening;

    PlayerDeath(final Listening listening){
        this.listening = listening;
    }

    void handle(final PlayerDeathEvent e){

        final PlayerTaupe playerTaupe = PlayerTaupe.getPlayerTaupe(e.getEntity());
        e.setDeathMessage("");
        Message.create("&c&lTaupe Gun &4&l» &7" + playerTaupe.getPlayer().getDisplayName() + " est mort.").broadcast();
        for(final Player pls : this.listening.getMain().getServer().getOnlinePlayers()){
            final PacketPlayOutNamedSoundEffect sound = new PacketPlayOutNamedSoundEffect(SoundEffects.ENTITY_WITHER_SPAWN, SoundCategory.BLOCKS, pls.getLocation().getX(), pls.getLocation().getY(), pls.getLocation().getZ(), 1, 1);
            ((CraftPlayer) pls).getHandle().playerConnection.sendPacket(sound);
        }
        if(this.listening.getMain().getTaupeGunManager().getTimer() > 10 * 60 * 30) {
            PlayerTaupe.getPlayerTaupeList().remove(playerTaupe);
            playerTaupe.setTaupe(null);
            playerTaupe.setTeam(null);
        }
    }
}