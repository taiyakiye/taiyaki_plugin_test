package com.github.taiyaki.stealpingplugin.manager;

import com.github.taiyaki.stealpingplugin.StealPingPlugin;
import com.github.taiyaki.stealpingplugin.entities.Binder;
import com.github.taiyaki.stealpingplugin.entities.PlayerBinderData;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;

public class BinderManager{
	private final StealPingPlugin plugin;
	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private final File binderDataFolder;
	
	// 召喚中のセッション管理
	private final Map<UUID, BinderSession> activeSessions = new HashMap<>();
	// プレイヤーごとのカードデータキャッシュ
	private final Map<UUID, PlayerBinderData> playerDataCache = new HashMap<>();
	
	// バインダー設定キャッシュ
	private Binder binderConfig;
	private Particle cachedParticleType;
	private Transformation cachedBookTransform;
	private ItemStack[] cachedBookModels;
	
	// 召喚中のエンティティ群をまとめる内部クラス
	private static class BinderSession{
		ItemDisplay bookDisplay;
		Map<Integer, ItemDisplay> cardDisplays = new HashMap<>();
		Map<UUID, Integer> interactionToSlot = new HashMap<>();
		Map<Integer, Interaction> slotToInteraction = new HashMap<>();
	}
}