package flaxbeard.immersivepetroleum.api.crafting.reservoir;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;

/**
 * TODO Move to flaxbeard.immersivepetroleum.common.util ?
 * 
 * @author TwistedGate
 */
public class IslandAxisAlignedBB{
	final int minX, minZ;
	final int maxX, maxZ;
	final BlockPos center;
	public IslandAxisAlignedBB(int minX, int minZ, int maxX, int maxZ){
		this.minX = minX;
		this.minZ = minZ;
		this.maxX = maxX;
		this.maxZ = maxZ;
		
		this.center = new BlockPos((this.minX + this.maxX) / 2, 0, (this.minZ + this.maxZ) / 2);
	}
	
	// May end up never using it, but its here already just incase i do
	public BlockPos getCenter(){
		return this.center;
	}
	
	public boolean contains(BlockPos pos){
		return contains(pos.getX(), pos.getZ());
	}
	
	public boolean contains(int x, int z){
		return x >= this.minX && x <= this.maxX && z >= this.minZ && z <= this.maxZ;
	}
	
	public CompoundNBT writeToNBT(){
		CompoundNBT bounds = new CompoundNBT();
		bounds.putInt("minX", this.minX);
		bounds.putInt("minZ", this.minZ);
		bounds.putInt("maxX", this.maxX);
		bounds.putInt("maxZ", this.maxZ);
		return bounds;
	}
	
	public static IslandAxisAlignedBB readFromNBT(CompoundNBT nbt){
		int minX = nbt.getInt("minX");
		int minZ = nbt.getInt("minZ");
		int maxX = nbt.getInt("maxX");
		int maxZ = nbt.getInt("maxZ");
		
		return new IslandAxisAlignedBB(minX, minZ, maxX, maxZ);
	}
}