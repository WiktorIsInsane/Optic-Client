package net.fabricmc.api;

/** Present so the Fabric entrypoint class can load on Forge. Forge calls it from OpticForgeMod. */
public interface ClientModInitializer {
	void onInitializeClient();
}
