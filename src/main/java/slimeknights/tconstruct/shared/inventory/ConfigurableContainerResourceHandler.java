package slimeknights.tconstruct.shared.inventory;

import net.minecraft.world.Container;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.IndexModifier;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** Native item resource view with independently configurable insertion and extraction. */
public final class ConfigurableContainerResourceHandler implements ResourceHandler<ItemResource>, IndexModifier<ItemResource> {
  private final Container container;
  private final ResourceHandler<ItemResource> delegate;
  private final boolean canInsert;
  private final boolean canExtract;

  public ConfigurableContainerResourceHandler(Container container, boolean canInsert, boolean canExtract) {
    this.container = container;
    this.delegate = VanillaContainerWrapper.of(container);
    this.canInsert = canInsert;
    this.canExtract = canExtract;
  }

  @Override
  public int size() {
    return delegate.size();
  }

  @Override
  public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
    return canInsert ? delegate.insert(index, resource, amount, transaction) : 0;
  }

  @Override
  public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
    return canExtract ? delegate.extract(index, resource, amount, transaction) : 0;
  }

  @Override
  public ItemResource getResource(int index) {
    return delegate.getResource(index);
  }

  @Override
  public long getAmountAsLong(int index) {
    return delegate.getAmountAsLong(index);
  }

  @Override
  public long getCapacityAsLong(int index, ItemResource resource) {
    return delegate.getCapacityAsLong(index, resource);
  }

  @Override
  public boolean isValid(int index, ItemResource resource) {
    return canInsert && delegate.isValid(index, resource);
  }

  @Override
  public void set(int index, ItemResource resource, int amount) {
    container.setItem(index, resource.toStack(amount));
  }
}
