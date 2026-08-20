package slimeknights.tconstruct.test;

/**
 * Common base for tests that require Minecraft and NeoForge registries.
 *
 * <p>The NeoForge unit-test runner now owns game bootstrap and mod loading, so
 * retained tests only need the shared registry-aware buffer helpers.</p>
 */
public abstract class BaseMcTest extends CoreTestBootstrap {}
