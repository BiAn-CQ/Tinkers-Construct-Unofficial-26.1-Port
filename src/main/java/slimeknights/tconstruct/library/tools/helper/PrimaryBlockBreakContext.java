package slimeknights.tconstruct.library.tools.helper;

/**
 * Separates the player's primary mining action from direct or nested programmatic block breaks.
 * Each server player owns one instance through its game mode mixin.
 */
public final class PrimaryBlockBreakContext {
  private int destroyDepth;
  private int playerActionDepth;

  /** Marks the next destroyBlock invocation as originating from Minecraft's player-action pipeline. */
  public int enterPlayerAction() {
    int previous = playerActionDepth;
    playerActionDepth = destroyDepth + 1;
    return previous;
  }

  /** Restores the previous player-action scope. */
  public void exitPlayerAction(int previous) {
    playerActionDepth = previous;
  }

  /** Enters any destroyBlock invocation, including calls made by other mods. */
  public void enterDestroyBlock() {
    destroyDepth++;
  }

  /** Leaves the current destroyBlock invocation. */
  public void exitDestroyBlock() {
    destroyDepth--;
  }

  /** True only for the exact destroyBlock invocation started by the player's mining action. */
  public boolean isPlayerAction() {
    return destroyDepth > 0 && destroyDepth == playerActionDepth;
  }
}
