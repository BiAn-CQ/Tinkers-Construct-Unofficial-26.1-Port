package slimeknights.tconstruct.plugin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Plugin to help us work with the test dummy mod */
public class DummmmmmyPlugin {
  @SubscribeEvent
  public void commonSetup(FMLCommonSetupEvent event) {
    event.enqueueWork(() -> {
      try {
        // The optional mod keeps this method private in 26.1.2; resolve it only when loaded.
        Class<?> targetClass = Class.forName("net.mehvahdjukaar.dummmmmmy.common.TargetDummyEntity");
        Method disableShield = targetClass.getDeclaredMethod("disableShield");
        disableShield.setAccessible(true);
        ModifierUtil.registerShieldDisabler(entity -> {
          if (targetClass.isInstance(entity) && entity instanceof LivingEntity target && target.isBlocking()) {
            try {
              disableShield.invoke(target);
            } catch (IllegalAccessException | InvocationTargetException e) {
              TConstruct.LOG.error("Failed to disable target dummy shield.", e);
            }
          }
        }, BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.fromNamespaceAndPath("dummmmmmy", "target_dummy"))
          .orElseThrow(() -> new IllegalStateException("Missing registered target dummy entity type")));
      } catch (ReflectiveOperationException | RuntimeException e) {
        TConstruct.LOG.error("Failed to locate TargetDummyEntity::disableShield, unable to disable shields.", e);
      }
    });
  }
}
