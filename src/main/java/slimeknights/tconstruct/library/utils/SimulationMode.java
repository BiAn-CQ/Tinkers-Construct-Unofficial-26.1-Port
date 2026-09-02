package slimeknights.tconstruct.library.utils;

/** Controls whether an operation only checks its result or applies its side effects. */
public enum SimulationMode {
  SIMULATE(false),
  EXECUTE(true);

  private final boolean execute;

  SimulationMode(boolean execute) {
    this.execute = execute;
  }

  /** Returns true when the operation should apply its side effects. */
  public boolean execute() {
    return execute;
  }

  /** Returns true when the operation should only check its result. */
  public boolean simulate() {
    return !execute;
  }
}
