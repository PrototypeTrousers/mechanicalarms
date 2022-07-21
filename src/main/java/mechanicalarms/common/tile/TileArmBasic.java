package mechanicalarms.common.tile;

import com.cleanroommc.modularui.api.ModularUITextures;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.Text;
import com.cleanroommc.modularui.api.math.Size;
import com.cleanroommc.modularui.api.screen.ITileWithModularUI;
import com.cleanroommc.modularui.api.screen.ModularWindow;
import com.cleanroommc.modularui.api.screen.UIBuildContext;
import mechanicalarms.common.logic.behavior.Action;
import mechanicalarms.common.logic.behavior.ActionResult;
import mechanicalarms.common.logic.behavior.InteractionType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TileArmBasic extends TileArmBase implements ITileWithModularUI {

    protected ItemStackHandler itemHandler = new ItemStackHandler(1);

    public TileArmBasic() {
        super(2, InteractionType.ITEM);
    }

    @Override
    public void writeInitialSyncData(PacketBuffer packetBuffer) {

    }

    @Override
    public void receiveInitialSyncData(PacketBuffer packetBuffer) {

    }

    @Override
    public void receiveCustomData(int i, PacketBuffer packetBuffer) {

    }

    @Override
    public ActionResult interact(Action action, Pair<BlockPos, EnumFacing> blkFace) {
        TileEntity te = world.getTileEntity(blkFace.getKey());
        if (te != null) {
            IItemHandler itemHandler = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, blkFace.getRight());
            if (itemHandler != null) {
                if (action == Action.RETRIEVE) {
                    if (this.itemHandler.getStackInSlot(0).isEmpty()) {
                        for (int i = 0; i < itemHandler.getSlots(); i++) {
                            if (!itemHandler.extractItem(i, 1, true).isEmpty()) {
                                ItemStack itemStack = itemHandler.extractItem(i, 1, false);
                                if (!itemStack.isEmpty()) {
                                    this.itemHandler.insertItem(0, itemStack, false);
                                    return ActionResult.SUCCESS;
                                }
                            }
                        }
                    }
                } else if (action == Action.DELIVER) {
                    ItemStack itemStack = this.itemHandler.extractItem(0, 1, true);
                    if (!itemStack.isEmpty()) {
                        itemStack = this.itemHandler.extractItem(0, 1, false);
                        if (!itemStack.isEmpty()) {
                            for (int i = 0; i < itemHandler.getSlots(); i++) {
                                if (itemHandler.insertItem(i, itemStack, false).isEmpty()) {
                                    return ActionResult.SUCCESS;
                                }
                            }
                        }
                    }
                }
            }
        }
        return ActionResult.CONTINUE;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setTag("inventory", itemHandler.serializeNBT());
        return super.writeToNBT(compound);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        itemHandler.deserializeNBT(compound.getCompoundTag("inventory"));
        super.readFromNBT(compound);
    }

    public ItemStack getItemStack() {
        return itemHandler.getStackInSlot(0);
    }

    @Override
    public ModularWindow createWindow(UIBuildContext uiBuildContext) {
        ModularWindow.Builder builder = ModularWindow.builder(new Size(176, 272));
        builder.setBackground( ModularUITextures.VANILLA_BACKGROUND ).bindPlayerInventory( uiBuildContext.getPlayer() );
        return builder.build();
    }

}
