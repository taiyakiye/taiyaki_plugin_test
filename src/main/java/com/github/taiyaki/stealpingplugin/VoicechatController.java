package com.github.taiyaki.stealpingplugin;

import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

public class VoicechatController implements VoicechatPlugin {

    private static VoicechatServerApi voicechatApi;

    @Override
    public String getPluginId() {
        return "stealpingplugin_voicechat";
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStarted);
    }

    private void onServerStarted(VoicechatServerStartedEvent event) {
        voicechatApi = event.getVoicechat();
        Bukkit.getLogger().info("[VoicechatController] Simple Voice Chat API が正常に接続されました。");
    }

    /**
     * .wav ファイルを 48kHz モノラル PCM にリサンプリングし、SVC 経由でプレイヤーの頭上から再生する
     */
    public static void playWavToPlayer(Path wavPath, Player targetPlayer) {
        if (voicechatApi == null) {
            Bukkit.getLogger().warning("[VoicechatController] voicechatApi が null です (SVC未初期化)");
            return;
        }

        try {
            File file = wavPath.toFile();
            if (!file.exists()) return;

            // 1. WAV を SVC の必須規格（48kHz / 16bit / 1ch モノラル / Little-Endian）にリサンプリング変換
            AudioInputStream originalStream = AudioSystem.getAudioInputStream(file);
            AudioFormat targetFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    48000.0f, // 48kHz
                    16,       // 16bit
                    1,        // 1ch (モノラル)
                    2,        // frameSize
                    48000.0f,
                    false     // little-endian
            );

            AudioInputStream pcmStream = AudioSystem.getAudioInputStream(targetFormat, originalStream);
            byte[] bytes = pcmStream.readAllBytes();

            // 16bit PCM データを short[] へ変換
            short[] pcmData = new short[bytes.length / 2];
            for (int i = 0; i < pcmData.length; i++) {
                pcmData[i] = (short) ((bytes[i * 2 + 1] << 8) | (bytes[i * 2] & 0xff));
            }

            Bukkit.getLogger().info("[VoicechatController] 48kHz モノラル変換完了. サンプル数: " + pcmData.length);

            // 2. プレイヤーを追従するエンティティオーディオチャネルを作成
            var entity = voicechatApi.fromEntity(targetPlayer);
            var channel = voicechatApi.createEntityAudioChannel(
                    java.util.UUID.randomUUID(),
                    entity
            );

            if (channel == null) {
                Bukkit.getLogger().warning("[VoicechatController] チャネル生成に失敗しました");
                return;
            }

            // 3. 20ms 分のフレーム（48000Hz * 0.02s = 960 サンプル）単位で送出
            final int frameSize = 960;
            final AtomicInteger offset = new AtomicInteger(0);

            // AudioPlayer を生成してバックグラウンド再生タスクを即時起動
            AudioPlayer player = voicechatApi.createAudioPlayer(
                    channel,
                    voicechatApi.createEncoder(),
                    () -> {
                        int currentOffset = offset.get();
                        if (currentOffset >= pcmData.length) {
                            return null; // 全フレーム送信完了（自動停止）
                        }

                        int length = Math.min(frameSize, pcmData.length - currentOffset);
                        short[] frame = new short[frameSize];
                        System.arraycopy(pcmData, currentOffset, frame, 0, length);
                        offset.addAndGet(frameSize);

                        return frame;
                    }
            );
            player.startPlaying();

        } catch (Exception e) {
            Bukkit.getLogger().warning("[VoicechatController] 再生処理例外: " + e.getMessage());
            e.printStackTrace();
        }
    }
}