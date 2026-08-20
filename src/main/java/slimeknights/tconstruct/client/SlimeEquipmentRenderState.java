package slimeknights.tconstruct.client;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;

/** Extra extracted state shared by the Tinkers slime renderers. */
final class SlimeEquipmentRenderState extends SlimeRenderState {
  final HumanoidRenderState armorState = new HumanoidRenderState();
  boolean metal;
}
