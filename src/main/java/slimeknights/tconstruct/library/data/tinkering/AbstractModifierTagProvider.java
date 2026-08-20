package slimeknights.tconstruct.library.data.tinkering;

import net.minecraft.data.PackOutput;
import net.minecraft.server.packs.resources.ResourceManager;
import slimeknights.tconstruct.library.data.AbstractTagProvider;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierManager;

/** Tag provider to generate modifier tags */
public abstract class AbstractModifierTagProvider extends AbstractTagProvider<Modifier> {
  protected AbstractModifierTagProvider(PackOutput packOutput, String modId, ResourceManager resourceManager) {
    // TODO: we don't fire modifier event during datagen, should we?
    super(packOutput, modId, ModifierManager.TAG_FOLDER, modifier -> modifier.getId().location(), id -> true/*ModifierManager.INSTANCE.containsStatic(new ModifierId(id))*/, resourceManager);
  }
}
