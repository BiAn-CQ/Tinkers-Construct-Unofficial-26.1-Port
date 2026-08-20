package slimeknights.tconstruct.gadgets.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import slimeknights.tconstruct.TConstruct;

@EventBusSubscriber(modid = TConstruct.MOD_ID)
public class DropperRailBlock extends RailBlock {

  public DropperRailBlock(Properties properties) {
    super(properties);
  }

  public void onMinecartPass(BlockState state, Level world, BlockPos pos, AbstractMinecart cart) {
    ResourceHandler<ItemResource> itemHandlerCart = cart.getCapability(Capabilities.Item.ENTITY_AUTOMATION, Direction.UP);
    if (itemHandlerCart == null || !(cart instanceof Hopper)) {
      return;
    }
    ResourceHandler<ItemResource> itemHandlerTE = world.getCapability(Capabilities.Item.BLOCK, pos.below(), Direction.UP);
    if (itemHandlerTE == null) {
      return;
    }

    for (int i = 0; i < itemHandlerCart.size(); i++) {
      ItemResource resource = itemHandlerCart.getResource(i);
      if (resource.isEmpty() || itemHandlerCart.getAmountAsLong(i) <= 0) {
        continue;
      }
      try (Transaction transaction = Transaction.openRoot()) {
        int inserted = itemHandlerTE.insert(resource, 1, transaction);
        if (inserted > 0 && itemHandlerCart.extract(i, resource, inserted, transaction) == inserted) {
          transaction.commit();
          break;
        }
      }
    }
  }

  /** 26.1 removed the rail callback; invoke the same transfer once per server tick while a hopper minecart is on the rail. */
  @SubscribeEvent
  private static void onMinecartTick(EntityTickEvent.Post event) {
    if (!(event.getEntity() instanceof AbstractMinecart cart) || cart.level().isClientSide() || !(cart instanceof Hopper)) {
      return;
    }
    BlockPos railPos = cart.getCurrentBlockPosOrRailBelow();
    BlockState state = cart.level().getBlockState(railPos);
    if (state.getBlock() instanceof DropperRailBlock rail) {
      rail.onMinecartPass(state, cart.level(), railPos, cart);
    }
  }

}
