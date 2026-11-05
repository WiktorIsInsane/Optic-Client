package gg.optic.client.render;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.model.BiPedModel;
import net.minecraft.client.render.model.ModelPart;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Slim (3px, "Alex") arms for 1.7.10, which only knows the 4px model. Each player model gets a
 * second pair of arm parts; {@link #apply} swaps them in before that model draws a player.
 * Arms are the same UVs as 1.8's slim model, on 1.7's 64x32 skin layout (left arm mirrors right).
 */
public final class OpticSlimArms {
	private static final Map<BiPedModel, ModelPart[]> ARMS = new WeakHashMap<BiPedModel, ModelPart[]>();
	/** Profile id (or name) -> slim, once known. */
	private static final Map<String, Boolean> BY_PROFILE = new java.util.HashMap<String, Boolean>();

	private OpticSlimArms() {
	}

	/** Puts slim or classic arms on the model. Pose fields are copied so nothing jumps. */
	public static void apply(BiPedModel model, boolean slim) {
		ModelPart[] arms = ARMS.get(model);
		if (arms == null) {
			arms = new ModelPart[] { model.field_1476, model.field_1477, slimArm(model, true), slimArm(model, false) };
			ARMS.put(model, arms);
		}
		ModelPart right = slim ? arms[2] : arms[0];
		ModelPart left = slim ? arms[3] : arms[1];
		if (model.field_1476 != right) {
			copyPose(model.field_1476, right);
			model.field_1476 = right;
		}
		if (model.field_1477 != left) {
			copyPose(model.field_1477, left);
			model.field_1477 = left;
		}
	}

	/**
	 * Whether this player's skin uses slim arms, as Mojang says: the arm model in the profile's
	 * signed textures, or (when the server sent none) a Mojang lookup of the name in the background.
	 * Classic until known. Pixels are not guessed from: plenty of classic skins look "slim".
	 */
	public static boolean isSlim(AbstractClientPlayerEntity player) {
		// Nick Hider shows Steve for this player, so Steve's arms too.
		if (OpticNickHider.hideSkin(player)) return false;
		GameProfile profile = player.getGameProfile();
		if (profile == null) return false;
		String key = profile.getId() != null ? profile.getId().toString() : String.valueOf(profile.getName());
		Boolean known = BY_PROFILE.get(key);
		if (known != null) return known.booleanValue();
		Boolean fromProfile = modelFromProfile(profile);
		if (fromProfile != null) {
			BY_PROFILE.put(key, fromProfile);
			return fromProfile.booleanValue();
		}
		String name = profile.getName();
		if (name == null || name.isEmpty()) return false;
		// Starts (once) a Mojang lookup; slim() answers false until it finishes.
		gg.optic.client.account.AccountHeads.skin(MinecraftClient.getInstance(), null, name);
		Boolean looked = gg.optic.client.account.AccountHeads.slimIfKnown(name);
		if (looked != null) BY_PROFILE.put(key, looked);
		return Boolean.TRUE.equals(looked);
	}

	/** True when the model currently has the slim arms on. */
	public static boolean hasSlim(BiPedModel model) {
		ModelPart[] arms = ARMS.get(model);
		return arms != null && model.field_1476 == arms[2];
	}

	private static Boolean modelFromProfile(GameProfile profile) {
		try {
			for (Property p : profile.getProperties().get("textures")) {
				String json = new String(java.util.Base64.getDecoder().decode(p.getValue()), java.nio.charset.StandardCharsets.UTF_8);
				JsonObject textures = new JsonParser().parse(json).getAsJsonObject().getAsJsonObject("textures");
				if (textures == null || !textures.has("SKIN")) return Boolean.FALSE;
				JsonObject skin = textures.getAsJsonObject("SKIN");
				return Boolean.valueOf(skin.has("metadata") && skin.getAsJsonObject("metadata").has("model")
					&& "slim".equals(skin.getAsJsonObject("metadata").get("model").getAsString()));
			}
		} catch (Exception ignored) {
		}
		return null;
	}

	private static ModelPart slimArm(BiPedModel model, boolean right) {
		ModelPart arm = new ModelPart(model, 40, 16);
		if (right) {
			arm.addCuboid(-2.0F, -2.0F, -2.0F, 3, 12, 4, 0.0F);
			arm.setPivot(-5.0F, 2.5F, 0.0F);
		} else {
			arm.mirror = true;
			arm.addCuboid(-1.0F, -2.0F, -2.0F, 3, 12, 4, 0.0F);
			arm.setPivot(5.0F, 2.5F, 0.0F);
		}
		// Not part of the model's random-part list (arrows stuck in players use it).
		model.parts.remove(arm);
		return arm;
	}

	private static void copyPose(ModelPart from, ModelPart to) {
		to.posX = from.posX;
		to.posY = from.posY;
		to.posZ = from.posZ;
		to.visible = from.visible;
		to.hide = from.hide;
	}
}
