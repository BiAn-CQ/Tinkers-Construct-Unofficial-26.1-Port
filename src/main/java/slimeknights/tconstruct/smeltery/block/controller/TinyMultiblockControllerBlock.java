package slimeknights.tconstruct.smeltery.block.controller;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import slimeknights.mantle.fluid.FluidTransferHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.smeltery.block.entity.ITankBlockEntity;

public abstract class TinyMultiblockControllerBlock extends ControllerBlock {
  private static final Component NO_FUEL_TANK = TConstruct.makeTranslation("multiblock", "tiny.no_fuel_tank");

  protected TinyMultiblockControllerBlock(Properties builder) {
    super(builder);
  }


  /*
   * Fuel tank
   */

  /**
   * Checks if the given state is a valid melter fuel source
   * @param state  State instance
   * @return  True if its a valid fuel source
   */
  protected boolean isValidFuelSource(BlockState state) {
    return state.is(TinkerTags.Blocks.FUEL_TANKS);
  }

  @Override
  public BlockState getStateForPlacement(BlockPlaceContext context) {
    return super.getStateForPlacement(context)
      .setValue(IN_STRUCTURE, isValidFuelSource(context.getLevel().getBlockState(context.getClickedPos().below())));
  }

  @Deprecated
  @Override
  public BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess scheduledTickAccess, BlockPos pos,
                                Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
    if (direction == Direction.DOWN) {
      boolean hasFuel = isValidFuelSource(neighbor);
      state = state.setValue(IN_STRUCTURE, hasFuel);
      if (!hasFuel) {
        state = state.setValue(ACTIVE, false);
      }
    }
    return state;
  }

  @Deprecated
  @Override
  protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    if (FluidTransferHelper.interactWithTank(world, pos, player, hand, hit)) {
      return InteractionResult.SUCCESS;
    }
    return super.useItemOn(stack, state, world, pos, player, hand, hit);
  }

  @Override
  protected boolean displayStatus(Player player, Level world, BlockPos pos, BlockState state) {
    if (!world.isClientSide() && !state.getValue(IN_STRUCTURE)) {
      player.sendOverlayMessage(NO_FUEL_TANK);
    }
    return true;
  }


  /*
   * Comparator
   */

  @Deprecated
  public boolean hasAnalogOutputSignal(BlockState state) {
    return true;
  }

  @Deprecated
  public int getAnalogOutputSignal(BlockState blockState, Level worldIn, BlockPos pos) {
    return ITankBlockEntity.getComparatorInputOverride(worldIn, pos);
  }

}
