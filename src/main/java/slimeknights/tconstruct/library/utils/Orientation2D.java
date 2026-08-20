package slimeknights.tconstruct.library.utils;

import lombok.Getter;

import static slimeknights.tconstruct.library.utils.Orientation2D.Orientation1D.END;
import static slimeknights.tconstruct.library.utils.Orientation2D.Orientation1D.MIDDLE;
import static slimeknights.tconstruct.library.utils.Orientation2D.Orientation1D.START;

/** Enum representation one of the 8 cardinal directions */
@Getter
public enum Orientation2D {
  TOP_LEFT    (START, START),
  TOP         (MIDDLE, START),
  TOP_RIGHT   (END, START),
  LEFT        (START, MIDDLE),
  RIGHT       (END, MIDDLE),
  BOTTOM_LEFT (START, END),
  BOTTOM      (MIDDLE, END),
  BOTTOM_RIGHT(END, END);

  private final Orientation1D x;
  private final Orientation1D y;

  Orientation2D(Orientation1D x, Orientation1D y) {
    this.x = x;
    this.y = y;
  }

  public Orientation1D getX() {
    return x;
  }

  public Orientation1D getY() {
    return y;
  }

  public enum Orientation1D {
    START, MIDDLE, END;

    /** Scales the value along the orientation */
    public int align(int max) {
      return switch (this) {
        default -> 0;
        case MIDDLE -> max / 2;
        case END -> max;
      };
    }
  }
}
