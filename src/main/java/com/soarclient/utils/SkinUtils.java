package com.soarclient.utils;

import java.io.File;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

public class SkinUtils {

    /** 1.21.1 没有可用的 assetDir 访问器，退回到 runs/ 下的 skins 目录。 */
    public static File getSkin(Identifier identifier) {
        String fileName = identifier.getPath().replace("skins/", "");
        String folder = fileName.substring(0, 2);
        File base = new File(MinecraftClient.getInstance().runDirectory, "assets");
        return new File(base, "skins/" + folder + "/" + fileName);
    }
}
