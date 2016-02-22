package com.github.soerxpso.portalfix;
import org.bukkit.configuration.file.FileConfiguration;

public class Config {

	public static FileConfiguration config;
	
	public static void setupConfig() {

		config = PortalFix.getPlugin().getConfig();
	}
	
	public static String getSpawnName() {
		try {
			return config.getString("spawn_world", "world");
		}catch(NullPointerException e) {
			return "world";
		}
	}
	
	public static int getWaitTime() {
		try {
			return config.getInt("wait_time", 120);
		}catch(NullPointerException e) {
			return 120;
		}
	}
	
	public static int getCountdownInterval() {
		try {
			return config.getInt("countdown_interval", 5);
		}catch(NullPointerException e) {
			return 5;
		}
	}

	public static int getRecheckTime() {
		try {
			return config.getInt("recheck_time", 5);
		} catch(NullPointerException e) {
			return 5;
		}
	}
	
	public static String getMessage() {
		try {
			return PortalFix.doColors(
					config.getString("teleport_message", "&c&lIf you are stuck, please stand still and wait to be teleported.&r"));
		}catch(NullPointerException e) {
			return PortalFix.doColors("&c&lIf you are stuck, please stand still and wait to be teleported.&r");
		}
	}
	
	public static String getCountdownMessage() {
		try {
			return config.getString("countdown_message", "&4&lWait %0 more seconds to be teleported.&r");
		}catch(NullPointerException e) {
			return "&4&lWait %0 more seconds to be teleported.&r";
		}
	}

	public static String getPostMessage() {
		try {
			return config.getString("post_teleport_message", "You've been sent back to spawn because you appeared to be stuck in a portal.");
		} catch(NullPointerException e) {
			return "You've been sent back to spawn because you appeared to be stuck in a portal.";
		}
	}
}
