package com.github.taiyaki.stealpingplugin;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.bukkit.plugin.java.JavaPlugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class JsonFileManager<T>{
	private final JavaPlugin plugin;
	private final File file;
	private final Class<T> clazz;
	private final Supplier<T> defaultSupplier;
	private final Gson gson;
	private T data;
	
	public JsonFileManager(JavaPlugin plugin, String fileName, Class<T> clazz, Supplier<T> defaultSupplier) {
		this.plugin = plugin;
		this.file = new File(plugin.getDataFolder(), fileName);
		this.clazz = clazz;
		this.defaultSupplier = defaultSupplier;
		this.gson = new GsonBuilder().setPrettyPrinting().create();
		
		load();
	}
	
	// JSONファイルを読み込む(なければ初期値で自動生成)
	public synchronized void load() {
		if(!plugin.getDataFolder().exists()) {
			plugin.getDataFolder().mkdirs();
		}
		if(!file.exists()) {
			this.data = defaultSupplier.get();
			save();
			return;
		}
		
		try(FileReader reader = new FileReader(file, StandardCharsets.UTF_8)){
			T loaded = gson.fromJson(reader, clazz);
			this.data = (loaded != null) ? loaded : defaultSupplier.get();
		} catch(IOException e) {
			plugin.getLogger().severe(file.getName()+" の読み込みに失敗しました: " + e.getMessage());
			this.data = defaultSupplier.get();
		}
	}
	
	// 現在のデータをJSONファイルに同期保存する。
	public synchronized void save() {
		try(FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)){
			gson.toJson(this.data, writer);
		} catch(IOException e) {
			plugin.getLogger().severe(file.getName() + " の保存に失敗しました: "+e.getMessage());
		}
	}
	
	
}