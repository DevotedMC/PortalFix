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
	
	public static String getMessage() {
		try {
			return config.getString("teleport_message", "If you are stuck, please stand still and wait to be teleported.");
		}catch(NullPointerException e) {
			return "If you are stuck, please stand still and wait to be teleported.";
		}
	}
}
