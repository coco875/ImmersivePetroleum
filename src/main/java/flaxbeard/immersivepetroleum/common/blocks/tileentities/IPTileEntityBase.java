package flaxbeard.immersivepetroleum.common.blocks.tileentities;

import java.util.Objects;

import javax.annotation.Nonnull;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class IPTileEntityBase extends BlockEntity{
	public IPTileEntityBase(BlockEntityType<?> blockEntityType, BlockPos pWorldPosition, BlockState pBlockState){
		super(blockEntityType, pWorldPosition, pBlockState);
	}
	
	@Nonnull
	public Level getLevelNonnull(){
		return Objects.requireNonNull(super.getLevel());
	}
	
	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket(){
		return ClientboundBlockEntityDataPacket.create(this, b -> getUpdateTag());
	}
	
	@Override
	public void handleUpdateTag(CompoundTag tag){
		load(tag);
	}
	
	@Override
	@Nonnull
	public CompoundTag getUpdateTag(){
		CompoundTag nbt = new CompoundTag();
		saveAdditional(nbt);
		return nbt;
	}
	
	@Override
	public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt){
		if(pkt.getTag() != null)
			load(pkt.getTag());
	}
	
	@Override
	public void saveAdditional(@Nonnull CompoundTag compound){
		writeCustom(compound);
	}
	
	@Override
	public void load(@Nonnull CompoundTag compound){
		super.load(compound);
		readCustom(compound);
	}
	
	protected abstract void writeCustom(CompoundTag compound);
	
	protected abstract void readCustom(CompoundTag compound);
}
