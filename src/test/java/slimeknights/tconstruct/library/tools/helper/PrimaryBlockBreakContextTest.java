package slimeknights.tconstruct.library.tools.helper;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PrimaryBlockBreakContextTest {
  @Test
  void onlyPlayerActionOwnsPrimaryHarvest() {
    PrimaryBlockBreakContext context = new PrimaryBlockBreakContext();
    int previous = context.enterPlayerAction();
    context.enterDestroyBlock();

    assertThat(context.isPlayerAction()).isTrue();

    context.exitDestroyBlock();
    context.exitPlayerAction(previous);
    assertThat(context.isPlayerAction()).isFalse();
  }

  @Test
  void directProgrammaticBreakDoesNotOwnPrimaryHarvest() {
    PrimaryBlockBreakContext context = new PrimaryBlockBreakContext();
    context.enterDestroyBlock();

    assertThat(context.isPlayerAction()).isFalse();

    context.exitDestroyBlock();
  }

  @Test
  void nestedBreakDoesNotInheritPlayerAction() {
    PrimaryBlockBreakContext context = new PrimaryBlockBreakContext();
    int previous = context.enterPlayerAction();
    context.enterDestroyBlock();
    assertThat(context.isPlayerAction()).isTrue();

    context.enterDestroyBlock();
    assertThat(context.isPlayerAction()).isFalse();

    context.exitDestroyBlock();
    assertThat(context.isPlayerAction()).isTrue();
    context.exitDestroyBlock();
    context.exitPlayerAction(previous);
  }
}
