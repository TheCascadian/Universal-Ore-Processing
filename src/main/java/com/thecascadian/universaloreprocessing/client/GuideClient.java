package com.thecascadian.universaloreprocessing.client;

import com.thecascadian.universaloreprocessing.guide.GuideContent;
import net.minecraft.client.Minecraft;

/** Client-only entry point, kept separate so the item class never loads screen code on a server. */
public final class GuideClient {

    private GuideClient() {
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new GuideScreen(GuideContent.build()));
    }
}
