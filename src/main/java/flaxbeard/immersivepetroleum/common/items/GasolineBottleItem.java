package flaxbeard.immersivepetroleum.common.items;

import flaxbeard.immersivepetroleum.ImmersivePetroleum;
import net.minecraft.world.item.Item;

public class GasolineBottleItem extends IPItemBase{
	public GasolineBottleItem(){
		super(new Item.Properties().tab(ImmersivePetroleum.creativeTab));
	}
}
