package fr.aless.TaupeGun.game;

import fr.aless.TaupeGun.plugin.TaupeGunPlugin;
import fr.aless.TaupeGun.utils.*;
import net.minecraft.server.v1_15_R1.PacketPlayOutNamedSoundEffect;
import net.minecraft.server.v1_15_R1.SoundCategory;
import net.minecraft.server.v1_15_R1.SoundEffects;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class TaupeGunManager {

    private final TaupeGunPlugin main;
    public List<TeamsColor> teamsColorList;
    public int taupeTime;
    public int borderTime;
    private State state;
    private Map<Player, ScoreboardSign> playerScoreboardMap;
    private int timer;
    private int task;
    private int chronoTask;
    private Consumer<Player> playerConsumer;

    private int minutes;
    private int seconds;

    public TaupeGunManager() {
        this.main = TaupeGunPlugin.getInstance();
    }

    public void init() {
        this.state = State.WAITING;
        this.timer = 0;
        this.chronoTask = -1;
        this.task = -1;
        this.taupeTime = 10 * this.main.getFileManager().getFile("config").getInt("config.timer.taupes.seconds") + 10 * 60 * this.main.getFileManager().getFile("config").getInt("config.timer.taupes.minutes");
        this.taupeTime = 10 * this.main.getFileManager().getFile("config").getInt("config.timer.border.seconds") + 10 * 60 * this.main.getFileManager().getFile("config").getInt("config.timer.border.minutes") + this.taupeTime;
        final int size = this.main.getFileManager().getFile("config").getInt("config.teams.normal");
        for (final Team team : this.main.getScoreboard().getTeams()) {
            team.unregister();
        }
        this.teamsColorList = Arrays.asList(TeamsColor.getColors());
        this.teamsColorList = MiscUtils.shuffleColor(this.teamsColorList);
        for (int i = 0; i < size; i++) {
            new Teams(false);
        }
        this.playerScoreboardMap = new HashMap<>();
        playerConsumer = player1 -> {
            player1.teleport(this.main.getFileManager().getFile("data").getLocation("spawn"));
            player1.setGameMode(GameMode.ADVENTURE);
            player1.setHealth(20);
            player1.setFoodLevel(20);
            player1.setTotalExperience(0);
            player1.getInventory().clear();
            player1.getScoreboard().clearSlot(DisplaySlot.SIDEBAR);
            for (PotionEffect potionEffectType : player1.getActivePotionEffects()) {
                player1.removePotionEffect(potionEffectType.getType());
            }
        };

        for (final Player player : this.main.getServer().getOnlinePlayers()) {
            if (player.isDead()) {
                this.main.getServer().getScheduler().scheduleSyncDelayedTask(this.main, () -> {
                    player.spigot().respawn();
                    playerConsumer.accept(player);
                }, 2L);
            } else {
                playerConsumer.accept(player);
            }
        }
    }

    public void chronoStart() {
        AtomicInteger timer = new AtomicInteger(5);
        if (!this.state.equals(State.WAITING)) return;
        this.state = State.STARTING;
        this.chronoTask = this.main.getServer().getScheduler().runTaskTimerAsynchronously(this.main, () -> {
            if (timer.get() > 0) {
                final int seconds = ((int) Math.ceil(timer.get()));
                for (final Player player : this.main.getServer().getOnlinePlayers()) {
                    ActionbarUtils.sendActionBar(player, this.main.getFileManager().getPrefixMessage("starting").replace("%seconds%", String.valueOf(seconds)));
                }
            } else if (timer.get() == 0) {
                Message.create(this.main.getFileManager().getPrefixMessage("start")).broadcast();
                for (final Player player : this.main.getServer().getOnlinePlayers()) {
                    final PacketPlayOutNamedSoundEffect sound = new PacketPlayOutNamedSoundEffect(SoundEffects.EVENT_RAID_HORN, SoundCategory.MASTER, player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ(), 10000, 1);
                    ((CraftPlayer) player).getHandle().playerConnection.sendPacket(sound);
                    TitleUtils.send(player, "§c§lTaupe Gun", "§8§o- §7§oBy Aless §8§o-", 1, 3, 1);
                }
                this.main.getServer().getScheduler().runTask(this.main, this::start);
                this.main.getServer().getScheduler().cancelTask(this.chronoTask);
                this.chronoTask = -1;
                return;
            }
            timer.getAndDecrement();
        }, 0L, 20L).getTaskId();
    }

    public Consumer<Player> getPlayerConsumer() {
        return this.playerConsumer;
    }

    public State getState() {
        return this.state;
    }

    public int getTimer() {
        return this.timer;
    }

    public List<TeamsColor> getAvailableColors() {
        return this.teamsColorList;
    }

    public void start() {
        this.main.getWorld().getWorldBorder().setCenter(this.main.getWorld().getSpawnLocation());
        this.main.getWorld().getWorldBorder().setSize(this.main.getFileManager().getFile("config").getInt("config.edge.start") * 2);
        this.main.getWorld().setGameRule(GameRule.NATURAL_REGENERATION, false);
        this.main.getWorld().setFullTime(0);
        this.minutes = this.main.getFileManager().getFile("config").getInt("config.timer.taupes.minutes");
        this.seconds = this.main.getFileManager().getFile("config").getInt("config.timer.taupes.seconds");
        this.main.getServer().getScheduler().runTaskAsynchronously(this.main, () -> {
            this.state = State.STARTED;
            List<Player> players = new ArrayList<>(this.main.getServer().getOnlinePlayers());
            final boolean random = this.main.getFileManager().getFile("config").getBoolean("config.random");
            final List<Teams> teamsList = MiscUtils.shuffleTeams(Teams.getTeams());
            if (random) {
                players = MiscUtils.shufflePlayers(players);
                teamsList.forEach(Teams::removeAllPlayers);
            }
            for (final Player player : players) {
                this.main.getServer().getScheduler().runTask(this.main, () -> {
                    player.getInventory().clear();
                    player.setHealth(20);
                    player.setGameMode(GameMode.SURVIVAL);
                    player.setFoodLevel(20);
                    player.setTotalExperience(0);
                    player.getInventory().addItem(new ItemStack(Material.getMaterial(this.main.getFileManager().getFile("config").getString("config.start.material").toUpperCase()), this.main.getFileManager().getFile("config").getInt("config.start.amount")));
                });
                final PlayerTaupe playerTaupe = new PlayerTaupe(player);
                Teams teams;
                if (random) {
                    teams = (Teams) teamsList.parallelStream().filter(team -> !team.isFull()).toArray()[new Random().nextInt(Math.abs(teamsList.parallelStream().filter(team -> !team.isFull()).toArray().length))];
                    teams.addPlayer(player);
                } else {
                    if (!Teams.getPlayerTeam(player).isPresent()) continue;
                    teams = Teams.getPlayerTeam(player).get();
                }
                final Teams team = teams;
                this.main.getServer().getScheduler().runTask(this.main, () ->
                        team.show(player));
                playerTaupe.setTeam(teams);
            }
            for (final Teams team : teamsList) {
                final Location location = new Location(this.main.getWorld(), this.main.getWorld().getSpawnLocation().getX() + MathUtils.randomRange(-990, 990), 200, this.main.getWorld().getSpawnLocation().getZ() + MathUtils.randomRange(-990, 990));
                this.main.getServer().getScheduler().runTask(this.main, () ->
                        team.getPlayers().forEach(player -> player.teleport(location)));
            }
        });

        // Tâche qui s'exécute tous les 10emes de secondes.
        this.task = this.main.getServer().getScheduler().scheduleSyncRepeatingTask(this.main, () -> {
            for (final Player player : this.main.getServer().getOnlinePlayers()) {
                if(!player.getGameMode().equals(GameMode.SPECTATOR) && PlayerTaupe.getPlayerTaupe(player) == null){
                    player.setGameMode(GameMode.SPECTATOR);
                }
                generateScoreBoard(player);
            }
            if (this.timer < this.taupeTime) {
                for (final Teams teams : Teams.getTeams()) {
                    final List<Player> playerList = teams.getPlayers();
                    for (final Player player : playerList) {
                        StringBuilder stringBuilder = new StringBuilder();
                        for (final Player mate : playerList) {
                            if (!player.equals(mate)) {
                                final Location start = player.getLocation().clone();
                                final Location stop = mate.getLocation().getBlock().getLocation().clone();
                                final int distance = MathUtils.getFlatDistance(start, stop);
                                stringBuilder.append("§b§l").append(mate.getDisplayName()).append(" ").append(distance).append(" ").append(MathUtils.getDirection(start, stop)).append(mate.equals(playerList.get(playerList.size() - 1)) ? "" : "              ");
                            }
                        }
                        ActionbarUtils.sendActionBar(player, stringBuilder.toString());
                    }
                }
            } else if (this.timer == this.taupeTime) {
                for (final Player pls : this.main.getServer().getOnlinePlayers()) {
                    final PacketPlayOutNamedSoundEffect sound = new PacketPlayOutNamedSoundEffect(SoundEffects.ENTITY_CAT_DEATH, SoundCategory.MASTER, pls.getLocation().getX(), pls.getLocation().getY(), pls.getLocation().getZ(), 1, 1);
                    ((CraftPlayer) pls).getHandle().playerConnection.sendPacket(sound);
                    if (PlayerTaupe.getPlayerTaupe(pls) != null) {
                        pls.setHealth(20);
                        Message.create(this.main.getFileManager().getPrefixMessage("heal")).sendMessage(pls);
                    }
                }
                List<PlayerTaupe> addTaupes = new ArrayList<>();
                for (final Teams teams : Teams.getTeams()) {
                    final Optional<Player> player = teams.getPlayers().parallelStream().findAny();
                    if (!player.isPresent())
                        continue;
                    final PlayerTaupe playerTaupe = PlayerTaupe.getPlayerTaupe(player.get());
                    addTaupes.add(playerTaupe);
                }
                Teams taupes = new Teams(true);
                for (final PlayerTaupe playerTaupe : addTaupes) {
                    if (taupes.isFull()) taupes = new Teams(true);
                    taupes.getPlayers().add(playerTaupe.getPlayer());
                    playerTaupe.getTeam().getPlayers().remove(playerTaupe.getPlayer());
                    playerTaupe.setTaupe(taupes);
                    if (playerTaupe.getTeam().getPlayers().size() == 0) {
                        playerTaupe.getPlayer().performCommand("reveal");
                    }
                }
                final Message commandes = Message.create(this.main.getFileManager().getPrefixMessage("taupes.commands"));
                for (final Teams team : Teams.getTeams()) {
                    if (!team.getColor().equals(TeamsColor.TAUPE)) continue;
                    for (final Player taupe : team.getPlayers()) {
                        final List<Player> playerList = new ArrayList<>();
                        final Message message = Message.create(this.main.getFileManager().getPrefixMessage("taupes.allied"));
                        for (final Player player : team.getPlayers()) {
                            if (player.equals(taupe)) continue;
                            playerList.add(player);
                        }
                        for (final Player player : playerList) {
                            message.append("&c").append(player.getDisplayName()).append("&7").append((player.equals(playerList.get(playerList.size() - 1)) ? "." : ", "));
                        }
                        commandes.sendMessage(taupe);
                        message.sendMessage(taupe);
                        continue;
                    }
                }
                Message.create(this.main.getFileManager().getPrefixWarn("taupes.mission")).broadcast();

                this.seconds = this.main.getFileManager().getFile("config").getInt("config.timer.border.seconds");
                this.minutes = this.main.getFileManager().getFile("config").getInt("config.timer.border.minutes");
            }
            if (this.timer % 10 == 0) {
                if (this.timer >= this.borderTime && this.main.getWorld().getWorldBorder().getSize() > this.main.getFileManager().getFile("config").getInt("config.edge.end") * 2) {
                    this.main.getWorld().getWorldBorder().setSize(this.main.getWorld().getWorldBorder().getSize() - 2, 1);
                }
                this.seconds--;
                if (this.seconds < 0) {
                    this.minutes--;
                    this.seconds = 59;
                }
            }
            if (this.ifVictory()) {
                this.main.getServer().getScheduler().cancelTask(this.task);
                this.task = -1;
                final Consumer<Player> consumer = player -> {
                    final PacketPlayOutNamedSoundEffect sound = new PacketPlayOutNamedSoundEffect(SoundEffects.ENTITY_FIREWORK_ROCKET_SHOOT, SoundCategory.MASTER, player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ(), 6, 2);
                    ((CraftPlayer) player).getHandle().playerConnection.sendPacket(sound);
                    player.getLocation().getWorld().spawnEntity(player.getLocation(), EntityType.FIREWORK);
                };
                final Message message = Message.create(this.main.getFileManager().getPrefixMessage("victory") + TeamsColor.getColor(Teams.getTeams().get(0).getColor().getValue()) + Teams.getTeams().get(0).getName()).append("&7: ");
                final List<Player> playerList = new ArrayList<>(Teams.getTeams().get(0).getPlayers());
                for (final Player player : playerList) {
                    message.append(TeamsColor.getColor(Teams.getTeams().get(0).getColor().getValue()) + "").append(player.getDisplayName()).append("&7").append((player.equals(playerList.get(playerList.size() - 1)) ? "." : ", "));
                }
                message.broadcast();
                for (final Player player : this.main.getServer().getOnlinePlayers()) {
                    if (player.isDead()) {
                        this.main.getServer().getScheduler().runTaskLater(this.main, () -> {
                            player.spigot().respawn();
                            consumer.accept(player);
                        }, 2L);
                    } else {
                        consumer.accept(player);
                    }
                }
                this.stop();
                return;
            }
            if(PlayerTaupe.getPlayerTaupeList().size() == 0){
                this.stop();
                return;
            }
            List<Teams> teams = new ArrayList<>(Teams.getTeams());
            for(final Teams team : teams){
                if(team.getPlayers().size() == 0) {
                    for(final PlayerTaupe playerTaupe : PlayerTaupe.getPlayerTaupeList()){
                        if(playerTaupe.isTaupe() && !playerTaupe.isReveal() && playerTaupe.getTeam().equals(team)){
                            playerTaupe.getPlayer().performCommand("reveal");
                            break;
                        }
                    }
                    team.destroy();
                }
            }
            this.timer++;
        }, 0L, 2L);
    }

    public void stop() {
        if (this.task != -1) {
            this.main.getServer().getScheduler().cancelTask(this.task);
            this.task = -1;
        } else if (this.chronoTask != -1) {
            this.main.getServer().getScheduler().cancelTask(this.chronoTask);
            this.chronoTask = -1;
        }
        this.playerScoreboardMap.forEach((player, scoreboard) -> {
            scoreboard.destroy();
            PlayerTaupe.getPlayerTaupeList().remove(PlayerTaupe.getPlayerTaupe(player));
        });
        this.playerScoreboardMap.clear();
        List<Teams> teams = new ArrayList<>();
        for (Teams value : Teams.getTeams()) {
            teams.add(value);
        }
        for (Teams team : teams) {
            team.destroy();
        }
        this.playerScoreboardMap.clear();
        this.init();
    }

    private boolean ifVictory() {
        return Teams.getTeams().size() <= 1;
    }

    private void generateScoreBoard(final Player player) {
        final Location start = player.getLocation();
        final Location stop = new Location(this.main.getWorld(), 0, 0, 0);
        if (!this.playerScoreboardMap.containsKey(player)) {
            this.playerScoreboardMap.put(player, new ScoreboardSign(player, "§c§lTaupe Gun"));
            ScoreboardSign pointer = this.playerScoreboardMap.get(player);
            pointer.create();
            pointer.setLine(0, "§0");
            pointer.setLine(1, "§8§m-----------------");
            pointer.setLine(2, "§1");
            pointer.setLine(3, this.main.getFileManager().getMessage("scoreboard.center") + MathUtils.getFlatDistance(start, stop) + " " + MathUtils.getDirection(start, stop));
            pointer.setLine(4, "§2");
            pointer.setLine(5, "§0§8§m-----------------");
            pointer.setLine(6, "§3");
            pointer.setLine(7, this.main.getFileManager().getMessage("scoreboard.border") + (int) Math.ceil(this.main.getWorld().getWorldBorder().getSize() / 2));
            pointer.setLine(8, "§4");
            pointer.setLine(9, "§1§8§m-----------------");
            pointer.setLine(10, "§5");
            if (this.timer < this.taupeTime) {
                pointer.setLine(11, this.main.getFileManager().getMessage("scoreboard.timer.taupes") + (this.minutes < 10 ? "0" + this.minutes : this.minutes) + ":" + (this.seconds < 10 ? "0" + this.seconds : this.seconds));
            } else if (this.timer < this.borderTime) {
                pointer.setLine(11, this.main.getFileManager().getMessage("scoreboard.timer.border"));
                pointer.setLine(12, "       §7" + (this.minutes < 10 ? "0" + this.minutes : this.minutes) + ":" + (this.seconds < 10 ? "0" + this.seconds : this.seconds));
            }
        }
        final ScoreboardSign scoreboardSign = this.playerScoreboardMap.get(player);
        scoreboardSign.setLine(3, this.main.getFileManager().getMessage("scoreboard.center") + MathUtils.getFlatDistance(start, stop) + " " + MathUtils.getDirection(start, stop));
        scoreboardSign.setLine(7, this.main.getFileManager().getMessage("scoreboard.border") + (int) Math.ceil(this.main.getWorld().getWorldBorder().getSize() / 2));
        if (this.timer < this.taupeTime) {
            scoreboardSign.setLine(11, this.main.getFileManager().getMessage("scoreboard.timer.taupes") + (this.minutes < 10 ? "0" + this.minutes : this.minutes) + ":" + (this.seconds < 10 ? "0" + this.seconds : this.seconds));
        } else if (this.timer < this.borderTime) {
            scoreboardSign.setLine(11, this.main.getFileManager().getMessage("scoreboard.timer.border"));
            scoreboardSign.setLine(12, "       §7" + (this.minutes < 10 ? "0" + this.minutes : this.minutes) + ":" + (this.seconds < 10 ? "0" + this.seconds : this.seconds));
        } else if (this.timer == this.borderTime) {
            if (scoreboardSign.getLine(11) != null || !scoreboardSign.getLine(11).equalsIgnoreCase("")) {
                scoreboardSign.removeLine(11);
            }
            if (scoreboardSign.getLine(12) != null || !scoreboardSign.getLine(12).equalsIgnoreCase("")) {
                scoreboardSign.removeLine(12);
            }
        }
    }

    public enum State {
        WAITING,
        STARTING,
        STARTED
    }

}
