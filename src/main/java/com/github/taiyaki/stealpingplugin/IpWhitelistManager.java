package com.github.taiyaki.stealpingplugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
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
	
	/** ファイルからIP一覧をロード */
	public synchronized void loadFromFile() {
		try {
			if(!dataFolder.exists()) {
				dataFolder.mkdirs();
			}
			if(!ipFile.exists()) {
				ipFile.createNewFile();
				return;
			}
			
			allowedIps.clear();
			List<String> lines = Files.readAllLines(ipFile.toPath(),StandardCharsets.UTF_8);
			for(String line:lines) {
				String ip = line.trim();
				if(!ip.isEmpty() && !ip.startsWith("#")) {
					allowedIps.add(ip);
				}
			}
			logger.info("IPリストファイルから "+allowedIps.size()+" 件の許可IPを読み込みました。");
		} catch(IOException e) {
			logger.info("IPリストファイルの読み込みに失敗しました: "+e.getMessage());
		}
	}
	
	/** IPを登録してファイルに同期 */
	public synchronized boolean addIp(String ip) {
		if(allowedIps.add(ip)) {
			saveToFile();
			return true;
		}
		return false;
	}
	
	/** IPを削除してファイルに同期*/
	public synchronized boolean removeIp(String ip) {
		if(allowedIps.remove(ip)) {
			saveToFile();
			return true;
		}
		return false;
	}
	
	/** Setの内容をファイルへ保存 */
	private synchronized void saveToFile() {
		try {
			if(!dataFolder.exists()) {
				dataFolder.mkdirs();
			}
			Files.write(
					ipFile.toPath(),
					allowedIps,
					StandardCharsets.UTF_8,
					StandardOpenOption.CREATE,
					StandardOpenOption.TRUNCATE_EXISTING,
					StandardOpenOption.WRITE
				);
		} catch(IOException e) {
			logger.info("IPリストファイルの保存に失敗しました: "+ e.getMessage());
		}
	}
	
	/** 許可されているか判定 */
	public boolean isAllowed(String ip) {
		return allowedIps.contains(ip);
	}
}
