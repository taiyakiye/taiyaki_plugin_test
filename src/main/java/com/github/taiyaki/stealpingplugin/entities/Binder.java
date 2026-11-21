package com.github.taiyaki.stealpingplugin.entities;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.bukkit.Sound;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Binder{
	// 別の階層(BinderCommand)からアクセスできるように public をつける
	public double scale = 1.0;
	public long delay = 0L;
	
	public Translation translation = new Translation();
	public playSound playsound = new playSound();
	public spawnParticle spawnParticle = new spawnParticle();
	
	public static class playSound{
		public String sound = "";
		public double volume = 1.0f;
		public double pitch = 1.0f;
		
	}
	
	public static class spawnParticle{
		public String particle = "";
		public double count = 0.0f;
		public Offset delta = new Offset();
		public double speed = 0.0f;
	}
	
	public static class Offset{
		public double x=0.0;
		public double y=0.0;
		public double z=0.0;
	}
	
	public static class Translation{
		public double x=0.0;
		public double y=0.0;
		public double z=0.0;
	}
	
	
	// ファイルが存在しない場合に、初期化の binder.json　を作成するメソッド
	public static Binder createDefault(File file) throws IOException{
		File parent = file.getParentFile();
		if(parent != null && !parent.exists()) {
			parent.mkdirs();
		}
		
		Binder defaultBinder = new Binder();
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		
		try(FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)){
			gson.toJson(defaultBinder, writer);
		}
		
		return defaultBinder;
	}
}