package com.synergy902.projectrose;

import com.mojang.logging.LogUtils;
import com.synergy902.projectrose.config.RoseConfig;
import com.synergy902.projectrose.config.RoseClientConfig;
import com.synergy902.projectrose.network.RoseNetwork;
import com.synergy902.projectrose.menu.RoseMenus;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(ProjectRose.MOD_ID)
public final class ProjectRose {
    public static final String MOD_ID = "projectrose";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ProjectRose() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        RoseMenus.register(modBus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, RoseConfig.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, RoseClientConfig.SPEC);
        RoseNetwork.initialize();
    }
}
