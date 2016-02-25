package com.github.soerxpso.portalfix;

import java.util.logging.Level;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
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
 * Basic methodology here: When a player teleports, start a task that checks them after N seconds. If they are still in
 * the portal after N seconds, send them a message to stay still if they are trapped, then schedule a task to check
 * again after N seconds. If still trapped, teleport them.
 * 
 * @author Soerxpso
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

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerPortalUse(PlayerPortalEvent event) {
		Player p = event.getPlayer();
		if (p == null) return;
		Location l = p.getEyeLocation();
		if (l == null) return;
		Location t = event.getTo();
		log("{0} used a portal at {1} to get to {2}, checking in {3} seconds if stuck", p.getDisplayName(), l, t,
				Config.getRecheckTime());

		new CheckIfStuck(p).runTaskLater(plugin, Config.getRecheckTime() * 20);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerJoinWithinPortal(PlayerJoinEvent event) {
		Player p = event.getPlayer();
		if (p == null) return;
		Location l = p.getEyeLocation();
		if (l == null) return;
		Block b = l.getBlock();
		if (b == null) return;
		if (Material.PORTAL == b.getType()) {
			log("{0} logged into a portal at {1}, checking in {2} seconds if stuck", p.getDisplayName(), l,
					Config.getRecheckTime());

			new CheckIfStuck(p).runTaskLater(plugin, Config.getRecheckTime() * 20);
		}
	}

	/**
	 * Runnable to do initial check after join or teleport for presence in a portal after N seconds.
	 */
	class CheckIfStuck extends BukkitRunnable {
		private UUID p;

		public CheckIfStuck(Player p) {
			this.p = p.getUniqueId();
		}

		public void run() {
			try {
				Player qp = Bukkit.getPlayer(this.p);
				if (qp == null) return; // else, they logged off.
				Location ql = qp.getEyeLocation();
				if (ql == null) return;
				Block qb = ql.getBlock();
				if (qb == null) return;
				if (Material.PORTAL == qb.getType()) {
					log("Looks like {0} is stuck in a portal at {1}, checking again in {2} seconds.",
							qp.getDisplayName(), ql, Config.getWaitTime());
					// They are still in the portal.
					qp.sendMessage(Config.getMessage().replace("%0", String.valueOf(Config.getWaitTime())));
					new TeleportIfStill(p, ql).runTaskLater(plugin, Config.getWaitTime() * 20);
				}
			} catch (Exception e) { // waffling a bit here
				PortalFix.severe("Was tracking a player but an exception occurred", e);
				this.cancel(); // just in case.
			}
		}
	}

	/**
	 * Runnable to do the teleportation task & listen for movement out of portal.
	 */
	class TeleportIfStill extends BukkitRunnable implements Listener {
		private UUID p;
		private Location l;
		private Countdown countdown;

		public TeleportIfStill(UUID p, Location l) {
			this.p = p;
			this.l = l;
			getServer().getPluginManager().registerEvents(this, PortalFix.getPlugin());
			int interval = Config.getCountdownInterval() * 20;
			int waitTime = Config.getWaitTime() * 20;
			countdown = new Countdown(waitTime - interval, interval, p);
			countdown.runTaskTimer(PortalFix.getPlugin(), interval, interval);
		}

		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onPlayerMove(PlayerMoveEvent event) {
			if (p.equals(event.getPlayer().getUniqueId())) {
				Location ql = event.getTo();
				if (ql == null) return;
				Block qb = ql.getBlock();
				if (qb == null) return;
				if (Material.PORTAL == qb.getType() && l.getWorld().equals(ql.getWorld())) {
					// Don't test distance, just quick check if haven't teleported and in portal still.
					return;
				} else {
					this.doCancel(true);
				}
			}
		}

		private void doCancel(boolean unset) {
			if (unset) {
				this.cancel();
				countdown.cancel();
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
				if (Material.PORTAL == qb.getType() && l.getWorld().equals(ql.getWorld())) {
					log("{0} was stuck in a portal at {1}, teleporting to spawn.", qp.getDisplayName(), ql);
					// They are still in the portal.
					qp.teleport(getServer().getWorld(Config.getSpawnName()).getSpawnLocation());
					new BukkitRunnable() {
						private final UUID ap = p;

						@Override
						public void run() {
							Player qp = Bukkit.getPlayer(this.ap);
							if (qp == null) return; // else, they logged off.
							qp.sendMessage(Config.getPostMessage());
						}
					}.runTaskLater(PortalFix.getPlugin(), 10l); // wait half a second then send message.
				}
			} catch (Exception e) { // waffling a bit here
				PortalFix.severe("Tried to recheck if a player was stuck but an exception occurred", e);
			}
		}
	}

	/**
	 * Runnable to send a message to the player every N seconds
	 */
	class Countdown extends BukkitRunnable {
		int timeLeft, timeBetweenMessages;
		private UUID p;

		// timeLeft and timeBetweenMessages should be in seconds
		public Countdown(int timeLeft, int timeBetweenMessages, UUID p) {
			this.timeLeft = timeLeft;
			this.timeBetweenMessages = timeBetweenMessages;
			this.p = p;
		}

		public void run() {
			Player qp = Bukkit.getPlayer(p);
			if (qp == null) {
				// player logged off.
				this.cancel();
			}
			if (timeLeft > 0) {
				qp.sendMessage(Config.getCountdownMessage().replace("%0", String.valueOf(timeLeft / 20)));
				timeLeft -= timeBetweenMessages;
			} else {
				this.cancel(); // done.
			}
		}
	}

	private static void log(String message, Object...replace) {
		getPlugin().getLogger().log(Level.INFO, message, replace);
	}

	private static void severe(String message, Throwable error) {
		getPlugin().getLogger().log(Level.SEVERE, message, error);
	}
}
