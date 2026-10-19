package com.github.taiyaki.stealpingplugin;

import java.io.File;
import java.lang.System.Logger;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class IpWhitelistManager {
	private final Set<String> allowedIps = ConcurrentHashMap.newKeySet();
	private final File dataFolder;
	private final File ipFile;
	private final Logger logger;
	
	public IpWhitelistManager(File dataFolder, Logger logger) {
		this.dataFolder = dataFolder;
		this.logger = logger;
		this.ipFile = new File(dataFolder, "allowed_ips.txt");
		
		// 初期化時にファイルを読み込む
		loadFromFile();
		
		
	}
}
