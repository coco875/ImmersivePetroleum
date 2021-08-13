package flaxbeard.immersivepetroleum.common.gui;

import flaxbeard.immersivepetroleum.common.blocks.tileentities.DerrickTileEntity;
import flaxbeard.immersivepetroleum.common.multiblocks.DerrickMultiblock;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.Fluids;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;

public class DerrickContainer extends MultiblockAwareGuiContainer<DerrickTileEntity>{
	public DerrickContainer(int id, PlayerInventory playerInventory, DerrickTileEntity tile){
		super(playerInventory, tile, id, DerrickMultiblock.INSTANCE);
		
		this.addSlot(new Slot(this.inv, 0, 164, 120));
		
		this.addSlot(new Slot(this.inv, 1, 36, 89){
			@Override
			public boolean isItemValid(ItemStack stack){
				return stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null).map(h -> {
					if(h.getTanks() <= 0)
						return false;
					
					FluidStack fs = h.getFluidInTank(0);
					if(fs.isEmpty())
						return false;
					
					if(tile.waterTank.getFluidAmount() > 0 && !fs.isFluidEqual(tile.waterTank.getFluid()))
						return false;
					
					return fs.getFluid() == Fluids.WATER;
				}).orElse(false);
			}
		});
		
		this.addSlot(new Slot(this.inv, 2, 36, 120){
			@Override
			public boolean isItemValid(ItemStack stack){
				return false;
			}
		});
		
		slotCount = 3;
		
		// Player Hotbar
		for(int i = 0;i < 9;i++){
			addSlot(new Slot(playerInventory, i, 20 + i * 18, 163));
		}
	}
}
