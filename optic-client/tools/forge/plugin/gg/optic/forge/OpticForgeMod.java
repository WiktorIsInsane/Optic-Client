package gg.optic.forge;

import gg.optic.client.OpticClient;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(modid = "optic", name = "Optic", version = "1.0.0", clientSideOnly = true, acceptedMinecraftVersions = "[1.8.9]")
public class OpticForgeMod {
	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		OpticClient.get().onInitializeClient();
	}
}
