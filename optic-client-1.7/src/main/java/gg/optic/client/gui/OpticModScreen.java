package gg.optic.client.gui;

import gg.optic.client.OpticClient;
import gg.optic.client.OpticFreeLook;
import gg.optic.client.OpticConfig;
import gg.optic.client.OpticEnvironment;
import gg.optic.client.OpticFrame;
import gg.optic.client.OpticProfiles;
import gg.optic.client.OpticSmooth;
import gg.optic.client.OpticTextures;
import gg.optic.client.OpticUiDraw;
import gg.optic.client.OpticUiFont;
import gg.optic.client.hud.OpticCrosshair;
import gg.optic.client.display.OpticBorderless;
import gg.optic.client.input.OpticRawInput;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.Window;
import net.minecraft.util.Identifier;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import net.minecraft.entity.player.PlayerEntity;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class OpticModScreen extends Screen {
	public static final int MODULE_FPS = 10;
	public static final int MODULE_CPS = 11;
	public static final int MODULE_KEYSTROKES = 12;
	public static final int MODULE_PING = 13;
	public static final int MODULE_COORDINATES = 14;
	public static final int MODULE_TOGGLE_SPRINT = 15;
	public static final int MODULE_FULLBRIGHT = 16;
	public static final int MODULE_ANIMATIONS = 17;
	public static final int MODULE_PARTICLES = 18;
	public static final int MODULE_BLOCK_OVERLAY = 19;
	public static final int MODULE_NAMETAGS = 20;
	public static final int MODULE_CHAT = 21;
	public static final int MODULE_SCOREBOARD = 22;
	public static final int MODULE_HOTBAR = 23;
	public static final int MODULE_TIME_CHANGER = 24;
	public static final int MODULE_WEATHER_CHANGER = 25;
	public static final int MODULE_HURT_CAM = 26;
	public static final int MODULE_ZOOM = 27;
	public static final int MODULE_HITBOXES = 28;
	public static final int MODULE_HIT_PREDICT = 29;
	public static final int MODULE_FOV = 30;
	public static final int MODULE_FREE_LOOK = 31;
	public static final int MODULE_CROSSHAIR = 32;
	public static final int MODULE_GUI_SCALE = 33;
	public static final int MODULE_TAB = 34;
	public static final int MODULE_TITLES = 35;
	public static final int MODULE_HIT_COLOR = 36;
	public static final int MODULE_POTIONS = 37;
	public static final int MODULE_SKINS_3D = 38;
	public static final int MODULE_PVP_TWEAKS = 39;
	public static final int MODULE_BOSS_BAR = 40;
	public static final int MODULE_NICK_HIDER = 41;
	public static final int MODULE_MOTION_BLUR = 42;

	private static final int FPS = MODULE_FPS;
	private static final int CPS = MODULE_CPS;
	private static final int KEYSTROKES = MODULE_KEYSTROKES;
	private static final int PING = MODULE_PING;
	private static final int COORDINATES = MODULE_COORDINATES;
	private static final int TOGGLE_SPRINT = MODULE_TOGGLE_SPRINT;
	private static final int FULLBRIGHT = MODULE_FULLBRIGHT;
	private static final int ANIMATIONS = MODULE_ANIMATIONS;
	private static final int PARTICLES = MODULE_PARTICLES;
	private static final int BLOCK_OVERLAY = MODULE_BLOCK_OVERLAY;
	private static final int NAMETAGS = MODULE_NAMETAGS;
	private static final int CHAT = MODULE_CHAT;
	private static final int SCOREBOARD = MODULE_SCOREBOARD;
	private static final int HOTBAR = MODULE_HOTBAR;
	private static final int TIME_CHANGER = MODULE_TIME_CHANGER;
	private static final int WEATHER_CHANGER = MODULE_WEATHER_CHANGER;
	private static final int HURT_CAM = MODULE_HURT_CAM;
	private static final int ZOOM = MODULE_ZOOM;
	private static final int HITBOXES = MODULE_HITBOXES;
	private static final int HIT_PREDICT = MODULE_HIT_PREDICT;
	private static final int FOV = MODULE_FOV;
	private static final int FREE_LOOK = MODULE_FREE_LOOK;
	private static final int CROSSHAIR = MODULE_CROSSHAIR;
	private static final int GUI_SCALE = MODULE_GUI_SCALE;
	private static final int TAB = MODULE_TAB;
	private static final int TITLES = MODULE_TITLES;
	private static final int HIT_COLOR = MODULE_HIT_COLOR;
	private static final int POTIONS = MODULE_POTIONS;
	private static final int SKINS_3D = MODULE_SKINS_3D;
	private static final int PVP_TWEAKS = MODULE_PVP_TWEAKS;
	private static final int BOSS_BAR = MODULE_BOSS_BAR;
	private static final int NICK_HIDER = MODULE_NICK_HIDER;
	private static final int MOTION_BLUR = MODULE_MOTION_BLUR;

	// Colors and sizes live in OpticTheme so the look can be tuned in one place.
	private static final int ACCENT = OpticTheme.ACCENT;
	private static final int ACCENT_SOFT = OpticTheme.ACCENT_SOFT;
	private static final int ACCENT_DIM = OpticTheme.ACCENT_DIM;
	// Cosmetics-menu look: a light see-through grey body, black lines, nothing solid.
	private static final int PANEL = 0x40303236;
	private static final int TOPBAR = 0x00000000;
	// Boxes inside the menu: dark and mostly solid, so they stand out from the see-through body.
	private static final int CARD = 0x8C3A3D44;
	private static final int CARD_HOVER = 0xA6474B54;
	private static final int CARD_ON = CARD;
	private static final int BORDER = 0xFF4A4E57;
	private static final int BORDER_SOFT = 0xFF4A4E57;
	// Secondary text, kept bright enough to read over the game.
	private static final int MUTED = 0xFFC9CED8;
	private static final int OPTION_ROW = 0x00000000;
	private static final int SIDEBAR = 0x00000000;
	private static final int TAB_IDLE = 0x8C3A3D44;
	private static final int TAB_OUTLINE = BORDER;
	/** Activated mods and on switches: same green as an equipped item in Cosmetics. */
	private static final int LIME = OpticTheme.ACCENT;
	private static final int LIME_EDGE = OpticTheme.ACCENT;
	/** Divider lines: grey, one real pixel thick. */
	private static final int DIVIDER = 0xFF555962;
	/** Selected top tab and selected sidebar row: less see-through than everything else. */
	private static final int TAB_ACTIVE = 0xB3474B54;
	private static final int STAR_ON = OpticTheme.ACCENT;
	private static final int STAR_OFF = OpticTheme.ACCENT;
	private static final int SCRIM = OpticTheme.SCRIM;
	private static final int RADIUS = OpticTheme.RADIUS;
	private static final int RADIUS_SM = OpticTheme.RADIUS_SM;

	private final List<ModuleCard> modules = new ArrayList<ModuleCard>();
	private final List<CardBounds> cardBounds = new ArrayList<CardBounds>();
	private final List<OptionHit> optionHits = new ArrayList<OptionHit>();
	private ModuleCard selected;
	private int panelX;
	private int panelY;
	private int panelW;
	private int panelH;
	private int closeX;
	private int closeY;
	private int backX;
	private int backY;
	private int backW;
	private int backH;
	private SliderDrag activeSlider;
	private ColorDrag activeColor;
	private int configScroll;
	private String openColorKey;
	private String bindingKey;
	private int colorPopupX;
	private int colorPopupY;
	private int colorPopupW;
	private int colorPopupH;
	private static final int HEADER_LOGO = 28;
	private static final int COLOR_POPUP_W = 228;
	private static final int COLOR_POPUP_H = 150;
	private static final int TOGGLE_ON = LIME;
	private static final int TOGGLE_OFF = 0xFF2A2A32;

	private int editX;
	private int editY;
	private int editW = 92;
	private int editH = 22;
	private int sidebarW = 108;
	private int contentLeft;

	private enum MainTab {
		MODS,
		SETTINGS,
		PROFILES
	}

	private enum SettingsCategory {
		GENERAL,
		PERFORMANCE
	}

	private enum ModFilter {
		ALL,
		FAVORITES,
		PVP
	}

	private int settingsScroll;
	private int modsScroll;
	private boolean modsScrollDrag;
	private int modsTrackX;
	private int modsTrackY;
	private int modsTrackW;
	private int modsTrackH;
	private int modsThumbH;
	private int modsMaxScroll;
	private boolean settingsScrollDrag;
	private int settingsTrackX;
	private int settingsTrackY;
	private int settingsTrackW;
	private int settingsTrackH;
	private int settingsThumbH;
	private int settingsMaxScroll;
	/** Rows outside this band are scrolled out of view: drawn clipped, and not clickable. */
	private int settingsClipTop = Integer.MIN_VALUE;
	private int settingsClipBottom = Integer.MAX_VALUE;
	private boolean configScrollDrag;
	private int configTrackX;
	private int configTrackY;
	private int configTrackW;
	private int configTrackH;
	private int configThumbH;
	private int configMaxScroll;
	private ModFilter modFilter = ModFilter.ALL;
	private int filterAllX;
	private int filterAllY;
	private int filterFavX;
	private int filterFavY;
	private int filterPvpX;
	private int filterPvpY;
	private int filterRowW;
	private int filterRowH = 22;
	private int modsGridX;
	private int modsGridY;
	private int modsGridW;
	private int modsGridH;

	private static MainTab resumeTab = MainTab.MODS;
	private static int resumeModule = -1;
	private static SettingsCategory resumeSettings = SettingsCategory.GENERAL;
	private static int resumeConfigScroll;
	private static int resumeModsScroll;
	private final boolean restorePlace;
	private MainTab mainTab = MainTab.MODS;
	private SettingsCategory settingsCategory = SettingsCategory.GENERAL;
	private int modsTabX;
	private int settingsTabX;
	private int profilesTabX;
	private int cosmeticsTabX;
	private int cosmeticsTabW;
	private int tabY;
	private int modsTabW;
	private int settingsTabW;
	private int profilesTabW;
	private final List<SettingHit> settingHits = new ArrayList<SettingHit>();
	private final List<ProfileHit> profileHits = new ArrayList<ProfileHit>();
	private TextFieldWidget profileNameField;
	private TextFieldWidget modSearchField;
	private TextFieldWidget settingsSearchField;
	private int modSearchX = -1;
	private int modSearchY = -1;
	private int modSearchW = -1;
	private int settingsSearchX = -1;
	private int settingsSearchY = -1;
	private int settingsSearchW = -1;
	private String profileStatus = "";
	private int profileScroll;
	private File renamingProfile;
	private boolean profileFieldOpen;
	private boolean profileSearchMode;
	private int profileFieldX = -1;
	private int profileFieldY = -1;
	private int profileFieldW = -1;
	private enum ProfileSide {
		NEW,
		SEARCH,
		OPEN_FOLDER
	}

	public OpticModScreen() {
		this(-1);
	}

	public OpticModScreen(int openModuleId) {
		restorePlace = openModuleId < 0;
		modules.add(new ModuleCard(BLOCK_OVERLAY, "Block Overlay", "Fill and outline on lookat block", "\uD83D\uDCE6"));
		modules.add(new ModuleCard(FOV, "FOV", "Field of view from 30 to 150", "\uD83D\uDC41"));
		modules.add(new ModuleCard(FREE_LOOK, "Free Look", "Look around without turning your player", "\u21BA"));
		modules.add(new ModuleCard(GUI_SCALE, "GUI Scale", "Crosshair, menus, and inventories", "\uD83D\uDD0D"));
		modules.add(new ModuleCard(HIT_COLOR, "Hit Color", "Custom damage tint", "\uD83D\uDCA5"));
		modules.add(new ModuleCard(HOTBAR, "Hotbar", "Move and resize the hotbar", "\uD83C\uDFAE"));
		modules.add(new ModuleCard(HURT_CAM, "Hurt Cam", "Damage camera shake", "\uD83D\uDC94"));
		modules.add(new ModuleCard(HITBOXES, "Hitboxes", "Colored entity boxes and look lines", "\uD83C\uDFAF"));
		modules.add(new ModuleCard(NAMETAGS, "Nametags", "Opacity, shadow, and toggles", "\uD83C\uDFF7"));
		modules.add(new ModuleCard(SKINS_3D, "3D Skins", "Skin outer layer in 3D", "\uD83D\uDC64"));
		modules.add(new ModuleCard(PVP_TWEAKS, "PotPvP", "Low fire, clear water, overlays", "\uD83D\uDD25"));
		modules.add(new ModuleCard(NICK_HIDER, "Nick Hider", "Hide names and skins on screen", "🎭"));
		modules.add(new ModuleCard(MOTION_BLUR, "Motion Blur", "Smooth trails when you move", "🌀"));
		modules.add(new ModuleCard(PARTICLES, "Particles", "Client-side hit particles", "\u2728"));
		modules.add(new ModuleCard(SCOREBOARD, "Scoreboard", "Sidebar scoreboard style", "\uD83D\uDCCB"));
		modules.add(new ModuleCard(TIME_CHANGER, "Time Changer", "Freeze time and sky type", "\uD83C\uDF19"));
		modules.add(new ModuleCard(WEATHER_CHANGER, "Weather Changer", "Client-side weather override", "\uD83C\uDF27"));
		modules.add(new ModuleCard(ZOOM, "Zoom", "Badlion-style hold zoom (C)", "\uD83D\uDD0D"));
		modules.add(new ModuleCard(CHAT, "Chat", "Spam filter, history, background", "\uD83D\uDCAC"));
		modules.add(new ModuleCard(COORDINATES, "Coordinates", "Position, facing, and biome", "\uD83D\uDCCD"));
		modules.add(new ModuleCard(CPS, "CPS", "Left and right click speed", "\uD83D\uDDB1"));
		modules.add(new ModuleCard(CROSSHAIR, "Crosshair", "Shape, color, and dynamic gap", "\uD83C\uDFAF"));
		modules.add(new ModuleCard(FPS, "FPS", "Frame counter", "\uD83D\uDCCA"));
		modules.add(new ModuleCard(FULLBRIGHT, "Fullbright", "Maximum brightness", "\u2600\uFE0F"));
		modules.add(new ModuleCard(KEYSTROKES, "Keystrokes", "WASD, clicks, and spacebar", "\u2328\uFE0F"));
		modules.add(new ModuleCard(PING, "Ping", "Latency, spikes, and nametags", "\uD83D\uDCF6"));
		modules.add(new ModuleCard(POTIONS, "Potion Status", "Effect names and timers", "\uD83E\uDDEA"));
		modules.add(new ModuleCard(TAB, "Tab", "Player list colors and ping", "\uD83D\uDC65"));
		modules.add(new ModuleCard(TOGGLE_SPRINT, "Toggle Sprint", "Persistent sprint", "\uD83C\uDFC3"));

		Collections.sort(modules, new Comparator<ModuleCard>() {
			@Override
			public int compare(ModuleCard left, ModuleCard right) {
				return left.name.compareToIgnoreCase(right.name);
			}
		});
		if (openModuleId >= 0) {
			mainTab = MainTab.MODS;
			selected = findModule(openModuleId);
		} else {
			mainTab = resumeTab;
			settingsCategory = resumeSettings;
			if (resumeTab == MainTab.MODS && resumeModule >= 0) selected = findModule(resumeModule);
		}
	}

	public static OpticModScreen openModule(int moduleId) {
		return new OpticModScreen(moduleId);
	}

	@Override
	public void init() {
		buttons.clear();
		Keyboard.enableRepeatEvents(true);
		// Stay inside the game window. A fixed minimum used to push the last column off the screen.
		int margin = 12;
		panelW = Math.min(560, Math.max(1, width - margin * 2));
		panelH = Math.min(330, Math.max(1, height - margin * 2));
		panelX = (width - panelW) / 2;
		panelY = (height - panelH) / 2;
		closeX = panelX + panelW - 26;
		closeY = panelY + 10;
		backX = panelX + 14;
		backY = panelY + 58;
		backW = 54;
		backH = 16;
		activeSlider = null;
		activeColor = null;
		openColorKey = null;
		configScroll = restorePlace ? resumeConfigScroll : 0;
		modsScroll = restorePlace ? resumeModsScroll : 0;
		editX = panelX + 10;
		editY = panelY + panelH - 34;
		editW = Math.max(88, sidebarW - 8);
		contentLeft = panelX + sidebarW + 14;
		int fieldX = panelX + 118;
		int fieldY = panelY + 58;
		profileNameField = new TextFieldWidget(textRenderer, fieldX, fieldY, Math.min(180, panelW - 140), 18);
		profileNameField.setMaxLength(32);
		profileNameField.setText("");
		profileScroll = 0;
		renamingProfile = null;
		profileFieldOpen = false;
		profileSearchMode = false;
		profileStatus = "";
	}

	@Override
	public void removed() {
		resumeTab = mainTab;
		resumeModule = selected == null ? -1 : selected.id;
		resumeSettings = settingsCategory;
		resumeConfigScroll = configScroll;
		resumeModsScroll = modsScroll;
		Keyboard.enableRepeatEvents(false);
		OpticClient.get().config().save();
	}

	@Override
	public void tick() {
		if (mainTab == MainTab.PROFILES && profileFieldOpen && profileNameField != null) {
			profileNameField.tick();
		}
		// Raw Input can keep LWJGL from seeing the wheel; scroll from WM_INPUT instead.
		int raw = OpticRawInput.consumeWheel();
		if (raw != 0 && !vanillaWheel) scroll(raw);
		vanillaWheel = false;
	}

	private boolean enabled(int id) {
		OpticConfig c = OpticClient.get().config();
		switch (id) {
			case FPS: return c.fps;
			case CPS: return c.cps;
			case KEYSTROKES: return c.keystrokes;
			case PING: return c.ping;
			case COORDINATES: return c.coordinates;
			case TOGGLE_SPRINT: return c.toggleSprint;
			case FULLBRIGHT: return c.fullbright;
			case ANIMATIONS: return c.animations;
			case PARTICLES: return c.particles;
			case BLOCK_OVERLAY: return c.blockOverlay;
			case NAMETAGS: return c.nametags;
			case CHAT: return c.chat;
			case SCOREBOARD: return c.scoreboard;
			case HOTBAR: return c.hotbar;
			case TIME_CHANGER: return c.timeChanger;
			case WEATHER_CHANGER: return c.weatherChanger;
			case HURT_CAM: return c.hurtCam;
			case ZOOM: return c.zoom;
			case HITBOXES: return c.hitboxes;
			case SKINS_3D: return c.skins3d;
			case PVP_TWEAKS: return c.pvpTweaks;
			case BOSS_BAR: return c.bossBar;
			case NICK_HIDER: return c.nickHider;
			case MOTION_BLUR: return c.motionBlur;
			case HIT_PREDICT: return c.hitPredict;
			case FOV: return c.fovMod;
			case FREE_LOOK: return c.freeLook;
			case CROSSHAIR: return c.crosshair;
			case GUI_SCALE: return c.guiScale;
			case TAB: return c.tab;
			case TITLES: return c.titles;
			case HIT_COLOR: return c.hitColor;
			case POTIONS: return c.potions;
			default: return false;
		}
	}

	private int moduleAccent(int id) {
		OpticConfig c = OpticClient.get().config();
		switch (id) {
			case FPS: return c.fpsColor;
			case CPS: return c.cpsColor;
			case KEYSTROKES: return c.keystrokesColor;
			case PING: return c.pingColor;
			case COORDINATES: return c.coordinatesColor;
			case TOGGLE_SPRINT: return c.toggleSprintColor;
			default: return OpticConfig.DEFAULT_ACCENT;
		}
	}

	private void toggleSetting(String key) {
		if (key != null && key.startsWith("cat:")) {
			if ("cat:GENERAL".equals(key)) {
				settingsCategory = SettingsCategory.GENERAL;
				settingsScroll = 0;
			} else if ("cat:PERFORMANCE".equals(key)) {
				settingsCategory = SettingsCategory.PERFORMANCE;
				settingsScroll = 0;
			}
			return;
		}
		OpticConfig c = OpticClient.get().config();
		if ("rawMouseInput".equals(key)) {
			if (!OpticRawInput.isSupported()) return;
			c.rawMouseInput = !c.rawMouseInput;
			c.save();
			OpticRawInput.setEnabled(c.rawMouseInput);
			return;
		}
		if ("borderlessFullscreen".equals(key)) {
			c.borderlessFullscreen = !c.borderlessFullscreen;
			c.save();
			OpticBorderless.apply(c.borderlessFullscreen);
			return;
		}
		if ("mouseDelayFix".equals(key)) {
			c.mouseDelayFix = !c.mouseDelayFix;
			c.save();
			return;
		}
		if ("minimalBobbing".equals(key)) {
			c.minimalBobbing = !c.minimalBobbing;
			c.save();
			return;
		}
		if ("disableMouseScrollWheel".equals(key)) {
			c.disableMouseScrollWheel = !c.disableMouseScrollWheel;
			c.save();
			return;
		}
		if ("copyChatMessages".equals(key)) {
			c.copyChatMessages = !c.copyChatMessages;
			c.save();
			return;
		}
		if ("disableAchievements".equals(key)) {
			c.disableAchievements = !c.disableAchievements;
			c.save();
			if (c.disableAchievements && client != null && client.notification != null) {
				client.notification.reset();
			}
			return;
		}
		if ("itemInfo".equals(key)) {
			c.itemInfo = !c.itemInfo;
			c.save();
			if (client != null && client.options != null) {
				client.options.heldItemTooltips = c.itemInfo;
			}
			return;
		}
		if ("fpsOptimizer".equals(key)) {
			c.fpsOptimizer = !c.fpsOptimizer;
			c.save();
			return;
		}
		if ("opticSnap".equals(key)) {
			c.opticSnap = !c.opticSnap;
			c.save();
			return;
		}
		if ("entityCulling".equals(key)) {
			c.entityCulling = !c.entityCulling;
			c.save();
		}
	}

	private void toggle(int id) {
		OpticConfig c = OpticClient.get().config();
		switch (id) {
			case FPS: c.fps = !c.fps; break;
			case CPS: c.cps = !c.cps; break;
			case KEYSTROKES: c.keystrokes = !c.keystrokes; break;
			case PING: c.ping = !c.ping; break;
			case COORDINATES: c.coordinates = !c.coordinates; break;
			case TOGGLE_SPRINT: c.toggleSprint = !c.toggleSprint; break;
			case FULLBRIGHT:
				c.fullbright = !c.fullbright;
				client.options.gamma = c.fullbright ? c.fullbrightGamma : 1.0F;
				break;
			case ANIMATIONS: c.animations = !c.animations; break;
			case PARTICLES:
				c.particles = !c.particles;
				if (c.particles && !c.showCriticalHitParticles && !c.alwaysSharpnessParticles && !c.renderOwnParticles) {
					c.showCriticalHitParticles = true;
					c.alwaysSharpnessParticles = true;
					c.renderOwnParticles = true;
					c.blockBreakParticles = true;
					if (c.particleMultiplier < 1.0F) c.particleMultiplier = 1.0F;
				}
				break;
			case BLOCK_OVERLAY: c.blockOverlay = !c.blockOverlay; break;
			case NAMETAGS: c.nametags = !c.nametags; break;
			case CHAT: c.chat = !c.chat; break;
			case SCOREBOARD: c.scoreboard = !c.scoreboard; break;
			case HOTBAR: c.hotbar = !c.hotbar; break;
			case TIME_CHANGER: c.timeChanger = !c.timeChanger; break;
			case WEATHER_CHANGER: c.weatherChanger = !c.weatherChanger; break;
			case HURT_CAM: c.hurtCam = !c.hurtCam; break;
			case ZOOM: c.zoom = !c.zoom; break;
			case HITBOXES: c.hitboxes = !c.hitboxes; break;
			case SKINS_3D: c.skins3d = !c.skins3d; break;
			case PVP_TWEAKS: c.pvpTweaks = !c.pvpTweaks; break;
			case BOSS_BAR: c.bossBar = !c.bossBar; break;
			case NICK_HIDER: c.nickHider = !c.nickHider; break;
			case MOTION_BLUR: c.motionBlur = !c.motionBlur; break;
			case HIT_PREDICT: c.hitPredict = !c.hitPredict; break;
			case FOV: c.fovMod = !c.fovMod; break;
			case FREE_LOOK: c.freeLook = !c.freeLook; break;
			case CROSSHAIR: c.crosshair = !c.crosshair; break;
			case GUI_SCALE: c.guiScale = !c.guiScale; break;
			case TAB: c.tab = !c.tab; break;
			case TITLES:
				c.titles = !c.titles;
				if (!c.titles || !c.windowTitle) gg.optic.client.hud.OpticTitles.restoreWindowTitle();
				break;
			case HIT_COLOR: c.hitColor = !c.hitColor; break;
			case POTIONS: c.potions = !c.potions; break;
			default: return;
		}
		c.save();
	}

	private ModuleCard findModule(int id) {
		for (ModuleCard module : modules) {
			if (module.id == id) return module;
		}
		return null;
	}

	@Override
	public void render(int mouseX, int mouseY, float delta) {
		if (activeSlider != null) {
			if (!Mouse.isButtonDown(0)) {
				activeSlider = null;
			} else {
				updateSlider(mouseX);
			}
		}
		if (activeColor != null) {
			if (!Mouse.isButtonDown(0)) {
				activeColor = null;
			} else {
				updateColorDrag(mouseX, mouseY);
			}
		}
		if (modsScrollDrag) {
			if (!Mouse.isButtonDown(0)) {
				modsScrollDrag = false;
			} else {
				dragModsScroll(mouseY);
			}
		}
		if (settingsScrollDrag) {
			if (!Mouse.isButtonDown(0) || mainTab != MainTab.SETTINGS) {
				settingsScrollDrag = false;
			} else {
				dragSettingsScroll(mouseY);
			}
		}
		if (configScrollDrag) {
			if (!Mouse.isButtonDown(0) || selected == null) {
				configScrollDrag = false;
			} else {
				dragConfigScroll(mouseY);
			}
		}
		// Same as Cosmetics: in a world the game shows through untouched (so changes to the
		// block overlay, crosshair, HUD... are visible live); on the title screen, its wallpaper.
		if (client.world == null) OpticMenuChrome.drawWallpaper(client, width, height);
		OpticFrame.begin(client);
		try {
			drawPanel();
			drawHeader(mouseX, mouseY);
			if (mainTab == MainTab.SETTINGS) {
				drawSettingsShell(mouseX, mouseY);
			} else if (mainTab == MainTab.PROFILES) {
				drawProfilesView(mouseX, mouseY);
			} else {
				drawModuleGrid(mouseX, mouseY);
			}
		} finally {
			// Panel border last, on top of header and sidebars, so the edge is one clean line.
			outlineExact(panelX, panelY, panelW, panelH, RADIUS, 0xFF000000);
			// Open dropdown list sits above everything, including the panel edge.
			drawDropdownList(mouseX, mouseY);
			OpticFrame.end();
		}
	}

	// ---- Dropdowns (options with a short fixed list of values, e.g. Weather) ----

	private static final int DROP_ROW_H = 16;
	/** Key of the open dropdown, or null. */
	private String openDropdown;
	/** Where the open dropdown's box was drawn this frame: {x, y, w, h}; null if offscreen. */
	private int[] dropdownAnchor;

	/** Option keys shown as a dropdown instead of click-to-cycle. */
	private static boolean isDropdown(String key) {
		return "weatherMode".equals(key) || "motionBlurType".equals(key);
	}

	private static String[] dropdownValues(String key) {
		if ("weatherMode".equals(key)) return new String[] { "Clear", "Rain", "Snow", "Storm" };
		if ("motionBlurType".equals(key)) return gg.optic.client.render.OpticMotionBlur.TYPES;
		return new String[0];
	}

	private static int dropdownIndex(String key) {
		OpticConfig c = OpticClient.get().config();
		if ("weatherMode".equals(key)) return c.weatherMode;
		if ("motionBlurType".equals(key)) return c.motionBlurType;
		return 0;
	}

	private static void setDropdown(String key, int index) {
		OpticConfig c = OpticClient.get().config();
		if ("weatherMode".equals(key)) c.weatherMode = Math.max(0, Math.min(3, index));
		if ("motionBlurType".equals(key)) c.motionBlurType = Math.max(0, Math.min(2, index));
		c.save();
	}

	// ---- Text fields (Nick Hider names) ----

	/** Key of the text option being typed into, or null. */
	private String editingText;

	private static String getText(String key) {
		OpticConfig c = OpticClient.get().config();
		if ("nickOwnName".equals(key)) return c.nickOwnName;
		if ("nickPrefix".equals(key)) return c.nickPrefix;
		if ("nickSuffix".equals(key)) return c.nickSuffix;
		return "";
	}

	private static void setText(String key, String value) {
		OpticConfig c = OpticClient.get().config();
		String v = OpticConfig.cleanNick(value, "");
		if ("nickOwnName".equals(key)) c.nickOwnName = v;
		else if ("nickPrefix".equals(key)) c.nickPrefix = v;
		else if ("nickSuffix".equals(key)) c.nickSuffix = v;
		c.save();
	}

	/** Leaving a name box: names that can't be empty go back to their default. */
	private void finishText() {
		if (editingText == null) return;
		String key = editingText;
		editingText = null;
		if (getText(key).isEmpty()) {
			if ("nickOwnName".equals(key)) setText(key, "You");
			else if ("nickPrefix".equals(key)) setText(key, "Player");
		}
	}

	/** @return true when a name box is focused and took the key. */
	private boolean textKey(char character, int keyCode) {
		if (editingText == null) return false;
		if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
			finishText();
			return true;
		}
		String value = getText(editingText);
		if (keyCode == Keyboard.KEY_BACK) {
			if (!value.isEmpty()) setText(editingText, value.substring(0, value.length() - 1));
			return true;
		}
		// Letters, digits and _ only (space types _), 16 max, like Minecraft names.
		char ch = character == ' ' ? '_' : character;
		boolean ok = (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9') || ch == '_';
		if (ok && value.length() < 16) setText(editingText, value + ch);
		return true;
	}

	private void drawTextBox(ConfigOption option, int x, int y, int w, int h, int mouseX, int mouseY) {
		String value = getText(option.key);
		boolean editing = option.key.equals(editingText);
		int boxW = 104;
		int boxH = 16;
		int boxX = x + w - boxW;
		int boxY = y + (h - boxH) / 2;
		boolean hover = inside(mouseX, mouseY, boxX, boxY, boxW, boxH);
		fillExact(boxX, boxY, boxW, boxH, RADIUS_SM, editing || hover ? CARD_HOVER : TAB_IDLE);
		outlineExact(boxX, boxY, boxW, boxH, RADIUS_SM, editing ? LIME_EDGE : BORDER);
		boolean caret = editing && (System.currentTimeMillis() / 500L) % 2L == 0L;
		String shown = value.isEmpty() && !editing ? "-" : value;
		int th = OpticUiFont.heightSized(client, "Ag", 0xFFFFFFFF, 10.0F);
		int ty = boxY + (boxH - th) / 2;
		shadowed(shown, boxX + 6, ty, value.isEmpty() ? 0xFFC9CED8 : 0xFFFFFFFF, 10.0F);
		if (caret) {
			int cx = boxX + 6 + (value.isEmpty() ? 0 : OpticUiFont.widthSized(client, value, 0xFFFFFFFF, 10.0F)) + 1;
			fill(cx, boxY + 3, cx + 1, boxY + boxH - 3, ACCENT);
		}
		optionHits.add(OptionHit.text(option.key, boxX, boxY, boxW, boxH));
	}

	/** The closed box: current value and a chevron, right-aligned in the row. */
	private void drawDropdownBox(ConfigOption option, int x, int y, int w, int h, int mouseX, int mouseY) {
		String[] values = dropdownValues(option.key);
		int index = dropdownIndex(option.key);
		String value = index >= 0 && index < values.length ? values[index] : "?";
		int boxW = 0;
		for (String v : values) boxW = Math.max(boxW, OpticUiFont.widthSized(client, v, 0xFFFFFFFF, 10.0F));
		boxW += 26;
		int boxH = DROP_ROW_H;
		int boxX = x + w - boxW;
		int boxY = y + (h - boxH) / 2;
		boolean open = option.key.equals(openDropdown);
		boolean hover = inside(mouseX, mouseY, boxX, boxY, boxW, boxH);
		fillExact(boxX, boxY, boxW, boxH, RADIUS_SM, hover || open ? CARD_HOVER : TAB_IDLE);
		outlineExact(boxX, boxY, boxW, boxH, RADIUS_SM, open ? LIME_EDGE : BORDER);
		int th = OpticUiFont.heightSized(client, value, 0xFFFFFFFF, 10.0F);
		shadowed(value, boxX + 7, boxY + (boxH - th) / 2, 0xFFFFFFFF, 10.0F);
		OpticUiFont.drawLineIconCentered(client, "chevron-down", boxX + boxW - 9, boxY + boxH / 2, 9.0F, open ? ACCENT : 0xFFDCE0E6);
		if (open) dropdownAnchor = new int[] { boxX, boxY, boxW, boxH };
		optionHits.add(OptionHit.cycle(option.key, boxX, boxY, boxW, boxH));
	}

	private void drawDropdownList(int mouseX, int mouseY) {
		if (openDropdown == null || dropdownAnchor == null) return;
		String[] values = dropdownValues(openDropdown);
		int index = dropdownIndex(openDropdown);
		int x = dropdownAnchor[0];
		int w = dropdownAnchor[2];
		int listH = values.length * DROP_ROW_H + 4;
		int y = dropdownAnchor[1] + dropdownAnchor[3] + 2;
		// Open upward when there is no room below.
		if (y + listH > panelY + panelH - 4) y = dropdownAnchor[1] - listH - 2;
		fillExact(x + 1, y + 2, w, listH, RADIUS_SM, OpticTheme.SHADOW);
		fillExact(x, y, w, listH, RADIUS_SM, 0xF6121318);
		outlineExact(x, y, w, listH, RADIUS_SM, BORDER);
		for (int i = 0; i < values.length; i++) {
			int ry = y + 2 + i * DROP_ROW_H;
			boolean hover = inside(mouseX, mouseY, x, ry, w, DROP_ROW_H);
			if (hover) fillExact(x + 2, ry, w - 4, DROP_ROW_H, RADIUS_SM, ACCENT_SOFT);
			int color = i == index ? ACCENT : (hover ? 0xFFFFFFFF : 0xFFC9CED8);
			int th = OpticUiFont.heightSized(client, values[i], color, 10.0F);
			shadowed(values[i], x + 7, ry + (DROP_ROW_H - th) / 2, color, 10.0F);
		}
	}

	/** @return true when the click was used by an open dropdown (pick a value or close it). */
	private boolean clickDropdown(int mouseX, int mouseY) {
		if (openDropdown == null) return false;
		String key = openDropdown;
		int[] a = dropdownAnchor;
		openDropdown = null;
		dropdownAnchor = null;
		if (a == null) return true;
		String[] values = dropdownValues(key);
		int listH = values.length * DROP_ROW_H + 4;
		int y = a[1] + a[3] + 2;
		if (y + listH > panelY + panelH - 4) y = a[1] - listH - 2;
		for (int i = 0; i < values.length; i++) {
			if (inside(mouseX, mouseY, a[0], y + 2 + i * DROP_ROW_H, a[2], DROP_ROW_H)) {
				setDropdown(key, i);
				return true;
			}
		}
		return true;
	}

	private void drawProfilesView(int mouseX, int mouseY) {
		profileHits.clear();
		int bodyTop = panelY + 52;
		int sideW = 100;
		// Same rounded bottom-left as the panel (a plain fill poked a square corner out).
		fillExact(panelX, bodyTop, sideW, panelY + panelH - bodyTop, RADIUS, SIDEBAR);
		fill(panelX + RADIUS, bodyTop, panelX + sideW, panelY + panelH, SIDEBAR);
		fill(panelX, bodyTop, panelX + sideW, panelY + panelH - RADIUS, SIDEBAR);
		vLine(panelX + sideW, bodyTop, panelY + panelH);

		int sideX = panelX + 8;
		int sideY = bodyTop + 12;
		sideY = drawProfileSide(sideX, sideY, sideW - 16, "New Profile", ProfileSide.NEW, mouseX, mouseY);
		sideY = drawProfileSide(sideX, sideY, sideW - 16, "Search", ProfileSide.SEARCH, mouseX, mouseY);
		sideY = drawProfileSide(sideX, sideY, sideW - 16, "Open Folder", ProfileSide.OPEN_FOLDER, mouseX, mouseY);

		int gridX = panelX + sideW + 12;
		int gridY = bodyTop + 10;
		int gridW = panelW - sideW - 24;
		int gridH = panelH - 52 - 28;

		if (profileFieldOpen && profileNameField != null) {
			String label = renamingProfile != null ? "Rename:" : (profileSearchMode ? "Search:" : "Name:");
			shadowed(label, gridX, gridY, MUTED, 8.5F);
			int fw = Math.min(160, gridW - 120);
			ensureProfileField(gridX + 42, gridY - 2, fw);
			drawSmoothField(profileNameField, gridX + 42, gridY - 2, fw, 18, null);
			int confX = gridX + 42 + fw + 8;
			boolean confHover = inside(mouseX, mouseY, confX, gridY - 2, 56, 18);
			fillRound(confX, gridY - 2, 56, 18, RADIUS_SM, confHover ? ACCENT : TAB_IDLE);
			OpticUiFont.drawCenteredSized(
				client,
				profileSearchMode ? "Done" : "Confirm",
				confX + 28,
				gridY + 2,
				confHover ? 0xFF07110A : 0xFFFFFFFF,
				8.5F
			);
			profileHits.add(ProfileHit.confirm(confX, gridY - 2, 56, 18));
			gridY += 24;
			gridH -= 24;
		}

		String active = OpticClient.get().config().activeProfile;
		List<File> profiles = profileFieldOpen && profileSearchMode && profileNameField != null
			? OpticProfiles.listFiltered(profileNameField.getText())
			: OpticProfiles.list();

		int cardW = 128;
		int cardH = 152;
		int gap = 10;
		int cols = Math.max(1, (gridW + gap) / (cardW + gap));
		int rows = (profiles.size() + cols - 1) / cols;
		int contentH = rows * (cardH + gap);
		int maxScroll = Math.max(0, contentH - gridH);
		profileScroll = Math.max(0, Math.min(profileScroll, maxScroll));

		if (profiles.isEmpty()) {
			shadowed("No profiles yet — click New Profile", gridX, gridY + 8, MUTED, 9.0F);
		}

		for (int i = 0; i < profiles.size(); i++) {
			File file = profiles.get(i);
			int col = i % cols;
			int row = i / cols;
			int x = gridX + col * (cardW + gap);
			int y = gridY + row * (cardH + gap) - profileScroll;
			if (y + cardH < gridY || y > gridY + gridH) continue;
			drawProfileCard(file, x, y, cardW, cardH, active, mouseX, mouseY);
		}

		if (maxScroll > 0) {
			int trackX = panelX + panelW - 8;
			int trackY = gridY;
			int trackH = gridH;
			fill(trackX, trackY, trackX + 3, trackY + trackH, 0x44101010);
			int thumbH = Math.max(16, trackH * trackH / Math.max(1, contentH));
			int thumbY = trackY + (int) ((trackH - thumbH) * (profileScroll / (float) maxScroll));
			fill(trackX, thumbY, trackX + 3, thumbY + thumbH, ACCENT);
		}

		String footer = active == null || active.isEmpty()
			? "Default profile is selected"
			: "Active: " + active + " · saved in the profiles folder";
		shadowed(footer, gridX, panelY + panelH - 18, MUTED, 8.0F);
		if (!profileStatus.isEmpty()) {
			shadowed(profileStatus, gridX + 220, panelY + panelH - 18, ACCENT, 8.0F);
		}
	}

	private int drawProfileSide(int x, int y, int w, String label, ProfileSide side, int mouseX, int mouseY) {
		boolean hover = inside(mouseX, mouseY, x, y, w, 18);
		if (hover) fillRound(x - 2, y - 1, w + 4, 18, RADIUS_SM, ACCENT_SOFT);
		shadowed(label, x, y + 4, hover ? 0xFFFFFFFF : MUTED, 9.0F);
		profileHits.add(ProfileHit.side(side, x, y, w, 18));
		return y + 22;
	}

	private void drawProfileCard(File file, int x, int y, int w, int h, String active, int mouseX, int mouseY) {
		String name = OpticProfiles.displayName(file);
		boolean selectedProfile = active != null && active.equalsIgnoreCase(name);
		boolean hover = inside(mouseX, mouseY, x, y, w, h);
		fillExact(x, y, w, h, RADIUS, hover ? CARD_HOVER : CARD);
		outlineExact(x, y, w, h, RADIUS, selectedProfile || hover ? ACCENT : BORDER_SOFT);

		shadowedCentered(name, x + w / 2, y + 10, 0xFFFFFFFF, 10.0F);

		int iconBox = 48;
		int ix = x + (w - iconBox) / 2;
		int iy = y + 32;
		fillExact(ix, iy, iconBox, iconBox, RADIUS_SM, 0xFF15181C);
		outlineExact(ix, iy, iconBox, iconBox, RADIUS_SM, LIME_EDGE);
		Identifier logo = OpticTextures.logo(client);
		if (logo != null) {
		OpticSmooth.bindMip(client, logo);
		DrawableHelper.drawTexture(ix + 8, iy + 8, 0.0F, 0.0F, 32, 32, 32.0F, 32.0F);
		} else {
			shadowedCentered("O", ix + iconBox / 2, iy + 16, ACCENT, 16.0F);
		}

		float emojiSize = 10.0F;
		int iconSize = 18;
		int iconGap = 5;
		int totalIcons = 4;
		int rowW = totalIcons * iconSize + (totalIcons - 1) * iconGap;
		int iconX = x + (w - rowW) / 2;
		int iconY = y + h - iconSize - 10;
		drawProfileActionIcon(iconX, iconY, iconSize, emojiSize, ProfileHit.Action.RENAME, file, mouseX, mouseY);
		drawProfileActionIcon(iconX + (iconSize + iconGap), iconY, iconSize, emojiSize, ProfileHit.Action.DUPLICATE, file, mouseX, mouseY);
		drawProfileActionIcon(iconX + 2 * (iconSize + iconGap), iconY, iconSize, emojiSize, ProfileHit.Action.DELETE, file, mouseX, mouseY);
		drawProfileActionIcon(iconX + 3 * (iconSize + iconGap), iconY, iconSize, emojiSize, ProfileHit.Action.EXPORT, file, mouseX, mouseY);

		profileHits.add(ProfileHit.select(file, x, y, w, h - iconSize - 14));
	}

	private void drawProfileActionIcon(
		int x,
		int y,
		int hitSize,
		float emojiSize,
		ProfileHit.Action action,
		File file,
		int mouseX,
		int mouseY
	) {
		boolean hover = inside(mouseX, mouseY, x, y, hitSize, hitSize);
		boolean danger = action == ProfileHit.Action.DELETE;
		int hot = danger ? 0xFFFF5C6A : ACCENT;
		// Badlion-style square action button with a crisp line icon, fitted to the button.
		fillExact(x, y, hitSize, hitSize, RADIUS_SM, hover ? CARD_HOVER : 0xFF15171C);
		outlineExact(x, y, hitSize, hitSize, RADIUS_SM, hover ? hot : OpticTheme.BORDER);
		String icon;
		if (action == ProfileHit.Action.RENAME) icon = "pencil";
		else if (action == ProfileHit.Action.DUPLICATE) icon = "copy";
		else if (danger) icon = "trash";
		else icon = "export";
		OpticUiFont.drawLineIconCentered(client, icon, x + hitSize / 2, y + hitSize / 2, emojiSize, hover ? hot : OpticTheme.MUTED);
		profileHits.add(ProfileHit.action(file, action, x, y, hitSize, hitSize));
	}

	private void applyLoadedProfile() {
		OpticConfig c = OpticClient.get().config();
		if (c.fullbright) client.options.gamma = c.fullbrightGamma;
		else client.options.gamma = Math.min(client.options.gamma, 1.0F);
		OpticRawInput.setEnabled(c.rawMouseInput);
		OpticBorderless.apply(c.borderlessFullscreen);
		if (client != null && client.options != null) {
			client.options.heldItemTooltips = c.itemInfo;
		}
	}

	private void drawEditGuiButton(int mouseX, int mouseY) {
		boolean hover = inside(mouseX, mouseY, editX, editY, editW, editH);
		// Call-to-action: accent outline, fills solid accent on hover.
		// Same smooth fill + outline as the mod cards and buttons.
		fillExact(editX, editY, editW, editH, RADIUS_SM, hover ? ACCENT : ACCENT_SOFT);
		outlineExact(editX, editY, editW, editH, RADIUS_SM, ACCENT);
		shadowedCenteredMid("EDIT HUD", editX + editW / 2, editY, editH, hover ? OpticTheme.ON_ACCENT : OpticTheme.TEXT, 8.5F);
	}

	/** Settings tab: same sidebar as the Mods tab, below the header, so nothing above moves. */
	private void drawSettingsShell(int mouseX, int mouseY) {
		int bodyTop = panelY + 48;
		fillExact(panelX, bodyTop, sidebarW, panelY + panelH - bodyTop, RADIUS, SIDEBAR);
		fill(panelX + RADIUS, bodyTop, panelX + sidebarW, panelY + panelH, SIDEBAR);
		fill(panelX, bodyTop, panelX + sidebarW, panelY + panelH - RADIUS, SIDEBAR);
		vLine(panelX + sidebarW, bodyTop, panelY + panelH);

		shadowed("SETTINGS", panelX + 12, bodyTop + 12, 0xFFAEB4BF, 7.5F);
		drawSettingsCategory("General", SettingsCategory.GENERAL, panelX + 8, settingsCatY(), sidebarW - 16, filterRowH, mouseX, mouseY);
		drawSettingsCategory("Performance", SettingsCategory.PERFORMANCE, panelX + 8, settingsCatY() + filterRowH + 4, sidebarW - 16, filterRowH, mouseX, mouseY);

		editX = panelX + 8;
		editY = panelY + panelH - 34;
		editW = sidebarW - 16;
		drawEditGuiButton(mouseX, mouseY);
		drawSettingsList(mouseX, mouseY);
	}

	private void drawSettingsCategory(String label, SettingsCategory cat, int x, int y, int w, int h, int mouseX, int mouseY) {
		boolean active = settingsCategory == cat;
		boolean hover = inside(mouseX, mouseY, x, y, w, h);
		if (active) {
			fillExact(x, y, w, h, RADIUS_SM, TAB_ACTIVE);
		} else if (hover) {
			fillExact(x, y, w, h, RADIUS_SM, CARD_HOVER);
		}
		shadowedMid(label, x + 10, y, h, active || hover ? OpticTheme.TEXT : MUTED, 10.0F);
	}

	/** Same spot as the first filter row on the Mods tab. */
	private int settingsCatY() {
		return panelY + 48 + 30;
	}

	private void drawSettingsList(int mouseX, int mouseY) {
		settingHits.clear();
		settingHits.add(new SettingHit("cat:GENERAL", panelX + 8, settingsCatY(), sidebarW - 16, filterRowH));
		settingHits.add(new SettingHit("cat:PERFORMANCE", panelX + 8, settingsCatY() + filterRowH + 4, sidebarW - 16, filterRowH));

		int contentX = panelX + sidebarW + 16;
		int contentY = panelY + 58;
		int contentW = panelW - sidebarW - 28;
		int contentBottom = panelY + panelH - 40;
		String page = settingsCategory == SettingsCategory.PERFORMANCE ? "Performance" : "General";
		shadowed(page, contentX, contentY + 1, 0xFFFFFFFF, 12.0F);
		int settingsSearchWidth = Math.min(160, Math.max(72, contentW / 2));
		int settingsSearchLeft = contentX + contentW - settingsSearchWidth;
		ensureSettingsSearch(settingsSearchLeft, contentY, settingsSearchWidth);
		drawSmoothField(settingsSearchField, settingsSearchLeft, contentY, settingsSearchWidth, 16, "Search");
		contentY += 24;

		if (settingsCategory == SettingsCategory.PERFORMANCE) {
			drawPerformanceSettings(contentX, contentY, contentW, contentBottom, mouseX, mouseY);
			return;
		}

		// General page scrolls inside [contentY, contentBottom] with a scroll bar on the right.
		OpticConfig gc = OpticClient.get().config();
		int listTop = contentY;
		int listBottom = contentBottom;
		int listW = contentW - 8;
		settingsClipTop = listTop;
		settingsClipBottom = listBottom;
		clipGui(contentX - 4, listTop, contentW + 8, Math.max(1, listBottom - listTop));
		// One continuous list: the page title already says "General".
		int rowY = listTop - settingsScroll;
		rowY = drawSettingToggle("Raw Mouse Input", gc.rawMouseInput, "rawMouseInput", contentX, rowY, listW, mouseX, mouseY);
		rowY = drawSettingToggle("Minimal Bobbing", gc.minimalBobbing, "minimalBobbing", contentX, rowY, listW, mouseX, mouseY);
		rowY = drawSettingToggle("Disable Hotbar Scroll", gc.disableMouseScrollWheel, "disableMouseScrollWheel", contentX, rowY, listW, mouseX, mouseY);
		rowY = drawSettingToggle("Borderless Fullscreen", gc.borderlessFullscreen, "borderlessFullscreen", contentX, rowY, listW, mouseX, mouseY);
		rowY = drawSettingToggle("Disable Achievements", gc.disableAchievements, "disableAchievements", contentX, rowY, listW, mouseX, mouseY);
		rowY = drawSettingToggle("Copy Chat Messages (Ctrl+C)", gc.copyChatMessages, "copyChatMessages", contentX, rowY, listW, mouseX, mouseY);
		GL11.glDisable(GL11.GL_SCISSOR_TEST);
		settingsClipTop = Integer.MIN_VALUE;
		settingsClipBottom = Integer.MAX_VALUE;

		int totalH = rowY + settingsScroll - listTop;
		int visibleH = Math.max(1, listBottom - listTop);
		settingsMaxScroll = Math.max(0, totalH - visibleH);
		settingsScroll = Math.max(0, Math.min(settingsScroll, settingsMaxScroll));
		if (settingsMaxScroll > 0) {
			int trackX = contentX + contentW + 2;
			int thumbH = Math.max(18, visibleH * visibleH / Math.max(1, totalH));
			int thumbY = listTop + (int) ((visibleH - thumbH) * (settingsScroll / (float) settingsMaxScroll));
			settingsTrackX = trackX - 4;
			settingsTrackY = listTop;
			settingsTrackW = 12;
			settingsTrackH = visibleH;
			settingsThumbH = thumbH;
			boolean hover = settingsScrollDrag || inside(mouseX, mouseY, settingsTrackX, listTop, settingsTrackW, visibleH);
			fill(trackX, listTop, trackX + 3, listTop + visibleH, 0x44FFFFFF);
			fill(trackX, thumbY, trackX + 3, thumbY + thumbH, hover ? 0xFFFFFFFF : ACCENT);
		} else {
			settingsTrackW = 0;
			settingsTrackH = 0;
		}

		if (!OpticRawInput.isSupported()) {
			shadowed("Raw mouse input is Windows-only.", contentX, panelY + panelH - 36, 0xFFAA6666, 8.0F);
		}
	}

	private void dragSettingsScroll(int mouseY) {
		if (settingsMaxScroll <= 0 || settingsTrackH <= settingsThumbH) return;
		float span = settingsTrackH - settingsThumbH;
		float t = (mouseY - settingsTrackY - settingsThumbH * 0.5F) / span;
		if (t < 0.0F) t = 0.0F;
		if (t > 1.0F) t = 1.0F;
		settingsScroll = Math.round(t * settingsMaxScroll);
	}

	private void drawPerformanceSettings(int contentX, int contentY, int contentW, int contentBottom, int mouseX, int mouseY) {
		// Empty for now; performance options will go here.
		settingsScroll = 0;
	}

	private int drawSettingsHeader(int x, int y, String label) {
		if (!settingsSearchText().isEmpty()) return y;
		if (y >= panelY + 58 && y < panelY + panelH - 44) {
			shadowed(label, x, y, MUTED, 8.5F);
		}
		return y + 14;
	}

	private String settingsSearchText() {
		if (settingsSearchField == null) return "";
		return settingsSearchField.getText().trim().toLowerCase();
	}

	private boolean settingMatches(String label) {
		String query = settingsSearchText();
		if (query.isEmpty()) return true;
		return label.toLowerCase().startsWith(query);
	}

	private int drawSettingToggleClipped(
		String label,
		boolean on,
		String key,
		int x,
		int y,
		int w,
		int clipTop,
		int clipBottom,
		int mouseX,
		int mouseY
	) {
		int rowH = 24;
		if (!settingMatches(label)) return y;
		if (y + rowH > clipTop && y < clipBottom) {
			boolean hover = inside(mouseX, mouseY, x, y, w, rowH);
			if (hover) fill(x - 4, y, x + w + 4, y + rowH, 0x181A1E22);
			shadowedMid(label, x, y, rowH, 0xFFFFFFFF, 10.0F);
			int toggleW = 36;
			int toggleH = 14;
			int toggleX = x + w - toggleW;
			int toggleY = y + (rowH - toggleH) / 2;
			drawToggle("s:" + key, toggleX, toggleY, toggleW, toggleH, on);
			settingHits.add(new SettingHit(key, toggleX - 8, y, toggleW + 16, rowH));
		}
		return y + rowH + 4;
	}

	private int drawSettingToggle(
		String label,
		boolean on,
		String key,
		int x,
		int y,
		int w,
		int mouseX,
		int mouseY
	) {
		int rowH = 28;
		if (!settingMatches(label)) return y;
		if (y + rowH <= settingsClipTop || y >= settingsClipBottom) return y + rowH + 6;
		boolean hover = inside(mouseX, mouseY, x, y, w, rowH) && mouseY >= settingsClipTop && mouseY < settingsClipBottom;
		if (hover) fill(x - 4, y, x + w + 4, y + rowH, 0x181A1E22);
		shadowedMid(label, x, y, rowH, 0xFFFFFFFF, 11.0F);

		int toggleW = 38;
		int toggleH = 16;
		int toggleX = x + w - toggleW;
		int toggleY = y + (rowH - toggleH) / 2;
		drawToggle("s:" + key, toggleX, toggleY, toggleW, toggleH, on);
		int hitTop = Math.max(y, settingsClipTop);
		int hitBottom = Math.min(y + rowH, settingsClipBottom);
		if (hitBottom > hitTop) settingHits.add(new SettingHit(key, toggleX - 8, hitTop, toggleW + 16, hitBottom - hitTop));
		return y + rowH + 6;
	}

	/** Menu text with the same soft drop shadow as the Cosmetics menu, so it reads over the game. */
	private void shadowed(String text, int x, int y, int argb, float size) {
		int alpha = (argb >>> 24) & 0xFF;
		if (alpha > 0) OpticUiFont.drawSized(client, text, x + 1, y + 1, shadowColor(alpha), size);
		OpticUiFont.drawSized(client, text, x, y, argb, size);
	}

	/** One-screen-pixel lines (a GUI pixel is 2-4 screen pixels, which read as thick bars). */
	private float hairline() {
		return 1.0F / Math.max(1, new Window(client, client.width, client.height).getScaleFactor());
	}

	private void vLine(int x, int y1, int y2) {
		hairQuad(x, y1, x + hairline(), y2, DIVIDER);
	}

	private void hLine(int x1, int x2, int y) {
		hairQuad(x1, y, x2, y + hairline(), DIVIDER);
	}

	private static void hairQuad(float x1, float y1, float x2, float y2, int argb) {
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glColor4f(((argb >> 16) & 0xFF) / 255.0F, ((argb >> 8) & 0xFF) / 255.0F, (argb & 0xFF) / 255.0F, ((argb >>> 24) & 0xFF) / 255.0F);
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glVertex2f(x1, y2);
		GL11.glVertex2f(x2, y2);
		GL11.glVertex2f(x2, y1);
		GL11.glVertex2f(x1, y1);
		GL11.glEnd();
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
	}

	/** Text vertically centred in a box (measured, not a fixed offset), so it sits in the middle of its hover box. */
	private void shadowedMid(String text, int x, int boxY, int boxH, int argb, float size) {
		int th = OpticUiFont.heightSized(client, text, argb, size);
		shadowed(text, x, boxY + (boxH - th) / 2, argb, size);
	}

	private void shadowedCenteredMid(String text, int centerX, int boxY, int boxH, int argb, float size) {
		int th = OpticUiFont.heightSized(client, text, argb, size);
		shadowedCentered(text, centerX, boxY + (boxH - th) / 2, argb, size);
	}

	/** Near-black shadow, as see-through as the text itself. */
	private static int shadowColor(int textAlpha) {
		return (textAlpha * 0xE6 / 255) << 24;
	}

	private void shadowedCentered(String text, int centerX, int y, int argb, float size) {
		int alpha = (argb >>> 24) & 0xFF;
		if (alpha > 0) OpticUiFont.drawCenteredSized(client, text, centerX + 1, y + 1, shadowColor(alpha), size);
		OpticUiFont.drawCenteredSized(client, text, centerX, y, argb, size);
	}

	private void crispShadowed(net.minecraft.client.MinecraftClient c, String text, int centerX, int centerY, int argb, float size) {
		int alpha = (argb >>> 24) & 0xFF;
		if (alpha > 0) OpticUiFont.drawCrispCentered(c, text, centerX + 1, centerY + 1, shadowColor(alpha), size);
		OpticUiFont.drawCrispCentered(c, text, centerX, centerY, argb, size);
	}

	private void drawPanel() {
		// Light see-through grey body; the black frame is drawn last (see render).
		fillExact(panelX, panelY, panelW, panelH, RADIUS, PANEL);
	}

	private void drawHeader(int mouseX, int mouseY) {
		int headerBottom = panelY + 48;
		fillExact(panelX, panelY, panelW, headerBottom - panelY, RADIUS, TOPBAR);
		fill(panelX, headerBottom - RADIUS, panelX + panelW, headerBottom, TOPBAR);
		// Plain black line under the header.
		hLine(panelX, panelX + panelW, headerBottom - 1);

		// Same header on every tab: logo and tabs never move when switching.
		int brandX = panelX + 12;
		drawHeaderLogo(brandX);
		shadowed("OPTIC", brandX + 32, panelY + 12, OpticTheme.TEXT, 13.0F);
		shadowed("CLIENT · 1.7.10", brandX + 32, panelY + 27, ACCENT, 7.5F);

		this.tabY = panelY + 12;
		modsTabX = brandX + 128;
		modsTabW = 56;
		settingsTabX = modsTabX + modsTabW + 8;
		settingsTabW = 68;
		cosmeticsTabX = settingsTabX + settingsTabW + 8;
		cosmeticsTabW = 78;
		profilesTabX = cosmeticsTabX + cosmeticsTabW + 8;
		profilesTabW = 66;

		drawPillTab("Mods", modsTabX, tabY, modsTabW, 24, mainTab == MainTab.MODS, mouseX, mouseY);
		drawPillTab("Settings", settingsTabX, tabY, settingsTabW, 24, mainTab == MainTab.SETTINGS, mouseX, mouseY);
		// Cosmetics opens its own screen, so it is never the active tab here.
		drawPillTab("Cosmetics", cosmeticsTabX, tabY, cosmeticsTabW, 24, false, mouseX, mouseY);
		drawPillTab("Profiles", profilesTabX, tabY, profilesTabW, 24, mainTab == MainTab.PROFILES, mouseX, mouseY);

		if (selected != null && mainTab == MainTab.MODS) {
			String tab = selected.name;
			int detailX = profilesTabX + profilesTabW + 14;
			shadowed(tab, detailX, tabY + 7, MUTED, 10.0F);
		}

		// Close: a drawn X, centred exactly in the 22px hover box (a font glyph sat off-centre).
		boolean closeHover = inside(mouseX, mouseY, closeX - 3, closeY - 3, 22, 22);
		int closeCx = closeX + 8;
		int closeCy = closeY + 8;
		if (closeHover) fillExact(closeCx - 11, closeCy - 11, 22, 22, RADIUS_SM, CARD_HOVER);
		OpticUiFont.drawLineIconCentered(client, "close", closeCx + 1, closeCy + 1, 13.0F, shadowColor(0xFF));
		OpticUiFont.drawLineIconCentered(client, "close", closeCx, closeCy, 13.0F, closeHover ? 0xFFFFFFFF : 0xFFDCE0E6);
	}

	private void drawPillTab(String label, int x, int y, int w, int h, boolean active, int mouseX, int mouseY) {
		boolean hover = inside(mouseX, mouseY, x, y, w, h);
		// Active (Settings, Profiles...): a more solid box with an accent underline. Idle: quiet, box on hover.
		if (active) {
			fillExact(x, y, w, h, RADIUS_SM, TAB_ACTIVE);
			outlineExact(x, y, w, h, RADIUS_SM, LIME_EDGE);
			fill(x + 8, y + h - 2, x + w - 8, y + h - 1, ACCENT);
		} else if (hover) {
			fillExact(x, y, w, h, RADIUS_SM, CARD_HOVER);
			outlineExact(x, y, w, h, RADIUS_SM, BORDER);
		}
		crispShadowed(
			client,
			label,
			x + w / 2,
			y + h / 2,
			active ? OpticTheme.TEXT : (hover ? OpticTheme.TEXT : MUTED),
			11.0F
		);
	}

	private void drawHeaderLogo(int x) {
		Identifier logo = OpticTextures.logo(client);
		int y = panelY + (44 - HEADER_LOGO) / 2;
		if (logo == null) {
			shadowed("OPTIC", x, panelY + 14, ACCENT, 14.0F);
			return;
		}
		OpticSmooth.bindMip(client, logo);
		DrawableHelper.drawTexture(x, y, 0.0F, 0.0F, HEADER_LOGO, HEADER_LOGO, (float) HEADER_LOGO, (float) HEADER_LOGO);
	}

	private void drawModuleGrid(int mouseX, int mouseY) {
		cardBounds.clear();
		int bodyTop = panelY + 48;
		int sideH = panelY + panelH - bodyTop;
		fillExact(panelX, bodyTop, sidebarW, sideH, RADIUS, SIDEBAR);
		fill(panelX + RADIUS, bodyTop, panelX + sidebarW, panelY + panelH, SIDEBAR);
		fill(panelX, bodyTop, panelX + sidebarW, panelY + panelH - RADIUS, SIDEBAR);
		// One thin grey line where the sidebar ends (the sidebar itself is see-through).
		vLine(panelX + sidebarW, bodyTop, panelY + panelH);

		shadowed("FILTER", panelX + 12, bodyTop + 12, 0xFFAEB4BF, 7.5F);
		filterRowW = sidebarW - 16;
		filterAllX = panelX + 8;
		filterAllY = bodyTop + 30;
		filterFavX = panelX + 8;
		filterFavY = filterAllY + filterRowH + 4;
		drawFilterRow("All", ModFilter.ALL, filterAllX, filterAllY, filterRowW, filterRowH, mouseX, mouseY);
		drawFilterRow("Favorites", ModFilter.FAVORITES, filterFavX, filterFavY, filterRowW, filterRowH, mouseX, mouseY);
		filterPvpX = panelX + 8;
		filterPvpY = filterFavY + filterRowH + 4;
		drawFilterRow("PvP", ModFilter.PVP, filterPvpX, filterPvpY, filterRowW, filterRowH, mouseX, mouseY);

		editX = panelX + 8;
		editY = panelY + panelH - 34;
		editW = sidebarW - 16;
		drawEditGuiButton(mouseX, mouseY);

		int edge = 14;
		int contentX = panelX + sidebarW + 12;
		int contentY = bodyTop + 8;
		int contentRight = panelX + panelW - edge;
		int contentBottom = panelY + panelH - edge;
		int contentW = Math.max(1, contentRight - contentX);
		int contentH = Math.max(1, contentBottom - contentY);
		if (selected != null) {
			drawConfigView(mouseX, mouseY, contentX, contentY, contentW, contentH);
			return;
		}

		List<ModuleCard> visible = visibleModules();
		modsGridX = contentX;
		modsGridY = contentY;
		modsGridW = contentW;
		modsGridH = contentH;
		int scrollGap = 12;
		int searchW = Math.min(160, Math.max(72, modsGridW - scrollGap));
		int searchX = modsGridX + modsGridW - scrollGap - searchW;
		ensureModSearch(searchX, modsGridY, searchW);
		drawSmoothField(modSearchField, searchX, modsGridY, searchW, 16, "Search");
		modsGridY += 22;
		modsGridH = Math.max(32, contentBottom - modsGridY);

		int gap = 6;
		int innerW = Math.max(1, modsGridW - scrollGap);
		int columns = Math.max(1, (innerW + gap) / (64 + gap));
		int cardW = (innerW - gap * (columns - 1)) / columns;
		if (cardW > 96) {
			columns = Math.max(1, (innerW + gap) / (96 + gap));
			cardW = Math.min(96, (innerW - gap * (columns - 1)) / columns);
		}
		if (cardW < 1) cardW = 1;
		int cardH = cardW + 6;
		int rows = (visible.size() + columns - 1) / columns;
		int gridContentH = rows * (cardH + gap) - (rows > 0 ? gap : 0);
		modsMaxScroll = Math.max(0, gridContentH - modsGridH);
		modsScroll = Math.max(0, Math.min(modsScroll, modsMaxScroll));

		if (visible.isEmpty()) {
			String empty = modSearchField != null && !modSearchField.getText().trim().isEmpty()
				? "No mods match that"
				: modFilter == ModFilter.FAVORITES
					? "No favorite mods yet — click the star on a card"
					: "No mods in this filter";
			shadowed(empty, modsGridX, modsGridY + 8, MUTED, 9.0F);
		}

		for (int i = 0; i < visible.size(); i++) {
			ModuleCard module = visible.get(i);
			int column = i % columns;
			int row = i / columns;
			int x = modsGridX + column * (cardW + gap);
			int y = modsGridY + row * (cardH + gap) - modsScroll;
			if (y + cardH < modsGridY || y > modsGridY + modsGridH) continue;
			if (x + cardW > modsGridX + innerW) continue;
			boolean hover = inside(mouseX, mouseY, x, y, cardW, cardH)
				&& inside(mouseX, mouseY, modsGridX, modsGridY, innerW, modsGridH);
			int clipX = x;
			int clipY = Math.max(y, modsGridY);
			int clipR = Math.min(x + cardW, modsGridX + innerW);
			int clipB = Math.min(y + cardH, modsGridY + modsGridH);
			clipGui(clipX, clipY, clipR - clipX, clipB - clipY);
			try {
				cardBounds.add(drawCard(module, x, y, cardW, cardH, hover));
			} finally {
				GL11.glDisable(GL11.GL_SCISSOR_TEST);
			}
		}

		if (modsMaxScroll > 0) {
			int trackX = modsGridX + innerW + 4;
			if (trackX > panelX + panelW - 8) trackX = panelX + panelW - 8;
			int trackY = modsGridY;
			int trackH = modsGridH;
			modsTrackX = trackX - 4;
			modsTrackY = trackY;
			modsTrackW = 12;
			modsTrackH = trackH;
			fill(trackX, trackY, trackX + 3, trackY + trackH, 0x44101010);
			int thumbH = Math.max(18, trackH * trackH / Math.max(1, gridContentH));
			modsThumbH = thumbH;
			int thumbY = trackY + (int) ((trackH - thumbH) * (modsScroll / (float) modsMaxScroll));
			fill(trackX, thumbY, trackX + 3, thumbY + thumbH, ACCENT);
		} else {
			modsTrackW = 0;
			modsTrackH = 0;
		}
	}

	private void dragConfigScroll(int mouseY) {
		if (configMaxScroll <= 0 || configTrackH <= configThumbH) return;
		float span = configTrackH - configThumbH;
		float t = (mouseY - configTrackY - configThumbH * 0.5F) / span;
		if (t < 0.0F) t = 0.0F;
		if (t > 1.0F) t = 1.0F;
		configScroll = Math.round(t * configMaxScroll);
	}

	private void dragModsScroll(int mouseY) {
		if (modsMaxScroll <= 0 || modsTrackH <= modsThumbH) return;
		float span = modsTrackH - modsThumbH;
		float t = (mouseY - modsTrackY - modsThumbH * 0.5F) / span;
		if (t < 0.0F) t = 0.0F;
		if (t > 1.0F) t = 1.0F;
		modsScroll = Math.round(t * modsMaxScroll);
	}

	private List<ModuleCard> visibleModules() {
		OpticConfig c = OpticClient.get().config();
		String query = modSearchField == null ? "" : modSearchField.getText().trim().toLowerCase();
		List<ModuleCard> out = new ArrayList<ModuleCard>();
		for (ModuleCard module : modules) {
			if (modFilter == ModFilter.FAVORITES && !c.isFavoriteModule(module.id)) continue;
			if (modFilter == ModFilter.PVP && !isPvpModule(module.id)) continue;
			// Spaces don't matter and the text can be anywhere: "bossbar" finds "Boss Bar".
			if (!query.isEmpty() && !module.name.toLowerCase().replace(" ", "").contains(query.replace(" ", ""))) continue;
			out.add(module);
		}
		return out;
	}

	private static boolean isPvpModule(int id) {
		return id == ANIMATIONS
			|| id == CPS
			|| id == FOV
			|| id == HITBOXES
			|| id == HURT_CAM
			|| id == KEYSTROKES
			|| id == PARTICLES
			|| id == TOGGLE_SPRINT
			|| id == ZOOM
			|| id == FREE_LOOK
			|| id == CROSSHAIR
			|| id == HIT_COLOR
			|| id == PVP_TWEAKS
			|| id == POTIONS
			|| id == PING
			|| id == TAB;
	}

	private void drawFilterRow(String label, ModFilter filter, int x, int y, int w, int h, int mouseX, int mouseY) {
		boolean active = modFilter == filter;
		boolean hover = inside(mouseX, mouseY, x, y, w, h);
		// Active row: soft accent fill. Hover: faint lift.
		if (active) {
			fillExact(x, y, w, h, RADIUS_SM, TAB_ACTIVE);
		} else if (hover) {
			fillExact(x, y, w, h, RADIUS_SM, CARD_HOVER);
		}
		int ink = active ? OpticTheme.TEXT : (hover ? OpticTheme.TEXT : MUTED);
		int iconInk = active ? ACCENT : ink;
		if (filter == ModFilter.FAVORITES) {
			drawFavoriteStar(x + 14, y + h / 2, 11.0F, true, active || hover);
		} else if (filter == ModFilter.PVP) {
			OpticUiFont.drawSwordsCentered(client, x + 14, y + h / 2, 11.0F, iconInk);
		} else {
			OpticUiFont.drawGridCentered(client, x + 14, y + h / 2, 10.0F, iconInk);
		}
		shadowedMid(label, x + 24, y, h, ink, 10.0F);
	}

	private CardBounds drawCard(ModuleCard module, int x, int y, int w, int h, boolean hover) {
		boolean on = enabled(module.id);
		boolean fav = OpticClient.get().config().isFavoriteModule(module.id);
		// Same as Cosmetics: one grey card for on and off; on just gets the green outline.
		fillExact(x, y, w, h, RADIUS, on ? CARD_ON : (hover ? CARD_HOVER : CARD));
		outlineExact(x, y, w, h, RADIUS, on ? (hover ? LIME : LIME_EDGE) : (hover ? 0xFF8A8F99 : BORDER_SOFT));

		float titleSize = w < 80 ? 8.5F : 9.5F;
		int titleLimit = Math.max(8, w - 10);
		int titleWidth = OpticUiFont.widthSized(client, module.name, 0xFFFFFFFF, titleSize);
		if (titleWidth > titleLimit && titleWidth > 0) {
			titleSize = Math.max(5.0F, titleSize * titleLimit / (float) titleWidth);
			int fitted = OpticUiFont.widthSized(client, module.name, 0xFFFFFFFF, titleSize);
			if (fitted > titleLimit && fitted > 0) {
				titleSize = Math.max(5.0F, titleSize * titleLimit / (float) fitted);
			}
		}
		shadowedCentered(module.name, x + w / 2, y + 7, 0xFFFFFFFF, titleSize);

		// Icons are baked as fitted squares; center them between the title and the toggle.
		float emojiSize = Math.min(22.0F, Math.max(16.0F, w * 0.26F));
		int areaTop = y + 18;
		int areaBottom = y + h - 24;
		int emojiY = (areaTop + areaBottom) / 2 - Math.round(emojiSize) / 2;
		OpticUiFont.drawEmojiCentered(client, module.icon, x + w / 2, emojiY, emojiSize);

		int toggleW = Math.min(34, Math.max(18, w - 20));
		int toggleH = 14;
		int toggleX = x + (w - toggleW) / 2;
		int toggleY = y + h - toggleH - 8;
		drawToggle("m:" + module.id, toggleX, toggleY, toggleW, toggleH, on);

		int starSize = 8;
		int starHitX = x + w - starSize - 5;
		int starHitY = y + h - starSize - 5;
		drawFavoriteStar(starHitX + starSize / 2, starHitY + starSize / 2, 7.0F, fav, hover);
		return new CardBounds(module.id, x, y, w, h, toggleX, toggleY, toggleW, toggleH, starHitX - 2, starHitY - 2, starSize + 4, starSize + 4);
	}

	/** Per toggle: {progress 0 (off) .. 1 (on), last frame time in ns}. Each has its own clock. */
	private static final java.util.Map<String, double[]> TOGGLE_ANIM = new java.util.HashMap<String, double[]>();
	/** Seconds for a full flip. */
	private static final double TOGGLE_FLIP_SECONDS = 0.14D;

	/**
	 * Pill switch with a knob that slides and a colour that fades when it flips. Uses the exact
	 * (supersampled) rounded shapes so the pill and knob edges stay smooth at any GUI scale.
	 * The knob is drawn in GUI pixels with a fractional offset via the matrix, so it glides
	 * instead of stepping a whole pixel at a time.
	 */
	private void drawToggle(String key, int x, int y, int w, int h, boolean on) {
		long now = System.nanoTime();
		double target = on ? 1.0D : 0.0D;
		double[] state = TOGGLE_ANIM.get(key);
		if (state == null) {
			// First time this toggle is drawn: show it where it already is.
			state = new double[] { target, now };
			TOGGLE_ANIM.put(key, state);
		}
		// Time since this toggle last drew (capped, so a toggle off screen for a while doesn't jump).
		double dt = Math.min(0.05D, (now - (long) state[1]) / 1.0E9D);
		state[1] = now;
		double step = dt / TOGGLE_FLIP_SECONDS;
		state[0] = state[0] < target ? Math.min(target, state[0] + step) : Math.max(target, state[0] - step);
		float p = (float) state[0];
		// Ease in and out: quick but not abrupt.
		float eased = p * p * (3.0F - 2.0F * p);

		fillExact(x, y, w, h, h / 2, OpticTheme.lerp(TOGGLE_OFF, TOGGLE_ON, eased));
		int knob = h - 4;
		float travel = w - knob - 4;
		float knobX = x + 2 + travel * eased;
		int base = (int) Math.floor(knobX);
		com.mojang.blaze3d.platform.GlStateManager.pushMatrix();
		com.mojang.blaze3d.platform.GlStateManager.translate(knobX - base, 0.0F, 0.0F);
		fillExact(base, y + 2, knob, knob, knob / 2, 0xFFF2F4F7);
		com.mojang.blaze3d.platform.GlStateManager.popMatrix();
	}

	private void clipGui(int x, int y, int w, int h) {
		int sf = Math.max(1, new Window(client, client.width, client.height).getScaleFactor());
		if (w < 0) w = 0;
		if (h < 0) h = 0;
		GL11.glEnable(GL11.GL_SCISSOR_TEST);
		GL11.glScissor(x * sf, client.height - (y + h) * sf, w * sf, h * sf);
	}

	private void drawConfigView(int mouseX, int mouseY, int areaX, int areaY, int areaW, int areaH) {
		optionHits.clear();
		backW = 18;
		backH = 18;
		backX = areaX;
		backY = areaY;
		boolean backHover = inside(mouseX, mouseY, backX, backY, backW, backH);
		fillExact(backX, backY, backW, backH, RADIUS_SM, backHover ? ACCENT_SOFT : TAB_IDLE);
		outlineExact(backX, backY, backW, backH, RADIUS_SM, backHover ? ACCENT : BORDER);
		OpticUiFont.drawLineIconCentered(client, "back", backX + backW / 2, backY + backH / 2, 12.0F, 0xFFFFFFFF);

		boolean on = enabled(selected.id);
		int toggleW = 34;
		int toggleH = 14;
		int toggleX = areaX + areaW - toggleW;
		int toggleY = areaY + 2;
		drawToggle("m:" + selected.id, toggleX, toggleY, toggleW, toggleH, on);
		optionHits.add(OptionHit.toggle(selected.id, toggleX - 4, toggleY - 3, toggleW + 8, toggleH + 6));

		int textX = backX + backW + 8;
		int textLimit = toggleX - 8;
		shadowed(selected.name, textX, areaY + 1, 0xFFFFFFFF, 12.0F);
		int nameW = OpticUiFont.widthSized(client, selected.name, 0xFFFFFFFF, 12.0F);
		int sepX = textX + nameW + 6;
		if (sepX + 8 < textLimit) {
			shadowed("|", sepX, areaY + 1, MUTED, 12.0F);
			shadowed(selected.description, sepX + 8, areaY + 2, MUTED, 10.0F);
		}

		int rowX = areaX;
		int rowW = areaW;
		int previewH = 0;
		if (selected.id == CROSSHAIR) {
			previewH = 78;
			int boxY = areaY + 24;
			fillRound(areaX, boxY, areaW, 70, RADIUS_SM, 0xFF101014);
			outlineRound(areaX, boxY, areaW, 70, RADIUS_SM, BORDER_SOFT);
			clipGui(areaX + 4, boxY + 4, areaW - 8, 62);
			OpticCrosshair.preview(areaX + areaW / 2, boxY + 35);
			GL11.glDisable(GL11.GL_SCISSOR_TEST);
		}
		List<ConfigOption> options = optionsFor(selected.id);
		boolean leadHeader = options.isEmpty() || options.get(0).type != OptionType.HEADER;
		int lead = leadHeader ? 18 : 0;
		int contentTop = areaY + 26 + previewH;
		int contentBottom = areaY + areaH - 16;
		int totalH = lead;
		for (int i = 0; i < options.size(); i++) {
			totalH += optionStride(options.get(i));
		}
		int visibleH = Math.max(1, contentBottom - contentTop);
		int maxScroll = Math.max(0, totalH - visibleH);
		configScroll = Math.max(0, Math.min(configScroll, maxScroll));

		int rowY = contentTop - configScroll;
		clipGui(rowX, contentTop, rowW, Math.max(1, contentBottom - contentTop));
		if (leadHeader && rowY + 16 >= contentTop && rowY <= contentBottom) {
			drawSectionHeader(rowX, rowY, "General");
		}
		if (leadHeader) rowY += lead;

		for (int i = 0; i < options.size(); i++) {
			ConfigOption option = options.get(i);
			int stride = optionStride(option);
			if (rowY + stride >= contentTop && rowY <= contentBottom) {
				drawOptionRow(option, rowX, rowY, rowW, stride, mouseX, mouseY, contentTop, contentBottom);
			}
			rowY += stride;
		}
		GL11.glDisable(GL11.GL_SCISSOR_TEST);

		// Scroll bar for long option lists (drag it, or use the wheel).
		configMaxScroll = maxScroll;
		if (maxScroll > 0) {
			int trackX = Math.min(rowX + rowW + 3, panelX + panelW - 6);
			int trackY = contentTop;
			int trackH = visibleH;
			int thumbH = Math.max(18, trackH * trackH / Math.max(1, totalH));
			int thumbY = trackY + (int) ((trackH - thumbH) * (configScroll / (float) maxScroll));
			configTrackX = trackX - 4;
			configTrackY = trackY;
			configTrackW = 12;
			configTrackH = trackH;
			configThumbH = thumbH;
			boolean hover = configScrollDrag || inside(mouseX, mouseY, configTrackX, trackY, configTrackW, trackH);
			fill(trackX, trackY, trackX + 3, trackY + trackH, 0x44FFFFFF);
			fill(trackX, thumbY, trackX + 3, thumbY + thumbH, hover ? 0xFFFFFFFF : ACCENT);
		} else {
			configTrackW = 0;
			configTrackH = 0;
		}

		if (options.isEmpty()) {
			shadowed("No extra options for this mod.", rowX + 30, contentTop + 2, MUTED, 10.0F);
		}

		int resetY = areaY + areaH - 12;
		boolean resetHover = inside(mouseX, mouseY, rowX, resetY - 2, 40, 14);
		shadowed("Reset", rowX, resetY, resetHover ? ACCENT : MUTED, 9.0F);
		optionHits.add(OptionHit.reset(selected.id, rowX, resetY - 2, 40, 14));

		if (openColorKey != null) {
			drawColorPopup(openColorKey, getColor(openColorKey));
		}
	}

	private void drawSectionHeader(int x, int y, String label) {
		shadowed("\u25BC", x, y + 1, ACCENT, 8.0F);
		shadowed(label, x + 12, y + 1, 0xFFFFFFFF, 11.0F);
	}

	private static int optionStride(ConfigOption option) {
		if (option.type == OptionType.HEADER) return 18;
		if (option.type == OptionType.SLIDER) return 34;
		return 22;
	}

	private void drawBlcToggle(String key, int x, int y, boolean on) {
		drawToggle("o:" + key, x, y, 28, 16, on);
	}

	private void drawOptionRow(
		ConfigOption option,
		int x,
		int y,
		int w,
		int h,
		int mouseX,
		int mouseY,
		int clipTop,
		int clipBottom
	) {
		if (y + h < clipTop || y > clipBottom) return;
		if (option.type == OptionType.HEADER) {
			drawSectionHeader(x, y, option.label);
			return;
		}

		if (option.type == OptionType.TOGGLE || option.type == OptionType.VERSION) {
			boolean value = getBool(option.key);
			int toggleX = x;
			int toggleY = y + 2;
			drawBlcToggle(option.key, toggleX, toggleY, value);
			shadowed(option.label, toggleX + 34, y + 4, 0xFFFFFFFF, 11.0F);
			optionHits.add(OptionHit.bool(option.key, x, y, w, h));
		} else if (option.type == OptionType.KEY) {
			Integer bound = OpticRevampOptions.getKey(OpticClient.get().config(), option.key);
			int code = bound == null ? OpticClient.get().config().freeLookKeyCode : bound.intValue();
			String value = bindingKey != null && bindingKey.equals(option.key) ? "Press a key" : OpticFreeLook.keyName(code);
			shadowed(option.label, x, y + 4, 0xFFFFFFFF, 11.0F);
			int valueW = OpticUiFont.widthSized(client, value, 0xFFFFFFFF, 11.0F);
			int valueColor = bindingKey != null && bindingKey.equals(option.key) ? ACCENT : 0xFFDCE0E6;
			shadowed(value, x + w - valueW, y + 4, valueColor, 11.0F);
			optionHits.add(OptionHit.key(option.key, x, y, w, h));
		} else if (option.type == OptionType.TEXT) {
			shadowed(option.label, x, y + 4, 0xFFFFFFFF, 11.0F);
			drawTextBox(option, x, y, w, h, mouseX, mouseY);
		} else if (option.type == OptionType.CYCLE && isDropdown(option.key)) {
			shadowed(option.label, x, y + 4, 0xFFFFFFFF, 11.0F);
			drawDropdownBox(option, x, y, w, h, mouseX, mouseY);
		} else if (option.type == OptionType.CYCLE) {
			String value = getCycleLabel(option.key);
			shadowed(option.label, x, y + 4, 0xFFFFFFFF, 11.0F);
			int valueW = OpticUiFont.widthSized(client, value, 0xFFFFFFFF, 11.0F);
			shadowed(value, x + w - valueW, y + 4, 0xFFDCE0E6, 11.0F);
			optionHits.add(OptionHit.cycle(option.key, x, y, w, h));
		} else if (option.type == OptionType.SLIDER) {
			float value = getFloat(option.key);
			float span = option.max - option.min;
			float t = span == 0.0F ? 0.0F : (value - option.min) / span;
			t = Math.max(0.0F, Math.min(1.0F, t));
			shadowed(option.label, x, y, 0xFFFFFFFF, 11.0F);
			String number = formatFloat(value);
			int numberW = OpticUiFont.widthSized(client, number, 0xFFFFFFFF, 10.0F);
			shadowed(number, x + w - numberW, y, 0xFFDCE0E6, 10.0F);
			int trackY = y + 16;
			int trackH = 4;
			fillRound(x, trackY, w, trackH, 2, 0xFF2C3038);
			int filled = Math.round(w * t);
			if (filled > 2) fillRound(x, trackY, filled, trackH, 2, ACCENT);
			int knob = 10;
			int knobX = x + filled - knob / 2;
			if (knobX < x) knobX = x;
			if (knobX > x + w - knob) knobX = x + w - knob;
			fillRound(knobX, trackY - 3, knob, knob, knob / 2, 0xFFFFFFFF);
			optionHits.add(OptionHit.slider(option.key, x, trackY - 6, w, 16, option.min, option.max));
		} else if (option.type == OptionType.COLOR) {
			int selectedColor = getColor(option.key);
			shadowed(option.label, x, y + 4, 0xFFFFFFFF, 11.0F);
			int sq = 14;
			int sqX = x + w - sq;
			int sqY = y + (h - sq) / 2;
			fill(sqX, sqY, sqX + sq, sqY + sq, OpticConfig.withAlpha(selectedColor, 0xFF));
			outline(sqX, sqY, sq, sq, BORDER);
			optionHits.add(OptionHit.colorSwatch(option.key, sqX, sqY, sq, sq));
			if (option.key.equals(openColorKey)) {
				colorPopupX = Math.min(sqX - COLOR_POPUP_W + sq, panelX + panelW - COLOR_POPUP_W - 8);
				colorPopupY = Math.min(y + h + 4, panelY + panelH - COLOR_POPUP_H - 8);
				if (colorPopupY < y + h && y - COLOR_POPUP_H - 4 > panelY + 70) {
					colorPopupY = y - COLOR_POPUP_H - 4;
				}
				colorPopupW = COLOR_POPUP_W;
				colorPopupH = COLOR_POPUP_H;
			}
		}
	}

	// Badlion-style color picker: saturation/brightness square, hue strip, old/new preview,
	// typed hex code, RGB readout and quick swatches. Gradients are drawn per-vertex on the GPU,
	// so they are smooth at any GUI scale.
	private static final int PICK_PAD = 10;
	private static final int PICK_SV = 108;
	private static final int PICK_HUE_W = 12;
	private static final int PICK_SWATCH = 13;
	private static final int[] PICK_PRESETS = {
		0xFFFFFF, 0xAAAAAA, 0x555555, 0x000000, 0xFF5555, 0xFFAA00, 0xFFFF55,
		0x5CFF8A, 0x00AA00, 0x55FFFF, 0x55AAFF, 0x3355FF, 0xAA55FF, 0xFF55FF
	};

	/** Picker state for the open swatch; keeps the hue when the color is gray/black. */
	private String pickKey;
	private float pickHue;
	private float pickSat;
	private float pickVal;
	private int pickOriginal;
	private boolean hexEditing;
	private String hexBuffer = "";

	private void syncPicker(String key, int color) {
		color &= 0xFFFFFF;
		if (!key.equals(pickKey)) {
			pickKey = key;
			pickOriginal = color;
			hexEditing = false;
			float[] hsv = OpticTextures.rgbToHsv(color);
			pickHue = hsv[0];
			pickSat = hsv[1];
			pickVal = hsv[2];
			return;
		}
		if ((OpticTextures.hsvToRgb(pickHue, pickSat, pickVal) & 0xFFFFFF) == color) return;
		float[] hsv = OpticTextures.rgbToHsv(color);
		if (hsv[1] > 0.01F && hsv[2] > 0.01F) pickHue = hsv[0];
		if (hsv[2] > 0.01F) pickSat = hsv[1];
		pickVal = hsv[2];
	}

	private void applyPicker() {
		if (pickKey != null) setColor(pickKey, OpticTextures.hsvToRgb(pickHue, pickSat, pickVal) & 0xFFFFFF);
	}

	private void drawColorPopup(String key, int selectedColor) {
		syncPicker(key, selectedColor);
		int px = colorPopupX;
		int py = colorPopupY;
		int pw = colorPopupW;
		int ph = colorPopupH;
		OpticTheme.shadow(client, px, py, pw, ph, RADIUS);
		fillRound(px, py, pw, ph, RADIUS, 0xF4101115);
		outlineRound(px, py, pw, ph, RADIUS, OpticTheme.BORDER);

		// Saturation (left -> right) / brightness (top -> bottom) square for the current hue.
		int svX = px + PICK_PAD;
		int svY = py + PICK_PAD;
		int hueRgb = OpticTextures.hsvToRgb(pickHue, 1.0F, 1.0F);
		gradientRect(svX, svY, PICK_SV, PICK_SV, 0xFFFFFFFF, 0xFF000000 | hueRgb, 0xFF000000 | hueRgb, 0xFFFFFFFF);
		gradientRect(svX, svY, PICK_SV, PICK_SV, 0x00000000, 0x00000000, 0xFF000000, 0xFF000000);
		outline(svX - 1, svY - 1, PICK_SV + 2, PICK_SV + 2, OpticTheme.BORDER);
		int mx = svX + Math.round(pickSat * (PICK_SV - 1));
		int my = svY + Math.round((1.0F - pickVal) * (PICK_SV - 1));
		ring(mx, my, 4, pickVal > 0.55F && pickSat < 0.45F ? 0xFF000000 : 0xFFFFFFFF);

		// Hue strip, top to bottom through the rainbow.
		int hueX = svX + PICK_SV + 8;
		for (int i = 0; i < 6; i++) {
			int a = OpticTextures.hsvToRgb(i / 6.0F, 1.0F, 1.0F);
			int b = OpticTextures.hsvToRgb((i + 1) / 6.0F, 1.0F, 1.0F);
			int y1 = svY + PICK_SV * i / 6;
			int y2 = svY + PICK_SV * (i + 1) / 6;
			gradientRect(hueX, y1, PICK_HUE_W, y2 - y1, 0xFF000000 | a, 0xFF000000 | a, 0xFF000000 | b, 0xFF000000 | b);
		}
		outline(hueX - 1, svY - 1, PICK_HUE_W + 2, PICK_SV + 2, OpticTheme.BORDER);
		int hy = svY + Math.round(pickHue * (PICK_SV - 1));
		fill(hueX - 2, hy - 2, hueX + PICK_HUE_W + 2, hy + 2, 0xFF000000);
		fill(hueX - 1, hy - 1, hueX + PICK_HUE_W + 1, hy + 1, 0xFFFFFFFF);

		// Right column: old | new preview, hex code (click to type), RGB values.
		int colX = hueX + PICK_HUE_W + 10;
		int colW = px + pw - PICK_PAD - colX;
		int current = selectedColor & 0xFFFFFF;
		fill(colX, svY, colX + colW / 2, svY + 24, 0xFF000000 | pickOriginal);
		fill(colX + colW / 2, svY, colX + colW, svY + 24, 0xFF000000 | current);
		outline(colX - 1, svY - 1, colW + 2, 26, OpticTheme.BORDER);
		shadowed("OLD", colX + 2, svY + 27, 0xFFAEB4BF, 6.5F);
		int newW = OpticUiFont.widthSized(client, "NEW", 0xFFAEB4BF, 6.5F);
		shadowed("NEW", colX + colW - newW - 2, svY + 27, 0xFFAEB4BF, 6.5F);

		int hexY = svY + 40;
		fillRound(colX, hexY, colW, 16, RADIUS_SM, 0xFF0B0C0F);
		outlineRound(colX, hexY, colW, 16, RADIUS_SM, hexEditing ? ACCENT : OpticTheme.BORDER);
		String hex = hexEditing
			? "#" + hexBuffer + ((System.currentTimeMillis() / 500L) % 2L == 0L ? "_" : "")
			: String.format("#%06X", current);
		shadowed(hex, colX + 5, hexY + 4, hexEditing ? OpticTheme.TEXT : MUTED, 8.0F);

		int rgbY = hexY + 24;
		String[] names = { "R", "G", "B" };
		int[] parts = { (current >> 16) & 0xFF, (current >> 8) & 0xFF, current & 0xFF };
		for (int i = 0; i < 3; i++) {
			int ry = rgbY + i * 11;
			shadowed(names[i], colX + 1, ry, 0xFFAEB4BF, 7.5F);
			String v = Integer.toString(parts[i]);
			int vw = OpticUiFont.widthSized(client, v, OpticTheme.TEXT, 7.5F);
			shadowed(v, colX + colW - vw - 1, ry, OpticTheme.TEXT, 7.5F);
		}

		// Quick swatches along the bottom.
		int swY = svY + PICK_SV + 8;
		int gap = 2;
		for (int i = 0; i < PICK_PRESETS.length; i++) {
			int sx = svX + i * (PICK_SWATCH + gap);
			if (sx + PICK_SWATCH > px + pw - PICK_PAD) break;
			fill(sx, swY, sx + PICK_SWATCH, swY + PICK_SWATCH, 0xFF000000 | PICK_PRESETS[i]);
			boolean chosen = PICK_PRESETS[i] == current;
			outline(sx - 1, swY - 1, PICK_SWATCH + 2, PICK_SWATCH + 2, chosen ? ACCENT : OpticTheme.BORDER);
			optionHits.add(OptionHit.colorPreset(key, sx, swY, PICK_SWATCH, PICK_SWATCH, PICK_PRESETS[i]));
		}

		optionHits.add(OptionHit.colorPopup(px, py, pw, ph));
		optionHits.add(OptionHit.colorSv(key, svX, svY, PICK_SV, PICK_SV));
		optionHits.add(OptionHit.colorHue(key, hueX - 2, svY, PICK_HUE_W + 4, PICK_SV));
		optionHits.add(OptionHit.colorHex(key, colX, hexY, colW, 16));
	}

	/** Quad with one color per corner (top-left, top-right, bottom-right, bottom-left). */
	private static void gradientRect(int x, int y, int w, int h, int tl, int tr, int br, int bl) {
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_ALPHA_TEST);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glShadeModel(GL11.GL_SMOOTH);
		net.minecraft.client.render.Tessellator t = net.minecraft.client.render.Tessellator.INSTANCE;
		t.begin(GL11.GL_QUADS);
		vertex(t, x + w, y, tr);
		vertex(t, x, y, tl);
		vertex(t, x, y + h, bl);
		vertex(t, x + w, y + h, br);
		t.end();
		GL11.glShadeModel(GL11.GL_FLAT);
		GL11.glEnable(GL11.GL_ALPHA_TEST);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
	}

	private static void vertex(net.minecraft.client.render.Tessellator t, int x, int y, int argb) {
		t.color((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >>> 24) & 0xFF);
		t.vertex(x, y, 0.0);
	}

	/** Hollow square marker with a dark rim so it reads on any color. */
	private void ring(int cx, int cy, int r, int argb) {
		outline(cx - r - 1, cy - r - 1, r * 2 + 3, r * 2 + 3, 0x99000000);
		outline(cx - r, cy - r, r * 2 + 1, r * 2 + 1, argb);
	}

	/** Typed hex: 0-9 / A-F, Backspace, Enter to apply, Escape to cancel. @return handled */
	private boolean hexKey(char character, int keyCode) {
		if (!hexEditing) return false;
		if (keyCode == Keyboard.KEY_ESCAPE) {
			hexEditing = false;
			return true;
		}
		if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
			if (hexBuffer.length() == 6 && pickKey != null) {
				setColor(pickKey, Integer.parseInt(hexBuffer, 16));
			} else if (hexBuffer.length() == 3 && pickKey != null) {
				String full = "" + hexBuffer.charAt(0) + hexBuffer.charAt(0) + hexBuffer.charAt(1) + hexBuffer.charAt(1) + hexBuffer.charAt(2) + hexBuffer.charAt(2);
				setColor(pickKey, Integer.parseInt(full, 16));
			}
			hexEditing = false;
			return true;
		}
		if (keyCode == Keyboard.KEY_BACK) {
			if (hexBuffer.length() > 0) hexBuffer = hexBuffer.substring(0, hexBuffer.length() - 1);
			return true;
		}
		char c = Character.toUpperCase(character);
		if (((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F')) && hexBuffer.length() < 6) hexBuffer += c;
		return true;
	}
	private List<ConfigOption> optionsFor(int id) {
		List<ConfigOption> options = new ArrayList<ConfigOption>();
		switch (id) {
			case FPS:
				options.add(ConfigOption.toggle("fpsTextShadow", "Text Shadow"));
				options.add(ConfigOption.cycle("fpsOpticFont", "Font"));
				options.add(ConfigOption.color("fpsColor", "Accent Color"));
				options.add(ConfigOption.color("fpsTextColor", "Text Color"));
				options.add(ConfigOption.toggle("fpsShowLabel", "Show FPS Label"));
				options.add(ConfigOption.toggle("fpsReverseOrder", "Reverse Order"));
				options.add(ConfigOption.toggle("fpsBrackets", "Brackets"));
				break;
			case CPS:
				options.add(ConfigOption.toggle("cpsTextShadow", "Text Shadow"));
				options.add(ConfigOption.cycle("cpsOpticFont", "Font"));
				options.add(ConfigOption.color("cpsColor", "Accent Color"));
				options.add(ConfigOption.color("cpsTextColor", "Text Color"));
				options.add(ConfigOption.toggle("cpsShowLabel", "Show CPS Label"));
				options.add(ConfigOption.toggle("cpsBrackets", "Brackets"));
				options.add(ConfigOption.toggle("cpsSeparateButtons", "Separate LMB / RMB"));
				options.add(ConfigOption.toggle("cpsReverseText", "Reverse Text"));
				options.add(ConfigOption.toggle("cpsIgnoreCancelled", "Ignore Cancelled Clicks"));
				break;
			case KEYSTROKES:
				options.add(ConfigOption.toggle("keystrokesShowMovement", "Movement Keys"));
				options.add(ConfigOption.toggle("keystrokesUseArrows", "Use Arrow Keys"));
				options.add(ConfigOption.toggle("keystrokesShowMouse", "Mouse Buttons"));
				options.add(ConfigOption.toggle("keystrokesShowLeftCps", "Left CPS"));
				options.add(ConfigOption.toggle("keystrokesShowRightCps", "Right CPS"));
				options.add(ConfigOption.toggle("keystrokesShowSpace", "Spacebar"));
				options.add(ConfigOption.toggle("keystrokesTextShadow", "Text Shadow"));
				options.add(ConfigOption.toggle("keystrokesBorder", "Border"));
				options.add(ConfigOption.toggle("keystrokesInnerBorder", "Inner Border"));
				options.add(ConfigOption.slider("keystrokesBoxSize", "Box Size", 10.0F, 32.0F));
				options.add(ConfigOption.slider("keystrokesBorderThickness", "Border Thickness", 0.5F, 3.0F));
				options.add(ConfigOption.slider("keystrokesSpaceThickness", "Spacebar Thickness", 1.0F, 4.25F));
				options.add(ConfigOption.toggle("keystrokesFade", "Fade"));
				options.add(ConfigOption.slider("keystrokesFadeMs", "Fade Delay (ms)", 0.0F, 500.0F));
				options.add(ConfigOption.header("Animation"));
				options.add(ConfigOption.toggle("keystrokesAnimate", "Animate"));
				options.add(ConfigOption.cycle("keystrokesAnimation", "Animation"));
				options.add(ConfigOption.slider("keystrokesAnimDuration", "Duration (s)", 0.1F, 1.0F));
				options.add(ConfigOption.cycle("keystrokesEasing", "Timing"));
				options.add(ConfigOption.toggle("keystrokesAnimateColor", "Animate Color"));
				options.add(ConfigOption.color("keystrokesAnimStartColor", "Start Color"));
				options.add(ConfigOption.color("keystrokesAnimEndColor", "End Color"));
				options.add(ConfigOption.header("Colors"));
				options.add(ConfigOption.color("keystrokesBackgroundColor", "Background"));
				options.add(ConfigOption.color("keystrokesBackgroundPressedColor", "Pressed Background"));
				options.add(ConfigOption.color("keystrokesTextColor", "Text"));
				options.add(ConfigOption.color("keystrokesTextPressedColor", "Pressed Text"));
				options.add(ConfigOption.color("keystrokesBorderColor", "Border Color"));
				break;
			case PING:
				options.add(ConfigOption.toggle("pingTextShadow", "Text Shadow"));
				options.add(ConfigOption.cycle("pingOpticFont", "Font"));
				options.add(ConfigOption.color("pingColor", "Accent Color"));
				options.add(ConfigOption.color("pingTextColor", "Text Color"));
				options.add(ConfigOption.toggle("pingShowLabel", "Show Ping Label"));
				options.add(ConfigOption.toggle("pingBrackets", "Brackets"));
				options.add(ConfigOption.toggle("pingShowMs", "Show ms Suffix"));
				options.add(ConfigOption.toggle("pingReversePrefix", "Value Before Label"));
				options.add(ConfigOption.toggle("pingDynamicColor", "Dynamic Ping Color"));
				options.add(ConfigOption.color("pingLowColor", "Low Ping"));
				options.add(ConfigOption.color("pingMediumColor", "Medium Ping"));
				options.add(ConfigOption.color("pingHighColor", "High Ping"));
				options.add(ConfigOption.color("pingExtremeColor", "Extreme Ping"));
				options.add(ConfigOption.toggle("pingSpike", "Spike Detection"));
				options.add(ConfigOption.slider("pingSpikeThreshold", "Spike Threshold %", 1.0F, 200.0F));
				options.add(ConfigOption.color("pingSpikeColor", "Spike Color"));
				options.add(ConfigOption.toggle("pingBackground", "Background"));
				options.add(ConfigOption.color("pingBackgroundColor", "Background Color"));
				options.add(ConfigOption.toggle("pingNametag", "Ping On Nametags"));
				break;
			case COORDINATES:
				options.add(ConfigOption.toggle("coordinatesTextShadow", "Text Shadow"));
				options.add(ConfigOption.cycle("coordinatesOpticFont", "Font"));
				options.add(ConfigOption.color("coordinatesColor", "Accent Color"));
				options.add(ConfigOption.color("coordinatesTextColor", "Text Color"));
				options.add(ConfigOption.toggle("coordinatesShowLabel", "Show XYZ Label"));
				options.add(ConfigOption.toggle("coordinatesBrackets", "Brackets"));
				options.add(ConfigOption.toggle("coordinatesShowX", "Show X Coordinate"));
				options.add(ConfigOption.toggle("coordinatesShowY", "Show Y Coordinate"));
				options.add(ConfigOption.toggle("coordinatesShowZ", "Show Z Coordinate"));
				options.add(ConfigOption.cycle("coordinatesDecimals", "Decimal Places"));
				options.add(ConfigOption.toggle("coordinatesHorizontal", "Horizontal Layout"));
				options.add(ConfigOption.toggle("coordinatesDirection", "Show Direction"));
				options.add(ConfigOption.toggle("coordinatesDirectionMarks", "Direction Marks"));
				options.add(ConfigOption.toggle("coordinatesBiome", "Show Biome"));
				options.add(ConfigOption.toggle("coordinatesShowWhileTyping", "Show While Typing"));
				options.add(ConfigOption.toggle("coordinatesBackground", "Background"));
				options.add(ConfigOption.color("coordinatesBackgroundColor", "Background Color"));
				options.add(ConfigOption.toggle("coordinatesBorder", "Border"));
				options.add(ConfigOption.color("coordinatesBorderColor", "Border Color"));
				options.add(ConfigOption.key("coordinatesCopyKey", "Copy Coordinates"));
				break;
			case TOGGLE_SPRINT:
				options.add(ConfigOption.toggle("toggleSprintTextShadow", "Text Shadow"));
				options.add(ConfigOption.cycle("toggleSprintOpticFont", "Font"));
				options.add(ConfigOption.color("toggleSprintColor", "Accent Color"));
				options.add(ConfigOption.color("toggleSprintTextColor", "Text Color"));
				options.add(ConfigOption.toggle("toggleSprintShowHud", "Show Sprint HUD"));
				options.add(ConfigOption.toggle("toggleSprintBrackets", "Brackets"));
				break;
			case FULLBRIGHT:
				options.add(ConfigOption.slider("fullbrightGamma", "Brightness", 1.0F, 16.0F));
				break;
			case ANIMATIONS:
				options.add(ConfigOption.header("General"));
				options.add(ConfigOption.version("animHitbox", "Hitbox"));
				options.add(ConfigOption.version("animInventory", "Inventory"));
				options.add(ConfigOption.version("animHealth", "Health Animation"));
				options.add(ConfigOption.version("animDamage", "Damage Animation"));
				options.add(ConfigOption.version("animSneaking", "Sneaking"));
				options.add(ConfigOption.version("animOldEnchantGlint", "Old Enchant Glint"));
				options.add(ConfigOption.version("anim18Skins", "1.8 Skins"));
				options.add(ConfigOption.version("animTitles", "Titles"));
				options.add(ConfigOption.version("animFixGhostLiquid", "Fix Ghost Liquid Blocks"));
				options.add(ConfigOption.header("Items"));
				options.add(ConfigOption.version("animItemHold", "Item Hold"));
				options.add(ConfigOption.version("animItemSwitch", "Item Switch"));
				options.add(ConfigOption.version("animBlockHit", "Block Hit"));
				options.add(ConfigOption.version("animEating", "Eating"));
				options.add(ConfigOption.version("animThrowing", "Throwing"));
				options.add(ConfigOption.version("animFishingRod", "Fishing Rod"));
				options.add(ConfigOption.version("animSwordSwing", "Sword Swing"));
				options.add(ConfigOption.version("animBow", "Bow Animation"));
				options.add(ConfigOption.version("animLeftHand", "Left Hand"));
				options.add(ConfigOption.version("animMovableTitles", "Movable 1.8 Titles"));
				options.add(ConfigOption.slider("titleOffsetX", "Title Offset X", -80.0F, 80.0F));
				options.add(ConfigOption.slider("titleOffsetY", "Title Offset Y", -80.0F, 80.0F));
				break;
			case PARTICLES:
				options.add(ConfigOption.toggle("showCriticalHitParticles", "Show Critical Hit Particles"));
				options.add(ConfigOption.toggle("alwaysSharpnessParticles", "Always Sharpness Particles"));
				options.add(ConfigOption.slider("particleMultiplier", "Particle Multiplier", 0.0F, 10.0F));
				options.add(ConfigOption.toggle("removeVanillaColorFilter", "Remove Vanilla Color Filter"));
				options.add(ConfigOption.toggle("renderOwnParticles", "Render Own Particles"));
				options.add(ConfigOption.toggle("blockBreakParticles", "Block Break Particles"));
				break;
			case BLOCK_OVERLAY:
				options.add(ConfigOption.header("General"));
				options.add(ConfigOption.toggle("blockOverlay3d", "3D Overlay (see through blocks)"));
				options.add(ConfigOption.header("Fill"));
				options.add(ConfigOption.toggle("blockOverlayFill", "Fill"));
				options.add(ConfigOption.color("blockOverlayFillColor", "Fill Color"));
				options.add(ConfigOption.slider("blockOverlayFillAlpha", "Fill Opacity", 0.0F, 1.0F));
				options.add(ConfigOption.header("Outline"));
				options.add(ConfigOption.toggle("blockOverlayOutline", "Outline"));
				options.add(ConfigOption.color("blockOverlayOutlineColor", "Outline Color"));
				options.add(ConfigOption.slider("blockOverlayOutlineAlpha", "Outline Opacity", 0.0F, 1.0F));
				options.add(ConfigOption.slider("blockOverlayLineWidth", "Outline Width", 1.0F, 8.0F));
				break;
			case NAMETAGS:
				options.add(ConfigOption.header("General"));
				options.add(ConfigOption.toggle("nametagsTextShadow", "Text Shadow"));
				options.add(ConfigOption.toggle("nametagsShowInF5", "Show Own Nametag"));
				options.add(ConfigOption.toggle("nametagsToggleMessages", "Toggle Messages"));
				options.add(ConfigOption.key("nametagsToggleAllKey", "Toggle All Nametags"));
				options.add(ConfigOption.key("nametagsTogglePlayersKey", "Toggle Player Nametags"));
				options.add(ConfigOption.toggle("nametagsHideInF1", "Hide Nametags in F1"));
				options.add(ConfigOption.slider("nametagsBackgroundOpacity", "Background Opacity", 0.0F, 1.0F));
				options.add(ConfigOption.toggle("nametagsBackground", "Background"));
				options.add(ConfigOption.color("nametagsBackgroundColor", "Background Color"));
				options.add(ConfigOption.toggle("nametagsReplaceOwnColor", "Replace Own Nametag Color"));
				options.add(ConfigOption.color("nametagsPersonalColor", "Own Nametag Color"));
				options.add(ConfigOption.toggle("nametagsBadlionFont", "Badlion Font"));
				options.add(ConfigOption.toggle("nametagsRoundedCorners", "Rounded Corners"));
				addNametagUserColorOptions(options);
				break;
			case CHAT:
				options.add(ConfigOption.header("General"));
				options.add(ConfigOption.toggle("chatBold", "Bold"));
				options.add(ConfigOption.toggle("chatAntiSpam", "Anti-Spam"));
				options.add(ConfigOption.toggle("chatInfiniteHistory", "Infinite History"));
				options.add(ConfigOption.slider("chatBackgroundOpacity", "Background Opacity", 0.0F, 100.0F));
				break;
			case SCOREBOARD:
				options.add(ConfigOption.header("General"));
				options.add(ConfigOption.toggle("scoreboardShowNumbers", "Show Numbers"));
				options.add(ConfigOption.toggle("scoreboardRemoveNumberWidth", "Remove Number Width"));
				options.add(ConfigOption.toggle("scoreboardTextShadow", "Text Shadow"));
				options.add(ConfigOption.slider("scoreboardBackgroundOpacity", "Background Opacity", 0.0F, 100.0F));
				break;
			case HOTBAR:
				options.add(ConfigOption.header("General"));
				options.add(ConfigOption.slider("hotbarScale", "Scale", 0.5F, 3.0F));
				options.add(ConfigOption.cycle("hotbarRotation", "Rotation"));
				options.add(ConfigOption.header("Show"));
				options.add(ConfigOption.toggle("hotbarShowSlots", "Hotbar Slots"));
				options.add(ConfigOption.toggle("hotbarShowHearts", "Hearts & Armor"));
				options.add(ConfigOption.toggle("hotbarShowHunger", "Hunger & Air"));
				options.add(ConfigOption.toggle("hotbarShowXp", "XP Bar & Level"));
				options.add(ConfigOption.toggle("itemInfo", "Item Info"));
				break;
			case TIME_CHANGER:
				options.add(ConfigOption.header("General"));
				options.add(ConfigOption.cycle("timePreset", "Time Preset"));
				options.add(ConfigOption.slider("timeOfDay", "Day", 0.0F, 23999.0F));
				break;
			case WEATHER_CHANGER:
				options.add(ConfigOption.header("General"));
				options.add(ConfigOption.cycle("weatherMode", "Weather"));
				break;
			case NICK_HIDER: {
				OpticConfig nc = OpticClient.get().config();
				options.add(ConfigOption.header("Name Options"));
				options.add(ConfigOption.toggle("nickHideOwnName", "Hide Own Name"));
				if (nc.nickHideOwnName) options.add(ConfigOption.text("nickOwnName", "Own Name"));
				options.add(ConfigOption.toggle("nickHideOthers", "Hide Others' Names"));
				if (nc.nickHideOthers) {
					options.add(ConfigOption.text("nickPrefix", "Prefix"));
					options.add(ConfigOption.toggle("nickCustomSuffix", "Custom Suffix"));
					if (nc.nickCustomSuffix) options.add(ConfigOption.text("nickSuffix", "Suffix"));
				}
				options.add(ConfigOption.toggle("nickHideLobbyId", "Hide Lobby ID"));
				options.add(ConfigOption.header("Skin Options"));
				options.add(ConfigOption.toggle("nickHideOwnSkin", "Hide Own Skin"));
				options.add(ConfigOption.toggle("nickHideOthersSkin", "Hide Others' Skins"));
				break;
			}
			case MOTION_BLUR:
				options.add(ConfigOption.header("General"));
				options.add(ConfigOption.cycle("motionBlurType", "Type"));
				options.add(ConfigOption.slider("motionBlurAmount", "Amount", 1.0F, 10.0F));
				break;
			case FOV:
				options.add(ConfigOption.header("General"));
				options.add(ConfigOption.slider("fov", "FOV", 30.0F, 150.0F));
				break;
			case HURT_CAM:
				options.add(ConfigOption.header("General"));
				options.add(ConfigOption.toggle("disableHurtCam", "Disable Hurt Cam"));
				if (!OpticClient.get().config().disableHurtCam) {
					options.add(ConfigOption.slider("hurtCamIntensity", "Hurt Shaking Intensity", 0.0F, 2.0F));
				}
				break;
			case HIT_PREDICT:
				options.add(ConfigOption.header("Visual only. Clicks stay vanilla."));
				break;
			case BOSS_BAR:
				options.add(ConfigOption.header("General"));
				options.add(ConfigOption.toggle("bossBarShow", "Show Boss Bar"));
				options.add(ConfigOption.toggle("bossBarShowName", "Show Name"));
				options.add(ConfigOption.toggle("bossBarShowBar", "Show Health Bar"));
				options.add(ConfigOption.slider("bossBarScale", "Scale", 0.5F, 3.0F));
				break;
			case PVP_TWEAKS:
				options.add(ConfigOption.header("Overlays"));
				options.add(ConfigOption.toggle("pvpLowFire", "Low Fire"));
				options.add(ConfigOption.toggle("pvpNoPumpkin", "No Pumpkin Overlay"));
				options.add(ConfigOption.toggle("pvpClearWater", "Clear Water"));
				options.add(ConfigOption.toggle("pvpNoVignette", "No Vignette"));
				options.add(ConfigOption.toggle("pvpNoPortal", "No Portal Overlay"));
				break;
			case SKINS_3D:
				options.add(ConfigOption.header("Layers"));
				options.add(ConfigOption.toggle("skins3dHead", "Hat"));
				options.add(ConfigOption.toggle("skins3dBody", "Jacket"));
				options.add(ConfigOption.toggle("skins3dArms", "Sleeves"));
				options.add(ConfigOption.toggle("skins3dLegs", "Pants"));
				options.add(ConfigOption.header("General"));
				options.add(ConfigOption.slider("skins3dDistance", "Render Distance", 4.0F, 64.0F));
				break;
			case HITBOXES:
				options.add(ConfigOption.header("General"));
				options.add(ConfigOption.toggle("hitboxHideLook", "Disable Look Vectors"));
				options.add(ConfigOption.header("Colors"));
				options.add(ConfigOption.color("hitboxPlayerColor", "Players"));
				options.add(ConfigOption.color("hitboxMobColor", "Mobs"));
				options.add(ConfigOption.color("hitboxArrowColor", "Arrows"));
				options.add(ConfigOption.color("hitboxOtherColor", "Other"));
				break;
			case FREE_LOOK:
				options.add(ConfigOption.header("General"));
				options.add(ConfigOption.key("freeLookKeyCode", "Keybind"));
				options.add(ConfigOption.toggle("freeLookToggle", "Toggle Mode"));
				options.add(ConfigOption.cycle("freeLookPerspective", "Perspective"));
				options.add(ConfigOption.slider("freeLookDistance", "Camera Distance", 1.5F, 12.0F));
				options.add(ConfigOption.toggle("freeLookInvertYaw", "Invert Yaw"));
				options.add(ConfigOption.toggle("freeLookInvertPitch", "Invert Pitch"));
				options.add(ConfigOption.toggle("freeLookSmooth", "Smooth Camera"));
				break;
			case CROSSHAIR:
				options.add(ConfigOption.header("Shape"));
				options.add(ConfigOption.cycle("crosshairShape", "Shape"));
				options.add(ConfigOption.color("crosshairColor", "Color"));
				options.add(ConfigOption.slider("crosshairGap", "Gap", 0.0F, 12.0F));
				options.add(ConfigOption.slider("crosshairLength", "Length", 1.0F, 16.0F));
				options.add(ConfigOption.slider("crosshairThickness", "Thickness", 1.0F, 6.0F));
				options.add(ConfigOption.toggle("crosshairOutline", "Outline"));
				options.add(ConfigOption.color("crosshairOutlineColor", "Outline Color"));
				options.add(ConfigOption.header("Behavior"));
				options.add(ConfigOption.toggle("crosshairShowInF5", "Show In Third Person"));
				options.add(ConfigOption.toggle("crosshairDynamicAttack", "Spread On Attack"));
				options.add(ConfigOption.slider("crosshairAttackSpread", "Attack Spread", 0.0F, 12.0F));
				options.add(ConfigOption.toggle("crosshairDynamicBow", "Spread While Drawing"));
				options.add(ConfigOption.slider("crosshairBowSpread", "Bow Spread", 0.0F, 16.0F));
				options.add(ConfigOption.toggle("crosshairHealthShift", "Color When Low Health"));
				options.add(ConfigOption.slider("crosshairHealthThreshold", "Health Threshold", 1.0F, 20.0F));
				options.add(ConfigOption.color("crosshairHealthColor", "Low Health Color"));
				options.add(ConfigOption.toggle("crosshairEnemy", "Enemy Color"));
				options.add(ConfigOption.color("crosshairEnemyColor", "Enemy"));
				options.add(ConfigOption.toggle("crosshairFriendly", "Friendly Color"));
				options.add(ConfigOption.color("crosshairFriendlyColor", "Friendly"));
				break;
			case GUI_SCALE:
				options.add(ConfigOption.slider("crosshairSize", "Crosshair Size", 0.4F, 3.0F));
				options.add(ConfigOption.cycle("menuScale", "Menu Size"));
				options.add(ConfigOption.cycle("inventoryScale", "Inventory / Chest Size"));
				break;
			case TAB:
				options.add(ConfigOption.header("General"));
				options.add(ConfigOption.key("tabKeyCode", "Keybind"));
				options.add(ConfigOption.toggle("tabToggle", "Toggle Key"));
				options.add(ConfigOption.toggle("tabHeads", "Player Heads"));
				options.add(ConfigOption.toggle("tabHideNpc", "Hide 0 Ping Entries"));
				options.add(ConfigOption.toggle("tabMoveSelfTop", "Move Self To Top"));
				options.add(ConfigOption.toggle("tabNameShadow", "Name Shadow"));
				options.add(ConfigOption.toggle("tabHighlightSelf", "Highlight Own Name"));
				options.add(ConfigOption.color("tabSelfColor", "Own Name Color"));
				options.add(ConfigOption.header("Ping"));
				options.add(ConfigOption.toggle("tabHidePing", "Hide Ping"));
				options.add(ConfigOption.toggle("tabHidePingOver500", "Hide Ping Over 500"));
				options.add(ConfigOption.toggle("tabPingNumbers", "Ping As Number"));
				options.add(ConfigOption.toggle("tabPingNumberShadow", "Ping Number Shadow"));
				options.add(ConfigOption.toggle("tabDynamicPing", "Dynamic Ping Color"));
				options.add(ConfigOption.color("tabPingLowColor", "Low Ping"));
				options.add(ConfigOption.color("tabPingMediumColor", "Medium Ping"));
				options.add(ConfigOption.color("tabPingHighColor", "High Ping"));
				options.add(ConfigOption.color("tabPingExtremeColor", "Extreme Ping"));
				options.add(ConfigOption.color("tabPingColor", "Ping Color"));
				options.add(ConfigOption.header("Colors"));
				options.add(ConfigOption.color("tabBackgroundColor", "Background"));
				options.add(ConfigOption.toggle("tabPingRow", "Colored Rows"));
				options.add(ConfigOption.color("tabRowsColor", "Rows Color"));
				break;
			case TITLES:
				options.add(ConfigOption.header("Overlay"));
				options.add(ConfigOption.toggle("titlesShowTitle", "Show Title"));
				options.add(ConfigOption.toggle("titlesShowSubtitle", "Show Subtitle"));
				options.add(ConfigOption.color("titlesTitleColor", "Title Color"));
				options.add(ConfigOption.color("titlesSubtitleColor", "Subtitle Color"));
				options.add(ConfigOption.slider("titlesScale", "Scale", 0.5F, 3.0F));
				options.add(ConfigOption.toggle("titlesCentered", "Keep Centered"));
				options.add(ConfigOption.header("Window Title Bar"));
				options.add(ConfigOption.toggle("windowTitle", "Custom Window Title"));
				options.add(ConfigOption.cycle("windowTitleMode", "Title Bar Text"));
				break;
			case HIT_COLOR:
				options.add(ConfigOption.color("hitColorRgb", "Hit Color"));
				options.add(ConfigOption.slider("hitColorAlpha", "Strength", 0.05F, 1.0F));
				break;
			case POTIONS:
				options.add(ConfigOption.toggle("potionsSymbols", "Effect Icons"));
				options.add(ConfigOption.toggle("potionsHorizontal", "Horizontal"));
				options.add(ConfigOption.toggle("potionsShowName", "Effect Name"));
				options.add(ConfigOption.toggle("potionsShowAmplifier", "Amplifier"));
				options.add(ConfigOption.toggle("potionsShowDuration", "Duration"));
				options.add(ConfigOption.toggle("potionsBlink", "Red Time When Expiring"));
				options.add(ConfigOption.slider("potionsBlinkSeconds", "Red Under (s)", 2.0F, 20.0F));
				options.add(ConfigOption.toggle("potionsHideVanilla", "Hide Inventory Icons"));
				options.add(ConfigOption.toggle("potionsShowInInventory", "Show In Inventory"));
				options.add(ConfigOption.toggle("potionsBackground", "Background"));
				options.add(ConfigOption.color("potionsBackgroundColor", "Background Color"));
				options.add(ConfigOption.color("potionsTextColor", "Text Color"));
				options.add(ConfigOption.toggle("potionsTextShadow", "Text Shadow"));
				break;
			case ZOOM:
				options.add(ConfigOption.header("General"));
				options.add(ConfigOption.slider("zoomFactor", "Zoom Amount", 1.25F, 10.0F));
				options.add(ConfigOption.toggle("zoomScroll", "Scroll to Zoom In Farther"));
				if (OpticClient.get().config().zoomScroll) {
					options.add(ConfigOption.slider("zoomScrollSpeed", "Scroll Zoom Speed", 0.05F, 1.0F));
				}
				options.add(ConfigOption.toggle("zoomSmooth", "Smooth Zoom"));
				options.add(ConfigOption.toggle("zoomSmoothCamera", "Smooth Camera Movement"));
				options.add(ConfigOption.toggle("zoomMouseSensitivity", "Zoom Mouse Sensitivity"));
				break;
			default:
				break;
		}
		return options;
	}

	private void addNametagUserColorOptions(List<ConfigOption> options) {
		OpticConfig c = OpticClient.get().config();
		Set<String> names = new LinkedHashSet<String>();
		names.addAll(c.nametagNameColors.keySet());
		if (client != null && client.world != null) {
			@SuppressWarnings("unchecked")
			List<PlayerEntity> players = client.world.playerEntities;
			if (players != null) {
				for (PlayerEntity player : players) {
					if (player == null || player == client.field_3805) continue;
					String name = player.getGameProfile() != null ? player.getGameProfile().getName() : null;
					if (name == null || name.isEmpty()) {
						name = player.getName().asFormattedString();
					}
					if (name != null && !name.isEmpty()) names.add(name);
				}
			}
		}
		for (String name : names) {
			options.add(ConfigOption.color("nametagUser:" + name, name));
		}
	}

	private boolean getBool(String key) {
		OpticConfig c = OpticClient.get().config();
		if ("nickHideOwnName".equals(key)) return c.nickHideOwnName;
		if ("nickHideOthers".equals(key)) return c.nickHideOthers;
		if ("nickCustomSuffix".equals(key)) return c.nickCustomSuffix;
		if ("nickHideLobbyId".equals(key)) return c.nickHideLobbyId;
		if ("nickHideOwnSkin".equals(key)) return c.nickHideOwnSkin;
		if ("nickHideOthersSkin".equals(key)) return c.nickHideOthersSkin;
		Boolean extra = OpticRevampOptions.getBool(c, key);
		if (extra != null) return extra.booleanValue();
		if ("fpsTextShadow".equals(key)) return c.fpsTextShadow;
		if ("fpsShowLabel".equals(key)) return c.fpsShowLabel;
		if ("fpsBrackets".equals(key)) return c.fpsBrackets;
		if ("cpsTextShadow".equals(key)) return c.cpsTextShadow;
		if ("cpsShowLabel".equals(key)) return c.cpsShowLabel;
		if ("cpsBrackets".equals(key)) return c.cpsBrackets;
		if ("cpsSeparateButtons".equals(key)) return c.cpsSeparateButtons;
		if ("keystrokesTextShadow".equals(key)) return c.keystrokesTextShadow;
		if ("keystrokesLockKeys".equals(key)) return c.keystrokesLockKeys;
		if ("keystrokesShowMouse".equals(key)) return c.keystrokesShowMouse;
		if ("keystrokesModernStyle".equals(key)) return c.keystrokesModernStyle;
		if ("keystrokesBackground".equals(key)) return c.keystrokesBackground;
		if ("keystrokesShowLabels".equals(key)) return c.keystrokesShowLabels;
		if ("keystrokesInstant".equals(key)) return c.keystrokesInstant;
		if ("keystrokesFade".equals(key)) return c.keystrokesFade;
		if ("pingTextShadow".equals(key)) return c.pingTextShadow;
		if ("pingShowLabel".equals(key)) return c.pingShowLabel;
		if ("pingBrackets".equals(key)) return c.pingBrackets;
		if ("pingShowMs".equals(key)) return c.pingShowMs;
		if ("coordinatesTextShadow".equals(key)) return c.coordinatesTextShadow;
		if ("coordinatesBrackets".equals(key)) return c.coordinatesBrackets;
		if ("toggleSprintTextShadow".equals(key)) return c.toggleSprintTextShadow;
		if ("toggleSprintBrackets".equals(key)) return c.toggleSprintBrackets;
		if ("coordinatesShowLabel".equals(key)) return c.coordinatesShowLabel;
		if ("coordinatesShowX".equals(key)) return c.coordinatesShowX;
		if ("coordinatesShowY".equals(key)) return c.coordinatesShowY;
		if ("coordinatesShowZ".equals(key)) return c.coordinatesShowZ;
		if ("toggleSprintShowHud".equals(key)) return c.toggleSprintShowHud;
		if ("showCriticalHitParticles".equals(key)) return c.showCriticalHitParticles;
		if ("alwaysSharpnessParticles".equals(key)) return c.alwaysSharpnessParticles;
		if ("removeVanillaColorFilter".equals(key)) return c.removeVanillaColorFilter;
		if ("renderOwnParticles".equals(key)) return c.renderOwnParticles;
		if ("blockBreakParticles".equals(key)) return c.blockBreakParticles;
		if ("blockOverlayFill".equals(key)) return c.blockOverlayFill;
		if ("blockOverlay3d".equals(key)) return c.blockOverlay3d;
		if ("blockOverlayOutline".equals(key)) return c.blockOverlayOutline;
		if ("nametagsShowInF5".equals(key)) return c.nametagsShowInF5;
		if ("nametagsShowInF1".equals(key)) return c.nametagsShowInF1;
		if ("nametagsHideInF1".equals(key)) return c.nametagsHideInF1;
		if ("nametagsTextShadow".equals(key)) return c.nametagsTextShadow;
		if ("nametagsToggleMessages".equals(key)) return c.nametagsToggleMessages;
		if ("nametagsReplaceOwnColor".equals(key)) return c.nametagsReplaceOwnColor;
		if ("nametagsBadlionFont".equals(key)) return c.nametagsBadlionFont;
		if ("nametagsRoundedCorners".equals(key)) return c.nametagsRoundedCorners;
		if ("nametagsBackground".equals(key)) return c.nametagsBackground;
		if ("freezeTime".equals(key)) return c.freezeTime;
		if ("disableHurtCam".equals(key)) return c.disableHurtCam;
		if ("zoomSmooth".equals(key)) return c.zoomSmooth;
		if ("zoomSmoothCamera".equals(key)) return c.zoomSmoothCamera;
		if ("zoomMouseSensitivity".equals(key)) return c.zoomMouseSensitivity;
		if ("zoomScroll".equals(key)) return c.zoomScroll;
		if ("freeLookToggle".equals(key)) return c.freeLookToggle;
		if ("hitboxHideLook".equals(key)) return c.hitboxHideLook;
		if ("itemInfo".equals(key)) return c.itemInfo;
		if ("hotbarShowSlots".equals(key)) return c.hotbarShowSlots;
		if ("hotbarShowHearts".equals(key)) return c.hotbarShowHearts;
		if ("hotbarShowHunger".equals(key)) return c.hotbarShowHunger;
		if ("hotbarShowXp".equals(key)) return c.hotbarShowXp;
		if ("skins3dHead".equals(key)) return c.skins3dHead;
		if ("skins3dBody".equals(key)) return c.skins3dBody;
		if ("skins3dArms".equals(key)) return c.skins3dArms;
		if ("skins3dLegs".equals(key)) return c.skins3dLegs;
		if ("chatBold".equals(key)) return c.chatBold;
		if ("chatAntiSpam".equals(key)) return c.chatAntiSpam;
		if ("chatInfiniteHistory".equals(key)) return c.chatInfiniteHistory;
		if ("scoreboardShowNumbers".equals(key)) return c.scoreboardShowNumbers;
		if ("scoreboardRemoveNumberWidth".equals(key)) return c.scoreboardRemoveNumberWidth;
		if ("scoreboardTextShadow".equals(key)) return c.scoreboardTextShadow;
		if ("animHitbox".equals(key)) return c.animHitbox;
		if ("animInventory".equals(key)) return c.animInventory;
		if ("animHealth".equals(key)) return c.animHealth;
		if ("animDamage".equals(key)) return c.animDamage;
		if ("animSneaking".equals(key)) return c.animSneaking;
		if ("animOldEnchantGlint".equals(key)) return c.animOldEnchantGlint;
		if ("anim18Skins".equals(key)) return c.anim18Skins;
		if ("animTitles".equals(key)) return c.animTitles;
		if ("animFixGhostLiquid".equals(key)) return c.animFixGhostLiquid;
		if ("animItemHold".equals(key)) return c.animItemHold;
		if ("animItemSwitch".equals(key)) return c.animItemSwitch;
		if ("animBlockHit".equals(key)) return c.animBlockHit;
		if ("animEating".equals(key)) return c.animEating;
		if ("animThrowing".equals(key)) return c.animThrowing;
		if ("animFishingRod".equals(key)) return c.animFishingRod;
		if ("animSwordSwing".equals(key)) return c.animSwordSwing;
		if ("animBow".equals(key)) return c.animBow;
		if ("animLeftHand".equals(key)) return c.animLeftHand;
		if ("animMovableTitles".equals(key)) return c.animMovableTitles;
		return false;
	}

	private void setBool(String key, boolean value) {
		OpticConfig c = OpticClient.get().config();
		if (key.startsWith("nick")) {
			if ("nickHideOwnName".equals(key)) c.nickHideOwnName = value;
			else if ("nickHideOthers".equals(key)) c.nickHideOthers = value;
			else if ("nickCustomSuffix".equals(key)) c.nickCustomSuffix = value;
			else if ("nickHideLobbyId".equals(key)) c.nickHideLobbyId = value;
			else if ("nickHideOwnSkin".equals(key)) c.nickHideOwnSkin = value;
			else if ("nickHideOthersSkin".equals(key)) c.nickHideOthersSkin = value;
			c.save();
			return;
		}
		if (OpticRevampOptions.setBool(c, key, value)) {
			c.save();
			return;
		}
		if ("fpsTextShadow".equals(key)) c.fpsTextShadow = value;
		else if ("fpsShowLabel".equals(key)) c.fpsShowLabel = value;
		else if ("fpsBrackets".equals(key)) c.fpsBrackets = value;
		else if ("cpsTextShadow".equals(key)) c.cpsTextShadow = value;
		else if ("cpsShowLabel".equals(key)) c.cpsShowLabel = value;
		else if ("cpsBrackets".equals(key)) c.cpsBrackets = value;
		else if ("cpsSeparateButtons".equals(key)) c.cpsSeparateButtons = value;
		else if ("keystrokesTextShadow".equals(key)) c.keystrokesTextShadow = value;
		else if ("keystrokesLockKeys".equals(key)) c.keystrokesLockKeys = value;
		else if ("keystrokesShowMouse".equals(key)) c.keystrokesShowMouse = value;
		else if ("keystrokesModernStyle".equals(key)) c.keystrokesModernStyle = value;
		else if ("keystrokesBackground".equals(key)) c.keystrokesBackground = value;
		else if ("keystrokesShowLabels".equals(key)) c.keystrokesShowLabels = value;
		else if ("keystrokesInstant".equals(key)) c.keystrokesInstant = value;
		else if ("keystrokesFade".equals(key)) c.keystrokesFade = value;
		else if ("pingTextShadow".equals(key)) c.pingTextShadow = value;
		else if ("pingShowLabel".equals(key)) c.pingShowLabel = value;
		else if ("pingBrackets".equals(key)) c.pingBrackets = value;
		else if ("pingShowMs".equals(key)) c.pingShowMs = value;
		else if ("coordinatesTextShadow".equals(key)) c.coordinatesTextShadow = value;
		else if ("coordinatesShowLabel".equals(key)) c.coordinatesShowLabel = value;
		else if ("coordinatesBrackets".equals(key)) c.coordinatesBrackets = value;
		else if ("coordinatesShowX".equals(key)) c.coordinatesShowX = value;
		else if ("coordinatesShowY".equals(key)) c.coordinatesShowY = value;
		else if ("coordinatesShowZ".equals(key)) c.coordinatesShowZ = value;
		else if ("toggleSprintTextShadow".equals(key)) c.toggleSprintTextShadow = value;
		else if ("toggleSprintShowHud".equals(key)) c.toggleSprintShowHud = value;
		else if ("toggleSprintBrackets".equals(key)) c.toggleSprintBrackets = value;
		else if ("showCriticalHitParticles".equals(key)) c.showCriticalHitParticles = value;
		else if ("alwaysSharpnessParticles".equals(key)) c.alwaysSharpnessParticles = value;
		else if ("removeVanillaColorFilter".equals(key)) c.removeVanillaColorFilter = value;
		else if ("renderOwnParticles".equals(key)) c.renderOwnParticles = value;
		else if ("blockBreakParticles".equals(key)) c.blockBreakParticles = value;
		else if ("blockOverlayFill".equals(key)) c.blockOverlayFill = value;
		else if ("blockOverlay3d".equals(key)) c.blockOverlay3d = value;
		else if ("blockOverlayOutline".equals(key)) c.blockOverlayOutline = value;
		else if ("nametagsShowInF5".equals(key)) c.nametagsShowInF5 = value;
		else if ("nametagsShowInF1".equals(key)) c.nametagsShowInF1 = value;
		else if ("nametagsHideInF1".equals(key)) c.nametagsHideInF1 = value;
		else if ("nametagsTextShadow".equals(key)) c.nametagsTextShadow = value;
		else if ("nametagsToggleMessages".equals(key)) c.nametagsToggleMessages = value;
		else if ("nametagsReplaceOwnColor".equals(key)) c.nametagsReplaceOwnColor = value;
		else if ("nametagsBadlionFont".equals(key)) c.nametagsBadlionFont = value;
		else if ("nametagsRoundedCorners".equals(key)) c.nametagsRoundedCorners = value;
		else if ("nametagsBackground".equals(key)) c.nametagsBackground = value;
		else if ("freezeTime".equals(key)) c.freezeTime = value;
		else if ("disableHurtCam".equals(key)) c.disableHurtCam = value;
		else if ("zoomSmooth".equals(key)) c.zoomSmooth = value;
		else if ("zoomSmoothCamera".equals(key)) c.zoomSmoothCamera = value;
		else if ("zoomMouseSensitivity".equals(key)) c.zoomMouseSensitivity = value;
		else if ("zoomScroll".equals(key)) c.zoomScroll = value;
		else if ("freeLookToggle".equals(key)) c.freeLookToggle = value;
		else if ("hitboxHideLook".equals(key)) c.hitboxHideLook = value;
		else if ("itemInfo".equals(key)) {
			c.itemInfo = value;
			if (client != null && client.options != null) client.options.heldItemTooltips = value;
		}
		else if ("hotbarShowSlots".equals(key)) c.hotbarShowSlots = value;
		else if ("hotbarShowHearts".equals(key)) c.hotbarShowHearts = value;
		else if ("hotbarShowHunger".equals(key)) c.hotbarShowHunger = value;
		else if ("hotbarShowXp".equals(key)) c.hotbarShowXp = value;
		else if ("skins3dHead".equals(key)) c.skins3dHead = value;
		else if ("skins3dBody".equals(key)) c.skins3dBody = value;
		else if ("skins3dArms".equals(key)) c.skins3dArms = value;
		else if ("skins3dLegs".equals(key)) c.skins3dLegs = value;
		else if ("chatBold".equals(key)) c.chatBold = value;
		else if ("chatAntiSpam".equals(key)) c.chatAntiSpam = value;
		else if ("chatInfiniteHistory".equals(key)) c.chatInfiniteHistory = value;
		else if ("scoreboardShowNumbers".equals(key)) c.scoreboardShowNumbers = value;
		else if ("scoreboardRemoveNumberWidth".equals(key)) c.scoreboardRemoveNumberWidth = value;
		else if ("scoreboardTextShadow".equals(key)) c.scoreboardTextShadow = value;
		else if ("animHitbox".equals(key)) c.animHitbox = value;
		else if ("animInventory".equals(key)) c.animInventory = value;
		else if ("animHealth".equals(key)) c.animHealth = value;
		else if ("animDamage".equals(key)) c.animDamage = value;
		else if ("animSneaking".equals(key)) c.animSneaking = value;
		else if ("animOldEnchantGlint".equals(key)) c.animOldEnchantGlint = value;
		else if ("anim18Skins".equals(key)) c.anim18Skins = value;
		else if ("animTitles".equals(key)) c.animTitles = value;
		else if ("animFixGhostLiquid".equals(key)) c.animFixGhostLiquid = value;
		else if ("animItemHold".equals(key)) c.animItemHold = value;
		else if ("animItemSwitch".equals(key)) c.animItemSwitch = value;
		else if ("animBlockHit".equals(key)) c.animBlockHit = value;
		else if ("animEating".equals(key)) c.animEating = value;
		else if ("animThrowing".equals(key)) c.animThrowing = value;
		else if ("animFishingRod".equals(key)) c.animFishingRod = value;
		else if ("animSwordSwing".equals(key)) c.animSwordSwing = value;
		else if ("animBow".equals(key)) c.animBow = value;
		else if ("animLeftHand".equals(key)) c.animLeftHand = value;
		else if ("animMovableTitles".equals(key)) c.animMovableTitles = value;
		else return;
		c.save();
	}

	private int getColor(String key) {
		OpticConfig c = OpticClient.get().config();
		Integer extra = OpticRevampOptions.getColor(c, key);
		if (extra != null) return extra.intValue();
		if ("fpsColor".equals(key)) return c.fpsColor;
		if ("fpsTextColor".equals(key)) return c.fpsTextColor;
		if ("cpsColor".equals(key)) return c.cpsColor;
		if ("cpsTextColor".equals(key)) return c.cpsTextColor;
		if ("keystrokesColor".equals(key)) return c.keystrokesColor;
		if ("keystrokesTextColor".equals(key)) return c.keystrokesTextColor;
		if ("pingColor".equals(key)) return c.pingColor;
		if ("pingTextColor".equals(key)) return c.pingTextColor;
		if ("coordinatesColor".equals(key)) return c.coordinatesColor;
		if ("coordinatesTextColor".equals(key)) return c.coordinatesTextColor;
		if ("toggleSprintColor".equals(key)) return c.toggleSprintColor;
		if ("toggleSprintTextColor".equals(key)) return c.toggleSprintTextColor;
		if ("blockOverlayFillColor".equals(key)) return c.blockOverlayFillColor;
		if ("blockOverlayOutlineColor".equals(key)) return c.blockOverlayOutlineColor;
		if ("nametagsBackgroundColor".equals(key)) return c.nametagsBackgroundColor;
		if ("nametagsPersonalColor".equals(key)) return c.nametagsPersonalColor;
		if ("hitboxPlayerColor".equals(key)) return c.hitboxPlayerColor;
		if ("hitboxMobColor".equals(key)) return c.hitboxMobColor;
		if ("hitboxArrowColor".equals(key)) return c.hitboxArrowColor;
		if ("hitboxOtherColor".equals(key)) return c.hitboxOtherColor;
		if (key != null && key.startsWith("nametagUser:")) {
			return c.getNametagUserColor(key.substring("nametagUser:".length()), OpticConfig.DEFAULT_TEXT);
		}
		return OpticConfig.DEFAULT_ACCENT;
	}

	private void setColor(String key, int color) {
		OpticConfig c = OpticClient.get().config();
		color = color & 0xFFFFFF;
		if (OpticRevampOptions.setColor(c, key, color)) {
			c.save();
			return;
		}
		if ("fpsColor".equals(key)) c.fpsColor = color;
		else if ("fpsTextColor".equals(key)) c.fpsTextColor = color;
		else if ("cpsColor".equals(key)) c.cpsColor = color;
		else if ("cpsTextColor".equals(key)) c.cpsTextColor = color;
		else if ("keystrokesColor".equals(key)) c.keystrokesColor = color;
		else if ("keystrokesTextColor".equals(key)) c.keystrokesTextColor = color;
		else if ("pingColor".equals(key)) c.pingColor = color;
		else if ("pingTextColor".equals(key)) c.pingTextColor = color;
		else if ("coordinatesColor".equals(key)) c.coordinatesColor = color;
		else if ("coordinatesTextColor".equals(key)) c.coordinatesTextColor = color;
		else if ("toggleSprintColor".equals(key)) c.toggleSprintColor = color;
		else if ("toggleSprintTextColor".equals(key)) c.toggleSprintTextColor = color;
		else if ("blockOverlayFillColor".equals(key)) c.blockOverlayFillColor = color;
		else if ("blockOverlayOutlineColor".equals(key)) c.blockOverlayOutlineColor = color;
		else if ("nametagsBackgroundColor".equals(key)) c.nametagsBackgroundColor = color;
		else if ("nametagsPersonalColor".equals(key)) c.nametagsPersonalColor = color;
		else if ("hitboxPlayerColor".equals(key)) c.hitboxPlayerColor = color;
		else if ("hitboxMobColor".equals(key)) c.hitboxMobColor = color;
		else if ("hitboxArrowColor".equals(key)) c.hitboxArrowColor = color;
		else if ("hitboxOtherColor".equals(key)) c.hitboxOtherColor = color;
		else if (key != null && key.startsWith("nametagUser:")) {
			c.setNametagUserColor(key.substring("nametagUser:".length()), color);
		} else return;
		c.save();
	}

	private String getCycleLabel(String key) {
		OpticConfig c = OpticClient.get().config();
		String extra = OpticRevampOptions.cycleLabel(c, key);
		if (extra != null) return extra;
		if ("coordinatesDecimals".equals(key)) {
			return Integer.toString(c.coordinatesDecimals);
		}
		if ("hotbarRotation".equals(key)) {
			return OpticConfig.normalizeHotbarRotation(c.hotbarRotation) + "\u00b0";
		}
		if ("skyType".equals(key)) {
			switch (c.skyType) {
				case 1: return "Nether";
				case 2: return "End";
				default: return "Default";
			}
		}
		if ("timePreset".equals(key)) {
			switch (c.timePreset) {
				case OpticEnvironment.PRESET_DAY: return "Always Day";
				case OpticEnvironment.PRESET_NOON: return "Always Noon";
				case OpticEnvironment.PRESET_SUNSET: return "Always Sunset";
				case OpticEnvironment.PRESET_MIDNIGHT: return "Always Midnight";
				default: return "Custom";
			}
		}
		if ("weatherMode".equals(key)) {
			switch (c.weatherMode) {
				case OpticEnvironment.WEATHER_RAIN: return "Rain";
				case OpticEnvironment.WEATHER_SNOW: return "Snow";
				case OpticEnvironment.WEATHER_STORM: return "Storm";
				default: return "Clear";
			}
		}
		if ("fpsOpticFont".equals(key)) return c.fpsOpticFont ? "Optic" : "Minecraft";
		if ("cpsOpticFont".equals(key)) return c.cpsOpticFont ? "Optic" : "Minecraft";
		if ("keystrokesOpticFont".equals(key)) return c.keystrokesOpticFont ? "Optic" : "Minecraft";
		if ("pingOpticFont".equals(key)) return c.pingOpticFont ? "Optic" : "Minecraft";
		if ("coordinatesOpticFont".equals(key)) return c.coordinatesOpticFont ? "Optic" : "Minecraft";
		if ("toggleSprintOpticFont".equals(key)) return c.toggleSprintOpticFont ? "Optic" : "Minecraft";
		if ("keystrokesCpsMode".equals(key)) {
			switch (c.keystrokesCpsMode) {
				case 1: return "Replace";
				case 2: return "Under";
				case 3: return "While Click";
				default: return "Off";
			}
		}
		return "?";
	}

	private void cycle(String key) {
		OpticConfig c = OpticClient.get().config();
		if (OpticRevampOptions.cycle(c, key)) {
			c.save();
			return;
		}
		if ("coordinatesDecimals".equals(key)) {
			c.coordinatesDecimals = (c.coordinatesDecimals + 1) % 3;
			c.save();
			return;
		}
		if ("keystrokesCpsMode".equals(key)) {
			c.keystrokesCpsMode = (c.keystrokesCpsMode + 1) % 4;
			c.save();
			return;
		}
		if ("hotbarRotation".equals(key)) {
			int r = OpticConfig.normalizeHotbarRotation(c.hotbarRotation);
			c.hotbarRotation = (r + 90) % 360;
			c.save();
			return;
		}
		if ("skyType".equals(key)) {
			c.skyType = (c.skyType + 1) % 3;
			c.save();
			return;
		}
		if ("timePreset".equals(key)) {
			OpticEnvironment.applyTimePreset(c, (c.timePreset + 1) % 5);
			c.save();
			return;
		}
		if ("weatherMode".equals(key)) {
			c.weatherMode = (c.weatherMode + 1) % 4;
			c.save();
			return;
		}
		if ("fpsOpticFont".equals(key)) c.fpsOpticFont = !c.fpsOpticFont;
		else if ("cpsOpticFont".equals(key)) c.cpsOpticFont = !c.cpsOpticFont;
		else if ("keystrokesOpticFont".equals(key)) c.keystrokesOpticFont = !c.keystrokesOpticFont;
		else if ("pingOpticFont".equals(key)) c.pingOpticFont = !c.pingOpticFont;
		else if ("coordinatesOpticFont".equals(key)) c.coordinatesOpticFont = !c.coordinatesOpticFont;
		else if ("toggleSprintOpticFont".equals(key)) c.toggleSprintOpticFont = !c.toggleSprintOpticFont;
		else return;
		c.save();
	}

	private float getFloat(String key) {
		OpticConfig c = OpticClient.get().config();
		Float extra = OpticRevampOptions.getFloat(c, key);
		if (extra != null) return extra.floatValue();
		if ("motionBlurAmount".equals(key)) return c.motionBlurAmount;
		if ("fullbrightGamma".equals(key)) return c.fullbrightGamma;
		if ("skins3dDistance".equals(key)) return c.skins3dDistance;
		if ("particleMultiplier".equals(key)) return c.particleMultiplier;
		if ("keystrokesFadeMs".equals(key)) return c.keystrokesFadeMs;
		if ("blockOverlayFillAlpha".equals(key)) return c.blockOverlayFillAlpha;
		if ("blockOverlayOutlineAlpha".equals(key)) return c.blockOverlayOutlineAlpha;
		if ("blockOverlayLineWidth".equals(key)) return c.blockOverlayLineWidth;
		if ("chatBackgroundOpacity".equals(key)) return c.chatBackgroundOpacity;
		if ("scoreboardBackgroundOpacity".equals(key)) return c.scoreboardBackgroundOpacity;
		if ("hotbarScale".equals(key)) return c.hotbarScale;
		if ("timeOfDay".equals(key)) return c.timeOfDay;
		if ("hurtCamIntensity".equals(key)) return c.hurtCamIntensity;
		if ("fov".equals(key)) return c.fov;
		if ("zoomFactor".equals(key)) return c.zoomFactor;
		if ("zoomScrollSpeed".equals(key)) return c.zoomScrollSpeed;
		if ("freeLookDistance".equals(key)) return c.freeLookDistance;
		if ("titleOffsetX".equals(key)) return c.titleOffsetX;
		if ("titleOffsetY".equals(key)) return c.titleOffsetY;
		if ("nametagsBackgroundOpacity".equals(key)) return c.nametagsBackgroundOpacity;
		return 0.0F;
	}

	private void setFloat(String key, float value) {
		OpticConfig c = OpticClient.get().config();
		if (OpticRevampOptions.setFloat(c, key, value)) {
			c.save();
			return;
		}
		if ("motionBlurAmount".equals(key)) {
			// Whole steps 1-10, like the original.
			c.motionBlurAmount = Math.max(1.0F, Math.min(10.0F, Math.round(value)));
			c.save();
		} else if ("skins3dDistance".equals(key)) {
			c.skins3dDistance = Math.max(4.0F, Math.min(64.0F, value));
			c.save();
		} else if ("fullbrightGamma".equals(key)) {
			c.fullbrightGamma = Math.max(1.0F, Math.min(16.0F, value));
			if (c.fullbright) client.options.gamma = c.fullbrightGamma;
			c.save();
		} else if ("fov".equals(key)) {
			c.fov = Math.max(30.0F, Math.min(150.0F, value));
			c.save();
		} else if ("particleMultiplier".equals(key)) {
			c.particleMultiplier = Math.max(0.0F, Math.min(10.0F, value));
			c.save();
		} else if ("keystrokesFadeMs".equals(key)) {
			c.keystrokesFadeMs = Math.max(0.0F, Math.min(500.0F, value));
			c.save();
		} else if ("blockOverlayFillAlpha".equals(key)) {
			c.blockOverlayFillAlpha = Math.max(0.0F, Math.min(1.0F, value));
			c.save();
		} else if ("blockOverlayOutlineAlpha".equals(key)) {
			c.blockOverlayOutlineAlpha = Math.max(0.0F, Math.min(1.0F, value));
			c.save();
		} else if ("blockOverlayLineWidth".equals(key)) {
			c.blockOverlayLineWidth = Math.max(1.0F, Math.min(8.0F, value));
			c.save();
		} else if ("nametagsBackgroundOpacity".equals(key)) {
			c.nametagsBackgroundOpacity = Math.max(0.0F, Math.min(1.0F, value));
			c.save();
		} else if ("chatBackgroundOpacity".equals(key)) {
			c.chatBackgroundOpacity = Math.max(0.0F, Math.min(100.0F, value));
			c.save();
		} else if ("scoreboardBackgroundOpacity".equals(key)) {
			c.scoreboardBackgroundOpacity = Math.max(0.0F, Math.min(100.0F, value));
			c.save();
		} else if ("hotbarScale".equals(key)) {
			c.hotbarScale = Math.max(0.5F, Math.min(3.0F, value));
			c.save();
		} else if ("timeOfDay".equals(key)) {
			c.timeOfDay = Math.max(0.0F, Math.min(23999.0F, value));
			c.timePreset = OpticEnvironment.PRESET_CUSTOM;
			c.freezeTime = true;
			c.save();
		} else if ("hurtCamIntensity".equals(key)) {
			c.hurtCamIntensity = Math.max(0.0F, Math.min(2.0F, value));
			c.save();
		} else if ("zoomFactor".equals(key)) {
			c.zoomFactor = Math.max(1.25F, Math.min(10.0F, value));
			c.save();
		} else if ("zoomScrollSpeed".equals(key)) {
			c.zoomScrollSpeed = Math.max(0.05F, Math.min(1.0F, value));
			c.save();
		} else if ("freeLookDistance".equals(key)) {
			c.freeLookDistance = Math.max(1.5F, Math.min(12.0F, value));
			c.save();
		} else if ("titleOffsetX".equals(key)) {
			c.titleOffsetX = Math.round(Math.max(-80.0F, Math.min(80.0F, value)));
			c.save();
		} else if ("titleOffsetY".equals(key)) {
			c.titleOffsetY = Math.round(Math.max(-80.0F, Math.min(80.0F, value)));
			c.save();
		}
	}

	private static String versionLabel(String key, boolean value) {
		// Most animation options: true = 1.7. "1.8 Skins" stores true as 1.8.
		if ("anim18Skins".equals(key)) return value ? "1.8" : "1.7";
		return value ? "1.7" : "1.8";
	}

	private void updateSlider(int mouseX) {
		if (activeSlider == null) return;
		float t = (mouseX - activeSlider.x) / (float) activeSlider.w;
		t = Math.max(0.0F, Math.min(1.0F, t));
		float value = activeSlider.min + t * (activeSlider.max - activeSlider.min);
		setFloat(activeSlider.key, value);
	}

	private void updateColorDrag(int mouseX, int mouseY) {
		if (activeColor == null) return;
		if (activeColor.mode == ColorDragMode.SV) {
			pickSat = Math.max(0.0F, Math.min(1.0F, (mouseX - activeColor.centerX) / (float) Math.max(1, activeColor.radius - 1)));
			pickVal = 1.0F - Math.max(0.0F, Math.min(1.0F, (mouseY - activeColor.centerY) / (float) Math.max(1, activeColor.barH - 1)));
			applyPicker();
			return;
		}
		if (activeColor.mode == ColorDragMode.HUE) {
			pickHue = Math.max(0.0F, Math.min(0.999F, (mouseY - activeColor.barY) / (float) Math.max(1, activeColor.barH - 1)));
			applyPicker();
			return;
		}
		if (activeColor.mode == ColorDragMode.WHEEL) {
			float dx = mouseX - activeColor.centerX;
			float dy = mouseY - activeColor.centerY;
			float dist = (float) Math.sqrt(dx * dx + dy * dy);
			float sat = Math.max(0.0F, Math.min(1.0F, dist / Math.max(1, activeColor.radius)));
			float hue = (float) ((Math.atan2(dy, dx) + Math.PI) / (Math.PI * 2.0));
			setColor(activeColor.key, OpticTextures.hsvToRgb(hue, sat, activeColor.value));
		} else {
			float t = (mouseY - activeColor.barY) / (float) Math.max(1, activeColor.barH - 1);
			t = Math.max(0.0F, Math.min(1.0F, t));
			float value = 1.0F - t;
			setColor(activeColor.key, OpticTextures.hsvToRgb(activeColor.hue, activeColor.sat, value));
		}
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int button) {
		if (bindingKey != null) {
			if (button != 0) {
				assignBind(bindingKey, -100 + button);
			} else {
				bindingKey = null;
			}
			return;
		}
		if (clickDropdown(mouseX, mouseY)) return;
		// Clicking anywhere ends typing in a name box (the click still does its own thing).
		finishText();
		if (button != 0) return;
		if (inside(mouseX, mouseY, closeX - 3, closeY - 3, 22, 22)) {
			client.setScreen(null);
			return;
		}
		if (inside(mouseX, mouseY, modsTabX, tabY, modsTabW, 24)) {
			mainTab = MainTab.MODS;
			selected = null;
			openColorKey = null;
			return;
		}
		if (inside(mouseX, mouseY, settingsTabX, tabY, settingsTabW, 24)) {
			mainTab = MainTab.SETTINGS;
			selected = null;
			openColorKey = null;
			return;
		}
		if (inside(mouseX, mouseY, cosmeticsTabX, tabY, cosmeticsTabW, 24)) {
			// Closing Cosmetics comes back to this menu.
			client.setScreen(new OpticCosmeticsScreen(this));
			return;
		}
		if (inside(mouseX, mouseY, profilesTabX, tabY, profilesTabW, 24)) {
			mainTab = MainTab.PROFILES;
			selected = null;
			openColorKey = null;
			try {
				OpticProfiles.ensureSelected(OpticClient.get().config());
			} catch (Exception ignored) {
			}
			profileStatus = "";
			profileFieldOpen = false;
			profileSearchMode = false;
			renamingProfile = null;
			profileScroll = 0;
			return;
		}
		if (mainTab == MainTab.PROFILES) {
			handleProfileClick(mouseX, mouseY, button);
			return;
		}
		if (mainTab == MainTab.SETTINGS) {
			if (settingsSearchField != null && inside(mouseX, mouseY, settingsSearchX, settingsSearchY, settingsSearchW, 16)) {
				settingsSearchField.mouseClicked(mouseX, mouseY, button);
				settingsSearchField.setFocused(true);
				return;
			}
			if (settingsSearchField != null) settingsSearchField.setFocused(false);
			if (inside(mouseX, mouseY, editX, editY, editW, editH)) {
				client.setScreen(new OpticHudEditorScreen(this));
				return;
			}
			if (settingsCategory == SettingsCategory.GENERAL && settingsTrackW > 0
				&& inside(mouseX, mouseY, settingsTrackX, settingsTrackY, settingsTrackW, settingsTrackH)) {
				settingsScrollDrag = true;
				dragSettingsScroll(mouseY);
				return;
			}
			for (SettingHit hit : settingHits) {
				if (!inside(mouseX, mouseY, hit.x, hit.y, hit.w, hit.h)) continue;
				toggleSetting(hit.key);
				return;
			}
			return;
		}
		if (inside(mouseX, mouseY, filterAllX, filterAllY, filterRowW, filterRowH)) {
			modFilter = ModFilter.ALL;
			modsScroll = 0;
			selected = null;
			openColorKey = null;
			configScroll = 0;
			return;
		}
		if (inside(mouseX, mouseY, filterFavX, filterFavY, filterRowW, filterRowH)) {
			modFilter = ModFilter.FAVORITES;
			modsScroll = 0;
			selected = null;
			openColorKey = null;
			configScroll = 0;
			return;
		}
		if (inside(mouseX, mouseY, filterPvpX, filterPvpY, filterRowW, filterRowH)) {
			modFilter = ModFilter.PVP;
			modsScroll = 0;
			selected = null;
			openColorKey = null;
			configScroll = 0;
			return;
		}
		if (inside(mouseX, mouseY, editX, editY, editW, editH)) {
			client.setScreen(new OpticHudEditorScreen(this));
			return;
		}
		if (selected != null && openColorKey == null && configTrackW > 0
			&& inside(mouseX, mouseY, configTrackX, configTrackY, configTrackW, configTrackH)) {
			configScrollDrag = true;
			dragConfigScroll(mouseY);
			return;
		}
		if (selected == null && modsTrackW > 0 && inside(mouseX, mouseY, modsTrackX, modsTrackY, modsTrackW, modsTrackH)) {
			modsScrollDrag = true;
			dragModsScroll(mouseY);
			return;
		}
		if (selected == null) {
			if (modSearchField != null && inside(mouseX, mouseY, modSearchX, modSearchY, modSearchW, 16)) {
				modSearchField.mouseClicked(mouseX, mouseY, button);
				modSearchField.setFocused(true);
				return;
			}
			if (modSearchField != null) modSearchField.setFocused(false);
		}

		if (selected != null) {
			if (inside(mouseX, mouseY, backX, backY, backW, backH)) {
				selected = null;
				openColorKey = null;
				configScroll = 0;
				return;
			}

			boolean clickedPopup = openColorKey != null
				&& inside(mouseX, mouseY, colorPopupX, colorPopupY, colorPopupW, colorPopupH);
			boolean clickedSwatch = false;
			for (OptionHit hit : optionHits) {
				if (hit.kind == HitKind.COLOR_SWATCH && inside(mouseX, mouseY, hit.x, hit.y, hit.w, hit.h)) {
					clickedSwatch = true;
					break;
				}
			}

			if (openColorKey != null && !clickedPopup && !clickedSwatch) {
				openColorKey = null;
				activeColor = null;
				hexEditing = false;
				return;
			}

			// The popup is drawn over the rows below the swatch. Only its own controls take the
			// click, otherwise toggles/sliders underneath (e.g. Outline) would flip too.
			if (clickedPopup) {
				boolean wasEditing = hexEditing;
				hexEditing = false;
				for (OptionHit hit : optionHits) {
					if (!inside(mouseX, mouseY, hit.x, hit.y, hit.w, hit.h)) continue;
					if (hit.kind == HitKind.COLOR_SV) {
						activeColor = ColorDrag.sv(hit.key, hit.x, hit.y, hit.w, hit.h);
						updateColorDrag(mouseX, mouseY);
						return;
					}
					if (hit.kind == HitKind.COLOR_HUE) {
						activeColor = ColorDrag.hue(hit.key, hit.y, hit.h);
						updateColorDrag(mouseX, mouseY);
						return;
					}
					if (hit.kind == HitKind.COLOR_PRESET) {
						setColor(hit.key, hit.centerX);
						return;
					}
					if (hit.kind == HitKind.COLOR_HEX) {
						hexEditing = !wasEditing;
						hexBuffer = "";
						return;
					}
					if (hit.kind == HitKind.COLOR_WHEEL) {
						activeColor = ColorDrag.wheel(hit.key, hit.centerX, hit.centerY, hit.radius, hit.value);
						updateColorDrag(mouseX, mouseY);
						return;
					}
					if (hit.kind == HitKind.COLOR_VALUE) {
						activeColor = ColorDrag.value(hit.key, hit.y, hit.h, hit.hue, hit.sat);
						updateColorDrag(mouseX, mouseY);
						return;
					}
				}
				return;
			}

			for (OptionHit hit : optionHits) {
				if (!inside(mouseX, mouseY, hit.x, hit.y, hit.w, hit.h)) continue;
				if (hit.kind == HitKind.MODULE_TOGGLE) {
					toggle(hit.moduleId);
					return;
				}
				if (hit.kind == HitKind.RESET) {
					OpticClient.get().config().resetModule(hit.moduleId);
					openColorKey = null;
					activeColor = null;
					activeSlider = null;
					return;
				}
				if (hit.kind == HitKind.BOOL) {
					setBool(hit.key, !getBool(hit.key));
					return;
				}
				if (hit.kind == HitKind.KEY) {
					bindingKey = hit.key;
					return;
				}
				if (hit.kind == HitKind.TEXT) {
					editingText = hit.key;
					return;
				}
				if (hit.kind == HitKind.CYCLE) {
					if (isDropdown(hit.key)) {
						openDropdown = hit.key;
						dropdownAnchor = new int[] { hit.x, hit.y, hit.w, hit.h };
					} else {
						cycle(hit.key);
					}
					return;
				}
				if (hit.kind == HitKind.SLIDER) {
					activeSlider = new SliderDrag(hit.key, hit.x, hit.w, hit.min, hit.max);
					updateSlider(mouseX);
					return;
				}
				if (hit.kind == HitKind.COLOR_SWATCH) {
					openColorKey = hit.key.equals(openColorKey) ? null : hit.key;
					activeColor = null;
					return;
				}
				if (hit.kind == HitKind.COLOR_WHEEL) {
					activeColor = ColorDrag.wheel(hit.key, hit.centerX, hit.centerY, hit.radius, hit.value);
					updateColorDrag(mouseX, mouseY);
					return;
				}
				if (hit.kind == HitKind.COLOR_VALUE) {
					activeColor = ColorDrag.value(hit.key, hit.y, hit.h, hit.hue, hit.sat);
					updateColorDrag(mouseX, mouseY);
					return;
				}
			}
			return;
		}

		for (CardBounds bounds : cardBounds) {
			if (inside(mouseX, mouseY, bounds.starX, bounds.starY, bounds.starW, bounds.starH)) {
				OpticClient.get().config().toggleFavoriteModule(bounds.id);
				return;
			}
			if (inside(mouseX, mouseY, bounds.toggleX, bounds.toggleY, bounds.toggleW, bounds.toggleH)) {
				toggle(bounds.id);
				return;
			}
			if (inside(mouseX, mouseY, bounds.x, bounds.y, bounds.w, bounds.h)) {
				selected = findModule(bounds.id);
				openColorKey = null;
				configScroll = 0;
				return;
			}
		}
	}

	private void assignBind(String key, int code) {
		bindingKey = null;
		OpticConfig c = OpticClient.get().config();
		if (!OpticRevampOptions.setKey(c, key, code)) return;
		c.save();
	}

	@Override
	protected void keyPressed(char character, int keyCode) {
		if (textKey(character, keyCode)) return;
		if (openColorKey != null && hexKey(character, keyCode)) return;
		if (bindingKey != null) {
			if (keyCode != Keyboard.KEY_ESCAPE && keyCode != Keyboard.KEY_RSHIFT && keyCode != Keyboard.KEY_NONE) {
				assignBind(bindingKey, keyCode);
			} else {
				bindingKey = null;
			}
			return;
		}
		if (keyCode == Keyboard.KEY_ESCAPE) {
			client.setScreen(null);
			return;
		}
		if (keyCode == Keyboard.KEY_RSHIFT) {
			client.setScreen(null);
			return;
		}
		if (mainTab == MainTab.MODS && selected == null && modSearchField != null) {
			if (modSearchField.isFocused() || isSearchChar(character) || keyCode == Keyboard.KEY_BACK) {
				if (!modSearchField.isFocused()) modSearchField.setFocused(true);
				modSearchField.keyPressed(character, keyCode);
				modsScroll = 0;
				return;
			}
		}
		if (mainTab == MainTab.SETTINGS && settingsSearchField != null) {
			if (settingsSearchField.isFocused() || isSearchChar(character) || keyCode == Keyboard.KEY_BACK) {
				if (!settingsSearchField.isFocused()) settingsSearchField.setFocused(true);
				settingsSearchField.keyPressed(character, keyCode);
				settingsScroll = 0;
				return;
			}
		}
		if (mainTab == MainTab.PROFILES && profileFieldOpen && profileNameField != null) {
			if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
				confirmProfileField();
				return;
			}
			profileNameField.keyPressed(character, keyCode);
			return;
		}
		super.keyPressed(character, keyCode);
	}

	@Override
	public void handleMouse() {
		super.handleMouse();
		int wheel = Mouse.getEventDWheel();
		if (wheel == 0) return;
		vanillaWheel = true;
		scroll(wheel);
	}

	/** True once LWJGL delivered a wheel event this tick (so the raw fallback stays idle). */
	private boolean vanillaWheel;

	private void scroll(int wheel) {
		openDropdown = null;
		dropdownAnchor = null;
		if (mainTab == MainTab.PROFILES) {
			profileScroll += wheel > 0 ? -28 : 28;
			if (profileScroll < 0) profileScroll = 0;
			return;
		}
		if (mainTab == MainTab.SETTINGS) {
			settingsScroll += wheel > 0 ? -28 : 28;
			if (settingsScroll < 0) settingsScroll = 0;
			return;
		}
		if (mainTab == MainTab.MODS && selected == null) {
			modsScroll += wheel > 0 ? -32 : 32;
			modsScroll = Math.max(0, Math.min(modsScroll, modsMaxScroll));
			return;
		}
		if (selected == null) return;
		configScroll += wheel > 0 ? -20 : 20;
		if (configScroll < 0) configScroll = 0;
	}

	private void handleProfileClick(int mouseX, int mouseY, int button) {
		if (profileFieldOpen && profileNameField != null) {
			profileNameField.mouseClicked(mouseX, mouseY, button);
		}
		// Action icons first (on top of select hitboxes).
		for (int i = profileHits.size() - 1; i >= 0; i--) {
			ProfileHit hit = profileHits.get(i);
			if (!inside(mouseX, mouseY, hit.x, hit.y, hit.w, hit.h)) continue;
			if (hit.kind == ProfileHit.Kind.SIDE) {
				if (hit.side == ProfileSide.NEW) {
					profileSearchMode = false;
					renamingProfile = null;
					profileFieldOpen = true;
					String name = OpticProfiles.uniqueNewName();
					profileNameField = new TextFieldWidget(textRenderer, panelX + 160, panelY + 58, 160, 18);
					profileNameField.setMaxLength(32);
					profileNameField.setText(name);
					profileNameField.setFocused(true);
					profileStatus = "";
				} else if (hit.side == ProfileSide.SEARCH) {
					profileSearchMode = true;
					renamingProfile = null;
					profileFieldOpen = true;
					profileNameField = new TextFieldWidget(textRenderer, panelX + 160, panelY + 58, 160, 18);
					profileNameField.setMaxLength(32);
					profileNameField.setText("");
					profileNameField.setFocused(true);
					profileStatus = "";
				} else if (hit.side == ProfileSide.OPEN_FOLDER) {
					OpticProfiles.openFolder();
					profileStatus = "Opened optic/profiles";
				}
				return;
			}
			if (hit.kind == ProfileHit.Kind.CONFIRM) {
				confirmProfileField();
				return;
			}
			if (hit.kind == ProfileHit.Kind.ACTION) {
				handleProfileAction(hit);
				return;
			}
			if (hit.kind == ProfileHit.Kind.SELECT) {
				try {
					OpticProfiles.load(OpticClient.get().config(), hit.file);
					applyLoadedProfile();
					profileStatus = "Loaded " + OpticProfiles.displayName(hit.file);
				} catch (Exception e) {
					profileStatus = "Load failed: " + e.getMessage();
				}
				return;
			}
		}
	}

	private void handleProfileAction(ProfileHit hit) {
		if (hit.action == ProfileHit.Action.RENAME) {
			profileSearchMode = false;
			renamingProfile = hit.file;
			profileFieldOpen = true;
			profileNameField = new TextFieldWidget(textRenderer, panelX + 160, panelY + 58, 160, 18);
			profileNameField.setMaxLength(32);
			profileNameField.setText(OpticProfiles.displayName(hit.file));
			profileNameField.setFocused(true);
			profileStatus = "";
			return;
		}
		if (hit.action == ProfileHit.Action.DUPLICATE) {
			try {
				File copy = OpticProfiles.duplicate(hit.file);
				profileStatus = "Duplicated as " + OpticProfiles.displayName(copy);
			} catch (Exception e) {
				profileStatus = "Duplicate failed: " + e.getMessage();
			}
			return;
		}
		if (hit.action == ProfileHit.Action.DELETE) {
			String name = OpticProfiles.displayName(hit.file);
			if (OpticProfiles.delete(hit.file)) {
				OpticConfig c = OpticClient.get().config();
				if (name.equalsIgnoreCase(c.activeProfile)) {
					c.activeProfile = "";
					try {
						OpticProfiles.ensureSelected(c);
						applyLoadedProfile();
						profileStatus = "Deleted " + name + " · using " + c.activeProfile;
					} catch (Exception e) {
						c.activeProfile = "default";
						c.saveSettingsOnly();
						profileStatus = "Deleted " + name;
					}
				} else {
					profileStatus = "Deleted " + name;
				}
			} else {
				profileStatus = "Could not delete profile";
			}
			return;
		}
		if (hit.action == ProfileHit.Action.EXPORT) {
			OpticProfiles.openFolder();
			profileStatus = "Share " + hit.file.getName() + " from optic/profiles";
		}
	}

	private void confirmProfileField() {
		if (!profileFieldOpen || profileNameField == null) return;
		if (profileSearchMode) {
			profileFieldOpen = false;
			profileSearchMode = false;
			return;
		}
		String name = profileNameField.getText();
		try {
			if (renamingProfile != null) {
				String previous = OpticProfiles.displayName(renamingProfile);
				File renamed = OpticProfiles.rename(renamingProfile, name);
				OpticConfig c = OpticClient.get().config();
				if (previous.equalsIgnoreCase(c.activeProfile)) {
					c.activeProfile = OpticProfiles.displayName(renamed);
					c.saveSettingsOnly();
				}
				profileStatus = "Renamed to " + OpticProfiles.displayName(renamed);
				renamingProfile = null;
			} else {
				File saved = OpticProfiles.save(OpticClient.get().config(), name);
				profileStatus = "Created " + OpticProfiles.displayName(saved);
			}
			profileFieldOpen = false;
		} catch (Exception e) {
			profileStatus = "Failed: " + e.getMessage();
		}
	}

	private static boolean isSearchChar(char character) {
		return character >= 32 && character < 127;
	}

	private void drawSmoothField(TextFieldWidget field, int x, int y, int w, int h, String placeholder) {
		if (field == null || w <= 0 || h <= 0) return;
		fillExact(x, y, w, h, RADIUS_SM, 0xFF101014);
		outlineExact(x, y, w, h, RADIUS_SM, field.isFocused() ? ACCENT : OpticTheme.BORDER);

		float size = 9.0F;
		String text = field.getText() == null ? "" : field.getText();
		boolean placeholderOn = text.isEmpty() && !field.isFocused() && placeholder != null && !placeholder.isEmpty();
		String shown = placeholderOn ? placeholder : text;
		int color = placeholderOn ? MUTED : 0xFFFFFFFF;
		int textH = OpticUiFont.heightSized(client, "Ag", color, size);
		int textY = y + Math.max(1, (h - textH) / 2);
		int cursorIndex = 0;
		int scroll = 0;
		if (!placeholderOn) {
			cursorIndex = field.getCursor();
			if (cursorIndex < 0) cursorIndex = 0;
			if (cursorIndex > text.length()) cursorIndex = text.length();
			int beforeW = OpticUiFont.widthSized(client, text.substring(0, cursorIndex), color, size);
			int limit = Math.max(8, w - 8);
			if (beforeW > limit) scroll = beforeW - limit;
		}

		int sf = Math.max(1, new Window(client, client.width, client.height).getScaleFactor());
		int clipX = x + 3;
		int clipY = y + 2;
		int clipW = Math.max(1, w - 6);
		int clipH = Math.max(1, h - 4);
		GL11.glEnable(GL11.GL_SCISSOR_TEST);
		GL11.glScissor(clipX * sf, client.height - (clipY + clipH) * sf, clipW * sf, clipH * sf);
		if (!shown.isEmpty()) {
			shadowed(shown, x + 4 - scroll, textY, color, size);
		}
		if (field.isFocused() && (System.currentTimeMillis() / 530L) % 2L == 0L) {
			String before = placeholderOn ? "" : text.substring(0, cursorIndex);
			int cx = x + 4 - scroll + OpticUiFont.widthSized(client, before, 0xFFFFFFFF, size);
			DrawableHelper.fill(cx, y + 3, cx + 1, y + h - 3, 0xFFE8E8EE);
		}
		GL11.glDisable(GL11.GL_SCISSOR_TEST);
	}

	private void ensureModSearch(int x, int y, int w) {
		if (modSearchField != null && modSearchX == x && modSearchY == y && modSearchW == w) return;
		String keep = modSearchField != null ? modSearchField.getText() : "";
		boolean focused = modSearchField != null && modSearchField.isFocused();
		modSearchField = new TextFieldWidget(textRenderer, x, y, w, 16);
		modSearchField.setMaxLength(32);
		modSearchField.setText(keep);
		modSearchField.setFocused(focused);
		modSearchX = x;
		modSearchY = y;
		modSearchW = w;
	}

	private void ensureSettingsSearch(int x, int y, int w) {
		if (settingsSearchField != null && settingsSearchX == x && settingsSearchY == y && settingsSearchW == w) return;
		String keep = settingsSearchField != null ? settingsSearchField.getText() : "";
		boolean focused = settingsSearchField != null && settingsSearchField.isFocused();
		settingsSearchField = new TextFieldWidget(textRenderer, x, y, w, 16);
		settingsSearchField.setMaxLength(32);
		settingsSearchField.setText(keep);
		settingsSearchField.setFocused(focused);
		settingsSearchX = x;
		settingsSearchY = y;
		settingsSearchW = w;
	}

	private void ensureProfileField(int x, int y, int w) {
		if (profileNameField != null
			&& profileFieldX == x
			&& profileFieldY == y
			&& profileFieldW == w) {
			return;
		}
		String keep = profileNameField != null ? profileNameField.getText() : "";
		boolean focused = profileNameField == null || profileNameField.isFocused();
		profileNameField = new TextFieldWidget(textRenderer, x, y, w, 18);
		profileNameField.setMaxLength(32);
		profileNameField.setText(keep);
		profileNameField.setFocused(focused);
		profileFieldX = x;
		profileFieldY = y;
		profileFieldW = w;
	}

	@Override
	public boolean shouldPauseGame() {
		return false;
	}

	private static String formatFloat(float value) {
		if (Math.abs(value - Math.round(value)) < 0.05F) return Integer.toString(Math.round(value));
		return String.format("%.1f", value);
	}

	private static boolean inside(int mx, int my, int x, int y, int w, int h) {
		return mx >= x && my >= y && mx < x + w && my < y + h;
	}

	private void outline(int x, int y, int w, int h, int color) {
		outlineRound(x, y, w, h, RADIUS_SM, color);
	}

	private void fillRound(int x, int y, int w, int h, int radius, int argb) {
		OpticUiDraw.fillRound(client, x, y, w, h, radius, argb);
	}

	private void outlineRound(int x, int y, int w, int h, int radius, int argb) {
		OpticUiDraw.outlineRound(client, x, y, w, h, radius, argb);
	}

	private void fillExact(int x, int y, int w, int h, int radius, int argb) {
		OpticUiDraw.fillExact(client, x, y, w, h, radius, argb);
	}

	private void outlineExact(int x, int y, int w, int h, int radius, int argb) {
		OpticUiDraw.outlineExact(client, x, y, w, h, radius, argb);
	}

	private void drawFavoriteStar(int cx, int cy, float size, boolean favorited, boolean hover) {
		if (favorited) {
			OpticUiFont.drawStarCentered(client, cx, cy, size + 2.0F, 0x665CFF8A, true);
			OpticUiFont.drawStarCentered(client, cx, cy, size, STAR_ON, true);
		} else {
			int outline = hover ? ACCENT : 0xCC5CFF8A;
			OpticUiFont.drawStarCentered(client, cx, cy, size + 1.0F, 0x445CFF8A, false);
			OpticUiFont.drawStarCentered(client, cx, cy, size, outline, false);
		}
	}

	private static final class SettingHit {
		final String key;
		final int x;
		final int y;
		final int w;
		final int h;

		SettingHit(String key, int x, int y, int w, int h) {
			this.key = key;
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
		}
	}

	private static final class ProfileHit {
		enum Kind {
			SIDE,
			SELECT,
			ACTION,
			CONFIRM
		}

		enum Action {
			RENAME,
			DUPLICATE,
			DELETE,
			EXPORT
		}

		final Kind kind;
		final ProfileSide side;
		final Action action;
		final File file;
		final int x;
		final int y;
		final int w;
		final int h;

		private ProfileHit(Kind kind, ProfileSide side, Action action, File file, int x, int y, int w, int h) {
			this.kind = kind;
			this.side = side;
			this.action = action;
			this.file = file;
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
		}

		static ProfileHit side(ProfileSide side, int x, int y, int w, int h) {
			return new ProfileHit(Kind.SIDE, side, null, null, x, y, w, h);
		}

		static ProfileHit select(File file, int x, int y, int w, int h) {
			return new ProfileHit(Kind.SELECT, null, null, file, x, y, w, h);
		}

		static ProfileHit action(File file, Action action, int x, int y, int w, int h) {
			return new ProfileHit(Kind.ACTION, null, action, file, x, y, w, h);
		}

		static ProfileHit confirm(int x, int y, int w, int h) {
			return new ProfileHit(Kind.CONFIRM, null, null, null, x, y, w, h);
		}
	}

	private static final class ModuleCard {
		final int id;
		final String name;
		final String description;
		final String icon;

		ModuleCard(int id, String name, String description, String icon) {
			this.id = id;
			this.name = name;
			this.description = description;
			this.icon = icon;
		}
	}

	private static final class CardBounds {
		final int id;
		final int x;
		final int y;
		final int w;
		final int h;
		final int toggleX;
		final int toggleY;
		final int toggleW;
		final int toggleH;
		final int starX;
		final int starY;
		final int starW;
		final int starH;

		CardBounds(
			int id,
			int x,
			int y,
			int w,
			int h,
			int toggleX,
			int toggleY,
			int toggleW,
			int toggleH,
			int starX,
			int starY,
			int starW,
			int starH
		) {
			this.id = id;
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
			this.toggleX = toggleX;
			this.toggleY = toggleY;
			this.toggleW = toggleW;
			this.toggleH = toggleH;
			this.starX = starX;
			this.starY = starY;
			this.starW = starW;
			this.starH = starH;
		}
	}

	private enum OptionType {
		TOGGLE,
		VERSION,
		CYCLE,
		SLIDER,
		COLOR,
		KEY,
		HEADER,
		TEXT
	}

	private enum HitKind {
		MODULE_TOGGLE,
		RESET,
		BOOL,
		KEY,
		CYCLE,
		SLIDER,
		COLOR_SWATCH,
		COLOR_POPUP,
		COLOR_WHEEL,
		COLOR_VALUE,
		COLOR_SV,
		COLOR_HUE,
		COLOR_PRESET,
		COLOR_HEX,
		TEXT
	}

	private enum ColorDragMode {
		WHEEL,
		VALUE,
		SV,
		HUE
	}

	private static final class ConfigOption {
		final OptionType type;
		final String key;
		final String label;
		final float min;
		final float max;

		private ConfigOption(OptionType type, String key, String label, float min, float max) {
			this.type = type;
			this.key = key;
			this.label = label;
			this.min = min;
			this.max = max;
		}

		static ConfigOption toggle(String key, String label) {
			return new ConfigOption(OptionType.TOGGLE, key, label, 0.0F, 0.0F);
		}

		static ConfigOption version(String key, String label) {
			return new ConfigOption(OptionType.VERSION, key, label, 0.0F, 0.0F);
		}

		static ConfigOption cycle(String key, String label) {
			return new ConfigOption(OptionType.CYCLE, key, label, 0.0F, 0.0F);
		}

		static ConfigOption slider(String key, String label, float min, float max) {
			return new ConfigOption(OptionType.SLIDER, key, label, min, max);
		}

		static ConfigOption color(String key, String label) {
			return new ConfigOption(OptionType.COLOR, key, label, 0.0F, 0.0F);
		}

		static ConfigOption key(String key, String label) {
			return new ConfigOption(OptionType.KEY, key, label, 0.0F, 0.0F);
		}

		static ConfigOption header(String label) {
			return new ConfigOption(OptionType.HEADER, "", label, 0.0F, 0.0F);
		}

		static ConfigOption text(String key, String label) {
			return new ConfigOption(OptionType.TEXT, key, label, 0.0F, 0.0F);
		}
	}

	private static final class OptionHit {
		final HitKind kind;
		final int moduleId;
		final String key;
		final int x;
		final int y;
		final int w;
		final int h;
		final float min;
		final float max;
		final int centerX;
		final int centerY;
		final int radius;
		final float hue;
		final float sat;
		final float value;

		private OptionHit(
			HitKind kind,
			int moduleId,
			String key,
			int x,
			int y,
			int w,
			int h,
			float min,
			float max,
			int centerX,
			int centerY,
			int radius,
			float hue,
			float sat,
			float value
		) {
			this.kind = kind;
			this.moduleId = moduleId;
			this.key = key;
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
			this.min = min;
			this.max = max;
			this.centerX = centerX;
			this.centerY = centerY;
			this.radius = radius;
			this.hue = hue;
			this.sat = sat;
			this.value = value;
		}

		static OptionHit toggle(int moduleId, int x, int y, int w, int h) {
			return new OptionHit(HitKind.MODULE_TOGGLE, moduleId, null, x, y, w, h, 0.0F, 0.0F, 0, 0, 0, 0.0F, 0.0F, 0.0F);
		}

		static OptionHit reset(int moduleId, int x, int y, int w, int h) {
			return new OptionHit(HitKind.RESET, moduleId, null, x, y, w, h, 0.0F, 0.0F, 0, 0, 0, 0.0F, 0.0F, 0.0F);
		}

		static OptionHit bool(String key, int x, int y, int w, int h) {
			return new OptionHit(HitKind.BOOL, -1, key, x, y, w, h, 0.0F, 0.0F, 0, 0, 0, 0.0F, 0.0F, 0.0F);
		}

		static OptionHit key(String key, int x, int y, int w, int h) {
			return new OptionHit(HitKind.KEY, -1, key, x, y, w, h, 0.0F, 0.0F, 0, 0, 0, 0.0F, 0.0F, 0.0F);
		}

		static OptionHit text(String key, int x, int y, int w, int h) {
			return new OptionHit(HitKind.TEXT, -1, key, x, y, w, h, 0.0F, 0.0F, 0, 0, 0, 0.0F, 0.0F, 0.0F);
		}

		static OptionHit cycle(String key, int x, int y, int w, int h) {
			return new OptionHit(HitKind.CYCLE, -1, key, x, y, w, h, 0.0F, 0.0F, 0, 0, 0, 0.0F, 0.0F, 0.0F);
		}

		static OptionHit slider(String key, int x, int y, int w, int h, float min, float max) {
			return new OptionHit(HitKind.SLIDER, -1, key, x, y, w, h, min, max, 0, 0, 0, 0.0F, 0.0F, 0.0F);
		}

		static OptionHit colorSwatch(String key, int x, int y, int w, int h) {
			return new OptionHit(HitKind.COLOR_SWATCH, -1, key, x, y, w, h, 0.0F, 0.0F, 0, 0, 0, 0.0F, 0.0F, 0.0F);
		}

		static OptionHit colorPopup(int x, int y, int w, int h) {
			return new OptionHit(HitKind.COLOR_POPUP, -1, null, x, y, w, h, 0.0F, 0.0F, 0, 0, 0, 0.0F, 0.0F, 0.0F);
		}

		static OptionHit colorWheel(String key, int centerX, int centerY, int radius, float value) {
			int size = radius * 2 + 2;
			return new OptionHit(
				HitKind.COLOR_WHEEL,
				-1,
				key,
				centerX - radius,
				centerY - radius,
				size,
				size,
				0.0F,
				0.0F,
				centerX,
				centerY,
				radius,
				0.0F,
				0.0F,
				value
			);
		}

		static OptionHit colorValue(String key, int x, int y, int w, int h, float hue, float sat) {
			return new OptionHit(HitKind.COLOR_VALUE, -1, key, x, y, w, h, 0.0F, 0.0F, 0, 0, 0, hue, sat, 0.0F);
		}

		static OptionHit colorSv(String key, int x, int y, int w, int h) {
			return new OptionHit(HitKind.COLOR_SV, -1, key, x, y, w, h, 0.0F, 0.0F, 0, 0, 0, 0.0F, 0.0F, 0.0F);
		}

		static OptionHit colorHue(String key, int x, int y, int w, int h) {
			return new OptionHit(HitKind.COLOR_HUE, -1, key, x, y, w, h, 0.0F, 0.0F, 0, 0, 0, 0.0F, 0.0F, 0.0F);
		}

		/** centerX carries the preset's RGB. */
		static OptionHit colorPreset(String key, int x, int y, int w, int h, int rgb) {
			return new OptionHit(HitKind.COLOR_PRESET, -1, key, x, y, w, h, 0.0F, 0.0F, rgb, 0, 0, 0.0F, 0.0F, 0.0F);
		}

		static OptionHit colorHex(String key, int x, int y, int w, int h) {
			return new OptionHit(HitKind.COLOR_HEX, -1, key, x, y, w, h, 0.0F, 0.0F, 0, 0, 0, 0.0F, 0.0F, 0.0F);
		}
	}

	private static final class SliderDrag {
		final String key;
		final int x;
		final int w;
		final float min;
		final float max;

		SliderDrag(String key, int x, int w, float min, float max) {
			this.key = key;
			this.x = x;
			this.w = w;
			this.min = min;
			this.max = max;
		}
	}

	private static final class ColorDrag {
		final ColorDragMode mode;
		final String key;
		final int centerX;
		final int centerY;
		final int radius;
		final float value;
		final int barY;
		final int barH;
		final float hue;
		final float sat;

		private ColorDrag(
			ColorDragMode mode,
			String key,
			int centerX,
			int centerY,
			int radius,
			float value,
			int barY,
			int barH,
			float hue,
			float sat
		) {
			this.mode = mode;
			this.key = key;
			this.centerX = centerX;
			this.centerY = centerY;
			this.radius = radius;
			this.value = value;
			this.barY = barY;
			this.barH = barH;
			this.hue = hue;
			this.sat = sat;
		}

		static ColorDrag wheel(String key, int centerX, int centerY, int radius, float value) {
			return new ColorDrag(ColorDragMode.WHEEL, key, centerX, centerY, radius, value, 0, 0, 0.0F, 0.0F);
		}

		static ColorDrag value(String key, int barY, int barH, float hue, float sat) {
			return new ColorDrag(ColorDragMode.VALUE, key, 0, 0, 0, 0.0F, barY, barH, hue, sat);
		}

		/** Square at (x, y) of w x h: centerX/centerY = origin, radius = width, barH = height. */
		static ColorDrag sv(String key, int x, int y, int w, int h) {
			return new ColorDrag(ColorDragMode.SV, key, x, y, w, 0.0F, y, h, 0.0F, 0.0F);
		}

		static ColorDrag hue(String key, int y, int h) {
			return new ColorDrag(ColorDragMode.HUE, key, 0, 0, 0, 0.0F, y, h, 0.0F, 0.0F);
		}
	}
}
