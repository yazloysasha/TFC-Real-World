package net.yazloysasha.tfcrealworld.client;

/**
 * Shared geography screen / overview-map constants (layout + palette).
 * Keeps magic numbers in one place for the book-sized geography UI.
 */
public final class GeographyUI {

  private GeographyUI() {}

  // --- Book panel (matches Patchouli GuiBook 272×180) ---
  public static final int PANEL_WIDTH = 272;
  public static final int PANEL_HEIGHT = 180;

  /**
   * Content margin: left of map = bottom below map = filter→map gap.
   * Title uses equal pad {@link #TITLE_PAD} above and below.
   * Stack: TITLE_PAD + TITLE_H + TITLE_PAD + CHIP_H + MARGIN + MAP_H + MARGIN
   * = PANEL_HEIGHT.
   */
  public static final int MARGIN = 8;
  public static final int TITLE_PAD = 8;
  public static final int TITLE_H = 8;
  public static final int CHIP_H = 12;
  public static final int CHIP_Y = TITLE_PAD + TITLE_H + TITLE_PAD; // 24
  public static final int MAP_X = MARGIN;
  public static final int MAP_Y = CHIP_Y + CHIP_H + MARGIN; // 44
  public static final int MAP_W = PANEL_WIDTH - 2 * MARGIN; // 256
  /** Exact 2:1 with {@link #MAP_W} so full-profile contain fills with no gutters. */
  public static final int MAP_H = 128;

  public static final int CHIP_GAP = 2;
  public static final int CHIP_W = (MAP_W - 2 * CHIP_GAP) / 3; // 84
  public static final int CHIP_X0 = MAP_X;
  public static final int CHIP_X1 = CHIP_X0 + CHIP_W + CHIP_GAP;
  public static final int CHIP_X2 = CHIP_X1 + CHIP_W + CHIP_GAP;
  public static final int DROPDOWN_W = (MAP_W * 2) / 3;
  public static final int DROPDOWN_TOP = CHIP_Y + CHIP_H;
  public static final int DROPDOWN_ROW_H = 12;
  public static final int DROPDOWN_VISIBLE = 10;
  public static final int SCROLLBAR_W = 3;
  public static final int SCROLLBAR_PAD_RIGHT = 2;
  public static final int SCROLLBAR_PAD_LEFT = SCROLLBAR_PAD_RIGHT / 2;

  public static final int MIN_ZOOM = 1;
  public static final int MAX_ZOOM = 16;

  /** Drawn XP-orb pixel size (and matching hit half-extent = size/2). */
  public static final int ORB_SIZE_NORMAL = 7;
  public static final int ORB_SIZE_CAPITAL = 9;

  // --- Overview map palette ---
  /** Ocean RGB(100, 140, 255) — artist stage-16 ocean. */
  public static final int OCEAN_ARGB = 0xFF648CFF;
  /** Land RGB(0, 130, 0) — artist stage-00 ADD_CONTINENTS. */
  public static final int LAND_ARGB = 0xFF008200;
  /** Empty panel / letterbox behind contain-fit. */
  public static final int PANEL_BG_ARGB = 0xFF181E28;
  public static final int PANEL_OUTLINE_ARGB = 0xFF4A5A6C;

  /** Longest edge for overview GPU texture (~75% of a 1280-wide mask). */
  public static final int TEXTURE_MAX_EDGE = 960;
}
