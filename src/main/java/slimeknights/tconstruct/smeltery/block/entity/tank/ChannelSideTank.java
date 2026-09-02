package slimeknights.tconstruct.smeltery.block.entity.tank;

import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.RootCommitJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import slimeknights.tconstruct.library.fluid.FillOnlyFluidHandler;
import slimeknights.tconstruct.smeltery.block.entity.ChannelBlockEntity;

/** Side-specific channel insertion view used to update flow rendering. */
public class ChannelSideTank extends FillOnlyFluidHandler {
  private final ChannelBlockEntity channel;
  private final Direction side;

  public ChannelSideTank(ChannelBlockEntity channel, ChannelTank tank, Direction side) {
    super(tank);
    assert side.getAxis() != Axis.Y;
    this.channel = channel;
    this.side = side;
  }

  @Override
  public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
    int inserted = super.insert(index, resource, amount, transaction);
    if (inserted > 0) {
      new RootCommitJournal(() -> channel.setFlow(side, true)).updateSnapshots(transaction);
    }
    return inserted;
  }

  @Override
  public int insert(FluidResource resource, int amount, TransactionContext transaction) {
    return insert(0, resource, amount, transaction);
  }
}
