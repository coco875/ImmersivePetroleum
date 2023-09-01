package flaxbeard.immersivepetroleum.common.blocks;

import net.minecraft.world.level.block.StairBlock;

import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

public class IPBlockStairs<B extends IPBlockBase> extends StairBlock{
	public IPBlockStairs(B base){
		super(base::defaultBlockState, Properties.copy(base));
	}
}
