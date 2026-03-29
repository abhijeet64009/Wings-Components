package com.satya.wingslibrary

// ─────────────────────────────────────────────────────────────
//  ExpandableFabDefs.kt  (v3)
//
//  Shared enums and interface used by both ExpandableFab
//  and ExpandableFabItem. Neither class owns these types.
//
//  Changes vs v2:
//   • EfabLayoutMode: RADIAL added (fan arc beside main FAB)
//   • EfabDirection:  LEFT and RIGHT restored; they are ONLY
//     meaningful in RADIAL mode. In LINEAR mode the direction
//     is fixed UP/DOWN. In CIRCULAR mode direction is unused.
//   • ScreenPosition: unchanged — used for circular arc math
// ─────────────────────────────────────────────────────────────


// ══════════════════════════════════════════════════════════════
//  Layout mode
// ══════════════════════════════════════════════════════════════

/**
 * Controls how child items are arranged around the main FAB.
 *
 *  LINEAR   — items stack vertically above or below the main FAB.
 *             expandDirection is forced to UP; horizontal label
 *             side is auto-detected from screen position.
 *
 *  CIRCULAR — items orbit the main FAB at a fixed radius.
 *             Arc start/sweep angles are auto-detected from the
 *             component's screen position (corner → quarter arc,
 *             edge → half arc, center → full 360° ring).
 *             expandDirection is ignored in this mode.
 *
 *  RADIAL   — items fan out horizontally beside the main FAB,
 *             like a hand of cards spreading from a corner.
 *             expandDirection must be LEFT or RIGHT:
 *               LEFT  → main FAB anchors to the RIGHT, children
 *                        spread to the left in a curved arc.
 *               RIGHT → main FAB anchors to the LEFT, children
 *                        spread to the right in a curved arc.
 *             If more children are added than fit in one arc,
 *             the overflow area becomes horizontally scrollable.
 */
enum class EfabLayoutMode {
    LINEAR,
    CIRCULAR,
    RADIAL
}


// ══════════════════════════════════════════════════════════════
//  Expand direction
// ══════════════════════════════════════════════════════════════

/**
 * Direction in which child items expand from the main FAB.
 *
 *  UP    — linear mode: items appear above the main FAB.
 *  DOWN  — linear mode: items appear below the main FAB.
 *          (UP is the default and DOWN is also valid for LINEAR.)
 *
 *  LEFT  — radial mode only: main FAB sits at the RIGHT edge,
 *           children fan out to the left in a curved arc.
 *           Ignored in LINEAR (forced UP) and CIRCULAR (unused).
 *
 *  RIGHT — radial mode only: main FAB sits at the LEFT edge,
 *           children fan out to the right in a curved arc.
 *           Ignored in LINEAR (forced UP) and CIRCULAR (unused).
 *
 * In RADIAL mode the default is LEFT (main FAB on the right,
 * children spread left — matching bottom-end gravity usage).
 */
enum class EfabDirection {
    UP,
    DOWN,
    LEFT,
    RIGHT
}


// ══════════════════════════════════════════════════════════════
//  Animation type
// ══════════════════════════════════════════════════════════════

/** Animation style applied to child items on expand / collapse. */
enum class EfabAnimType {
    SLIDE,    // items slide in from the expand direction
    SCALE,    // items scale up from zero with overshoot
    FADE,     // pure opacity transition
    FAN,      // rotate + scale from an offset angle
    FLIP,     // Y-axis 3D card flip
    SPIRAL,   // cascading Z-rotation + scale
    BOUNCE    // scale with BounceInterpolator
}


// ══════════════════════════════════════════════════════════════
//  Animation speed
// ══════════════════════════════════════════════════════════════

/**
 * Speed preset applied to all child animation durations.
 * The [multiplier] divides base durations so higher = faster:
 *   SLOW   0.70×  →  appear ≈ 400 ms, disappear ≈ 315 ms
 *   NORMAL 1.00×  →  appear ≈ 280 ms, disappear ≈ 220 ms
 *   FAST   1.40×  →  appear ≈ 200 ms, disappear ≈ 157 ms
 */
enum class EfabSpeed(val multiplier: Float) {
    SLOW(0.70f),
    NORMAL(1.00f),
    FAST(1.40f)
}


// ══════════════════════════════════════════════════════════════
//  Label position
// ══════════════════════════════════════════════════════════════

/**
 * Where the text-label chip is drawn relative to the mini-FAB.
 *
 *  AUTO   — resolved by the parent from the current layout mode
 *           and the component's screen position:
 *             LINEAR + right half of screen → LEFT of mini-FAB
 *             LINEAR + left  half of screen → RIGHT of mini-FAB
 *             CIRCULAR / RADIAL             → HIDDEN
 *  LEFT   — chip drawn to the left  of the mini-FAB  [chip]─[●]
 *  RIGHT  — chip drawn to the right of the mini-FAB  [●]─[chip]
 *  TOP    — chip drawn above the mini-FAB
 *  BOTTOM — chip drawn below the mini-FAB
 *  HIDDEN — no chip shown at all
 */
enum class EfabLabelPos {
    AUTO,
    LEFT,
    RIGHT,
    TOP,
    BOTTOM,
    HIDDEN
}


// ══════════════════════════════════════════════════════════════
//  Main and child FAB size
// ══════════════════════════════════════════════════════════════

enum class EfabSize {
    MINI,     // 40 dp
    NORMAL,   // 56 dp  (default)
    LARGE     // 96 dp
}


// ══════════════════════════════════════════════════════════════
//  Screen position  (internal — used for circular arc math)
// ══════════════════════════════════════════════════════════════

/**
 * Detected position of the ExpandableFab on the physical screen.
 * Computed after the first layout using getLocationInWindow().
 *
 * Naming: END = right side, START = left side (gravity convention).
 *
 *  Corner positions → quarter-arc facing the open screen space
 *  Edge positions   → half-arc facing inward
 *  Center           → full 360° ring
 *  UNKNOWN          → safe default (360°), before first layout
 */
internal enum class ScreenPosition {
    BOTTOM_END,
    BOTTOM_START,
    TOP_END,
    TOP_START,
    BOTTOM_CENTER,
    TOP_CENTER,
    CENTER_START,
    CENTER_END,
    CENTER,
    UNKNOWN
}


// ══════════════════════════════════════════════════════════════
//  State-change listener
// ══════════════════════════════════════════════════════════════

/**
 * Callback fired when the ExpandableFab expands or collapses.
 * Register via:  expandableFab.stateChangeListener = object : … { }
 */
interface OnExpandableFabStateChangeListener {
    fun onExpanded()
    fun onCollapsed()
}