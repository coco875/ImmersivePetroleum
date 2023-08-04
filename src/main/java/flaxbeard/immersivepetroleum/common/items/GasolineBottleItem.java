package flaxbeard.immersivepetroleum.common.items;

import flaxbeard.immersivepetroleum.ImmersivePetroleum;
import net.minecraft.world.item.Item;

public class GasolineBottleItem extends IPItemBase{
	/** How much gasoline a filled bottle contains in mB. */
	public static final int FILLED_AMOUNT = 250;
	
	public GasolineBottleItem(){
		super(new Item.Properties().tab(ImmersivePetroleum.creativeTab));
	}
}
