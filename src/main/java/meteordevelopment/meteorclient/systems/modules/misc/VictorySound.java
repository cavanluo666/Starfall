/*
 * Meteor + Soar 合并版 —— 自研实现，GPL-3.0。
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.sound.SoundEvents;

/**
 * 胜利音效：检测到胜利 / 击杀提示时播放一段提示音。
 *
 * <p>音效都取自原版音效表，不引入外部音频资源。
 */
public class VictorySound extends Module {

    public enum Tone {
        清脆高音,
        低沉钟声,
        经验提升,
        末影龙咆哮
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Tone> tone = sgGeneral.add(new EnumSetting.Builder<Tone>()
        .name("音效")
        .description("播放哪种提示音。")
        .defaultValue(Tone.经验提升)
        .build()
    );

    private final Setting<Double> volume = sgGeneral.add(new DoubleSetting.Builder()
        .name("音量")
        .description("音量倍率。")
        .defaultValue(1.0).min(0).sliderRange(0, 2)
        .build()
    );

    private final Setting<String> triggers = sgGeneral.add(new StringSetting.Builder()
        .name("触发关键词")
        .description("用逗号分隔的触发词。")
        .defaultValue("victory,winner,胜利,游戏结束")
        .build()
    );

    private final Setting<Integer> cooldown = sgGeneral.add(new IntSetting.Builder()
        .name("冷却")
        .description("两次播放的最小间隔（秒）。")
        .defaultValue(5).min(1).sliderRange(1, 120)
        .build()
    );

    private final Setting<Boolean> notify = sgGeneral.add(new BoolSetting.Builder()
        .name("聊天提示")
        .description("播放时在聊天栏提示。")
        .defaultValue(false)
        .build()
    );

    private long lastPlay = 0;

    public VictorySound() {
        super(Categories.Misc, "victory-sound", "检测到胜利提示时播放音效。");
    }

    @EventHandler
    private void onReceiveMessage(ReceiveMessageEvent event) {
        if (mc.player == null) return;
        if (System.currentTimeMillis() - lastPlay < cooldown.get() * 1000L) return;

        String msg = event.getMessage().getString().toLowerCase();
        for (String t : triggers.get().split(",")) {
            String k = t.trim().toLowerCase();
            if (!k.isEmpty() && msg.contains(k)) {
                play();
                return;
            }
        }
    }

    /** 手动播放一次。 */
    public void play() {
        if (mc.player == null) return;
        lastPlay = System.currentTimeMillis();

        float v = volume.get().floatValue();
        switch (tone.get()) {
            case 清脆高音 -> mc.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), v, 1.6F);
            case 低沉钟声 -> mc.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), v, 0.8F);
            case 经验提升 -> mc.player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, v, 1.0F);
            case 末影龙咆哮 -> mc.player.playSound(SoundEvents.ENTITY_ENDER_DRAGON_GROWL, v, 1.0F);
        }

        if (notify.get()) info("播放胜利音效");
    }
}
