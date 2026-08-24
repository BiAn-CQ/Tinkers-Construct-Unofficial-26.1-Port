package slimeknights.tconstruct.client;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/** Shared contract for native Tinkers armor extensions that support emissive material layers. */
interface TinkerArmorClientExtension extends IClientItemExtensions {
  int getArmorLuminosity(ItemStack stack, String layerPath);
}
