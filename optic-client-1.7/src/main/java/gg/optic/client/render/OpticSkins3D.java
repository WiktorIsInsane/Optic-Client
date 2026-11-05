package gg.optic.client.render;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.entity.model.BiPedModel;
import net.minecraft.client.render.model.ModelPart;
import net.minecraft.client.texture.Texture;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 3D Skins for 1.7.10: draws the skin's outer layer (hat, and on 64x64 skins the jacket,
 * sleeves and pants) as a shell of one-pixel voxels around the body instead of a flat layer.
 *
 * 1.7 crops every skin to 64x32 before upload, so the uncropped image is captured when the
 * skin is parsed (see DownloadedSkinParserMixin / PlayerSkinTextureMixin) and meshed here into
 * GL display lists, one per layer, cached per skin.
 */
public final class OpticSkins3D {
	/** Implemented by PlayerSkinTexture through a mixin: the skin image before 1.7 crops it. */
	public interface RawSkin {
		BufferedImage optic$rawSkin();

		void optic$setRawSkin(BufferedImage image);
	}

	private static final float DEG = 57.295776F;
	private static final int MAX_CACHED = 48;

	// Layer ids
	private static final int HAT = 0;
	private static final int JACKET = 1;
	private static final int RIGHT_SLEEVE = 2;
	private static final int LEFT_SLEEVE = 3;
	private static final int RIGHT_PANTS = 4;
	private static final int LEFT_PANTS = 5;

	/** u, v, w, h, d, x0, y0, z0 of each overlay box (64x64 layout, base box coordinates). */
	private static final int[][] LAYERS = {
		{32, 0, 8, 8, 8, -4, -8, -4},
		{16, 32, 8, 12, 4, -4, 0, -2},
		{40, 32, 4, 12, 4, -3, -2, -2},
		{48, 48, 4, 12, 4, -1, -2, -2},
		{0, 32, 4, 12, 4, -2, 0, -2},
		{0, 48, 4, 12, 4, -2, 0, -2},
		// Slim (3px) sleeves, used when OpticSlimArms put slim arms on the model.
		{40, 32, 3, 12, 4, -2, -2, -2},
		{48, 48, 3, 12, 4, -1, -2, -2}
	};
	private static final int SLIM_RIGHT_SLEEVE = 6;
	private static final int SLIM_LEFT_SLEEVE = 7;

	/** Parsed (cropped) image -> image it was parsed from, until the texture picks it up. */
	private static final Map<BufferedImage, BufferedImage> PENDING =
		Collections.synchronizedMap(new WeakHashMap<BufferedImage, BufferedImage>());

	/** Raw skin -> display lists per layer (0 = empty layer). Render thread only. */
	private static final LinkedHashMap<BufferedImage, int[]> MESHES = new LinkedHashMap<BufferedImage, int[]>(16, 0.75F, true);

	private static boolean hatWasVisible;
	private static BufferedImage active;

	private OpticSkins3D() {
	}

	public static void rememberRaw(BufferedImage parsed, BufferedImage raw) {
		if (parsed != null && raw != null) PENDING.put(parsed, raw);
	}

	public static BufferedImage takeRaw(BufferedImage parsed) {
		return parsed == null ? null : PENDING.remove(parsed);
	}

	/** Called before the player model renders. Hides the flat hat when the 3D one will draw. */
	public static void before(Entity entity, BiPedModel model) {
		active = null;
		BufferedImage raw = rawFor(entity);
		if (raw == null) return;
		active = raw;
		OpticConfig c = OpticClient.get().config();
		hatWasVisible = model.hat.visible;
		if (c.skins3dHead) model.hat.visible = false;
	}

	/** Called after the player model rendered with the same transform. */
	public static void after(Entity entity, BiPedModel model, float scale) {
		BufferedImage raw = active;
		active = null;
		if (raw == null) return;
		model.hat.visible = hatWasVisible;
		if (entity.isInvisible()) return;

		OpticConfig c = OpticClient.get().config();
		int[] lists = meshes(raw);
		boolean texture = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		if (c.skins3dHead) draw(model.head, lists[HAT], scale);
		if (c.skins3dBody) draw(model.body, lists[JACKET], scale);
		if (c.skins3dArms) {
			boolean slim = OpticSlimArms.hasSlim(model);
			draw(model.field_1476, lists[slim ? SLIM_RIGHT_SLEEVE : RIGHT_SLEEVE], scale);
			draw(model.field_1477, lists[slim ? SLIM_LEFT_SLEEVE : LEFT_SLEEVE], scale);
		}
		if (c.skins3dLegs) {
			draw(model.field_1478, lists[RIGHT_PANTS], scale);
			draw(model.field_1479, lists[LEFT_PANTS], scale);
		}
		if (texture) GL11.glEnable(GL11.GL_TEXTURE_2D);
	}

	private static BufferedImage rawFor(Entity entity) {
		OpticConfig c = OpticClient.get().config();
		if (!c.skins3d || !(entity instanceof AbstractClientPlayerEntity)) return null;
		if (!c.skins3dHead && !c.skins3dBody && !c.skins3dArms && !c.skins3dLegs) return null;
		MinecraftClient client = MinecraftClient.getInstance();
		Entity camera = client.field_6279;
		if (camera != null && camera != entity) {
			double max = c.skins3dDistance;
			if (entity.squaredDistanceTo(camera) > max * max) return null;
		}
		Identifier id = ((AbstractClientPlayerEntity) entity).getSkinId();
		if (id == null || client.getTextureManager() == null) return null;
		Texture texture = client.getTextureManager().getTexture(id);
		if (!(texture instanceof RawSkin)) return null;
		return ((RawSkin) texture).optic$rawSkin();
	}

	/** Same transform as ModelPart.render, then the cached voxel list in model units. */
	private static void draw(ModelPart part, int list, float scale) {
		if (list == 0 || part == null || part.hide) return;
		GL11.glPushMatrix();
		GL11.glTranslatef(part.offsetX, part.offsetY, part.offsetZ);
		GL11.glTranslatef(part.pivotX * scale, part.pivotY * scale, part.pivotZ * scale);
		if (part.posZ != 0.0F) GL11.glRotatef(part.posZ * DEG, 0.0F, 0.0F, 1.0F);
		if (part.posY != 0.0F) GL11.glRotatef(part.posY * DEG, 0.0F, 1.0F, 0.0F);
		if (part.posX != 0.0F) GL11.glRotatef(part.posX * DEG, 1.0F, 0.0F, 0.0F);
		GL11.glScalef(scale, scale, scale);
		GL11.glCallList(list);
		GL11.glPopMatrix();
	}

	private static int[] meshes(BufferedImage raw) {
		int[] lists = MESHES.get(raw);
		if (lists != null) return lists;
		lists = new int[LAYERS.length];
		boolean tall = raw.getHeight() * 2 >= raw.getWidth();
		for (int i = 0; i < LAYERS.length; i++) {
			if (i != HAT && !tall) continue; // 64x32 skins only have the hat layer
			lists[i] = compile(raw, LAYERS[i], i == HAT);
		}
		MESHES.put(raw, lists);
		if (MESHES.size() > MAX_CACHED) {
			Iterator<Map.Entry<BufferedImage, int[]>> it = MESHES.entrySet().iterator();
			int[] old = it.next().getValue();
			it.remove();
			for (int list : old) if (list != 0) GL11.glDeleteLists(list, 1);
		}
		return lists;
	}

	/**
	 * Builds a 1-voxel shell around the base box. Each opaque overlay texel becomes one voxel
	 * next to the face it belongs to; only faces with an empty neighbour are emitted.
	 */
	private static int compile(BufferedImage raw, int[] layer, boolean hat) {
		int u = layer[0], v = layer[1], w = layer[2], h = layer[3], d = layer[4];
		int x0 = layer[5], y0 = layer[6], z0 = layer[7];
		int texScale = Math.max(1, raw.getWidth() / 64);
		int gw = w + 2, gh = h + 2, gd = d + 2;
		int[] cells = new int[gw * gh * gd]; // ARGB, 0 = empty

		if (hat && !hasTransparency(raw, u, v, 2 * (w + d), h + d, texScale)) return 0;

		for (int a = 0; a < w; a++) {
			for (int b = 0; b < h; b++) {
				put(cells, gw, gh, 1 + a, 1 + b, 0, texel(raw, u + d + a, v + d + b, texScale)); // front (-Z)
				put(cells, gw, gh, w - a, 1 + b, d + 1, texel(raw, u + 2 * d + w + a, v + d + b, texScale)); // back (+Z)
			}
		}
		for (int a = 0; a < d; a++) {
			for (int b = 0; b < h; b++) {
				put(cells, gw, gh, w + 1, 1 + b, 1 + a, texel(raw, u + d + w + a, v + d + b, texScale)); // +X
				put(cells, gw, gh, 0, 1 + b, d - a, texel(raw, u + a, v + d + b, texScale)); // -X
			}
		}
		for (int a = 0; a < w; a++) {
			for (int b = 0; b < d; b++) {
				put(cells, gw, gh, 1 + a, 0, d - b, texel(raw, u + d + a, v + b, texScale)); // top (-Y)
				put(cells, gw, gh, 1 + a, h + 1, d - b, texel(raw, u + d + w + a, v + b, texScale)); // bottom (+Y)
			}
		}
		fillEdges(cells, gw, gh, gd);

		boolean any = false;
		for (int cell : cells) {
			if (cell != 0) {
				any = true;
				break;
			}
		}
		if (!any) return 0;

		int list = GL11.glGenLists(1);
		if (list == 0) return 0;
		GL11.glNewList(list, GL11.GL_COMPILE);
		Tessellator t = Tessellator.INSTANCE;
		t.begin(GL11.GL_QUADS);
		for (int i = 0; i < gw; i++) {
			for (int j = 0; j < gh; j++) {
				for (int k = 0; k < gd; k++) {
					int argb = cells[index(gw, gh, i, j, k)];
					if (argb == 0) continue;
					float x1 = x0 - 1 + i, y1 = y0 - 1 + j, z1 = z0 - 1 + k;
					float x2 = x1 + 1, y2 = y1 + 1, z2 = z1 + 1;
					t.color((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, 255);
					if (open(cells, gw, gh, gd, w, h, d, i + 1, j, k)) {
						t.normal(1.0F, 0.0F, 0.0F);
						t.vertex(x2, y1, z2);
						t.vertex(x2, y1, z1);
						t.vertex(x2, y2, z1);
						t.vertex(x2, y2, z2);
					}
					if (open(cells, gw, gh, gd, w, h, d, i - 1, j, k)) {
						t.normal(-1.0F, 0.0F, 0.0F);
						t.vertex(x1, y1, z1);
						t.vertex(x1, y1, z2);
						t.vertex(x1, y2, z2);
						t.vertex(x1, y2, z1);
					}
					if (open(cells, gw, gh, gd, w, h, d, i, j - 1, k)) {
						t.normal(0.0F, -1.0F, 0.0F);
						t.vertex(x2, y1, z2);
						t.vertex(x1, y1, z2);
						t.vertex(x1, y1, z1);
						t.vertex(x2, y1, z1);
					}
					if (open(cells, gw, gh, gd, w, h, d, i, j + 1, k)) {
						t.normal(0.0F, 1.0F, 0.0F);
						t.vertex(x2, y2, z1);
						t.vertex(x1, y2, z1);
						t.vertex(x1, y2, z2);
						t.vertex(x2, y2, z2);
					}
					if (open(cells, gw, gh, gd, w, h, d, i, j, k - 1)) {
						t.normal(0.0F, 0.0F, -1.0F);
						t.vertex(x2, y1, z1);
						t.vertex(x1, y1, z1);
						t.vertex(x1, y2, z1);
						t.vertex(x2, y2, z1);
					}
					if (open(cells, gw, gh, gd, w, h, d, i, j, k + 1)) {
						t.normal(0.0F, 0.0F, 1.0F);
						t.vertex(x1, y1, z2);
						t.vertex(x2, y1, z2);
						t.vertex(x2, y2, z2);
						t.vertex(x1, y2, z2);
					}
				}
			}
		}
		t.end();
		GL11.glEndList();
		return list;
	}

	/** Fills an edge cell when both faces meeting there have a voxel, so edges don't notch. */
	private static void fillEdges(int[] cells, int gw, int gh, int gd) {
		int[] copy = cells.clone();
		for (int i = 0; i < gw; i++) {
			for (int j = 0; j < gh; j++) {
				for (int k = 0; k < gd; k++) {
					int outside = (i == 0 || i == gw - 1 ? 1 : 0) + (j == 0 || j == gh - 1 ? 1 : 0) + (k == 0 || k == gd - 1 ? 1 : 0);
					if (outside != 2 || copy[index(gw, gh, i, j, k)] != 0) continue;
					int di = i == 0 ? 1 : i == gw - 1 ? -1 : 0;
					int dj = j == 0 ? 1 : j == gh - 1 ? -1 : 0;
					int dk = k == 0 ? 1 : k == gd - 1 ? -1 : 0;
					int[] pair = new int[2];
					int n = 0;
					if (di != 0) pair[n++] = copy[index(gw, gh, i + di, j, k)];
					if (dj != 0) pair[n++] = copy[index(gw, gh, i, j + dj, k)];
					if (dk != 0 && n < 2) pair[n++] = copy[index(gw, gh, i, j, k + dk)];
					if (n == 2 && pair[0] != 0 && pair[1] != 0) cells[index(gw, gh, i, j, k)] = pair[0];
				}
			}
		}
	}

	/** True when the neighbour cell is empty and not part of the base body box. */
	private static boolean open(int[] cells, int gw, int gh, int gd, int w, int h, int d, int i, int j, int k) {
		if (i < 0 || j < 0 || k < 0 || i >= gw || j >= gh || k >= gd) return true;
		if (i >= 1 && i <= w && j >= 1 && j <= h && k >= 1 && k <= d) return false;
		return cells[index(gw, gh, i, j, k)] == 0;
	}

	private static void put(int[] cells, int gw, int gh, int i, int j, int k, int argb) {
		if (((argb >>> 24) & 0xFF) < 0x80) return;
		cells[index(gw, gh, i, j, k)] = argb | 0xFF000000;
	}

	private static int index(int gw, int gh, int i, int j, int k) {
		return (k * gh + j) * gw + i;
	}

	private static int texel(BufferedImage raw, int x, int y, int scale) {
		int px = x * scale + scale / 2;
		int py = y * scale + scale / 2;
		if (px < 0 || py < 0 || px >= raw.getWidth() || py >= raw.getHeight()) return 0;
		return raw.getRGB(px, py);
	}

	/** Vanilla ignores a hat layer with no transparent pixels (old skins filled it solid). */
	private static boolean hasTransparency(BufferedImage raw, int u, int v, int w, int h, int scale) {
		for (int x = u; x < u + w; x++) {
			for (int y = v; y < v + h; y++) {
				if (((texel(raw, x, y, scale) >>> 24) & 0xFF) < 0x80) return true;
			}
		}
		return false;
	}
}
