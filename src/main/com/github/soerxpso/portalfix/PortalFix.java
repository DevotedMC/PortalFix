package com.github.soerxpso.portalfix;

import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class PortalFix extends JavaPlugin implements Listener {

	private static PortalFix plugin;
	
	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
		plugin = this;
		Config.setupConfig();
	}
	
	public static PortalFix getPlugin() {
		return plugin;
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerPortalUse(PlayerPortalEvent event) {
		if(!event.isCancelled()) {
			Player p = event.getPlayer();
			p.sendMessage(Config.getMessage());
			new TeleportIfStill(p).runTaskLater(plugin, Config.getWaitTime() * 20);
		}
	}
	
	public class TeleportIfStill extends BukkitRunnable implements Listener {
		private Player p;
		
		public TeleportIfStill(Player p) {
			this.p = p;
			getServer().getPluginManager().registerEvents(this, PortalFix.getPlugin());
		}
		
		@EventHandler(priority = EventPriority.MONITOR)
		public void onPlayerMove(PlayerMoveEvent event) {
			if(!event.isCancelled()) {
				if(event.getPlayer().equals(p)) {
					//Don't include the PlayerPortalEvent from the player using the portal
					//(PlayerPortalEvent extends PlayerMoveEvent)
					if(event.getClass() != PlayerPortalEvent.class) {
						this.cancel();
					}
				}
			}
		}
		
		@Override
		public void run() {
			Location oloc = p.getLocation();
			p.teleport(getServer().getWorld(Config.getSpawnName()).getSpawnLocation());
			log("Teleported player from portal at {0} to {1}", oloc, p.getLocation());
		}
	}
	
	private static void log(String message, Object...replace) {
		PortalFix.getPlugin().getLogger().log(Level.INFO, message, replace);
	}
}
