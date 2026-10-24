package gg.optic.client;

import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public final class OpticConfig {
	public static final int DEFAULT_ACCENT = 0xFFFFFF;
	public static final int DEFAULT_TEXT = 0xFFFFFF;

	// Master toggles — all mods start off.
	public boolean fps = false;
	public boolean cps = false;
	public boolean keystrokes = false;
	public boolean coordinates = false;
	public boolean ping = false;
	public boolean toggleSprint = false;
	public boolean fullbright = false;
	public boolean blockOverlay = false;
	public boolean nametags = false;
	public boolean chat = false;
	public boolean scoreboard = false;
	public boolean hotbar = false;
	public boolean timeChanger = false;
	public boolean weatherChanger = false;
	public boolean hurtCam = false;
	/** Badlion-style hold-to-zoom (default key C). */
	public boolean zoom = false;
	/** LWJGL key code; 46 = C. */
	public int zoomKeyCode = 46;
	/** Base zoom strength (FOV divisor); Badlion "Zoom Amount". */
	public float zoomFactor = 4.0F;
	/** Smooth FOV transition when pressing/releasing zoom. */
	public boolean zoomSmooth = true;
	/** Cinematic camera while zoomed — sticky / hard-to-control feel. Off by default for PvP. */
	public boolean zoomSmoothCamera = false;
	/** Scale mouse sens with zoom so aim speed feels consistent. */
	public boolean zoomMouseSensitivity = true;
	/** Scroll wheel changes zoom level while holding zoom. */
	public boolean zoomScroll = true;
	/** How fast scroll changes zoom (0.05–1). */
	public float zoomScrollSpeed = 0.15F;
	/** Hold a key to orbit the camera. The player yaw sent to the server stays put. */
	public boolean freeLook = false;
	/** Left Alt. Negative codes are mouse buttons, matching Minecraft (-100 + button). */
	public int freeLookKeyCode = 56;
	/** Press to stay in free look until the key is pressed again. */
	public boolean freeLookToggle = false;
	public float freeLookDistance = 4.0F;

	// Hitboxes
	public boolean hitboxes = false;
	/** Hide the blue look-direction line. */
	public boolean hitboxHideLook = false;
	public int hitboxPlayerColor = 0xFFFFFF;
	public int hitboxMobColor = 0x000000;
	public int hitboxArrowColor = 0xFFFF00;
	public int hitboxOtherColor = 0xFFFFFF;

	/**
	 * Visual 1.8 hit prediction. Off by default. Does not change attacks.
	 */
	public boolean hitPredict = false;

	// FPS
	public boolean fpsShowLabel = true;
	public boolean fpsBrackets = true;
	public boolean fpsTextShadow = true;
	public boolean fpsOpticFont = false;
	public float fpsScale = 1.0F;
	public int fpsColor = DEFAULT_ACCENT;
	public int fpsTextColor = DEFAULT_TEXT;
	public int fpsX = 6;
	public int fpsY = 6;

	// CPS
	public boolean cpsSeparateButtons = true;
	public boolean cpsShowLabel = true;
	public boolean cpsBrackets = true;
	public boolean cpsTextShadow = true;
	public boolean cpsOpticFont = false;
	public float cpsScale = 1.0F;
	public int cpsColor = DEFAULT_ACCENT;
	public int cpsTextColor = DEFAULT_TEXT;
	public int cpsX = 6;
	public int cpsY = 21;

	// Keystrokes
	public boolean keystrokesShowMouse = true;
	public boolean keystrokesModernStyle = true;
	public boolean keystrokesTextShadow = true;
	public boolean keystrokesOpticFont = false;
	public boolean keystrokesBackground = true;
	public boolean keystrokesShowLabels = true;
	public boolean keystrokesInstant = false;
	public boolean keystrokesFade = true;
	public boolean keystrokesLockKeys = true;
	public float keystrokesFadeMs = 200.0F;
	/** 0=Off, 1=Replace label, 2=Under label, 3=While clicking */
	public int keystrokesCpsMode = 0;
	public float keystrokesScale = 1.0F;
	public int keystrokesColor = DEFAULT_ACCENT;
	public int keystrokesTextColor = DEFAULT_TEXT;
	public int keystrokesX = 6;
	public int keystrokesY = -1; // -1 = centered vertically at runtime
	/** Base-unit offsets for W,A,S,D,LMB,RMB (scaled at render). */
	public float keystrokesOffX0 = 21.0F;
	public float keystrokesOffY0 = 0.0F;
	public float keystrokesOffX1 = 0.0F;
	public float keystrokesOffY1 = 21.0F;
	public float keystrokesOffX2 = 21.0F;
	public float keystrokesOffY2 = 21.0F;
	public float keystrokesOffX3 = 42.0F;
	public float keystrokesOffY3 = 21.0F;
	public float keystrokesOffX4 = 0.0F;
	public float keystrokesOffY4 = 42.0F;
	public float keystrokesOffX5 = 32.0F;
	public float keystrokesOffY5 = 42.0F;

	// Ping
	public boolean pingShowMs = true;
	public boolean pingShowLabel = true;
	public boolean pingBrackets = true;
	public boolean pingTextShadow = true;
	public boolean pingOpticFont = false;
	public float pingScale = 1.0F;
	public int pingColor = DEFAULT_ACCENT;
	public int pingTextColor = DEFAULT_TEXT;
	public int pingX = 6;
	public int pingY = 36;

	// Coordinates
	public boolean coordinatesShowY = true;
	public boolean coordinatesShowLabel = true;
	public boolean coordinatesBrackets = true;
	public boolean coordinatesTextShadow = true;
	public boolean coordinatesOpticFont = false;
	public float coordinatesScale = 1.0F;
	public int coordinatesDecimals = 0;
	public int coordinatesColor = DEFAULT_ACCENT;
	public int coordinatesTextColor = DEFAULT_TEXT;
	public int coordinatesX = 6;
	public int coordinatesY = 51;

	// Toggle sprint
	public boolean toggleSprintShowHud = false;
	public boolean toggleSprintBrackets = true;
	public boolean toggleSprintTextShadow = true;
	public boolean toggleSprintOpticFont = false;
	public float toggleSprintScale = 1.0F;
	public int toggleSprintColor = DEFAULT_ACCENT;
	public int toggleSprintTextColor = DEFAULT_TEXT;
	public int toggleSprintX = 6;
	public int toggleSprintY = 66;

	// Fullbright
	public float fullbrightGamma = 16.0F;

	// Animations (Badlion-style 1.7 options; ON = 1.7 behavior)
	public boolean animations = true;
	public boolean animHitbox = true;
	public boolean animInventory = true;
	public boolean animHealth = true;
	public boolean animDamage = true;
	public boolean animSneaking = true;
	public boolean animOldEnchantGlint = true;
	public boolean anim18Skins = true;
	public boolean animTitles = true;
	public boolean animFixGhostLiquid = false;
	public boolean animItemHold = true;
	public boolean animItemSwitch = true;
	public boolean animBlockHit = true;
	public boolean animEating = true;
	public boolean animThrowing = true;
	public boolean animFishingRod = true;
	public boolean animSwordSwing = true;
	public boolean animBow = true;
	public boolean animLeftHand = false;
	public boolean animMovableTitles = false;
	public int titleOffsetX = 0;
	public int titleOffsetY = 0;

	// Particles
	public boolean particles = false;
	public boolean showCriticalHitParticles = true;
	public boolean alwaysSharpnessParticles = true;
	public float particleMultiplier = 1.0F;
	public boolean removeVanillaColorFilter = false;
	public boolean renderOwnParticles = true;
	public boolean blockBreakParticles = true;

	// Block Overlay
	public boolean blockOverlayFill = true;
	public int blockOverlayFillColor = 0xFFFFFF;
	public float blockOverlayFillAlpha = 0.25F;
	public boolean blockOverlayOutline = true;
	public int blockOverlayOutlineColor = 0x000000;
	public float blockOverlayOutlineAlpha = 1.0F;
	public float blockOverlayLineWidth = 2.0F;

	// Nametags
	public boolean nametagsShowInF5 = true;
	public boolean nametagsShowInF1 = false;
	public boolean nametagsTextShadow = true;
	public boolean nametagsBadlionFont = false;
	public boolean nametagsRoundedCorners = false;
	public boolean nametagsBackground = true;
	public int nametagsBackgroundColor = 0x000000;
	public int nametagsPersonalColor = 0xFFFFFF;
	/** Per-player name text colors (username -> RGB). */
	public final LinkedHashMap<String, Integer> nametagNameColors = new LinkedHashMap<String, Integer>();

	// Chat
	public boolean chatBold = false;
	public boolean chatAntiSpam = false;
	public boolean chatInfiniteHistory = false;
	/** 0–100; 0 = no chat background. */
	public float chatBackgroundOpacity = 100.0F;

	// Scoreboard
	public boolean scoreboardShowNumbers = true;
	public boolean scoreboardRemoveNumberWidth = false;
	public boolean scoreboardTextShadow = false;
	/** 0–100; 0 = no scoreboard background. */
	public float scoreboardBackgroundOpacity = 100.0F;
	/** Top-left of scoreboard box; -1 = vanilla right-side position. */
	public int scoreboardX = -1;
	public int scoreboardY = -1;

	// Hotbar
	public float hotbarScale = 1.0F;
	/** 0 / 90 / 180 / 270 — bar rotates; items stay upright. */
	public int hotbarRotation = 0;
	/** Top-left of hotbar; -1 = vanilla center-bottom. */
	public int hotbarX = -1;
	public int hotbarY = -1;

	// Time Changer
	/** 0 Default, 1 Nether, 2 End. */
	public int skyType = 0;
	public boolean freezeTime = false;
	/** 0 Day, 1 Noon, 2 Sunset, 3 Midnight, 4 Custom. */
	public int timePreset = OpticEnvironment.PRESET_MIDNIGHT;
	/** Frozen time of day in ticks (0–23999). */
	public float timeOfDay = 18000.0F;

	// Weather Changer
	/** 0 Clear, 1 Rain, 2 Snow, 3 Storm. */
	public int weatherMode = OpticEnvironment.WEATHER_CLEAR;

	// Hurt Cam (Lunar-style)
	public boolean disableHurtCam = false;
	/** Multiplier on vanilla shake; 0–2, default 1. */
	public float hurtCamIntensity = 1.0F;

	/** FOV module. Degrees are 30–150. Sprint zoom stays OptiFine's escape-menu option. */
	public boolean fovMod = false;
	public float fov = 110.0F;
	public boolean dynamicFov = true;

	// Settings
	public boolean rawMouseInput = false;
	/** Aim the crosshair ray with the camera yaw instead of the per-tick head yaw. */
	public boolean mouseDelayFix = true;
	public boolean borderlessFullscreen = false;
	public boolean disableMouseScrollWheel = false;
	/** Show held-item name ("Diamond Sword") when switching slots. */
	public boolean itemInfo = true;

	/** Settings → Performance. Reserved; the toggle does not change rendering yet. */
	public boolean fpsOptimizer = false;

	// OptiFine-style performance (Settings → Performance)
	public boolean fastRender = true;
	public boolean fastMath = true;
	public boolean smoothFps = false;
	public boolean fog = false;
	public boolean clouds = false;
	public boolean sky = false;
	public boolean stars = false;
	public boolean weather = false;
	public boolean entityShadows = false;
	public boolean animWater = false;
	public boolean animLava = false;
	public boolean animFire = false;
	public boolean animPortal = false;
	public boolean animTerrain = false;

	/** Active profile name (no extension). Changes auto-sync into this profile file. */
	public String activeProfile = "";

	/** Module IDs starred / favourited in the Mods click GUI. */
	public final LinkedHashSet<Integer> favoriteModules = new LinkedHashSet<Integer>();

	public boolean isFavoriteModule(int moduleId) {
		return favoriteModules.contains(Integer.valueOf(moduleId));
	}

	public void toggleFavoriteModule(int moduleId) {
		Integer id = Integer.valueOf(moduleId);
		if (!favoriteModules.remove(id)) favoriteModules.add(id);
		save();
	}

	private File file() {
		return new File(new File(System.getProperty("user.dir"), "optic"), "settings.properties");
	}

	public void load() {
		Properties values = new Properties();
		File file = file();
		if (file.isFile()) {
			try (FileInputStream in = new FileInputStream(file)) {
				values.load(in);
			} catch (IOException ignored) {
			}
		}
		fps = read(values, "fps", fps);
		cps = read(values, "cps", cps);
		keystrokes = read(values, "keystrokes", keystrokes);
		coordinates = read(values, "coordinates", coordinates);
		ping = read(values, "ping", ping);
		toggleSprint = read(values, "toggleSprint", toggleSprint);
		fullbright = read(values, "fullbright", fullbright);
		animations = read(values, "animations", animations);
		particles = read(values, "particles", particles);
		blockOverlay = read(values, "blockOverlay", blockOverlay);
		nametags = read(values, "nametags", nametags);
		chat = read(values, "chat", chat);
		scoreboard = read(values, "scoreboard", scoreboard);
		hotbar = read(values, "hotbar", hotbar);
		timeChanger = read(values, "timeChanger", timeChanger);
		weatherChanger = read(values, "weatherChanger", weatherChanger);
		hurtCam = read(values, "hurtCam", hurtCam);
		zoom = read(values, "zoom", zoom);
		zoomKeyCode = readInt(values, "zoomKeyCode", zoomKeyCode);
		zoomFactor = clampFloat(readFloat(values, "zoomFactor", zoomFactor), 1.25F, 10.0F);
		zoomSmooth = read(values, "zoomSmooth", zoomSmooth);
		zoomSmoothCamera = read(values, "zoomSmoothCamera", zoomSmoothCamera);
		zoomMouseSensitivity = read(
			values,
			"zoomMouseSensitivity",
			read(values, "zoomReduceSensitivity", zoomMouseSensitivity)
		);
		zoomScroll = read(values, "zoomScroll", read(values, "zoomScrollAdjust", zoomScroll));
		zoomScrollSpeed = clampFloat(readFloat(values, "zoomScrollSpeed", zoomScrollSpeed), 0.05F, 1.0F);
		freeLook = read(values, "freeLook", freeLook);
		freeLookKeyCode = readInt(values, "freeLookKeyCode", freeLookKeyCode);
		freeLookToggle = read(values, "freeLookToggle", freeLookToggle);
		freeLookDistance = clampFloat(readFloat(values, "freeLookDistance", freeLookDistance), 1.5F, 12.0F);
		if (values.getProperty("particlesConfigured") == null
			&& particles
			&& !showCriticalHitParticles
			&& !alwaysSharpnessParticles
			&& !renderOwnParticles) {
			showCriticalHitParticles = true;
			alwaysSharpnessParticles = true;
			renderOwnParticles = true;
			if (particleMultiplier < 1.0F) particleMultiplier = 1.0F;
		}
		hitboxes = read(values, "hitboxes", hitboxes);
		hitboxHideLook = read(values, "hitboxHideLook", hitboxHideLook);
		hitboxPlayerColor = readColor(values, "hitboxPlayerColor", hitboxPlayerColor);
		hitboxMobColor = readColor(values, "hitboxMobColor", hitboxMobColor);
		hitboxArrowColor = readColor(values, "hitboxArrowColor", hitboxArrowColor);
		hitboxOtherColor = readColor(values, "hitboxOtherColor", hitboxOtherColor);
		hitPredict = read(values, "hitPredict", hitPredict);

		boolean legacyShadow = read(values, "hudShadow", true);

		fpsShowLabel = read(values, "fpsShowLabel", fpsShowLabel);
		fpsBrackets = read(values, "fpsBrackets", fpsBrackets);
		fpsTextShadow = read(values, "fpsTextShadow", legacyShadow);
		fpsOpticFont = read(values, "fpsOpticFont", fpsOpticFont);
		fpsScale = clampFloat(readFloat(values, "fpsScale", fpsScale), 0.5F, 3.0F);
		fpsColor = readColor(values, "fpsColor", fpsColor);
		fpsTextColor = readColor(values, "fpsTextColor", fpsTextColor);
		fpsX = readInt(values, "fpsX", fpsX);
		fpsY = readInt(values, "fpsY", fpsY);

		cpsSeparateButtons = read(values, "cpsSeparateButtons", cpsSeparateButtons);
		cpsShowLabel = read(values, "cpsShowLabel", cpsShowLabel);
		cpsBrackets = read(values, "cpsBrackets", cpsBrackets);
		cpsTextShadow = read(values, "cpsTextShadow", legacyShadow);
		cpsOpticFont = read(values, "cpsOpticFont", cpsOpticFont);
		cpsScale = clampFloat(readFloat(values, "cpsScale", cpsScale), 0.5F, 3.0F);
		cpsColor = readColor(values, "cpsColor", cpsColor);
		cpsTextColor = readColor(values, "cpsTextColor", cpsTextColor);
		cpsX = readInt(values, "cpsX", cpsX);
		cpsY = readInt(values, "cpsY", cpsY);

		keystrokesShowMouse = read(values, "keystrokesShowMouse", keystrokesShowMouse);
		keystrokesModernStyle = read(values, "keystrokesModernStyle", keystrokesModernStyle);
		keystrokesTextShadow = read(values, "keystrokesTextShadow", legacyShadow);
		keystrokesOpticFont = read(values, "keystrokesOpticFont", keystrokesOpticFont);
		keystrokesBackground = read(values, "keystrokesBackground", keystrokesBackground);
		keystrokesShowLabels = read(values, "keystrokesShowLabels", keystrokesShowLabels);
		keystrokesInstant = read(values, "keystrokesInstant", keystrokesInstant);
		keystrokesFade = read(values, "keystrokesFade", keystrokesFade);
		keystrokesLockKeys = read(values, "keystrokesLockKeys", keystrokesLockKeys);
		keystrokesFadeMs = clampFloat(readFloat(values, "keystrokesFadeMs", keystrokesFadeMs), 50.0F, 1000.0F);
		keystrokesCpsMode = clamp(readInt(values, "keystrokesCpsMode", keystrokesCpsMode), 0, 3);
		keystrokesScale = clampFloat(readFloat(values, "keystrokesScale", keystrokesScale), 0.5F, 3.0F);
		keystrokesColor = readColor(values, "keystrokesColor", keystrokesColor);
		keystrokesTextColor = readColor(values, "keystrokesTextColor", keystrokesTextColor);
		keystrokesX = readInt(values, "keystrokesX", keystrokesX);
		keystrokesY = readInt(values, "keystrokesY", keystrokesY);
		keystrokesOffX0 = readFloat(values, "keystrokesOffX0", keystrokesOffX0);
		keystrokesOffY0 = readFloat(values, "keystrokesOffY0", keystrokesOffY0);
		keystrokesOffX1 = readFloat(values, "keystrokesOffX1", keystrokesOffX1);
		keystrokesOffY1 = readFloat(values, "keystrokesOffY1", keystrokesOffY1);
		keystrokesOffX2 = readFloat(values, "keystrokesOffX2", keystrokesOffX2);
		keystrokesOffY2 = readFloat(values, "keystrokesOffY2", keystrokesOffY2);
		keystrokesOffX3 = readFloat(values, "keystrokesOffX3", keystrokesOffX3);
		keystrokesOffY3 = readFloat(values, "keystrokesOffY3", keystrokesOffY3);
		keystrokesOffX4 = readFloat(values, "keystrokesOffX4", keystrokesOffX4);
		keystrokesOffY4 = readFloat(values, "keystrokesOffY4", keystrokesOffY4);
		keystrokesOffX5 = readFloat(values, "keystrokesOffX5", keystrokesOffX5);
		keystrokesOffY5 = readFloat(values, "keystrokesOffY5", keystrokesOffY5);

		pingShowMs = read(values, "pingShowMs", pingShowMs);
		pingShowLabel = read(values, "pingShowLabel", pingShowLabel);
		pingBrackets = read(values, "pingBrackets", pingBrackets);
		pingTextShadow = read(values, "pingTextShadow", legacyShadow);
		pingOpticFont = read(values, "pingOpticFont", pingOpticFont);
		pingScale = clampFloat(readFloat(values, "pingScale", pingScale), 0.5F, 3.0F);
		pingColor = readColor(values, "pingColor", pingColor);
		pingTextColor = readColor(values, "pingTextColor", pingTextColor);
		pingX = readInt(values, "pingX", pingX);
		pingY = readInt(values, "pingY", pingY);

		coordinatesShowY = read(values, "coordinatesShowY", coordinatesShowY);
		coordinatesShowLabel = read(values, "coordinatesShowLabel", coordinatesShowLabel);
		coordinatesBrackets = read(values, "coordinatesBrackets", coordinatesBrackets);
		coordinatesTextShadow = read(values, "coordinatesTextShadow", legacyShadow);
		coordinatesOpticFont = read(values, "coordinatesOpticFont", coordinatesOpticFont);
		coordinatesScale = clampFloat(readFloat(values, "coordinatesScale", coordinatesScale), 0.5F, 3.0F);
		coordinatesDecimals = clamp(readInt(values, "coordinatesDecimals", coordinatesDecimals), 0, 2);
		coordinatesColor = readColor(values, "coordinatesColor", coordinatesColor);
		coordinatesTextColor = readColor(values, "coordinatesTextColor", coordinatesTextColor);
		coordinatesX = readInt(values, "coordinatesX", coordinatesX);
		coordinatesY = readInt(values, "coordinatesY", coordinatesY);

		toggleSprintShowHud = read(values, "toggleSprintShowHud", toggleSprintShowHud);
		toggleSprintBrackets = read(values, "toggleSprintBrackets", toggleSprintBrackets);
		toggleSprintTextShadow = read(values, "toggleSprintTextShadow", legacyShadow);
		toggleSprintOpticFont = read(values, "toggleSprintOpticFont", toggleSprintOpticFont);
		toggleSprintScale = clampFloat(readFloat(values, "toggleSprintScale", toggleSprintScale), 0.5F, 3.0F);
		toggleSprintColor = readColor(values, "toggleSprintColor", toggleSprintColor);
		toggleSprintTextColor = readColor(values, "toggleSprintTextColor", toggleSprintTextColor);
		toggleSprintX = readInt(values, "toggleSprintX", toggleSprintX);
		toggleSprintY = readInt(values, "toggleSprintY", toggleSprintY);

		fullbrightGamma = clampFloat(readFloat(values, "fullbrightGamma", fullbrightGamma), 1.0F, 16.0F);

		animHitbox = read(values, "animHitbox", animHitbox);
		animInventory = read(values, "animInventory", animInventory);
		animHealth = read(values, "animHealth", animHealth);
		animDamage = read(values, "animDamage", animDamage);
		animSneaking = read(values, "animSneaking", animSneaking);
		animOldEnchantGlint = read(values, "animOldEnchantGlint", animOldEnchantGlint);
		anim18Skins = read(values, "anim18Skins", anim18Skins);
		animTitles = read(values, "animTitles", animTitles);
		animFixGhostLiquid = read(values, "animFixGhostLiquid", animFixGhostLiquid);
		animItemHold = read(values, "animItemHold", animItemHold);
		animItemSwitch = read(values, "animItemSwitch", animItemSwitch);
		animBlockHit = read(values, "animBlockHit", animBlockHit);
		animEating = read(values, "animEating", animEating);
		animThrowing = read(values, "animThrowing", animThrowing);
		animFishingRod = read(values, "animFishingRod", animFishingRod);
		animSwordSwing = read(values, "animSwordSwing", animSwordSwing);
		animBow = read(values, "animBow", animBow);
		animLeftHand = read(values, "animLeftHand", animLeftHand);
		animMovableTitles = read(values, "animMovableTitles", animMovableTitles);
		titleOffsetX = readInt(values, "titleOffsetX", titleOffsetX);
		titleOffsetY = readInt(values, "titleOffsetY", titleOffsetY);

		showCriticalHitParticles = read(values, "showCriticalHitParticles", showCriticalHitParticles);
		alwaysSharpnessParticles = read(values, "alwaysSharpnessParticles", alwaysSharpnessParticles);
		particleMultiplier = clampFloat(readFloat(values, "particleMultiplier", particleMultiplier), 0.0F, 10.0F);
		removeVanillaColorFilter = read(values, "removeVanillaColorFilter", removeVanillaColorFilter);
		renderOwnParticles = read(values, "renderOwnParticles", renderOwnParticles);
		blockBreakParticles = read(values, "blockBreakParticles", blockBreakParticles);

		blockOverlayFill = read(values, "blockOverlayFill", blockOverlayFill);
		blockOverlayFillColor = readColor(values, "blockOverlayFillColor", blockOverlayFillColor);
		blockOverlayFillAlpha = clampFloat(readFloat(values, "blockOverlayFillAlpha", blockOverlayFillAlpha), 0.0F, 1.0F);
		blockOverlayOutline = read(values, "blockOverlayOutline", blockOverlayOutline);
		blockOverlayOutlineColor = readColor(values, "blockOverlayOutlineColor", blockOverlayOutlineColor);
		blockOverlayOutlineAlpha = clampFloat(readFloat(values, "blockOverlayOutlineAlpha", blockOverlayOutlineAlpha), 0.0F, 1.0F);
		blockOverlayLineWidth = clampFloat(readFloat(values, "blockOverlayLineWidth", blockOverlayLineWidth), 1.0F, 8.0F);

		nametagsShowInF5 = read(values, "nametagsShowInF5", nametagsShowInF5);
		nametagsShowInF1 = read(values, "nametagsShowInF1", nametagsShowInF1);
		nametagsTextShadow = read(values, "nametagsTextShadow", nametagsTextShadow);
		nametagsBadlionFont = read(values, "nametagsBadlionFont", nametagsBadlionFont);
		nametagsRoundedCorners = read(values, "nametagsRoundedCorners", nametagsRoundedCorners);
		nametagsBackground = read(values, "nametagsBackground", nametagsBackground);
		nametagsBackgroundColor = readColor(values, "nametagsBackgroundColor", nametagsBackgroundColor);
		nametagsPersonalColor = readColor(values, "nametagsPersonalColor", nametagsPersonalColor);
		readNameColorMap(values.getProperty("nametagNameColors", ""), nametagNameColors);

		chatBold = read(values, "chatBold", chatBold);
		chatAntiSpam = read(values, "chatAntiSpam", chatAntiSpam);
		chatInfiniteHistory = read(values, "chatInfiniteHistory", chatInfiniteHistory);
		chatBackgroundOpacity = clampFloat(readFloat(values, "chatBackgroundOpacity", chatBackgroundOpacity), 0.0F, 100.0F);

		scoreboardShowNumbers = read(values, "scoreboardShowNumbers", scoreboardShowNumbers);
		scoreboardRemoveNumberWidth = read(values, "scoreboardRemoveNumberWidth", scoreboardRemoveNumberWidth);
		scoreboardTextShadow = read(values, "scoreboardTextShadow", scoreboardTextShadow);
		scoreboardBackgroundOpacity = clampFloat(readFloat(values, "scoreboardBackgroundOpacity", scoreboardBackgroundOpacity), 0.0F, 100.0F);
		scoreboardX = readInt(values, "scoreboardX", scoreboardX);
		scoreboardY = readInt(values, "scoreboardY", scoreboardY);

		hotbarScale = clampFloat(readFloat(values, "hotbarScale", hotbarScale), 0.5F, 3.0F);
		hotbarRotation = normalizeHotbarRotation(readInt(values, "hotbarRotation", hotbarRotation));
		hotbarX = readInt(values, "hotbarX", hotbarX);
		hotbarY = readInt(values, "hotbarY", hotbarY);

		skyType = Math.max(0, Math.min(2, readInt(values, "skyType", skyType)));
		freezeTime = read(values, "freezeTime", freezeTime);
		timePreset = Math.max(0, Math.min(4, readInt(values, "timePreset", timePreset)));
		timeOfDay = clampFloat(readFloat(values, "timeOfDay", timeOfDay), 0.0F, 23999.0F);

		weatherMode = Math.max(0, Math.min(3, readInt(values, "weatherMode", weatherMode)));

		disableHurtCam = read(values, "disableHurtCam", disableHurtCam);
		hurtCamIntensity = clampFloat(readFloat(values, "hurtCamIntensity", hurtCamIntensity), 0.0F, 2.0F);
		fovMod = read(values, "fovMod", fovMod);
		fov = clampFloat(readFloat(values, "fov", fov), 30.0F, 150.0F);
		dynamicFov = read(values, "dynamicFov", dynamicFov);

		rawMouseInput = read(values, "rawMouseInput", rawMouseInput);
		mouseDelayFix = read(values, "mouseDelayFix", mouseDelayFix);
		borderlessFullscreen = read(values, "borderlessFullscreen", borderlessFullscreen);
		disableMouseScrollWheel = read(values, "disableMouseScrollWheel", disableMouseScrollWheel);
		itemInfo = read(values, "itemInfo", itemInfo);
		fpsOptimizer = read(values, "fpsOptimizer", fpsOptimizer);

		fastRender = read(values, "fastRender", fastRender);
		fastMath = read(values, "fastMath", fastMath);
		smoothFps = read(values, "smoothFps", smoothFps);
		fog = read(values, "fog", fog);
		clouds = read(values, "clouds", clouds);
		sky = read(values, "sky", sky);
		stars = read(values, "stars", stars);
		weather = read(values, "weather", weather);
		entityShadows = read(values, "entityShadows", entityShadows);
		animWater = read(values, "animWater", animWater);
		animLava = read(values, "animLava", animLava);
		animFire = read(values, "animFire", animFire);
		animPortal = read(values, "animPortal", animPortal);
		animTerrain = read(values, "animTerrain", animTerrain);
		readIntSet(values.getProperty("favoriteModules", ""), favoriteModules);

		activeProfile = values.getProperty("activeProfile", activeProfile);
		if (activeProfile == null) activeProfile = "";
		OpticPerf.sync(this);

		// One-time: existing installs were saved with mods on. New defaults are all off.
		if (!read(values, "modsDefaultOff", false)) {
			fps = false;
			cps = false;
			keystrokes = false;
			coordinates = false;
			ping = false;
			toggleSprint = false;
			fullbright = false;
			blockOverlay = false;
			nametags = false;
			chat = false;
			scoreboard = false;
			hotbar = false;
			timeChanger = false;
			weatherChanger = false;
			hurtCam = false;
			zoom = false;
		}

		// One-time: switch old Optic-green defaults to white.
		if (!read(values, "whiteColorDefaults", false)) {
			fpsColor = whiteIfLegacy(fpsColor);
			fpsTextColor = whiteIfLegacy(fpsTextColor);
			cpsColor = whiteIfLegacy(cpsColor);
			cpsTextColor = whiteIfLegacy(cpsTextColor);
			keystrokesColor = whiteIfLegacy(keystrokesColor);
			keystrokesTextColor = whiteIfLegacy(keystrokesTextColor);
			pingColor = whiteIfLegacy(pingColor);
			pingTextColor = whiteIfLegacy(pingTextColor);
			coordinatesColor = whiteIfLegacy(coordinatesColor);
			coordinatesTextColor = whiteIfLegacy(coordinatesTextColor);
			toggleSprintColor = whiteIfLegacy(toggleSprintColor);
			toggleSprintTextColor = whiteIfLegacy(toggleSprintTextColor);
		}
		try {
			OpticProfiles.ensureSelected(this);
		} catch (Exception ignored) {
			if (activeProfile == null || activeProfile.trim().isEmpty()) {
				activeProfile = "default";
			}
		}
		save();
	}

	/** Persist settings.properties without rewriting the active profile JSON. */
	public void saveSettingsOnly() {
		writeSettingsFile();
	}

	public void save() {
		writeSettingsFile();
		// Keep the selected profile JSON in sync with live settings.
		if (activeProfile != null && !activeProfile.isEmpty()) {
			try {
				OpticProfiles.write(this, activeProfile);
			} catch (IOException ignored) {
			}
		}
	}

	private void writeSettingsFile() {
		File file = file();
		File parent = file.getParentFile();
		if (parent != null) parent.mkdirs();
		Properties values = new Properties();
		values.setProperty("fps", Boolean.toString(fps));
		values.setProperty("cps", Boolean.toString(cps));
		values.setProperty("keystrokes", Boolean.toString(keystrokes));
		values.setProperty("coordinates", Boolean.toString(coordinates));
		values.setProperty("ping", Boolean.toString(ping));
		values.setProperty("toggleSprint", Boolean.toString(toggleSprint));
		values.setProperty("fullbright", Boolean.toString(fullbright));
		values.setProperty("animations", Boolean.toString(animations));
		values.setProperty("particles", Boolean.toString(particles));
		values.setProperty("blockOverlay", Boolean.toString(blockOverlay));
		values.setProperty("nametags", Boolean.toString(nametags));
		values.setProperty("chat", Boolean.toString(chat));
		values.setProperty("scoreboard", Boolean.toString(scoreboard));
		values.setProperty("hotbar", Boolean.toString(hotbar));
		values.setProperty("timeChanger", Boolean.toString(timeChanger));
		values.setProperty("weatherChanger", Boolean.toString(weatherChanger));
		values.setProperty("hurtCam", Boolean.toString(hurtCam));
		values.setProperty("zoom", Boolean.toString(zoom));
		values.setProperty("zoomKeyCode", Integer.toString(zoomKeyCode));
		values.setProperty("zoomFactor", Float.toString(zoomFactor));
		values.setProperty("zoomSmooth", Boolean.toString(zoomSmooth));
		values.setProperty("zoomSmoothCamera", Boolean.toString(zoomSmoothCamera));
		values.setProperty("zoomMouseSensitivity", Boolean.toString(zoomMouseSensitivity));
		values.setProperty("zoomScroll", Boolean.toString(zoomScroll));
		values.setProperty("zoomScrollSpeed", Float.toString(zoomScrollSpeed));
		values.setProperty("freeLook", Boolean.toString(freeLook));
		values.setProperty("freeLookKeyCode", Integer.toString(freeLookKeyCode));
		values.setProperty("freeLookToggle", Boolean.toString(freeLookToggle));
		values.setProperty("freeLookDistance", Float.toString(freeLookDistance));
		values.setProperty("particlesConfigured", "true");
		values.setProperty("hitboxes", Boolean.toString(hitboxes));
		values.setProperty("hitboxHideLook", Boolean.toString(hitboxHideLook));
		values.setProperty("hitboxPlayerColor", toHex(hitboxPlayerColor));
		values.setProperty("hitboxMobColor", toHex(hitboxMobColor));
		values.setProperty("hitboxArrowColor", toHex(hitboxArrowColor));
		values.setProperty("hitboxOtherColor", toHex(hitboxOtherColor));
		values.setProperty("hitPredict", Boolean.toString(hitPredict));

		values.setProperty("whiteColorDefaults", "true");
		values.setProperty("modsDefaultOff", "true");

		values.setProperty("fpsShowLabel", Boolean.toString(fpsShowLabel));
		values.setProperty("fpsBrackets", Boolean.toString(fpsBrackets));
		values.setProperty("fpsTextShadow", Boolean.toString(fpsTextShadow));
		values.setProperty("fpsOpticFont", Boolean.toString(fpsOpticFont));
		values.setProperty("fpsScale", Float.toString(fpsScale));
		values.setProperty("fpsColor", toHex(fpsColor));
		values.setProperty("fpsTextColor", toHex(fpsTextColor));
		values.setProperty("fpsX", Integer.toString(fpsX));
		values.setProperty("fpsY", Integer.toString(fpsY));

		values.setProperty("cpsSeparateButtons", Boolean.toString(cpsSeparateButtons));
		values.setProperty("cpsShowLabel", Boolean.toString(cpsShowLabel));
		values.setProperty("cpsBrackets", Boolean.toString(cpsBrackets));
		values.setProperty("cpsTextShadow", Boolean.toString(cpsTextShadow));
		values.setProperty("cpsOpticFont", Boolean.toString(cpsOpticFont));
		values.setProperty("cpsScale", Float.toString(cpsScale));
		values.setProperty("cpsColor", toHex(cpsColor));
		values.setProperty("cpsTextColor", toHex(cpsTextColor));
		values.setProperty("cpsX", Integer.toString(cpsX));
		values.setProperty("cpsY", Integer.toString(cpsY));

		values.setProperty("keystrokesShowMouse", Boolean.toString(keystrokesShowMouse));
		values.setProperty("keystrokesModernStyle", Boolean.toString(keystrokesModernStyle));
		values.setProperty("keystrokesTextShadow", Boolean.toString(keystrokesTextShadow));
		values.setProperty("keystrokesOpticFont", Boolean.toString(keystrokesOpticFont));
		values.setProperty("keystrokesBackground", Boolean.toString(keystrokesBackground));
		values.setProperty("keystrokesShowLabels", Boolean.toString(keystrokesShowLabels));
		values.setProperty("keystrokesInstant", Boolean.toString(keystrokesInstant));
		values.setProperty("keystrokesFade", Boolean.toString(keystrokesFade));
		values.setProperty("keystrokesLockKeys", Boolean.toString(keystrokesLockKeys));
		values.setProperty("keystrokesFadeMs", Float.toString(keystrokesFadeMs));
		values.setProperty("keystrokesCpsMode", Integer.toString(keystrokesCpsMode));
		values.setProperty("keystrokesScale", Float.toString(keystrokesScale));
		values.setProperty("keystrokesColor", toHex(keystrokesColor));
		values.setProperty("keystrokesTextColor", toHex(keystrokesTextColor));
		values.setProperty("keystrokesX", Integer.toString(keystrokesX));
		values.setProperty("keystrokesY", Integer.toString(keystrokesY));
		values.setProperty("keystrokesOffX0", Float.toString(keystrokesOffX0));
		values.setProperty("keystrokesOffY0", Float.toString(keystrokesOffY0));
		values.setProperty("keystrokesOffX1", Float.toString(keystrokesOffX1));
		values.setProperty("keystrokesOffY1", Float.toString(keystrokesOffY1));
		values.setProperty("keystrokesOffX2", Float.toString(keystrokesOffX2));
		values.setProperty("keystrokesOffY2", Float.toString(keystrokesOffY2));
		values.setProperty("keystrokesOffX3", Float.toString(keystrokesOffX3));
		values.setProperty("keystrokesOffY3", Float.toString(keystrokesOffY3));
		values.setProperty("keystrokesOffX4", Float.toString(keystrokesOffX4));
		values.setProperty("keystrokesOffY4", Float.toString(keystrokesOffY4));
		values.setProperty("keystrokesOffX5", Float.toString(keystrokesOffX5));
		values.setProperty("keystrokesOffY5", Float.toString(keystrokesOffY5));

		values.setProperty("pingShowMs", Boolean.toString(pingShowMs));
		values.setProperty("pingShowLabel", Boolean.toString(pingShowLabel));
		values.setProperty("pingBrackets", Boolean.toString(pingBrackets));
		values.setProperty("pingTextShadow", Boolean.toString(pingTextShadow));
		values.setProperty("pingOpticFont", Boolean.toString(pingOpticFont));
		values.setProperty("pingScale", Float.toString(pingScale));
		values.setProperty("pingColor", toHex(pingColor));
		values.setProperty("pingTextColor", toHex(pingTextColor));
		values.setProperty("pingX", Integer.toString(pingX));
		values.setProperty("pingY", Integer.toString(pingY));

		values.setProperty("coordinatesShowY", Boolean.toString(coordinatesShowY));
		values.setProperty("coordinatesShowLabel", Boolean.toString(coordinatesShowLabel));
		values.setProperty("coordinatesBrackets", Boolean.toString(coordinatesBrackets));
		values.setProperty("coordinatesTextShadow", Boolean.toString(coordinatesTextShadow));
		values.setProperty("coordinatesOpticFont", Boolean.toString(coordinatesOpticFont));
		values.setProperty("coordinatesScale", Float.toString(coordinatesScale));
		values.setProperty("coordinatesDecimals", Integer.toString(coordinatesDecimals));
		values.setProperty("coordinatesColor", toHex(coordinatesColor));
		values.setProperty("coordinatesTextColor", toHex(coordinatesTextColor));
		values.setProperty("coordinatesX", Integer.toString(coordinatesX));
		values.setProperty("coordinatesY", Integer.toString(coordinatesY));

		values.setProperty("toggleSprintShowHud", Boolean.toString(toggleSprintShowHud));
		values.setProperty("toggleSprintBrackets", Boolean.toString(toggleSprintBrackets));
		values.setProperty("toggleSprintTextShadow", Boolean.toString(toggleSprintTextShadow));
		values.setProperty("toggleSprintOpticFont", Boolean.toString(toggleSprintOpticFont));
		values.setProperty("toggleSprintScale", Float.toString(toggleSprintScale));
		values.setProperty("toggleSprintColor", toHex(toggleSprintColor));
		values.setProperty("toggleSprintTextColor", toHex(toggleSprintTextColor));
		values.setProperty("toggleSprintX", Integer.toString(toggleSprintX));
		values.setProperty("toggleSprintY", Integer.toString(toggleSprintY));

		values.setProperty("fullbrightGamma", Float.toString(fullbrightGamma));

		values.setProperty("animHitbox", Boolean.toString(animHitbox));
		values.setProperty("animInventory", Boolean.toString(animInventory));
		values.setProperty("animHealth", Boolean.toString(animHealth));
		values.setProperty("animDamage", Boolean.toString(animDamage));
		values.setProperty("animSneaking", Boolean.toString(animSneaking));
		values.setProperty("animOldEnchantGlint", Boolean.toString(animOldEnchantGlint));
		values.setProperty("anim18Skins", Boolean.toString(anim18Skins));
		values.setProperty("animTitles", Boolean.toString(animTitles));
		values.setProperty("animFixGhostLiquid", Boolean.toString(animFixGhostLiquid));
		values.setProperty("animItemHold", Boolean.toString(animItemHold));
		values.setProperty("animItemSwitch", Boolean.toString(animItemSwitch));
		values.setProperty("animBlockHit", Boolean.toString(animBlockHit));
		values.setProperty("animEating", Boolean.toString(animEating));
		values.setProperty("animThrowing", Boolean.toString(animThrowing));
		values.setProperty("animFishingRod", Boolean.toString(animFishingRod));
		values.setProperty("animSwordSwing", Boolean.toString(animSwordSwing));
		values.setProperty("animBow", Boolean.toString(animBow));
		values.setProperty("animLeftHand", Boolean.toString(animLeftHand));
		values.setProperty("animMovableTitles", Boolean.toString(animMovableTitles));
		values.setProperty("titleOffsetX", Integer.toString(titleOffsetX));
		values.setProperty("titleOffsetY", Integer.toString(titleOffsetY));

		values.setProperty("showCriticalHitParticles", Boolean.toString(showCriticalHitParticles));
		values.setProperty("alwaysSharpnessParticles", Boolean.toString(alwaysSharpnessParticles));
		values.setProperty("particleMultiplier", Float.toString(particleMultiplier));
		values.setProperty("removeVanillaColorFilter", Boolean.toString(removeVanillaColorFilter));
		values.setProperty("renderOwnParticles", Boolean.toString(renderOwnParticles));
		values.setProperty("blockBreakParticles", Boolean.toString(blockBreakParticles));

		values.setProperty("blockOverlayFill", Boolean.toString(blockOverlayFill));
		values.setProperty("blockOverlayFillColor", toHex(blockOverlayFillColor));
		values.setProperty("blockOverlayFillAlpha", Float.toString(blockOverlayFillAlpha));
		values.setProperty("blockOverlayOutline", Boolean.toString(blockOverlayOutline));
		values.setProperty("blockOverlayOutlineColor", toHex(blockOverlayOutlineColor));
		values.setProperty("blockOverlayOutlineAlpha", Float.toString(blockOverlayOutlineAlpha));
		values.setProperty("blockOverlayLineWidth", Float.toString(blockOverlayLineWidth));

		values.setProperty("nametagsShowInF5", Boolean.toString(nametagsShowInF5));
		values.setProperty("nametagsShowInF1", Boolean.toString(nametagsShowInF1));
		values.setProperty("nametagsTextShadow", Boolean.toString(nametagsTextShadow));
		values.setProperty("nametagsBadlionFont", Boolean.toString(nametagsBadlionFont));
		values.setProperty("nametagsRoundedCorners", Boolean.toString(nametagsRoundedCorners));
		values.setProperty("nametagsBackground", Boolean.toString(nametagsBackground));
		values.setProperty("nametagsBackgroundColor", toHex(nametagsBackgroundColor));
		values.setProperty("nametagsPersonalColor", toHex(nametagsPersonalColor));
		values.setProperty("nametagNameColors", writeNameColorMap(nametagNameColors));

		values.setProperty("chatBold", Boolean.toString(chatBold));
		values.setProperty("chatAntiSpam", Boolean.toString(chatAntiSpam));
		values.setProperty("chatInfiniteHistory", Boolean.toString(chatInfiniteHistory));
		values.setProperty("chatBackgroundOpacity", Float.toString(chatBackgroundOpacity));

		values.setProperty("scoreboardShowNumbers", Boolean.toString(scoreboardShowNumbers));
		values.setProperty("scoreboardRemoveNumberWidth", Boolean.toString(scoreboardRemoveNumberWidth));
		values.setProperty("scoreboardTextShadow", Boolean.toString(scoreboardTextShadow));
		values.setProperty("scoreboardBackgroundOpacity", Float.toString(scoreboardBackgroundOpacity));
		values.setProperty("scoreboardX", Integer.toString(scoreboardX));
		values.setProperty("scoreboardY", Integer.toString(scoreboardY));

		values.setProperty("hotbarScale", Float.toString(hotbarScale));
		values.setProperty("hotbarRotation", Integer.toString(hotbarRotation));
		values.setProperty("hotbarX", Integer.toString(hotbarX));
		values.setProperty("hotbarY", Integer.toString(hotbarY));

		values.setProperty("skyType", Integer.toString(skyType));
		values.setProperty("freezeTime", Boolean.toString(freezeTime));
		values.setProperty("timePreset", Integer.toString(timePreset));
		values.setProperty("timeOfDay", Float.toString(timeOfDay));

		values.setProperty("weatherMode", Integer.toString(weatherMode));

		values.setProperty("disableHurtCam", Boolean.toString(disableHurtCam));
		values.setProperty("hurtCamIntensity", Float.toString(hurtCamIntensity));
		values.setProperty("fovMod", Boolean.toString(fovMod));
		values.setProperty("fov", Float.toString(fov));
		values.setProperty("dynamicFov", Boolean.toString(dynamicFov));

		values.setProperty("rawMouseInput", Boolean.toString(rawMouseInput));
		values.setProperty("mouseDelayFix", Boolean.toString(mouseDelayFix));
		values.setProperty("borderlessFullscreen", Boolean.toString(borderlessFullscreen));
		values.setProperty("disableMouseScrollWheel", Boolean.toString(disableMouseScrollWheel));
		values.setProperty("itemInfo", Boolean.toString(itemInfo));
		values.setProperty("fpsOptimizer", Boolean.toString(fpsOptimizer));

		values.setProperty("fastRender", Boolean.toString(fastRender));
		values.setProperty("fastMath", Boolean.toString(fastMath));
		values.setProperty("smoothFps", Boolean.toString(smoothFps));
		values.setProperty("fog", Boolean.toString(fog));
		values.setProperty("clouds", Boolean.toString(clouds));
		values.setProperty("sky", Boolean.toString(sky));
		values.setProperty("stars", Boolean.toString(stars));
		values.setProperty("weather", Boolean.toString(weather));
		values.setProperty("entityShadows", Boolean.toString(entityShadows));
		values.setProperty("animWater", Boolean.toString(animWater));
		values.setProperty("animLava", Boolean.toString(animLava));
		values.setProperty("animFire", Boolean.toString(animFire));
		values.setProperty("animPortal", Boolean.toString(animPortal));
		values.setProperty("animTerrain", Boolean.toString(animTerrain));
		values.setProperty("favoriteModules", writeIntSet(favoriteModules));

		values.setProperty("activeProfile", activeProfile == null ? "" : activeProfile);
		try (FileOutputStream out = new FileOutputStream(file)) {
			values.store(out, "Optic 1.0.0");
		} catch (IOException ignored) {
		}
		OpticPerf.sync(this);
	}

	public static int withAlpha(int rgb, int alpha) {
		return ((alpha & 0xFF) << 24) | (rgb & 0xFFFFFF);
	}

	/** Export shareable profile JSON (mods, options, colors, HUD layout). */
	public JsonObject toProfileJson() {
		JsonObject o = new JsonObject();
		o.addProperty("fps", fps);
		o.addProperty("cps", cps);
		o.addProperty("keystrokes", keystrokes);
		o.addProperty("coordinates", coordinates);
		o.addProperty("ping", ping);
		o.addProperty("toggleSprint", toggleSprint);
		o.addProperty("fullbright", fullbright);
		o.addProperty("animations", animations);
		o.addProperty("particles", particles);
		o.addProperty("blockOverlay", blockOverlay);
		o.addProperty("nametags", nametags);
		o.addProperty("chat", chat);
		o.addProperty("scoreboard", scoreboard);
		o.addProperty("hotbar", hotbar);
		o.addProperty("timeChanger", timeChanger);
		o.addProperty("weatherChanger", weatherChanger);
		o.addProperty("hurtCam", hurtCam);
		o.addProperty("zoom", zoom);
		o.addProperty("zoomKeyCode", zoomKeyCode);
		o.addProperty("zoomFactor", zoomFactor);
		o.addProperty("zoomSmooth", zoomSmooth);
		o.addProperty("zoomSmoothCamera", zoomSmoothCamera);
		o.addProperty("zoomMouseSensitivity", zoomMouseSensitivity);
		o.addProperty("zoomScroll", zoomScroll);
		o.addProperty("zoomScrollSpeed", zoomScrollSpeed);
		o.addProperty("freeLook", freeLook);
		o.addProperty("freeLookKeyCode", freeLookKeyCode);
		o.addProperty("freeLookToggle", freeLookToggle);
		o.addProperty("freeLookDistance", freeLookDistance);
		o.addProperty("hitboxes", hitboxes);
		o.addProperty("hitboxHideLook", hitboxHideLook);
		o.addProperty("hitboxPlayerColor", toHex(hitboxPlayerColor));
		o.addProperty("hitboxMobColor", toHex(hitboxMobColor));
		o.addProperty("hitboxArrowColor", toHex(hitboxArrowColor));
		o.addProperty("hitboxOtherColor", toHex(hitboxOtherColor));
		o.addProperty("hitPredict", hitPredict);
		o.addProperty("fpsShowLabel", fpsShowLabel);
		o.addProperty("fpsBrackets", fpsBrackets);
		o.addProperty("fpsTextShadow", fpsTextShadow);
		o.addProperty("fpsOpticFont", fpsOpticFont);
		o.addProperty("fpsScale", fpsScale);
		o.addProperty("fpsColor", toHex(fpsColor));
		o.addProperty("fpsTextColor", toHex(fpsTextColor));
		o.addProperty("fpsX", fpsX);
		o.addProperty("fpsY", fpsY);

		o.addProperty("cpsSeparateButtons", cpsSeparateButtons);
		o.addProperty("cpsShowLabel", cpsShowLabel);
		o.addProperty("cpsBrackets", cpsBrackets);
		o.addProperty("cpsTextShadow", cpsTextShadow);
		o.addProperty("cpsOpticFont", cpsOpticFont);
		o.addProperty("cpsScale", cpsScale);
		o.addProperty("cpsColor", toHex(cpsColor));
		o.addProperty("cpsTextColor", toHex(cpsTextColor));
		o.addProperty("cpsX", cpsX);
		o.addProperty("cpsY", cpsY);

		o.addProperty("keystrokesShowMouse", keystrokesShowMouse);
		o.addProperty("keystrokesModernStyle", keystrokesModernStyle);
		o.addProperty("keystrokesTextShadow", keystrokesTextShadow);
		o.addProperty("keystrokesOpticFont", keystrokesOpticFont);
		o.addProperty("keystrokesBackground", keystrokesBackground);
		o.addProperty("keystrokesShowLabels", keystrokesShowLabels);
		o.addProperty("keystrokesInstant", keystrokesInstant);
		o.addProperty("keystrokesFade", keystrokesFade);
		o.addProperty("keystrokesLockKeys", keystrokesLockKeys);
		o.addProperty("keystrokesFadeMs", keystrokesFadeMs);
		o.addProperty("keystrokesCpsMode", keystrokesCpsMode);
		o.addProperty("keystrokesScale", keystrokesScale);
		o.addProperty("keystrokesColor", toHex(keystrokesColor));
		o.addProperty("keystrokesTextColor", toHex(keystrokesTextColor));
		o.addProperty("keystrokesX", keystrokesX);
		o.addProperty("keystrokesY", keystrokesY);
		o.addProperty("keystrokesOffX0", keystrokesOffX0);
		o.addProperty("keystrokesOffY0", keystrokesOffY0);
		o.addProperty("keystrokesOffX1", keystrokesOffX1);
		o.addProperty("keystrokesOffY1", keystrokesOffY1);
		o.addProperty("keystrokesOffX2", keystrokesOffX2);
		o.addProperty("keystrokesOffY2", keystrokesOffY2);
		o.addProperty("keystrokesOffX3", keystrokesOffX3);
		o.addProperty("keystrokesOffY3", keystrokesOffY3);
		o.addProperty("keystrokesOffX4", keystrokesOffX4);
		o.addProperty("keystrokesOffY4", keystrokesOffY4);
		o.addProperty("keystrokesOffX5", keystrokesOffX5);
		o.addProperty("keystrokesOffY5", keystrokesOffY5);

		o.addProperty("pingShowMs", pingShowMs);
		o.addProperty("pingShowLabel", pingShowLabel);
		o.addProperty("pingBrackets", pingBrackets);
		o.addProperty("pingTextShadow", pingTextShadow);
		o.addProperty("pingOpticFont", pingOpticFont);
		o.addProperty("pingScale", pingScale);
		o.addProperty("pingColor", toHex(pingColor));
		o.addProperty("pingTextColor", toHex(pingTextColor));
		o.addProperty("pingX", pingX);
		o.addProperty("pingY", pingY);

		o.addProperty("coordinatesShowY", coordinatesShowY);
		o.addProperty("coordinatesShowLabel", coordinatesShowLabel);
		o.addProperty("coordinatesBrackets", coordinatesBrackets);
		o.addProperty("coordinatesTextShadow", coordinatesTextShadow);
		o.addProperty("coordinatesOpticFont", coordinatesOpticFont);
		o.addProperty("coordinatesScale", coordinatesScale);
		o.addProperty("coordinatesDecimals", coordinatesDecimals);
		o.addProperty("coordinatesColor", toHex(coordinatesColor));
		o.addProperty("coordinatesTextColor", toHex(coordinatesTextColor));
		o.addProperty("coordinatesX", coordinatesX);
		o.addProperty("coordinatesY", coordinatesY);

		o.addProperty("toggleSprintShowHud", toggleSprintShowHud);
		o.addProperty("toggleSprintBrackets", toggleSprintBrackets);
		o.addProperty("toggleSprintTextShadow", toggleSprintTextShadow);
		o.addProperty("toggleSprintOpticFont", toggleSprintOpticFont);
		o.addProperty("toggleSprintScale", toggleSprintScale);
		o.addProperty("toggleSprintColor", toHex(toggleSprintColor));
		o.addProperty("toggleSprintTextColor", toHex(toggleSprintTextColor));
		o.addProperty("toggleSprintX", toggleSprintX);
		o.addProperty("toggleSprintY", toggleSprintY);

		o.addProperty("fullbrightGamma", fullbrightGamma);
		o.addProperty("animHitbox", animHitbox);
		o.addProperty("animInventory", animInventory);
		o.addProperty("animHealth", animHealth);
		o.addProperty("animDamage", animDamage);
		o.addProperty("animSneaking", animSneaking);
		o.addProperty("animOldEnchantGlint", animOldEnchantGlint);
		o.addProperty("anim18Skins", anim18Skins);
		o.addProperty("animTitles", animTitles);
		o.addProperty("animFixGhostLiquid", animFixGhostLiquid);
		o.addProperty("animItemHold", animItemHold);
		o.addProperty("animItemSwitch", animItemSwitch);
		o.addProperty("animBlockHit", animBlockHit);
		o.addProperty("animEating", animEating);
		o.addProperty("animThrowing", animThrowing);
		o.addProperty("animFishingRod", animFishingRod);
		o.addProperty("animSwordSwing", animSwordSwing);
		o.addProperty("animBow", animBow);
		o.addProperty("animLeftHand", animLeftHand);
		o.addProperty("animMovableTitles", animMovableTitles);
		o.addProperty("titleOffsetX", titleOffsetX);
		o.addProperty("titleOffsetY", titleOffsetY);
		o.addProperty("showCriticalHitParticles", showCriticalHitParticles);
		o.addProperty("alwaysSharpnessParticles", alwaysSharpnessParticles);
		o.addProperty("particleMultiplier", particleMultiplier);
		o.addProperty("removeVanillaColorFilter", removeVanillaColorFilter);
		o.addProperty("renderOwnParticles", renderOwnParticles);
		o.addProperty("blockBreakParticles", blockBreakParticles);
		o.addProperty("blockOverlayFill", blockOverlayFill);
		o.addProperty("blockOverlayFillColor", toHex(blockOverlayFillColor));
		o.addProperty("blockOverlayFillAlpha", blockOverlayFillAlpha);
		o.addProperty("blockOverlayOutline", blockOverlayOutline);
		o.addProperty("blockOverlayOutlineColor", toHex(blockOverlayOutlineColor));
		o.addProperty("blockOverlayOutlineAlpha", blockOverlayOutlineAlpha);
		o.addProperty("blockOverlayLineWidth", blockOverlayLineWidth);
		o.addProperty("nametagsShowInF5", nametagsShowInF5);
		o.addProperty("nametagsShowInF1", nametagsShowInF1);
		o.addProperty("nametagsTextShadow", nametagsTextShadow);
		o.addProperty("nametagsBadlionFont", nametagsBadlionFont);
		o.addProperty("nametagsRoundedCorners", nametagsRoundedCorners);
		o.addProperty("nametagsBackground", nametagsBackground);
		o.addProperty("nametagsBackgroundColor", toHex(nametagsBackgroundColor));
		o.addProperty("nametagsPersonalColor", toHex(nametagsPersonalColor));
		o.addProperty("nametagNameColors", writeNameColorMap(nametagNameColors));
		o.addProperty("chatBold", chatBold);
		o.addProperty("chatAntiSpam", chatAntiSpam);
		o.addProperty("chatInfiniteHistory", chatInfiniteHistory);
		o.addProperty("chatBackgroundOpacity", chatBackgroundOpacity);
		o.addProperty("scoreboardShowNumbers", scoreboardShowNumbers);
		o.addProperty("scoreboardRemoveNumberWidth", scoreboardRemoveNumberWidth);
		o.addProperty("scoreboardTextShadow", scoreboardTextShadow);
		o.addProperty("scoreboardBackgroundOpacity", scoreboardBackgroundOpacity);
		o.addProperty("scoreboardX", scoreboardX);
		o.addProperty("scoreboardY", scoreboardY);
		o.addProperty("hotbarScale", hotbarScale);
		o.addProperty("hotbarRotation", hotbarRotation);
		o.addProperty("hotbarX", hotbarX);
		o.addProperty("hotbarY", hotbarY);
		o.addProperty("skyType", skyType);
		o.addProperty("freezeTime", freezeTime);
		o.addProperty("timePreset", timePreset);
		o.addProperty("timeOfDay", timeOfDay);
		o.addProperty("weatherMode", weatherMode);
		o.addProperty("disableHurtCam", disableHurtCam);
		o.addProperty("hurtCamIntensity", hurtCamIntensity);
		o.addProperty("fovMod", fovMod);
		o.addProperty("fov", fov);
		o.addProperty("dynamicFov", dynamicFov);
		o.addProperty("rawMouseInput", rawMouseInput);
		o.addProperty("mouseDelayFix", mouseDelayFix);
		o.addProperty("borderlessFullscreen", borderlessFullscreen);
		o.addProperty("disableMouseScrollWheel", disableMouseScrollWheel);
		o.addProperty("itemInfo", itemInfo);
		o.addProperty("fpsOptimizer", fpsOptimizer);
		o.addProperty("fastRender", fastRender);
		o.addProperty("fastMath", fastMath);
		o.addProperty("smoothFps", smoothFps);
		o.addProperty("fog", fog);
		o.addProperty("clouds", clouds);
		o.addProperty("sky", sky);
		o.addProperty("stars", stars);
		o.addProperty("weather", weather);
		o.addProperty("entityShadows", entityShadows);
		o.addProperty("animWater", animWater);
		o.addProperty("animLava", animLava);
		o.addProperty("animFire", animFire);
		o.addProperty("animPortal", animPortal);
		o.addProperty("animTerrain", animTerrain);
		o.addProperty("favoriteModules", writeIntSet(favoriteModules));
		return o;
	}

	public void applyProfileJson(JsonObject o) {
		fps = getBool(o, "fps", fps);
		cps = getBool(o, "cps", cps);
		keystrokes = getBool(o, "keystrokes", keystrokes);
		coordinates = getBool(o, "coordinates", coordinates);
		ping = getBool(o, "ping", ping);
		toggleSprint = getBool(o, "toggleSprint", toggleSprint);
		fullbright = getBool(o, "fullbright", fullbright);
		animations = getBool(o, "animations", animations);
		particles = getBool(o, "particles", particles);
		blockOverlay = getBool(o, "blockOverlay", blockOverlay);
		nametags = getBool(o, "nametags", nametags);
		chat = getBool(o, "chat", chat);
		scoreboard = getBool(o, "scoreboard", scoreboard);
		hotbar = getBool(o, "hotbar", hotbar);
		timeChanger = getBool(o, "timeChanger", timeChanger);
		weatherChanger = getBool(o, "weatherChanger", weatherChanger);
		hurtCam = getBool(o, "hurtCam", hurtCam);
		zoom = getBool(o, "zoom", zoom);
		zoomKeyCode = getInt(o, "zoomKeyCode", zoomKeyCode);
		zoomFactor = clampFloat(getFloat(o, "zoomFactor", zoomFactor), 1.25F, 10.0F);
		zoomSmooth = getBool(o, "zoomSmooth", zoomSmooth);
		zoomSmoothCamera = getBool(o, "zoomSmoothCamera", zoomSmoothCamera);
		zoomMouseSensitivity = getBool(
			o,
			"zoomMouseSensitivity",
			getBool(o, "zoomReduceSensitivity", zoomMouseSensitivity)
		);
		zoomScroll = getBool(o, "zoomScroll", getBool(o, "zoomScrollAdjust", zoomScroll));
		zoomScrollSpeed = clampFloat(getFloat(o, "zoomScrollSpeed", zoomScrollSpeed), 0.05F, 1.0F);
		freeLook = getBool(o, "freeLook", freeLook);
		freeLookKeyCode = getInt(o, "freeLookKeyCode", freeLookKeyCode);
		freeLookToggle = getBool(o, "freeLookToggle", freeLookToggle);
		freeLookDistance = clampFloat(getFloat(o, "freeLookDistance", freeLookDistance), 1.5F, 12.0F);
		hitboxes = getBool(o, "hitboxes", hitboxes);
		hitboxHideLook = getBool(o, "hitboxHideLook", hitboxHideLook);
		hitboxPlayerColor = getColor(o, "hitboxPlayerColor", hitboxPlayerColor);
		hitboxMobColor = getColor(o, "hitboxMobColor", hitboxMobColor);
		hitboxArrowColor = getColor(o, "hitboxArrowColor", hitboxArrowColor);
		hitboxOtherColor = getColor(o, "hitboxOtherColor", hitboxOtherColor);
		hitPredict = getBool(o, "hitPredict", hitPredict);
		boolean legacyShadow = getBool(o, "hudShadow", true);

		fpsShowLabel = getBool(o, "fpsShowLabel", fpsShowLabel);
		fpsBrackets = getBool(o, "fpsBrackets", fpsBrackets);
		fpsTextShadow = getBool(o, "fpsTextShadow", legacyShadow);
		fpsOpticFont = getBool(o, "fpsOpticFont", fpsOpticFont);
		fpsScale = clampFloat(getFloat(o, "fpsScale", fpsScale), 0.5F, 3.0F);
		fpsColor = getColor(o, "fpsColor", fpsColor);
		fpsTextColor = getColor(o, "fpsTextColor", fpsTextColor);
		fpsX = getInt(o, "fpsX", fpsX);
		fpsY = getInt(o, "fpsY", fpsY);

		cpsSeparateButtons = getBool(o, "cpsSeparateButtons", cpsSeparateButtons);
		cpsShowLabel = getBool(o, "cpsShowLabel", cpsShowLabel);
		cpsBrackets = getBool(o, "cpsBrackets", cpsBrackets);
		cpsTextShadow = getBool(o, "cpsTextShadow", legacyShadow);
		cpsOpticFont = getBool(o, "cpsOpticFont", cpsOpticFont);
		cpsScale = clampFloat(getFloat(o, "cpsScale", cpsScale), 0.5F, 3.0F);
		cpsColor = getColor(o, "cpsColor", cpsColor);
		cpsTextColor = getColor(o, "cpsTextColor", cpsTextColor);
		cpsX = getInt(o, "cpsX", cpsX);
		cpsY = getInt(o, "cpsY", cpsY);

		keystrokesShowMouse = getBool(o, "keystrokesShowMouse", keystrokesShowMouse);
		keystrokesModernStyle = getBool(o, "keystrokesModernStyle", keystrokesModernStyle);
		keystrokesTextShadow = getBool(o, "keystrokesTextShadow", legacyShadow);
		keystrokesOpticFont = getBool(o, "keystrokesOpticFont", keystrokesOpticFont);
		keystrokesBackground = getBool(o, "keystrokesBackground", keystrokesBackground);
		keystrokesShowLabels = getBool(o, "keystrokesShowLabels", keystrokesShowLabels);
		keystrokesInstant = getBool(o, "keystrokesInstant", keystrokesInstant);
		keystrokesFade = getBool(o, "keystrokesFade", keystrokesFade);
		keystrokesLockKeys = getBool(o, "keystrokesLockKeys", keystrokesLockKeys);
		keystrokesFadeMs = clampFloat(getFloat(o, "keystrokesFadeMs", keystrokesFadeMs), 50.0F, 1000.0F);
		keystrokesCpsMode = clamp(getInt(o, "keystrokesCpsMode", keystrokesCpsMode), 0, 3);
		keystrokesScale = clampFloat(getFloat(o, "keystrokesScale", keystrokesScale), 0.5F, 3.0F);
		keystrokesColor = getColor(o, "keystrokesColor", keystrokesColor);
		keystrokesTextColor = getColor(o, "keystrokesTextColor", keystrokesTextColor);
		keystrokesX = getInt(o, "keystrokesX", keystrokesX);
		keystrokesY = getInt(o, "keystrokesY", keystrokesY);
		keystrokesOffX0 = getFloat(o, "keystrokesOffX0", keystrokesOffX0);
		keystrokesOffY0 = getFloat(o, "keystrokesOffY0", keystrokesOffY0);
		keystrokesOffX1 = getFloat(o, "keystrokesOffX1", keystrokesOffX1);
		keystrokesOffY1 = getFloat(o, "keystrokesOffY1", keystrokesOffY1);
		keystrokesOffX2 = getFloat(o, "keystrokesOffX2", keystrokesOffX2);
		keystrokesOffY2 = getFloat(o, "keystrokesOffY2", keystrokesOffY2);
		keystrokesOffX3 = getFloat(o, "keystrokesOffX3", keystrokesOffX3);
		keystrokesOffY3 = getFloat(o, "keystrokesOffY3", keystrokesOffY3);
		keystrokesOffX4 = getFloat(o, "keystrokesOffX4", keystrokesOffX4);
		keystrokesOffY4 = getFloat(o, "keystrokesOffY4", keystrokesOffY4);
		keystrokesOffX5 = getFloat(o, "keystrokesOffX5", keystrokesOffX5);
		keystrokesOffY5 = getFloat(o, "keystrokesOffY5", keystrokesOffY5);

		pingShowMs = getBool(o, "pingShowMs", pingShowMs);
		pingShowLabel = getBool(o, "pingShowLabel", pingShowLabel);
		pingBrackets = getBool(o, "pingBrackets", pingBrackets);
		pingTextShadow = getBool(o, "pingTextShadow", legacyShadow);
		pingOpticFont = getBool(o, "pingOpticFont", pingOpticFont);
		pingScale = clampFloat(getFloat(o, "pingScale", pingScale), 0.5F, 3.0F);
		pingColor = getColor(o, "pingColor", pingColor);
		pingTextColor = getColor(o, "pingTextColor", pingTextColor);
		pingX = getInt(o, "pingX", pingX);
		pingY = getInt(o, "pingY", pingY);

		coordinatesShowY = getBool(o, "coordinatesShowY", coordinatesShowY);
		coordinatesShowLabel = getBool(o, "coordinatesShowLabel", coordinatesShowLabel);
		coordinatesBrackets = getBool(o, "coordinatesBrackets", coordinatesBrackets);
		coordinatesTextShadow = getBool(o, "coordinatesTextShadow", legacyShadow);
		coordinatesOpticFont = getBool(o, "coordinatesOpticFont", coordinatesOpticFont);
		coordinatesScale = clampFloat(getFloat(o, "coordinatesScale", coordinatesScale), 0.5F, 3.0F);
		coordinatesDecimals = clamp(getInt(o, "coordinatesDecimals", coordinatesDecimals), 0, 2);
		coordinatesColor = getColor(o, "coordinatesColor", coordinatesColor);
		coordinatesTextColor = getColor(o, "coordinatesTextColor", coordinatesTextColor);
		coordinatesX = getInt(o, "coordinatesX", coordinatesX);
		coordinatesY = getInt(o, "coordinatesY", coordinatesY);

		toggleSprintShowHud = getBool(o, "toggleSprintShowHud", toggleSprintShowHud);
		toggleSprintBrackets = getBool(o, "toggleSprintBrackets", toggleSprintBrackets);
		toggleSprintTextShadow = getBool(o, "toggleSprintTextShadow", legacyShadow);
		toggleSprintOpticFont = getBool(o, "toggleSprintOpticFont", toggleSprintOpticFont);
		toggleSprintScale = clampFloat(getFloat(o, "toggleSprintScale", toggleSprintScale), 0.5F, 3.0F);
		toggleSprintColor = getColor(o, "toggleSprintColor", toggleSprintColor);
		toggleSprintTextColor = getColor(o, "toggleSprintTextColor", toggleSprintTextColor);
		toggleSprintX = getInt(o, "toggleSprintX", toggleSprintX);
		toggleSprintY = getInt(o, "toggleSprintY", toggleSprintY);

		fullbrightGamma = clampFloat(getFloat(o, "fullbrightGamma", fullbrightGamma), 1.0F, 16.0F);
		animHitbox = getBool(o, "animHitbox", animHitbox);
		animInventory = getBool(o, "animInventory", animInventory);
		animHealth = getBool(o, "animHealth", animHealth);
		animDamage = getBool(o, "animDamage", animDamage);
		animSneaking = getBool(o, "animSneaking", animSneaking);
		animOldEnchantGlint = getBool(o, "animOldEnchantGlint", animOldEnchantGlint);
		anim18Skins = getBool(o, "anim18Skins", anim18Skins);
		animTitles = getBool(o, "animTitles", animTitles);
		animFixGhostLiquid = getBool(o, "animFixGhostLiquid", animFixGhostLiquid);
		animItemHold = getBool(o, "animItemHold", animItemHold);
		animItemSwitch = getBool(o, "animItemSwitch", animItemSwitch);
		animBlockHit = getBool(o, "animBlockHit", animBlockHit);
		animEating = getBool(o, "animEating", animEating);
		animThrowing = getBool(o, "animThrowing", animThrowing);
		animFishingRod = getBool(o, "animFishingRod", animFishingRod);
		animSwordSwing = getBool(o, "animSwordSwing", animSwordSwing);
		animBow = getBool(o, "animBow", animBow);
		animLeftHand = getBool(o, "animLeftHand", animLeftHand);
		animMovableTitles = getBool(o, "animMovableTitles", animMovableTitles);
		titleOffsetX = getInt(o, "titleOffsetX", titleOffsetX);
		titleOffsetY = getInt(o, "titleOffsetY", titleOffsetY);
		showCriticalHitParticles = getBool(o, "showCriticalHitParticles", showCriticalHitParticles);
		alwaysSharpnessParticles = getBool(o, "alwaysSharpnessParticles", alwaysSharpnessParticles);
		particleMultiplier = clampFloat(getFloat(o, "particleMultiplier", particleMultiplier), 0.0F, 10.0F);
		removeVanillaColorFilter = getBool(o, "removeVanillaColorFilter", removeVanillaColorFilter);
		renderOwnParticles = getBool(o, "renderOwnParticles", renderOwnParticles);
		blockBreakParticles = getBool(o, "blockBreakParticles", blockBreakParticles);
		blockOverlayFill = getBool(o, "blockOverlayFill", blockOverlayFill);
		blockOverlayFillColor = getColor(o, "blockOverlayFillColor", blockOverlayFillColor);
		blockOverlayFillAlpha = clampFloat(getFloat(o, "blockOverlayFillAlpha", blockOverlayFillAlpha), 0.0F, 1.0F);
		blockOverlayOutline = getBool(o, "blockOverlayOutline", blockOverlayOutline);
		blockOverlayOutlineColor = getColor(o, "blockOverlayOutlineColor", blockOverlayOutlineColor);
		blockOverlayOutlineAlpha = clampFloat(getFloat(o, "blockOverlayOutlineAlpha", blockOverlayOutlineAlpha), 0.0F, 1.0F);
		blockOverlayLineWidth = clampFloat(getFloat(o, "blockOverlayLineWidth", blockOverlayLineWidth), 1.0F, 8.0F);
		nametagsShowInF5 = getBool(o, "nametagsShowInF5", nametagsShowInF5);
		nametagsShowInF1 = getBool(o, "nametagsShowInF1", nametagsShowInF1);
		nametagsTextShadow = getBool(o, "nametagsTextShadow", nametagsTextShadow);
		nametagsBadlionFont = getBool(o, "nametagsBadlionFont", nametagsBadlionFont);
		nametagsRoundedCorners = getBool(o, "nametagsRoundedCorners", nametagsRoundedCorners);
		nametagsBackground = getBool(o, "nametagsBackground", nametagsBackground);
		nametagsBackgroundColor = getColor(o, "nametagsBackgroundColor", nametagsBackgroundColor);
		nametagsPersonalColor = getColor(o, "nametagsPersonalColor", nametagsPersonalColor);
		if (o.has("nametagNameColors")) {
			nametagNameColors.clear();
			readNameColorMap(o.get("nametagNameColors").getAsString(), nametagNameColors);
		}
		chatBold = getBool(o, "chatBold", chatBold);
		chatAntiSpam = getBool(o, "chatAntiSpam", chatAntiSpam);
		chatInfiniteHistory = getBool(o, "chatInfiniteHistory", chatInfiniteHistory);
		chatBackgroundOpacity = clampFloat(getFloat(o, "chatBackgroundOpacity", chatBackgroundOpacity), 0.0F, 100.0F);
		scoreboardShowNumbers = getBool(o, "scoreboardShowNumbers", scoreboardShowNumbers);
		scoreboardRemoveNumberWidth = getBool(o, "scoreboardRemoveNumberWidth", scoreboardRemoveNumberWidth);
		scoreboardTextShadow = getBool(o, "scoreboardTextShadow", scoreboardTextShadow);
		scoreboardBackgroundOpacity = clampFloat(getFloat(o, "scoreboardBackgroundOpacity", scoreboardBackgroundOpacity), 0.0F, 100.0F);
		scoreboardX = getInt(o, "scoreboardX", scoreboardX);
		scoreboardY = getInt(o, "scoreboardY", scoreboardY);
		hotbarScale = clampFloat(getFloat(o, "hotbarScale", hotbarScale), 0.5F, 3.0F);
		hotbarRotation = normalizeHotbarRotation(getInt(o, "hotbarRotation", hotbarRotation));
		hotbarX = getInt(o, "hotbarX", hotbarX);
		hotbarY = getInt(o, "hotbarY", hotbarY);
		skyType = Math.max(0, Math.min(2, getInt(o, "skyType", skyType)));
		freezeTime = getBool(o, "freezeTime", freezeTime);
		timePreset = Math.max(0, Math.min(4, getInt(o, "timePreset", timePreset)));
		timeOfDay = clampFloat(getFloat(o, "timeOfDay", timeOfDay), 0.0F, 23999.0F);
		weatherMode = Math.max(0, Math.min(3, getInt(o, "weatherMode", weatherMode)));
		disableHurtCam = getBool(o, "disableHurtCam", disableHurtCam);
		hurtCamIntensity = clampFloat(getFloat(o, "hurtCamIntensity", hurtCamIntensity), 0.0F, 2.0F);
		fovMod = getBool(o, "fovMod", fovMod);
		fov = clampFloat(getFloat(o, "fov", fov), 30.0F, 150.0F);
		dynamicFov = getBool(o, "dynamicFov", dynamicFov);
		// Settings tab is per-profile. Start from defaults so a profile never inherits the previous one.
		rawMouseInput = false;
		mouseDelayFix = true;
		borderlessFullscreen = false;
		disableMouseScrollWheel = false;
		itemInfo = true;
		fpsOptimizer = false;
		fastRender = true;
		fastMath = true;
		smoothFps = false;
		fog = false;
		clouds = false;
		sky = false;
		stars = false;
		weather = false;
		entityShadows = false;
		animWater = false;
		animLava = false;
		animFire = false;
		animPortal = false;
		animTerrain = false;
		rawMouseInput = getBool(o, "rawMouseInput", rawMouseInput);
		mouseDelayFix = getBool(o, "mouseDelayFix", mouseDelayFix);
		borderlessFullscreen = getBool(o, "borderlessFullscreen", borderlessFullscreen);
		disableMouseScrollWheel = getBool(o, "disableMouseScrollWheel", disableMouseScrollWheel);
		itemInfo = getBool(o, "itemInfo", itemInfo);
		fpsOptimizer = getBool(o, "fpsOptimizer", fpsOptimizer);
		fastRender = getBool(o, "fastRender", fastRender);
		fastMath = getBool(o, "fastMath", fastMath);
		smoothFps = getBool(o, "smoothFps", smoothFps);
		fog = getBool(o, "fog", fog);
		clouds = getBool(o, "clouds", clouds);
		sky = getBool(o, "sky", sky);
		stars = getBool(o, "stars", stars);
		weather = getBool(o, "weather", weather);
		entityShadows = getBool(o, "entityShadows", entityShadows);
		animWater = getBool(o, "animWater", animWater);
		animLava = getBool(o, "animLava", animLava);
		animFire = getBool(o, "animFire", animFire);
		animPortal = getBool(o, "animPortal", animPortal);
		animTerrain = getBool(o, "animTerrain", animTerrain);
		if (o.has("favoriteModules")) {
			readIntSet(o.get("favoriteModules").getAsString(), favoriteModules);
		}
		OpticPerf.sync(this);
	}

	/** True when the Animations mod is on and the given option is enabled (1.7-style). */
	public boolean anim(boolean option) {
		return animations && option;
	}

	public static String withBrackets(String text, boolean brackets) {
		return brackets ? "[" + text + "]" : text;
	}

	public static int normalizeHotbarRotation(int degrees) {
		int d = degrees % 360;
		if (d < 0) d += 360;
		if (d > 225) return 270;
		if (d > 135) return 180;
		if (d > 45) return 90;
		return 0;
	}

	public float keyOffX(int index) {
		switch (index) {
			case 0: return keystrokesOffX0;
			case 1: return keystrokesOffX1;
			case 2: return keystrokesOffX2;
			case 3: return keystrokesOffX3;
			case 4: return keystrokesOffX4;
			default: return keystrokesOffX5;
		}
	}

	public float keyOffY(int index) {
		switch (index) {
			case 0: return keystrokesOffY0;
			case 1: return keystrokesOffY1;
			case 2: return keystrokesOffY2;
			case 3: return keystrokesOffY3;
			case 4: return keystrokesOffY4;
			default: return keystrokesOffY5;
		}
	}

	public void setKeyOff(int index, float ox, float oy) {
		switch (index) {
			case 0: keystrokesOffX0 = ox; keystrokesOffY0 = oy; break;
			case 1: keystrokesOffX1 = ox; keystrokesOffY1 = oy; break;
			case 2: keystrokesOffX2 = ox; keystrokesOffY2 = oy; break;
			case 3: keystrokesOffX3 = ox; keystrokesOffY3 = oy; break;
			case 4: keystrokesOffX4 = ox; keystrokesOffY4 = oy; break;
			default: keystrokesOffX5 = ox; keystrokesOffY5 = oy; break;
		}
	}

	/** Reset one mod's options/colors/layout to built-in defaults. moduleId matches OpticModScreen.MODULE_*. */
	public void resetModule(int moduleId) {
		switch (moduleId) {
			case 10: // FPS
				fpsShowLabel = true;
				fpsBrackets = true;
				fpsTextShadow = true;
				fpsOpticFont = false;
				fpsScale = 1.0F;
				fpsColor = DEFAULT_ACCENT;
				fpsTextColor = DEFAULT_TEXT;
				fpsX = 6;
				fpsY = 6;
				break;
			case 11: // CPS
				cpsSeparateButtons = true;
				cpsShowLabel = true;
				cpsBrackets = true;
				cpsTextShadow = true;
				cpsOpticFont = false;
				cpsScale = 1.0F;
				cpsColor = DEFAULT_ACCENT;
				cpsTextColor = DEFAULT_TEXT;
				cpsX = 6;
				cpsY = 21;
				break;
			case 12: // Keystrokes
				keystrokesShowMouse = true;
				keystrokesModernStyle = true;
				keystrokesTextShadow = true;
				keystrokesOpticFont = false;
				keystrokesBackground = true;
				keystrokesShowLabels = true;
				keystrokesInstant = false;
				keystrokesFade = true;
				keystrokesLockKeys = true;
				keystrokesFadeMs = 200.0F;
				keystrokesCpsMode = 0;
				keystrokesScale = 1.0F;
				keystrokesColor = DEFAULT_ACCENT;
				keystrokesTextColor = DEFAULT_TEXT;
				keystrokesX = 6;
				keystrokesY = -1;
				keystrokesOffX0 = 21.0F; keystrokesOffY0 = 0.0F;
				keystrokesOffX1 = 0.0F; keystrokesOffY1 = 21.0F;
				keystrokesOffX2 = 21.0F; keystrokesOffY2 = 21.0F;
				keystrokesOffX3 = 42.0F; keystrokesOffY3 = 21.0F;
				keystrokesOffX4 = 0.0F; keystrokesOffY4 = 42.0F;
				keystrokesOffX5 = 32.0F; keystrokesOffY5 = 42.0F;
				break;
			case 13: // Ping
				pingShowMs = true;
				pingShowLabel = true;
				pingBrackets = true;
				pingTextShadow = true;
				pingOpticFont = false;
				pingScale = 1.0F;
				pingColor = DEFAULT_ACCENT;
				pingTextColor = DEFAULT_TEXT;
				pingX = 6;
				pingY = 36;
				break;
			case 14: // Coordinates
				coordinatesShowY = true;
				coordinatesShowLabel = true;
				coordinatesBrackets = true;
				coordinatesTextShadow = true;
				coordinatesOpticFont = false;
				coordinatesScale = 1.0F;
				coordinatesDecimals = 0;
				coordinatesColor = DEFAULT_ACCENT;
				coordinatesTextColor = DEFAULT_TEXT;
				coordinatesX = 6;
				coordinatesY = 51;
				break;
			case 15: // Toggle Sprint
				toggleSprintShowHud = false;
				toggleSprintBrackets = true;
				toggleSprintTextShadow = true;
				toggleSprintOpticFont = false;
				toggleSprintScale = 1.0F;
				toggleSprintColor = DEFAULT_ACCENT;
				toggleSprintTextColor = DEFAULT_TEXT;
				toggleSprintX = 6;
				toggleSprintY = 66;
				break;
			case 16: // Fullbright
				fullbrightGamma = 16.0F;
				break;
			case 17: // Animations
				animHitbox = true;
				animInventory = true;
				animHealth = true;
				animDamage = true;
				animSneaking = true;
				animOldEnchantGlint = true;
				anim18Skins = true;
				animTitles = true;
				animFixGhostLiquid = false;
				animItemHold = true;
				animItemSwitch = true;
				animBlockHit = true;
				animEating = true;
				animThrowing = true;
				animFishingRod = true;
				animSwordSwing = true;
				animBow = true;
				animLeftHand = false;
				animMovableTitles = false;
				titleOffsetX = 0;
				titleOffsetY = 0;
				break;
			case 18: // Particles
				particles = false;
				showCriticalHitParticles = true;
				alwaysSharpnessParticles = true;
				particleMultiplier = 1.0F;
				removeVanillaColorFilter = false;
				renderOwnParticles = true;
				blockBreakParticles = true;
				break;
			case 30: // FOV
				fov = 110.0F;
				break;
			case 19: // Block Overlay
				blockOverlayFill = true;
				blockOverlayFillColor = 0xFFFFFF;
				blockOverlayFillAlpha = 0.25F;
				blockOverlayOutline = true;
				blockOverlayOutlineColor = 0x000000;
				blockOverlayOutlineAlpha = 1.0F;
				blockOverlayLineWidth = 2.0F;
				break;
			case 20: // Nametags
				nametagsShowInF5 = true;
				nametagsShowInF1 = false;
				nametagsTextShadow = true;
				nametagsBadlionFont = false;
				nametagsRoundedCorners = false;
				nametagsBackground = true;
				nametagsBackgroundColor = 0x000000;
				nametagsPersonalColor = 0xFFFFFF;
				nametagNameColors.clear();
				break;
			case 21: // Chat
				chatBold = false;
				chatAntiSpam = false;
				chatInfiniteHistory = false;
				chatBackgroundOpacity = 100.0F;
				break;
			case 22: // Scoreboard
				scoreboardShowNumbers = true;
				scoreboardRemoveNumberWidth = false;
				scoreboardTextShadow = false;
				scoreboardBackgroundOpacity = 100.0F;
				scoreboardX = -1;
				scoreboardY = -1;
				break;
			case 23: // Hotbar
				hotbarScale = 1.0F;
				hotbarRotation = 0;
				hotbarX = -1;
				hotbarY = -1;
				break;
			case 24: // Time Changer
				skyType = 0;
				freezeTime = false;
				timePreset = OpticEnvironment.PRESET_MIDNIGHT;
				timeOfDay = 18000.0F;
				break;
			case 25: // Weather Changer
				weatherMode = OpticEnvironment.WEATHER_CLEAR;
				break;
			case 26: // Hurt Cam
				disableHurtCam = false;
				hurtCamIntensity = 1.0F;
				break;
			case 27: // Zoom
				zoomKeyCode = 46;
				zoomFactor = 4.0F;
				zoomSmooth = true;
				zoomSmoothCamera = false;
				zoomMouseSensitivity = true;
				zoomScroll = true;
				zoomScrollSpeed = 0.15F;
				break;
			case 31: // Free Look
				freeLookKeyCode = 56;
				freeLookToggle = false;
				freeLookDistance = 4.0F;
				break;
			case 28: // Hitboxes
				hitboxHideLook = false;
				hitboxPlayerColor = 0xFFFFFF;
				hitboxMobColor = 0x000000;
				hitboxArrowColor = 0xFFFF00;
				hitboxOtherColor = 0xFFFFFF;
				break;
			default:
				return;
		}
		save();
	}

	public int getNametagUserColor(String username, int fallback) {
		if (username == null || username.isEmpty()) return fallback & 0xFFFFFF;
		Integer color = nametagNameColors.get(username);
		return color != null ? color & 0xFFFFFF : fallback & 0xFFFFFF;
	}

	public void setNametagUserColor(String username, int color) {
		if (username == null || username.isEmpty()) return;
		nametagNameColors.put(username, color & 0xFFFFFF);
	}

	private static boolean getBool(JsonObject o, String key, boolean fallback) {
		return o.has(key) ? o.get(key).getAsBoolean() : fallback;
	}

	private static int getInt(JsonObject o, String key, int fallback) {
		try {
			return o.has(key) ? o.get(key).getAsInt() : fallback;
		} catch (Exception e) {
			return fallback;
		}
	}

	private static float getFloat(JsonObject o, String key, float fallback) {
		try {
			return o.has(key) ? o.get(key).getAsFloat() : fallback;
		} catch (Exception e) {
			return fallback;
		}
	}

	private static int getColor(JsonObject o, String key, int fallback) {
		if (!o.has(key)) return fallback & 0xFFFFFF;
		try {
			String raw = o.get(key).getAsString();
			if (raw.startsWith("#")) raw = raw.substring(1);
			return (int) Long.parseLong(raw, 16) & 0xFFFFFF;
		} catch (Exception e) {
			return fallback & 0xFFFFFF;
		}
	}

	private static int whiteIfLegacy(int color) {
		int rgb = color & 0xFFFFFF;
		if (rgb == 0x66CC33) return DEFAULT_ACCENT;
		return rgb;
	}

	private static boolean read(Properties values, String key, boolean fallback) {
		return Boolean.parseBoolean(values.getProperty(key, Boolean.toString(fallback)));
	}

	private static int readInt(Properties values, String key, int fallback) {
		try {
			return Integer.parseInt(values.getProperty(key, Integer.toString(fallback)));
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	private static float readFloat(Properties values, String key, float fallback) {
		try {
			return Float.parseFloat(values.getProperty(key, Float.toString(fallback)));
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	private static int readColor(Properties values, String key, int fallback) {
		String raw = values.getProperty(key);
		if (raw == null || raw.isEmpty()) return fallback & 0xFFFFFF;
		try {
			if (raw.startsWith("#")) raw = raw.substring(1);
			if (raw.startsWith("0x") || raw.startsWith("0X")) raw = raw.substring(2);
			return (int) Long.parseLong(raw, 16) & 0xFFFFFF;
		} catch (NumberFormatException e) {
			return fallback & 0xFFFFFF;
		}
	}

	private static String toHex(int rgb) {
		return String.format("%06X", rgb & 0xFFFFFF);
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private static float clampFloat(float value, float min, float max) {
		return Math.max(min, Math.min(max, value));
	}

	private static String writeNameColorMap(Map<String, Integer> map) {
		if (map == null || map.isEmpty()) return "";
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, Integer> e : map.entrySet()) {
			if (e.getKey() == null || e.getKey().isEmpty()) continue;
			if (sb.length() > 0) sb.append(';');
			sb.append(e.getKey().replace(';', '_').replace(':', '_'));
			sb.append(':');
			sb.append(toHex(e.getValue() == null ? DEFAULT_TEXT : e.getValue()));
		}
		return sb.toString();
	}

	private static void readNameColorMap(String raw, Map<String, Integer> out) {
		out.clear();
		if (raw == null || raw.isEmpty()) return;
		String[] entries = raw.split(";");
		for (String entry : entries) {
			if (entry == null || entry.isEmpty()) continue;
			int colon = entry.indexOf(':');
			if (colon <= 0 || colon >= entry.length() - 1) continue;
			String name = entry.substring(0, colon).trim();
			String hex = entry.substring(colon + 1).trim();
			if (name.isEmpty() || hex.isEmpty()) continue;
			try {
				if (hex.startsWith("#")) hex = hex.substring(1);
				out.put(name, (int) Long.parseLong(hex, 16) & 0xFFFFFF);
			} catch (NumberFormatException ignored) {
			}
		}
	}

	private static String writeIntSet(Set<Integer> set) {
		if (set == null || set.isEmpty()) return "";
		StringBuilder sb = new StringBuilder();
		for (Integer id : set) {
			if (id == null) continue;
			if (sb.length() > 0) sb.append(',');
			sb.append(id.intValue());
		}
		return sb.toString();
	}

	private static void readIntSet(String raw, Set<Integer> out) {
		out.clear();
		if (raw == null || raw.isEmpty()) return;
		String[] parts = raw.split(",");
		for (int i = 0; i < parts.length; i++) {
			String part = parts[i] == null ? "" : parts[i].trim();
			if (part.isEmpty()) continue;
			try {
				out.add(Integer.valueOf(Integer.parseInt(part)));
			} catch (NumberFormatException ignored) {
			}
		}
	}
}
