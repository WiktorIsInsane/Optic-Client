package gg.optic.client.gui;

import gg.optic.client.OpticConfig;

/** Settings rows for the revamped HUD mods. Null means the key belongs to an older option. */
public final class OpticRevampOptions {
	private OpticRevampOptions() {
	}

	public static Boolean getBool(OpticConfig c, String key) {
		if ("fpsReverseOrder".equals(key)) return c.fpsReverseOrder;
		if ("cpsReverseText".equals(key)) return c.cpsReverseText;
		if ("cpsIgnoreCancelled".equals(key)) return c.cpsIgnoreCancelled;
		if ("pingReversePrefix".equals(key)) return c.pingReversePrefix;
		if ("pingDynamicColor".equals(key)) return c.pingDynamicColor;
		if ("pingSpike".equals(key)) return c.pingSpike;
		if ("pingBackground".equals(key)) return c.pingBackground;
		if ("pingNametag".equals(key)) return c.pingNametag;
		if ("coordinatesHorizontal".equals(key)) return c.coordinatesHorizontal;
		if ("coordinatesDirection".equals(key)) return c.coordinatesDirection;
		if ("coordinatesDirectionMarks".equals(key)) return c.coordinatesDirectionMarks;
		if ("coordinatesBiome".equals(key)) return c.coordinatesBiome;
		if ("coordinatesBackground".equals(key)) return c.coordinatesBackground;
		if ("coordinatesBorder".equals(key)) return c.coordinatesBorder;
		if ("coordinatesShowWhileTyping".equals(key)) return c.coordinatesShowWhileTyping;
		if ("freeLookInvertYaw".equals(key)) return c.freeLookInvertYaw;
		if ("freeLookInvertPitch".equals(key)) return c.freeLookInvertPitch;
		if ("freeLookSmooth".equals(key)) return c.freeLookSmooth;
		if ("crosshairOutline".equals(key)) return c.crosshairOutline;
		if ("crosshairShowInF5".equals(key)) return c.crosshairShowInF5;
		if ("crosshairSpectator".equals(key)) return c.crosshairSpectator;
		if ("crosshairDynamicAttack".equals(key)) return c.crosshairDynamicAttack;
		if ("crosshairDynamicBow".equals(key)) return c.crosshairDynamicBow;
		if ("crosshairHealthShift".equals(key)) return c.crosshairHealthShift;
		if ("crosshairEnemy".equals(key)) return c.crosshairEnemy;
		if ("crosshairFriendly".equals(key)) return c.crosshairFriendly;
		if ("tabToggle".equals(key)) return c.tabToggle;
		if ("tabDisableHeader".equals(key)) return c.tabDisableHeader;
		if ("tabDisableFooter".equals(key)) return c.tabDisableFooter;
		if ("tabHeads".equals(key)) return c.tabHeads;
		if ("tabHidePing".equals(key)) return c.tabHidePing;
		if ("tabHidePingOver500".equals(key)) return c.tabHidePingOver500;
		if ("tabPingNumbers".equals(key)) return c.tabPingNumbers;
		if ("tabDynamicPing".equals(key)) return c.tabDynamicPing;
		if ("tabHighlightSelf".equals(key)) return c.tabHighlightSelf;
		if ("tabNameShadow".equals(key)) return c.tabNameShadow;
		if ("tabMoveSelfTop".equals(key)) return c.tabMoveSelfTop;
		if ("tabHideNpc".equals(key)) return c.tabHideNpc;
		if ("titlesShowTitle".equals(key)) return c.titlesShowTitle;
		if ("titlesShowSubtitle".equals(key)) return c.titlesShowSubtitle;
		if ("titlesCentered".equals(key)) return c.titlesCentered;
		if ("windowTitle".equals(key)) return c.windowTitle;
		if ("hitColorArmor".equals(key)) return c.hitColorArmor;
		if ("potionsHorizontal".equals(key)) return c.potionsHorizontal;
		if ("potionsShowName".equals(key)) return c.potionsShowName;
		if ("potionsShowDuration".equals(key)) return c.potionsShowDuration;
		if ("potionsShowAmplifier".equals(key)) return c.potionsShowAmplifier;
		if ("potionsSymbols".equals(key)) return c.potionsSymbols;
		if ("keystrokesShowMovement".equals(key)) return c.keystrokesShowMovement;
		if ("keystrokesShowSpace".equals(key)) return c.keystrokesShowSpace;
		if ("keystrokesUseArrows".equals(key)) return c.keystrokesUseArrows;
		if ("keystrokesShowLeftCps".equals(key)) return c.keystrokesShowLeftCps;
		if ("keystrokesShowRightCps".equals(key)) return c.keystrokesShowRightCps;
		if ("keystrokesBorder".equals(key)) return c.keystrokesBorder;
		if ("pvpLowFire".equals(key)) return c.pvpLowFire;
		if ("pvpNoPumpkin".equals(key)) return c.pvpNoPumpkin;
		if ("pvpClearWater".equals(key)) return c.pvpClearWater;
		if ("pvpNoVignette".equals(key)) return c.pvpNoVignette;
		if ("pvpNoPortal".equals(key)) return c.pvpNoPortal;
		if ("keystrokesInnerBorder".equals(key)) return c.keystrokesInnerBorder;
		if ("keystrokesAnimate".equals(key)) return c.keystrokesAnimate;
		if ("keystrokesAnimateColor".equals(key)) return c.keystrokesAnimateColor;
		if ("tabPingRow".equals(key)) return c.tabPingRow;
		if ("tabPingNumberShadow".equals(key)) return c.tabPingNumberShadow;
		if ("potionsBlink".equals(key)) return c.potionsBlink;
		if ("potionsHideVanilla".equals(key)) return c.potionsHideVanilla;
		if ("potionsShowInInventory".equals(key)) return c.potionsShowInInventory;
		if ("potionsBackground".equals(key)) return c.potionsBackground;
		if ("potionsTextShadow".equals(key)) return c.potionsTextShadow;
		return null;
	}

	public static boolean setBool(OpticConfig c, String key, boolean value) {
		if ("fpsReverseOrder".equals(key)) c.fpsReverseOrder = value;
		else if ("cpsReverseText".equals(key)) c.cpsReverseText = value;
		else if ("cpsIgnoreCancelled".equals(key)) c.cpsIgnoreCancelled = value;
		else if ("pingReversePrefix".equals(key)) c.pingReversePrefix = value;
		else if ("pingDynamicColor".equals(key)) c.pingDynamicColor = value;
		else if ("pingSpike".equals(key)) c.pingSpike = value;
		else if ("pingBackground".equals(key)) c.pingBackground = value;
		else if ("pingNametag".equals(key)) c.pingNametag = value;
		else if ("coordinatesHorizontal".equals(key)) c.coordinatesHorizontal = value;
		else if ("coordinatesDirection".equals(key)) c.coordinatesDirection = value;
		else if ("coordinatesDirectionMarks".equals(key)) c.coordinatesDirectionMarks = value;
		else if ("coordinatesBiome".equals(key)) c.coordinatesBiome = value;
		else if ("coordinatesBackground".equals(key)) c.coordinatesBackground = value;
		else if ("coordinatesBorder".equals(key)) c.coordinatesBorder = value;
		else if ("coordinatesShowWhileTyping".equals(key)) c.coordinatesShowWhileTyping = value;
		else if ("freeLookInvertYaw".equals(key)) c.freeLookInvertYaw = value;
		else if ("freeLookInvertPitch".equals(key)) c.freeLookInvertPitch = value;
		else if ("freeLookSmooth".equals(key)) c.freeLookSmooth = value;
		else if ("crosshairOutline".equals(key)) c.crosshairOutline = value;
		else if ("crosshairShowInF5".equals(key)) c.crosshairShowInF5 = value;
		else if ("crosshairSpectator".equals(key)) c.crosshairSpectator = value;
		else if ("crosshairDynamicAttack".equals(key)) c.crosshairDynamicAttack = value;
		else if ("crosshairDynamicBow".equals(key)) c.crosshairDynamicBow = value;
		else if ("crosshairHealthShift".equals(key)) c.crosshairHealthShift = value;
		else if ("crosshairEnemy".equals(key)) c.crosshairEnemy = value;
		else if ("crosshairFriendly".equals(key)) c.crosshairFriendly = value;
		else if ("tabToggle".equals(key)) c.tabToggle = value;
		else if ("tabDisableHeader".equals(key)) c.tabDisableHeader = value;
		else if ("tabDisableFooter".equals(key)) c.tabDisableFooter = value;
		else if ("tabHeads".equals(key)) c.tabHeads = value;
		else if ("tabHidePing".equals(key)) c.tabHidePing = value;
		else if ("tabHidePingOver500".equals(key)) c.tabHidePingOver500 = value;
		else if ("tabPingNumbers".equals(key)) c.tabPingNumbers = value;
		else if ("tabDynamicPing".equals(key)) c.tabDynamicPing = value;
		else if ("tabHighlightSelf".equals(key)) c.tabHighlightSelf = value;
		else if ("tabNameShadow".equals(key)) c.tabNameShadow = value;
		else if ("tabMoveSelfTop".equals(key)) c.tabMoveSelfTop = value;
		else if ("tabHideNpc".equals(key)) c.tabHideNpc = value;
		else if ("titlesShowTitle".equals(key)) c.titlesShowTitle = value;
		else if ("titlesShowSubtitle".equals(key)) c.titlesShowSubtitle = value;
		else if ("titlesCentered".equals(key)) c.titlesCentered = value;
		else if ("windowTitle".equals(key)) {
			c.windowTitle = value;
			if (!value) gg.optic.client.hud.OpticTitles.restoreWindowTitle();
		}
		else if ("hitColorArmor".equals(key)) c.hitColorArmor = value;
		else if ("potionsHorizontal".equals(key)) c.potionsHorizontal = value;
		else if ("potionsShowName".equals(key)) c.potionsShowName = value;
		else if ("potionsShowDuration".equals(key)) c.potionsShowDuration = value;
		else if ("potionsShowAmplifier".equals(key)) c.potionsShowAmplifier = value;
		else if ("potionsSymbols".equals(key)) c.potionsSymbols = value;
		else if ("keystrokesShowMovement".equals(key)) c.keystrokesShowMovement = value;
		else if ("keystrokesShowSpace".equals(key)) c.keystrokesShowSpace = value;
		else if ("keystrokesUseArrows".equals(key)) c.keystrokesUseArrows = value;
		else if ("keystrokesShowLeftCps".equals(key)) c.keystrokesShowLeftCps = value;
		else if ("keystrokesShowRightCps".equals(key)) c.keystrokesShowRightCps = value;
		else if ("keystrokesBorder".equals(key)) c.keystrokesBorder = value;
		else if ("pvpLowFire".equals(key)) c.pvpLowFire = value;
		else if ("pvpNoPumpkin".equals(key)) c.pvpNoPumpkin = value;
		else if ("pvpClearWater".equals(key)) c.pvpClearWater = value;
		else if ("pvpNoVignette".equals(key)) c.pvpNoVignette = value;
		else if ("pvpNoPortal".equals(key)) c.pvpNoPortal = value;
		else if ("keystrokesInnerBorder".equals(key)) c.keystrokesInnerBorder = value;
		else if ("keystrokesAnimate".equals(key)) c.keystrokesAnimate = value;
		else if ("keystrokesAnimateColor".equals(key)) c.keystrokesAnimateColor = value;
		else if ("tabPingRow".equals(key)) c.tabPingRow = value;
		else if ("tabPingNumberShadow".equals(key)) c.tabPingNumberShadow = value;
		else if ("potionsBlink".equals(key)) c.potionsBlink = value;
		else if ("potionsHideVanilla".equals(key)) c.potionsHideVanilla = value;
		else if ("potionsShowInInventory".equals(key)) c.potionsShowInInventory = value;
		else if ("potionsBackground".equals(key)) c.potionsBackground = value;
		else if ("potionsTextShadow".equals(key)) c.potionsTextShadow = value;
		else return false;
		return true;
	}

	public static Integer getColor(OpticConfig c, String key) {
		if ("pingLowColor".equals(key)) return c.pingLowColor;
		if ("pingMediumColor".equals(key)) return c.pingMediumColor;
		if ("pingHighColor".equals(key)) return c.pingHighColor;
		if ("pingExtremeColor".equals(key)) return c.pingExtremeColor;
		if ("pingSpikeColor".equals(key)) return c.pingSpikeColor;
		if ("pingBackgroundColor".equals(key)) return c.pingBackgroundColor;
		if ("coordinatesBackgroundColor".equals(key)) return c.coordinatesBackgroundColor;
		if ("coordinatesBorderColor".equals(key)) return c.coordinatesBorderColor;
		if ("crosshairColor".equals(key)) return c.crosshairColor;
		if ("crosshairOutlineColor".equals(key)) return c.crosshairOutlineColor;
		if ("crosshairHealthColor".equals(key)) return c.crosshairHealthColor;
		if ("crosshairEnemyColor".equals(key)) return c.crosshairEnemyColor;
		if ("crosshairFriendlyColor".equals(key)) return c.crosshairFriendlyColor;
		if ("keystrokesTextPressedColor".equals(key)) return c.keystrokesTextPressedColor;
		if ("keystrokesBackgroundColor".equals(key)) return c.keystrokesBackgroundColor;
		if ("keystrokesBackgroundPressedColor".equals(key)) return c.keystrokesBackgroundPressedColor;
		if ("keystrokesBorderColor".equals(key)) return c.keystrokesBorderColor;
		if ("keystrokesAnimStartColor".equals(key)) return c.keystrokesAnimStartColor;
		if ("keystrokesAnimEndColor".equals(key)) return c.keystrokesAnimEndColor;
		if ("tabRowsColor".equals(key)) return c.tabRowsColor;
		if ("tabPingLowColor".equals(key)) return c.tabPingLowColor;
		if ("tabPingMediumColor".equals(key)) return c.tabPingMediumColor;
		if ("tabPingHighColor".equals(key)) return c.tabPingHighColor;
		if ("tabPingExtremeColor".equals(key)) return c.tabPingExtremeColor;
		if ("tabPingColor".equals(key)) return c.tabPingColor;
		if ("tabSelfColor".equals(key)) return c.tabSelfColor;
		if ("tabBackgroundColor".equals(key)) return c.tabBackgroundColor;
		if ("tabHeaderColor".equals(key)) return c.tabHeaderColor;
		if ("tabFooterColor".equals(key)) return c.tabFooterColor;
		if ("titlesTitleColor".equals(key)) return c.titlesTitleColor;
		if ("titlesSubtitleColor".equals(key)) return c.titlesSubtitleColor;
		if ("hitColorRgb".equals(key)) return c.hitColorRgb;
		if ("potionsBackgroundColor".equals(key)) return c.potionsBackgroundColor;
		if ("potionsTextColor".equals(key)) return c.potionsTextColor;
		return null;
	}

	public static boolean setColor(OpticConfig c, String key, int color) {
		if ("pingLowColor".equals(key)) c.pingLowColor = color;
		else if ("pingMediumColor".equals(key)) c.pingMediumColor = color;
		else if ("pingHighColor".equals(key)) c.pingHighColor = color;
		else if ("pingExtremeColor".equals(key)) c.pingExtremeColor = color;
		else if ("pingSpikeColor".equals(key)) c.pingSpikeColor = color;
		else if ("pingBackgroundColor".equals(key)) c.pingBackgroundColor = color;
		else if ("coordinatesBackgroundColor".equals(key)) c.coordinatesBackgroundColor = color;
		else if ("coordinatesBorderColor".equals(key)) c.coordinatesBorderColor = color;
		else if ("crosshairColor".equals(key)) c.crosshairColor = color;
		else if ("crosshairOutlineColor".equals(key)) c.crosshairOutlineColor = color;
		else if ("crosshairHealthColor".equals(key)) c.crosshairHealthColor = color;
		else if ("crosshairEnemyColor".equals(key)) c.crosshairEnemyColor = color;
		else if ("crosshairFriendlyColor".equals(key)) c.crosshairFriendlyColor = color;
		else if ("keystrokesTextPressedColor".equals(key)) c.keystrokesTextPressedColor = color;
		else if ("keystrokesBackgroundColor".equals(key)) c.keystrokesBackgroundColor = color;
		else if ("keystrokesBackgroundPressedColor".equals(key)) c.keystrokesBackgroundPressedColor = color;
		else if ("keystrokesBorderColor".equals(key)) c.keystrokesBorderColor = color;
		else if ("keystrokesAnimStartColor".equals(key)) c.keystrokesAnimStartColor = color;
		else if ("keystrokesAnimEndColor".equals(key)) c.keystrokesAnimEndColor = color;
		else if ("tabRowsColor".equals(key)) c.tabRowsColor = color;
		else if ("tabPingLowColor".equals(key)) c.tabPingLowColor = color;
		else if ("tabPingMediumColor".equals(key)) c.tabPingMediumColor = color;
		else if ("tabPingHighColor".equals(key)) c.tabPingHighColor = color;
		else if ("tabPingExtremeColor".equals(key)) c.tabPingExtremeColor = color;
		else if ("tabPingColor".equals(key)) c.tabPingColor = color;
		else if ("tabSelfColor".equals(key)) c.tabSelfColor = color;
		else if ("tabBackgroundColor".equals(key)) c.tabBackgroundColor = color;
		else if ("tabHeaderColor".equals(key)) c.tabHeaderColor = color;
		else if ("tabFooterColor".equals(key)) c.tabFooterColor = color;
		else if ("titlesTitleColor".equals(key)) c.titlesTitleColor = color;
		else if ("titlesSubtitleColor".equals(key)) c.titlesSubtitleColor = color;
		else if ("hitColorRgb".equals(key)) c.hitColorRgb = color;
		else if ("potionsBackgroundColor".equals(key)) c.potionsBackgroundColor = color;
		else if ("potionsTextColor".equals(key)) c.potionsTextColor = color;
		else return false;
		return true;
	}

	public static String cycleLabel(OpticConfig c, String key) {
		if ("freeLookPerspective".equals(key)) return c.freeLookPerspective == 2 ? "Front" : "Behind";
		if ("crosshairShape".equals(key)) {
			switch (c.crosshairShape) {
				case 1: return "Dot";
				case 2: return "Circle";
				case 3: return "X";
				default: return "Cross";
			}
		}
		if ("inventoryScale".equals(key)) return c.inventoryScale <= 0 ? "Auto" : Integer.toString(c.inventoryScale);
		if ("menuScale".equals(key)) return c.menuScale <= 0 ? "Auto" : Integer.toString(c.menuScale);
		if ("keystrokesAnimation".equals(key)) return c.keystrokesAnimation == 1 ? "Grow" : "Ripple";
		if ("keystrokesEasing".equals(key)) {
			switch (c.keystrokesEasing) {
				case 1: return "Ease Out";
				case 2: return "Ease In-Out";
				default: return "Linear";
			}
		}
		if ("windowTitleMode".equals(key)) {
			switch (c.windowTitleMode) {
				case 1: return "Optic + FPS";
				case 2: return "Optic + FPS + Ping";
				default: return "Optic";
			}
		}
		return null;
	}

	public static boolean cycle(OpticConfig c, String key) {
		if ("freeLookPerspective".equals(key)) c.freeLookPerspective = c.freeLookPerspective == 2 ? 1 : 2;
		else if ("crosshairShape".equals(key)) c.crosshairShape = (c.crosshairShape + 1) % 4;
		else if ("inventoryScale".equals(key)) c.inventoryScale = (c.inventoryScale + 1) % 5;
		else if ("menuScale".equals(key)) {
			c.menuScale = (c.menuScale + 1) % 5;
			gg.optic.client.hud.OpticGuiScale.beforeScreen(net.minecraft.client.MinecraftClient.getInstance(), net.minecraft.client.MinecraftClient.getInstance().currentScreen);
		}
		else if ("windowTitleMode".equals(key)) c.windowTitleMode = (c.windowTitleMode + 1) % 3;
		else if ("keystrokesAnimation".equals(key)) c.keystrokesAnimation = (c.keystrokesAnimation + 1) % 2;
		else if ("keystrokesEasing".equals(key)) c.keystrokesEasing = (c.keystrokesEasing + 1) % 3;
		else return false;
		return true;
	}

	public static Float getFloat(OpticConfig c, String key) {
		if ("crosshairGap".equals(key)) return c.crosshairGap;
		if ("crosshairLength".equals(key)) return c.crosshairLength;
		if ("crosshairThickness".equals(key)) return c.crosshairThickness;
		if ("crosshairAttackSpread".equals(key)) return c.crosshairAttackSpread;
		if ("crosshairBowSpread".equals(key)) return c.crosshairBowSpread;
		if ("crosshairHealthThreshold".equals(key)) return c.crosshairHealthThreshold;
		if ("guiHotbarScale".equals(key)) return c.guiHotbarScale;
		if ("crosshairSize".equals(key)) return c.crosshairSize;
		if ("keystrokesBoxSize".equals(key)) return c.keystrokesBoxSize;
		if ("keystrokesBorderThickness".equals(key)) return c.keystrokesBorderThickness;
		if ("keystrokesSpaceThickness".equals(key)) return c.keystrokesSpaceThickness;
		if ("keystrokesAnimDuration".equals(key)) return c.keystrokesAnimDuration;
		if ("titlesScale".equals(key)) return c.titlesScale;
		if ("hitColorAlpha".equals(key)) return c.hitColorAlpha;
		if ("potionsScale".equals(key)) return c.potionsScale;
		if ("pingSpikeThreshold".equals(key)) return (float) c.pingSpikeThreshold;
		if ("potionsBlinkSeconds".equals(key)) return (float) c.potionsBlinkSeconds;
		return null;
	}

	public static boolean setFloat(OpticConfig c, String key, float value) {
		if ("crosshairGap".equals(key)) c.crosshairGap = clamp(value, 0.0F, 12.0F);
		else if ("crosshairLength".equals(key)) c.crosshairLength = clamp(value, 1.0F, 16.0F);
		else if ("crosshairThickness".equals(key)) c.crosshairThickness = clamp(value, 1.0F, 6.0F);
		else if ("crosshairAttackSpread".equals(key)) c.crosshairAttackSpread = clamp(value, 0.0F, 12.0F);
		else if ("crosshairBowSpread".equals(key)) c.crosshairBowSpread = clamp(value, 0.0F, 16.0F);
		else if ("crosshairHealthThreshold".equals(key)) c.crosshairHealthThreshold = clamp(value, 1.0F, 20.0F);
		else if ("guiHotbarScale".equals(key)) c.guiHotbarScale = clamp(value, 0.5F, 2.0F);
		else if ("crosshairSize".equals(key)) c.crosshairSize = clamp(value, 0.4F, 3.0F);
		else if ("keystrokesBoxSize".equals(key)) c.keystrokesBoxSize = clamp(value, 10.0F, 32.0F);
		else if ("keystrokesBorderThickness".equals(key)) c.keystrokesBorderThickness = clamp(value, 0.5F, 3.0F);
		else if ("keystrokesSpaceThickness".equals(key)) c.keystrokesSpaceThickness = clamp(value, 1.0F, 4.25F);
		else if ("keystrokesAnimDuration".equals(key)) c.keystrokesAnimDuration = clamp(value, 0.1F, 1.0F);
		else if ("titlesScale".equals(key)) c.titlesScale = clamp(value, 0.5F, 3.0F);
		else if ("hitColorAlpha".equals(key)) c.hitColorAlpha = clamp(value, 0.05F, 1.0F);
		else if ("potionsScale".equals(key)) c.potionsScale = clamp(value, 0.5F, 3.0F);
		else if ("pingSpikeThreshold".equals(key)) c.pingSpikeThreshold = Math.round(clamp(value, 1.0F, 200.0F));
		else if ("potionsBlinkSeconds".equals(key)) c.potionsBlinkSeconds = Math.round(clamp(value, 2.0F, 20.0F));
		else return false;
		return true;
	}

	public static Integer getKey(OpticConfig c, String key) {
		if ("coordinatesCopyKey".equals(key)) return c.coordinatesCopyKey;
		if ("tabKeyCode".equals(key)) return c.tabKeyCode;
		if ("freeLookKeyCode".equals(key)) return c.freeLookKeyCode;
		if ("nametagsToggleAllKey".equals(key)) return c.nametagsToggleAllKey;
		if ("nametagsTogglePlayersKey".equals(key)) return c.nametagsTogglePlayersKey;
		return null;
	}

	public static boolean setKey(OpticConfig c, String key, int code) {
		if ("coordinatesCopyKey".equals(key)) c.coordinatesCopyKey = code;
		else if ("tabKeyCode".equals(key)) c.tabKeyCode = code;
		else if ("freeLookKeyCode".equals(key)) c.freeLookKeyCode = code;
		else if ("nametagsToggleAllKey".equals(key)) c.nametagsToggleAllKey = code;
		else if ("nametagsTogglePlayersKey".equals(key)) c.nametagsTogglePlayersKey = code;
		else return false;
		return true;
	}

	private static float clamp(float value, float min, float max) {
		return Math.max(min, Math.min(max, value));
	}
}
