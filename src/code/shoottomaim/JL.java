package code.shoottomaim;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class JL implements Listener {
	private static Map<Player, Location> loc, loc1;
	private static JLH plugin;
	private Logger log = Logger.getLogger("Minecraft");
	
	public JL(JLH plugin) {
		JL.plugin = plugin;
		loc = new HashMap<Player, Location>();
		loc1 = new HashMap<Player, Location>();
	}
	
	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		Player player = event.getPlayer();
		if (!plugin.playerIsJailed(player)) return;
		event.setRespawnLocation(plugin.getJailLocation());
		
		World w = plugin.getJailLocation().getWorld();
		Wolf wolf = (Wolf) w.spawnCreature(plugin.getJailLocation(), EntityType.WOLF);
		wolf.setAdult();
		wolf.setTarget(player);
		wolf.setAngry(true);
	}
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		if (event.getPlayer().getItemInHand().getTypeId() == /* plugin.getConfig().getInt("JLH.wand") */280) {
			if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
				loc.put(player, event.getClickedBlock().getLocation());
				player.sendMessage(ChatColor.YELLOW + "[JailLikeHell] First point has been set.");
				if (player.getGameMode().equals(GameMode.CREATIVE)) {
					event.setCancelled(true);
				}
			} else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
				loc1.put(player, event.getClickedBlock().getLocation());
				player.sendMessage(ChatColor.YELLOW + "[JailLikeHell] Second point has been set.");
			}
		}
		Block block = event.getClickedBlock();
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK) {
			if (!plugin.hasPermission(player, "JailLikeHell.bypassProtection")) {
				if (!event.isCancelled()) event.setCancelled(isProtected(block));
				if (isProtected(block)) {
					event.getPlayer().sendMessage(ChatColor.RED + "[JailLikeHell] You cannot escape the jail!");
				}
			}
		}
	}
	
	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		Block block = event.getBlock();
		Player player = event.getPlayer();
		
		if (!plugin.hasPermission(player, "JailLikeHell.bypassProtection")) {
			if (isProtected(block)) {
				event.setCancelled(true);
				event.getPlayer().sendMessage(ChatColor.RED + "[JailLikeHell] You cannot escape the jail!");
			}
		}
	}
	
	public boolean isProtected(Block block) {
		double bX, bY, bZ;
		boolean succeed = false;
		bX = block.getX();
		bY = block.getY();
		bZ = block.getZ();
		World world = plugin.getJailLocation().getWorld();
		if (!block.getWorld().equals(world)) return false;
		try {
			BufferedReader br = new BufferedReader(new FileReader("plugins/JailLikeHell/protection.txt"));
			String ln = br.readLine();
			String[] coords = ln.split("\\,");
			br.close();
			Location loc = new Location(block.getWorld(), Double.parseDouble(coords[0]), Double.parseDouble(coords[1]), Double.parseDouble(coords[2]));
			Location loc1 = new Location(block.getWorld(), Double.parseDouble(coords[3]), Double.parseDouble(coords[4]), Double.parseDouble(coords[5]));
			if (loc.getBlock().getX() < loc1.getBlock().getX()) {
				if (bX >= loc.getBlock().getX() && bX <= loc1.getBlock().getX()) {
					succeed = true;
				} else {
					return false;
				}
			} else {
				if (bX <= loc.getBlock().getX() && bX >= loc1.getBlock().getX()) {
					succeed = true;
				} else {
					return false;
				}
			}
			if (loc.getBlock().getY() < loc1.getBlock().getY()) {
				if (bY >= loc.getBlock().getY() && bY <= loc1.getBlock().getY()) {
					succeed = true;
				} else {
					return false;
				}
			} else {
				if (bY <= loc.getBlock().getY() && bY >= loc1.getBlock().getY()) {
					succeed = true;
				} else {
					return false;
				}
			}
			if (loc.getBlock().getZ() < loc1.getBlock().getZ()) {
				if (bZ >= loc.getBlock().getZ() && bZ <= loc1.getBlock().getZ()) {
					succeed = true;
				} else {
					return false;
				}
			} else {
				if (bZ <= loc.getBlock().getZ() && bZ >= loc1.getBlock().getZ()) {
					succeed = true;
				} else {
					return false;
				}
			}
		} catch (IOException x) {
			log.info(x.toString());
		}
		return succeed;
	}
	
	public Map<Player, Location> getLoc() {
		return loc;
	}
	
	public Map<Player, Location> getLoc1() {
		return loc1;
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		final Player player = event.getPlayer();
		if ((!plugin.playerIsJailed(player)) || (!plugin.playerIsTempJailed(player))) {
			return;
		}
		double tempTime = plugin.getTempJailTime(player);
		long currentTime = System.currentTimeMillis();
		
		if (tempTime <= currentTime) {
			plugin.unjailPlayer(plugin.console, new String[] {"unjail", player.getName()}, true);
		}
		
		if (!plugin.playerIsJailed(player)) {
			return;
		}
		if (plugin.playerIsTempJailed(player)) {
			int minutes = (int) Math.round((plugin.getTimeSentenced(player) - plugin.getTimeServed(player)) / 60.0);
			// int minutes = (int)((plugin.getTempJailTime(player) -
			// System.currentTimeMillis()) / 60000.0D);
			player.sendMessage(ChatColor.RED + "[JailLikeHell] You are jailed for " + plugin.prettifyMinutes(minutes) + ".");
		} else {
			player.sendMessage(ChatColor.RED + "[JailLikeHell] You are permanently jailed.");
			plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
				@Override
				public void run() {
					player.teleport(JL.plugin.getJailLocation());
					
					World w = JL.plugin.getJailLocation().getWorld();
					Wolf wolf = (Wolf) w.spawnCreature(JL.plugin.getJailLocation(), EntityType.WOLF);
					wolf.setAdult();
					wolf.setTarget(player);
					wolf.setAngry(true);
				}
			}, 60L);
		}
	}
}