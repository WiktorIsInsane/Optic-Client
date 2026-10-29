package gg.optic.client.render;

import gg.optic.client.OpticClient;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.Arrays;

/**
 * Skips drawing entities that are fully behind solid blocks. The test is the
 * same idea as occlusion culling: walk the voxels from the camera to the
 * target, and hide it only when every sample hits a full block first.
 * Rendering is the only thing that changes. Nothing is sent to the server.
 */
public final class OpticEntityCull {
	private static final int REACH = 48;
	private static final int DIAM = REACH * 2 + 1;
	private static final int VOLUME = DIAM * DIAM * DIAM;
	private static final double NEAR = 2.5D;

	private static final BlockPos.Mutable POS = new BlockPos.Mutable();

	private static byte[] cache;
	private static int originX;
	private static int originY;
	private static int originZ;
	private static int anchorX = Integer.MIN_VALUE;
	private static int anchorY = Integer.MIN_VALUE;
	private static int anchorZ = Integer.MIN_VALUE;

	private static Entity camera;
	private static double camX;
	private static double camY;
	private static double camZ;
	private static boolean ready;

	private OpticEntityCull() {
	}

	public static void beginFrame(Entity view, float tickDelta) {
		ready = false;
		camera = view;
		if (view == null || !OpticClient.get().config().entityCulling) return;
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.world == null) return;
		float delta = tickDelta;
		if (delta < 0.0F) delta = 0.0F;
		if (delta > 1.0F) delta = 1.0F;
		camX = view.prevX + (view.x - view.prevX) * delta;
		camY = view.prevY + (view.y - view.prevY) * delta + view.getEyeHeight();
		camZ = view.prevZ + (view.z - view.prevZ) * delta;
		int cx = floor(camX);
		int cy = floor(camY);
		int cz = floor(camZ);
		if (cache == null
			|| Math.abs(cx - anchorX) > 12
			|| Math.abs(cy - anchorY) > 12
			|| Math.abs(cz - anchorZ) > 12) {
			anchorX = cx;
			anchorY = cy;
			anchorZ = cz;
			originX = cx - REACH;
			originY = cy - REACH;
			originZ = cz - REACH;
			if (cache == null || cache.length != VOLUME) cache = new byte[VOLUME];
			else Arrays.fill(cache, (byte) 0);
		}
		ready = true;
	}

	public static boolean hiddenEntity(Entity entity) {
		if (!ready || entity == null || entity == camera) return false;
		if (entity == camera.vehicle || entity.vehicle == camera) return false;
		Box box = entity.getBoundingBox();
		if (box == null) return false;
		return hiddenBox(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
	}

	public static boolean hiddenBlock(BlockEntity blockEntity) {
		if (!ready || blockEntity == null) return false;
		BlockPos pos = blockEntity.getPos();
		if (pos == null) return false;
		double x = pos.getX();
		double y = pos.getY();
		double z = pos.getZ();
		return hiddenBox(x, y, z, x + 1.0D, y + 1.0D, z + 1.0D);
	}

	private static boolean hiddenBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		if (camX >= minX - 0.5D && camX <= maxX + 0.5D
			&& camY >= minY - 0.5D && camY <= maxY + 0.5D
			&& camZ >= minZ - 0.5D && camZ <= maxZ + 0.5D) {
			return false;
		}
		double midX = (minX + maxX) * 0.5D;
		double midY = (minY + maxY) * 0.5D;
		double midZ = (minZ + maxZ) * 0.5D;
		double dx = midX - camX;
		double dy = midY - camY;
		double dz = midZ - camZ;
		if (dx * dx + dy * dy + dz * dz > (double) REACH * REACH) return false;
		double nearX = clamp(camX, minX, maxX);
		double nearY = clamp(camY, minY, maxY);
		double nearZ = clamp(camZ, minZ, maxZ);
		double nx = nearX - camX;
		double ny = nearY - camY;
		double nz = nearZ - camZ;
		if (nx * nx + ny * ny + nz * nz < NEAR * NEAR) return false;
		double topY = Math.min(maxY - 0.05D, minY + 1.62D);
		if (topY < minY) topY = midY;
		return !sees(midX, midY, midZ)
			&& !sees(midX, topY, midZ)
			&& !sees(nearX, nearY, nearZ);
	}

	private static boolean sees(double x, double y, double z) {
		return rayClear(camX, camY, camZ, x, y, z);
	}

	/** True when the voxels between the camera and the target cell are open. */
	private static boolean rayClear(double x0, double y0, double z0, double x1, double y1, double z1) {
		int x = floor(x0);
		int y = floor(y0);
		int z = floor(z0);
		int tx = floor(x1);
		int ty = floor(y1);
		int tz = floor(z1);
		if (x == tx && y == ty && z == tz) return true;

		double dx = x1 - x0;
		double dy = y1 - y0;
		double dz = z1 - z0;
		int stepX = dx > 0.0D ? 1 : (dx < 0.0D ? -1 : 0);
		int stepY = dy > 0.0D ? 1 : (dy < 0.0D ? -1 : 0);
		int stepZ = dz > 0.0D ? 1 : (dz < 0.0D ? -1 : 0);
		double tDeltaX = stepX == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0D / dx);
		double tDeltaY = stepY == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0D / dy);
		double tDeltaZ = stepZ == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0D / dz);
		double tMaxX = stepX > 0 ? (x + 1.0D - x0) * tDeltaX : (stepX < 0 ? (x0 - x) * tDeltaX : Double.POSITIVE_INFINITY);
		double tMaxY = stepY > 0 ? (y + 1.0D - y0) * tDeltaY : (stepY < 0 ? (y0 - y) * tDeltaY : Double.POSITIVE_INFINITY);
		double tMaxZ = stepZ > 0 ? (z + 1.0D - z0) * tDeltaZ : (stepZ < 0 ? (z0 - z) * tDeltaZ : Double.POSITIVE_INFINITY);

		int guard = REACH + 4;
		while (guard-- > 0) {
			if (tMaxX < tMaxY) {
				if (tMaxX < tMaxZ) {
					x += stepX;
					tMaxX += tDeltaX;
				} else {
					z += stepZ;
					tMaxZ += tDeltaZ;
				}
			} else if (tMaxY < tMaxZ) {
				y += stepY;
				tMaxY += tDeltaY;
			} else {
				z += stepZ;
				tMaxZ += tDeltaZ;
			}
			if (x == tx && y == ty && z == tz) return true;
			if (solid(x, y, z)) return false;
		}
		return true;
	}

	private static boolean solid(int x, int y, int z) {
		if (y < 0 || y > 255) return false;
		int lx = x - originX;
		int ly = y - originY;
		int lz = z - originZ;
		if (lx < 0 || ly < 0 || lz < 0 || lx >= DIAM || ly >= DIAM || lz >= DIAM) return false;
		int index = lx + lz * DIAM + ly * DIAM * DIAM;
		byte cached = cache[index];
		if (cached != 0) return cached == 2;
		boolean opaque = false;
		MinecraftClient client = MinecraftClient.getInstance();
		World world = client == null ? null : client.world;
		if (world != null) {
			POS.setPosition(x, y, z);
			if (world.isLoaded(POS, false)) {
				BlockState state = world.getBlockState(POS);
				Block block = state == null ? null : state.getBlock();
				if (block != null) {
					int opacity = block.getOpacity();
					opaque = opacity >= 255 || (opacity > 0 && block.isFullCube());
				}
			}
		}
		cache[index] = opaque ? (byte) 2 : (byte) 1;
		return opaque;
	}

	private static double clamp(double value, double min, double max) {
		if (value < min) return min;
		if (value > max) return max;
		return value;
	}

	private static int floor(double value) {
		int i = (int) value;
		return value < i ? i - 1 : i;
	}
}
