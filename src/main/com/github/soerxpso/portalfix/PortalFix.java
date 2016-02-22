package com.github.soerxpso.portalfix;

import java.util.logging.Level;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Basical methodology here: When a player teleports, start a task that checks them after N seconds.
 * If they are still in the portal after N seconds, send them a message to stay still if they are trapped,
 * then schedule a task to check again after N seconds. If still trapped, teleport them.
 */
public class PortalFix extends JavaPlugin implements Listener {

	private static PortalFix plugin;
	
	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
		plugin = this;
    	saveDefaultConfig();
    	reloadConfig();
		Config.setupConfig();
	}
	
	public static PortalFix getPlugin() {
		return plugin;
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled=true)
	public void onPlayerPortalUse(PlayerPortalEvent event) {
		Player p = event.getPlayer();
		new CheckIfStuck(p).runTaskLater(plugin, Config.getRecheckTime() * 20);
	}


	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled=true) 
	public void onPlayerJoinWithinPortal(PlayerJoinEvent event) {
		Player p = event.getPlayer();
		if (p == null) return;
		Location l = p.getEyeLocation();
		if (l == null) return;
		Block b = l.getBlock();
		if (b == null) return;
		if (Material.PORTAL == b.getType()){
			log("{0} logged into a portal at {1}, checking in {2} seconds if stuck",
				p.getDisplayName(), l, Config.getRecheckTime());
			// Logging in into a portal. Schedule stuck check.
			new CheckIfStuck(p).runTaskLater(plugin, Config.getRecheckTime() * 20);
		}
	}
	
	class CheckIfStuck extends BukkitRunnable {
		private Location l;
		private UUID p;

		public CheckIfStuck(Player p) {
			this.p = p.getUniqueId();
			this.l = p.getEyeLocation();
		}

		public void run() {
			try {
				Player qp = Bukkit.getPlayer(this.p);
				if (qp == null) return; // else, they logged off.
				Location ql = qp.getEyeLocation();
				if (ql == null) return;
				Block qb = ql.getBlock();
				if (qb == null) return;
				if (Material.PORTAL == qb.getType() && l.getWorld().equals(ql.getWorld()) &&
					l.distanceSquared(ql) <= 16.1d) {
					log("Looks like {0} is stuck in a portal at {1}, checking again in {2} seconds.",
						qp.getDisplayName(), ql, Config.getWaitTime());
					// They are still in the portal.
					qp.sendMessage(ChatColor.RED + Config.getMessage());
					new TeleportIfStill(p, l).runTaskLater(plugin, Config.getWaitTime() * 20);
				}
			} catch(Exception e) { // waffling a bit here
				PortalFix.severe("Was tracking a player but an exception occurred", e);
			}
		}
	}			

	class TeleportIfStill extends BukkitRunnable implements Listener {
		private UUID p;
		private Location l;
		private Countdown countdown;
		
		public TeleportIfStill(UUID p, Location l) {
			this.p = p;
			this.l = l;
			getServer().getPluginManager().registerEvents(this, PortalFix.getPlugin());
			countdown = new Countdown((Config.getWaitTime() - Config.getCountdownInterval()) * 20, 
					Config.getCountdownInterval() * 20, getPlugin().getServer().getPlayer(p));
			countdown.runTaskLater(PortalFix.getPlugin(), Config.getCountdownInterval() * 20);
		}
		
		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled=true)
		public void onPlayerMove(PlayerMoveEvent event) {
			if(event.getPlayer().equals(Bukkit.getPlayer(p))) {
				Location ql = event.getTo();
				if (ql == null) return;
				Block qb = ql.getBlock();
				if (qb == null) return;
				if (Material.PORTAL == qb.getType() && l.getWorld().equals(ql.getWorld())) {
					// Don't test distance, just quick check if haven't teleported and in portal still.
					return;
				} else {
					//if(event.getClass() != PlayerPortalEvent.class) {
					this.doCancel(true);
				}
			}
		}

		private void doCancel(boolean unset) {
			if (unset) {
				this.cancel();
				countdown.killSelfAndChildren();
			}
			HandlerList.unregisterAll(this);
		}
		
		@Override
		public void run() {
			try {
				Player qp = Bukkit.getPlayer(this.p);
				if (qp == null) return; // else, they logged off.
				Location ql = qp.getEyeLocation();
				if (ql == null) return;
				Block qb = ql.getBlock();
				if (qb == null) return;
				if (Material.PORTAL == qb.getType() && l.getWorld().equals(ql.getWorld()) &&
					l.distanceSquared(ql) <= 16.1d) {
					log("{0} was stuck in a portal at {1}, teleporting to spawn.",
						qp.getDisplayName(), ql);
					// They are still in the portal.
					qp.teleport(getServer().getWorld(Config.getSpawnName()).getSpawnLocation());
					new BukkitRunnable(){
						private final UUID ap = p;
						@Override
						public void run() {
							Player qp = Bukkit.getPlayer(this.ap);
							if (qp == null) return; // else, they logged off.
							qp.sendMessage(doColors(Config.getPostMessage()));
						}
					}.runTaskLater(PortalFix.getPlugin(), 10); // wait half a second then send message.
				}
			} catch(Exception e) { // waffling a bit here
				PortalFix.severe("Tried to recheck if a player was stuck but an exception occurred", e);
			}
		}
	}
	
	class Countdown extends BukkitRunnable {
		int timeLeft, timeBetweenMessages;
		Player p;
		Countdown child = null;
		
		//timeLeft and timeBetweenMessages should be in seconds
		public Countdown(int timeLeft, int timeBetweenMessages, Player p) {
			this.timeLeft = timeLeft;
			this.timeBetweenMessages = timeBetweenMessages;
			this.p = p;
		}
		
		public void run() {
			p.sendMessage(doColors(Config.getCountdownMessage().replace("%0", String.valueOf(timeLeft / 20))));
			if(timeLeft - timeBetweenMessages > 0) {
				child = new Countdown(timeLeft - timeBetweenMessages, timeBetweenMessages, p);
				child.runTaskLater(PortalFix.getPlugin(), timeBetweenMessages);
			}
		}
		
		//Devoted is not responsible for the possible results of taking life advice from method names
		public void killSelfAndChildren() {
			if(child == null) {
				this.cancel();
			}else {
				child.killSelfAndChildren();
			}
		}
	}
	
	public static String doColors(String s) {
		return s.replace('&', ChatColor.COLOR_CHAR);
	}
	
	private static void log(String message, Object...replace) {
		getPlugin().getLogger().log(Level.INFO, message, replace);
	}
	private static void severe(String message, Throwable error) {
		getPlugin().getLogger().log(Level.SEVERE, message, error);
	}
}
