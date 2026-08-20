package slimeknights.tconstruct.client;

import net.minecraft.resources.Identifier;
import net.neoforged.fml.ModList;
import slimeknights.tconstruct.TConstruct;

/** Keeps Jade's runtime mod-name mode in sync with its persisted client config. */
final class JadeModNameCacheCompat {
  private static final String JADE_CONFIG = "snownee.jade.api.config.IWailaConfig";
  private static final String JADE_PLUGIN_CONFIG = "snownee.jade.api.config.IPluginConfig";
  private static final String JADE_IDS = "snownee.jade.api.JadeIds";
  private static final String JADE_MOD_IDENTIFICATION = "snownee.jade.util.ModIdentification";

  private JadeModNameCacheCompat() {}

  static void synchronize() {
    if (!ModList.get().isLoaded("jade")) {
      return;
    }
    try {
      Class<?> configType = Class.forName(JADE_CONFIG);
      Object config = configType.getMethod("get").invoke(null);
      Object pluginConfig = configType.getMethod("plugin").invoke(config);
      Identifier translateKey = (Identifier) Class.forName(JADE_IDS)
        .getField("CORE_TRANSLATE_MOD_NAME").get(null);
      boolean translated = (boolean) Class.forName(JADE_PLUGIN_CONFIG)
        .getMethod("get", Identifier.class).invoke(pluginConfig, translateKey);

      Class<?> identification = Class.forName(JADE_MOD_IDENTIFICATION);
      identification.getMethod("setTranslated", boolean.class).invoke(null, translated);
      identification.getMethod("invalidateCache").invoke(null);
    } catch (ReflectiveOperationException | LinkageError exception) {
      TConstruct.LOG.debug("Unable to synchronize Jade's localized mod-name mode", exception);
    }
  }
}
