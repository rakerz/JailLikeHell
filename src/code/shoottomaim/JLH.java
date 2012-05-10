package code.shoottomaim;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class JLH extends JavaPlugin {
	private static final Logger log = Logger.getLogger("Minecraft");
	public ConsoleCommandSender console;
	private Location jailLoc;
	private Location unjailLoc;
	private String jailGroup;
	private YamlConfiguration jailed;
	private JL listener;
	private Permission permission = null;
	
	public void onDisable() {
		log.info("[JailLikeHell] " + getDescription().getName() + " v" + getDescription().getVersion() + " has been disabled.");
	}
	
	public void onEnable() {
		try {
		    Metrics metrics = new Metrics(this);
		    metrics.start();
		} catch (IOException e) {
		    // Failed to submit the stats :(
		}
		
		this.console = getServer().getConsoleSender();
		boolean success = (new File("plugins/JailLikeHell/")).mkdir();
		if (success) {
			log.info("[JailLikeHell] Directory: " + "plugins/JailLikeHell/" + " created!");
		}
		try {
			boolean success2 = (new File("plugins/JailLikeHell/protection.txt")).createNewFile();
			if (success2) {
				log.info("[JailLikeHell] File: " + "protection.txt" + " created!");
			}
		} catch (IOException x) {
			log.info(x.toString());
		}
		loadConfig();
		if (!isEnabled()) {
			return;
		}
		
		this.listener = new JL(this);
		getServer().getPluginManager().registerEvents(listener, this);
		
		getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			@SuppressWarnings("deprecation")
			public void run() {
				// player checks
				Player[] players = JLH.this.getServer().getOnlinePlayers();
				
				for (Player p : players) {
					if ((!JLH.this.playerIsJailed(p)) && (!JLH.this.playerIsTempJailed(p))) {
						continue;
					}
					
					if(JLH.this.playerIsTempJailed(p)){
						int timeSpent = JLH.this.getTimeServed(p); // measured in
						// seconds
						int timeSentenced = JLH.this.getTimeSentenced(p); // measured
						// in
						// seconds
						
						if (timeSpent >= timeSentenced) {
							JLH.this.unjailPlayer(JLH.this.console, new String[] { "unjail", p.getName() }, true);
						} else {
							JLH.this.incrementTimeServed(p, 10);
						}
					}
					
					//p.sendMessage("JailLikeHell task got to you!");
					p.getInventory().clear();
					
					p.updateInventory();
					setPotionEffects(p);
				}
				// lightning
				jailLoc.getWorld().strikeLightningEffect(jailLoc);
			}
		}, 200L, 200L);
		
		setupPermissions();
		log.info("[JailLikeHell] " + getDescription().getName() + " v" + getDescription().getVersion() + " enabled.");
	}
	
	private void setPotionEffects(Player player) {
		player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 300, 5), true);
		player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 300, 5), true);
	}
	
	private void removePotionEffects(Player player){
		player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 0, 0), true);
		player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 0, 0), true);
	}
	
	private void spawnAngryWolfOn(Player player) {
		World w = getJailLocation().getWorld();
		Wolf wolf = (Wolf) w.spawnCreature(getJailLocation(), EntityType.WOLF);
		// wolf.setAdult();
		wolf.setTarget(player);
		wolf.setAngry(true);
	}
	
	private boolean setupPermissions() {
		RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
		if (permissionProvider != null) {
			permission = permissionProvider.getProvider();
		}
		return (permission != null);
	}
	
	@EventHandler
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		boolean succeed = false;
		Player player = null;
		if (sender instanceof Player) {
			player = (Player) sender;
			if (cmd.getName().equalsIgnoreCase("jlh")) {
				if (args.length == 1) {
					if (args[0].equalsIgnoreCase("protect")) {
						if (!hasPermission(sender, "JailLikeHell.protect")) {
							return true;
						}
						protect(player);
						succeed = true;
					} else if (args[0].equalsIgnoreCase("unprotect")) {
						if (!hasPermission(sender, "JailLikeHell.unprotect")) {
							return true;
						}
						unprotect(player);
						succeed = true;
					} else if (args[0].equalsIgnoreCase("wand")) {
						if (!hasPermission(sender, "JailLikeHell.wand")) {
							return true;
						}
						player.getInventory().addItem(new ItemStack(this.getConfig().getInt("jailwand"), 1));
						succeed = true;
						player.sendMessage(ChatColor.AQUA + "[JailLikeHell] Wand Spawned!");
					} else if (args[0].equalsIgnoreCase("info")) {
						if (!hasPermission(sender, "JailLikeHell.info")) {
							return true;
						}
						
						player.sendMessage("");
						player.sendMessage(ChatColor.YELLOW + "______________[JailLikeHell] Command List: ______________");
						player.sendMessage("");
						player.sendMessage(ChatColor.YELLOW + "/jlh info     - Shows all JailLikeHell related commands.");
						player.sendMessage(ChatColor.YELLOW + "/jlh jail     - Jails a player!");
						player.sendMessage(ChatColor.YELLOW + "/jlh unjail   - Unjails a player!");
						player.sendMessage(ChatColor.YELLOW + "/jlh setjail  - Sets a jail point!");
						player.sendMessage(ChatColor.YELLOW + "/jlh jailtime - Checks the remaining jail time!");
						player.sendMessage(ChatColor.YELLOW + "/jlh protect  - Protects the area set!");
						player.sendMessage(ChatColor.YELLOW + "/jlh wand     - Spawns a wand for selecting 2 points!");
						
						return true;
					}
				}
				
				if (args[0].equalsIgnoreCase("jail") && ((args.length > 0) && (args.length < 4))) {
					if (!hasPermission(sender, "JailLikeHell.jail")) {
						return true;
					}
					if (args.length == 1) {
						String[] tmp = {"jail", player.getName()};
						
						jailPlayer(sender, tmp);
						//player.sendMessage(ChatColor.AQUA + "[JailLikeHell] Player " + tmp[1] + " jailed!");
					} else {
						jailPlayer(sender, args);
						//player.sendMessage(ChatColor.AQUA + "[JailLikeHell] Player " + args[1] + " jailed!");
					}
					return true;
				} else if (args[0].equalsIgnoreCase("unjail") && (args.length == 1) || (args.length == 2)) {
					if (!hasPermission(sender, "JailLikeHell.unjail")) {
						return true;
					}
					if(args.length == 1){
						String[] tmp = {"unjail", player.getName()};
						unjailPlayer(sender, tmp);
					} else {
						unjailPlayer(sender, args);
					}
					return true;
				} else if (args[0].equalsIgnoreCase("setjail") && ((args.length == 1) || (args.length == 4))) {
					if (!hasPermission(sender, "JailLikeHell.setjail")) {
						return true;
					}
					
					setJail(sender, args);
					
					player.sendMessage(ChatColor.AQUA + "[JailLikeHell] The jail point has been set!");
					return true;
				} else if (args[0].equalsIgnoreCase("setunjail") && ((args.length == 5) || (args.length == 1))) {
					if (!hasPermission(sender, "JailLikeHell.setjail")) {
						return true;
					}
					
					setUnjail(sender, args);
					
					player.sendMessage("[JailLikeHell] Unjail location set.");
					return true;
				} else if (args[0].equalsIgnoreCase("jailtime") && (args.length <= 1)) {
					if (!hasPermission(sender, "JailLikeHell.jailtime")) {
						return true;
					}
					jailTime(sender, args);
					return true;
				}
			}
		}
		return succeed;
	}
	
	private void protect(Player player) {
		if (listener.getLoc().containsKey(player) && listener.getLoc1().containsKey(player)) {
			try {
				Writer output = new FileWriter("plugins/JailLikeHell/protection.txt", false);
				output.write(listener.getLoc().get(player).getBlock().getX() + "," + listener.getLoc().get(player).getBlock().getY() + "," + listener.getLoc().get(player).getBlock().getZ() + "," + listener.getLoc1().get(player).getBlock().getX() + "," + listener.getLoc1().get(player).getBlock().getY() + "," + listener.getLoc1().get(player).getBlock().getZ());
				output.close();
				player.sendMessage(ChatColor.AQUA + "[JailLikeHell] The jail is now protected!");
			} catch (IOException x) {
				log.info(x.toString());
			}
		} else {
			player.sendMessage(ChatColor.RED + "[JailLikeHell] Set 2 points first!");
		}
	}
	
	private void unprotect(Player player) {
		try {
			Writer output = new FileWriter("plugins/JailLikeHell/protection.txt", false);
			output.write("0,0,0,0,0,0");
			output.close();
			player.sendMessage(ChatColor.RED + "[JailLikeHell] The jail is now unprotected!");
		} catch (IOException x) {
			log.info(x.toString());
		}
	}
	
	public void jailPlayer(CommandSender sender, String[] args) {
		Player player = getServer().getPlayer(args[1]);
		args[1] = (player == null ? args[1].toLowerCase() : player.getName().toLowerCase());
		if (this.jailed.get(args[1]) != null) {
			sender.sendMessage(ChatColor.RED + "[JailLikeHell] That player is already in jail!");
			return;
		}
		if (player != null) {
			player.teleport(this.jailLoc);
		}
		String[] groupName;
		try {
			groupName = getGroups(args[1]);
		} catch (Exception e) {
			String[] tmp = {};
			groupName = tmp;
		}
		this.jailed.set(args[1] + ".groups", groupName);
		try {
			setGroup(args[1], this.jailGroup);
		} catch (Exception e) {
			System.out.println("[JailLikeHell] Groups not supported. Do you have a permissions plugin enabled?");
		}
		int minutes = 0;
		if (args.length == 3) {
			minutes = parseTimeString(args[2]);
			if (minutes != -1) {
				System.out.println("[JailLikeHell] " + minutes);
				double tempTime = System.currentTimeMillis() + minutes * 60000;
				this.jailed.set(args[1] + ".tempTime", tempTime);
				this.jailed.set(args[1] + ".timeServed", 0D); // measured in
				// seconds
				this.jailed.set(args[1] + ".timeSentenced", (double) minutes * 60); // measured
				// in
				// seconds
			}
		}
		saveJail();
		if (player != null) {
			if ((args.length == 1) || (args.length == 2) || (minutes == -1)) {
				player.sendMessage(ChatColor.RED + "[JailLikeHell] You have been jailed!");
				log.info("[JailLikeHell] A player has been jailed!");
			} else {
				player.sendMessage(ChatColor.AQUA + "[JailLikeHell] You have been jailed for " + prettifyMinutes(minutes) + "!");
			}
		}
		setPotionEffects(player);
		for (int i = 0; i < 3; i++)
			spawnAngryWolfOn(player);
		sender.sendMessage(ChatColor.RED + "[JailLikeHell] Player sent to jail.");
	}
	
	@SuppressWarnings("unchecked")
	public void unjailPlayer(CommandSender sender, String[] args, boolean fromTempJail) {
		Player player = getServer().getPlayer(args[1]);
		args[1] = (player == null ? args[1].toLowerCase() : player.getName().toLowerCase());
		if (player == null) {
			sender.sendMessage(ChatColor.RED + "[JailLikeHell] Couldn't find player \"" + args[1] + ".");
			return;
		}
		
		if(!playerIsJailed(player)) {
			sender.sendMessage(ChatColor.RED + "[JailLikeHell] That player is not in jail!");
			return;
		}
		
		if (this.jailed.get(args[1] + ".groups") == null) {
			List<String> groups = (List<String>) this.jailed.getList(args[1]);
			this.jailed.set(args[1], null);
			this.jailed.set(args[1] + ".groups", groups);
		}
		
		player.teleport(this.unjailLoc);
		try {
			restoreGroup(args[1], (List<String>) this.jailed.getList(args[1] + ".groups"));
		} catch (Exception e) {
			System.out.println("[JailLikeHell] Groups not supported. Do you have a permissions plugin enabled?");
		}
		this.jailed.set(args[1], null);
		saveJail();
		removePotionEffects(player);
		player.sendMessage(ChatColor.AQUA + "[JailLikeHell] You have been removed from jail!");
		if (fromTempJail) {
			sender.sendMessage(ChatColor.AQUA + player.getName() + " auto-unjailed.");
		} else {
			sender.sendMessage(ChatColor.AQUA + "[JailLikeHell] Player removed from jail.");
		}
	}
	
	public void unjailPlayer(CommandSender sender, String[] args) {
		unjailPlayer(sender, args, false);
	}
	
	public void setJail(CommandSender sender, String[] args) {
		if ((!(sender instanceof Player)) && (args.length != 4)) {
			sender.sendMessage(ChatColor.RED + "[JailLikeHell] Only players can use that.");
			return;
		}
		if (args.length == 1) {
			Player player = (Player) sender;
			this.jailLoc = player.getLocation();
		} else if (args.length == 4) {
			if ((!new Scanner(args[0]).hasNextInt()) || (!new Scanner(args[1]).hasNextInt()) || (!new Scanner(args[2]).hasNextInt())) {
				sender.sendMessage(ChatColor.RED + "[JailLikeHell] Invalid coordinate!");
				return;
			}
			this.jailLoc = new Location(getServer().getWorld(args[3]), Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]));
		} else {
			sender.sendMessage(ChatColor.RED + "[JailLikeHell] Invalid arguments!");
		}
		YamlConfiguration config = (YamlConfiguration) getConfig();
		config.set("jail.x", Integer.valueOf((int) this.jailLoc.getX()));
		config.set("jail.y", Integer.valueOf((int) this.jailLoc.getY()));
		config.set("jail.z", Integer.valueOf((int) this.jailLoc.getZ()));
		config.set("jail.world", this.jailLoc.getWorld().getName());
		saveConfig();
		sender.sendMessage(ChatColor.AQUA + "Jail point saved.");
	}
	
	public void setUnjail(CommandSender sender, String[] args) {
		if ((!(sender instanceof Player)) && (args.length != 5)) {
			sender.sendMessage(ChatColor.RED + "[JailLikeHell] Only players can use that.");
			return;
		}
		if (args.length == 1) {
			Player player = (Player) sender;
			this.unjailLoc = player.getLocation();
		} else {
			if ((!new Scanner(args[1]).hasNextInt()) || (!new Scanner(args[2]).hasNextInt()) || (!new Scanner(args[3]).hasNextInt())) {
				sender.sendMessage(ChatColor.RED + "[JailLikeHell] Invalid coordinate!");
				return;
			}
			this.unjailLoc = new Location(getServer().getWorld(args[4]), Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]));
		}
		YamlConfiguration config = (YamlConfiguration) getConfig();
		config.set("unjail.x", Integer.valueOf((int) this.unjailLoc.getX()));
		config.set("unjail.y", Integer.valueOf((int) this.unjailLoc.getY()));
		config.set("unjail.z", Integer.valueOf((int) this.unjailLoc.getZ()));
		config.set("unjail.world", this.unjailLoc.getWorld().getName());
		saveConfig();
		sender.sendMessage(ChatColor.AQUA + "[JailLikeHell] Unjail point saved.");
	}
	
	public void jailTime(CommandSender sender, String[] args) {
		if ((!(sender instanceof Player)) && (args.length == 1)) {
			sender.sendMessage(ChatColor.RED + "[JailLikeHell] Must specify a player.");
			return;
		}
		Player player = args.length == 1 ? (Player) sender : getServer().getPlayer(args[1]);
		if (player == null) {
			sender.sendMessage(ChatColor.RED + "[JailLikeHell] Couldn't find player '" + args[1] + "'.");
			return;
		}
		if (!playerIsTempJailed(player)) {
			if (args.length == 0) {
				sender.sendMessage(ChatColor.RED + "[JailLikeHell] You are not tempjailed!");
			} else {
				sender.sendMessage(ChatColor.RED + "[JailLikeHell] That player is not tempjailed.");
			}
			return;
		}
		int minutes = (int) ((getTempJailTime(player) - System.currentTimeMillis()) / 60000.0D);
		sender.sendMessage(ChatColor.AQUA + "[JailLikeHell] Remaining jail time: " + prettifyMinutes(minutes));
	}
	
	public void loadConfig() {
		YamlConfiguration config = (YamlConfiguration) getConfig();
		config.options().copyDefaults(true);
		config.addDefault("jailgroup", "Jailed");
		config.addDefault("jailwand", 280);
		config.addDefault("jail.world", ((World) getServer().getWorlds().get(0)).getName());
		config.addDefault("jail.x", Integer.valueOf(0));
		config.addDefault("jail.y", Integer.valueOf(0));
		config.addDefault("jail.z", Integer.valueOf(0));
		config.addDefault("unjail.world", ((World) getServer().getWorlds().get(0)).getName());
		config.addDefault("unjail.x", Integer.valueOf(0));
		config.addDefault("unjail.y", Integer.valueOf(0));
		config.addDefault("unjail.z", Integer.valueOf(0));
		this.jailed = new YamlConfiguration();
		File f = new File(getDataFolder().getPath() + File.separator + "jailed.yml");
		try {
			if (!f.exists()) {
				f.getParentFile().mkdirs();
				f.createNewFile();
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		try {
			this.jailed.load(f);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		this.jailLoc = new Location(getServer().getWorld(config.getString("jail.world", ((World) getServer().getWorlds().get(0)).getName())), config.getInt("jail.x", 0), config.getInt("jail.y", 0), config.getInt("jail.z", 0));
		this.unjailLoc = new Location(getServer().getWorld(config.getString("unjail.world", ((World) getServer().getWorlds().get(0)).getName())), config.getInt("unjail.x", 0), config.getInt("unjail.y", 0), config.getInt("unjail.z", 0));
		this.jailGroup = config.getString("jailgroup", "Jailed");
		saveConfig();
	}
	
	public int incrementTimeServed(Player player, int seconds) {
		this.jailed.set(player.getName().toLowerCase() + ".timeServed", this.getTimeServed(player) + seconds);
		saveJail();
		return this.getTimeServed(player);
	}
	
	public int getTimeServed(Player player) {
		return this.jailed.getInt(player.getName().toLowerCase() + ".timeServed");
	}
	
	public int getTimeSentenced(Player player) {
		return this.jailed.getInt(player.getName().toLowerCase() + ".timeSentenced");
	}
	
	public Location getJailLocation() {
		return this.jailLoc;
	}
	
	public boolean playerIsJailed(Player player) {
		return this.jailed.get(player.getName().toLowerCase()) != null;
	}
	
	public boolean playerIsTempJailed(Player player) {
		return this.jailed.get(player.getName().toLowerCase() + ".tempTime") != null;
	}
	
	public int getTempJailTime(Player player) {
		return this.jailed.getInt(player.getName().toLowerCase() + ".tempTime", -1);
	}
	
	public boolean hasPermission(CommandSender sender, String permission) {
		if ((sender instanceof Player)) {
			return sender.hasPermission(permission);
		}
		return true;
	}
	
	public String[] getGroups(String player) {
		return permission.getPlayerGroups(this.getServer().getPlayer(player));
		
	}
	
	public void setGroup(String player, String group) {
		permission.playerAddGroup(this.getServer().getPlayer(player), group);
	}
	
	public void restoreGroup(String player, List<String> groups) {
		permission.playerRemoveGroup(this.getServer().getPlayer(player), this.jailGroup);
		for (String s : groups) {
			permission.playerAddGroup(this.getServer().getPlayer(player), s);
		}
	}
	
	public String prettifyMinutes(int minutes) {
		if (minutes == 1) return "one minute";
		if (minutes < 60) return minutes + " minutes";
		if (minutes % 60 == 0) {
			if (minutes / 60 == 1) return "one hour";
			return minutes / 60 + " hours";
		}
		int m = minutes % 60;
		int h = (minutes - m) / 60;
		return h + "h" + m + "m";
	}
	
	public int parseTimeString(String time) {
		if (!time.matches("[0-9]*h?[0-9]*m?") || !time.matches("[0-9]*h?[0-9]*m?")) return -1;
		if (time.matches("[0-9]+")) return Integer.parseInt(time);
		if (time.matches("[0-9]+m")) return Integer.parseInt(time.split("m")[0]);
		if (time.matches("[0-9]+h")) return Integer.parseInt(time.split("h")[0]) * 60;
		if (time.matches("[0-9]+h[0-9]+m")) {
			String[] split = time.split("[mh]");
			return Integer.parseInt(split[0]) * 60 + Integer.parseInt(split[1]);
		}
		if (time.matches("[0-9]+m[0-9]+h")) {
			String[] split = time.split("[mh]");
			return Integer.parseInt(split[1]) * 60 + Integer.parseInt(split[0]);
		}
		return -1;
	}
	
	public void saveJail() {
		try {
			this.jailed.save(new File(getDataFolder().getPath() + File.separator + "jailed.yml"));
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
}