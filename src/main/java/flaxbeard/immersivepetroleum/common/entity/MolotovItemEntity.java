package flaxbeard.immersivepetroleum.common.entity;

import java.util.HashSet;
import java.util.Set;

import flaxbeard.immersivepetroleum.ImmersivePetroleum;
import flaxbeard.immersivepetroleum.common.IPContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class MolotovItemEntity extends ThrowableItemProjectile{
	
	public static final EntityType<MolotovItemEntity> TYPE = createType();
	
	private static EntityType<MolotovItemEntity> createType(){
		EntityType<MolotovItemEntity> ret = EntityType.Builder.<MolotovItemEntity> of(MolotovItemEntity::new, MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10).build(ImmersivePetroleum.MODID + "molotov");
		ret.setRegistryName(ImmersivePetroleum.MODID, "molotov");
		return ret;
	}
	
	public MolotovItemEntity(Level world, LivingEntity living){
		this(TYPE, world, living);
	}
	
	public MolotovItemEntity(Level world, LivingEntity living, double x, double y, double z){
		this(TYPE, world, living);
		setPos(x, y, z);
		this.xo = x;
		this.yo = y;
		this.zo = z;
	}
	
	public MolotovItemEntity(EntityType<MolotovItemEntity> type, Level world){
		super(type, world);
		this.blocksBuilding = true;
	}
	
	public MolotovItemEntity(EntityType<MolotovItemEntity> type, Level world, LivingEntity living){
		super(type, living, world);
		this.blocksBuilding = true;
	}
	
	@Override
	protected Item getDefaultItem(){
		return IPContent.Items.MOLOTOV_LIT.get();
	}
	
	@Override
	protected void onHit(HitResult pResult){
		super.onHit(pResult);
		if(!this.level.isClientSide){
			this.level.broadcastEntityEvent(this, (byte) 3);
			this.discard();
		}
	}
	
	@Override
	protected void onHitEntity(EntityHitResult pResult){
		super.onHitEntity(pResult);
		
		if(!this.level.isClientSide){
			fire(new BlockPos(pResult.getLocation()));
		}
	}
	
	@Override
	protected void onHitBlock(BlockHitResult hitResult){
		super.onHitBlock(hitResult);
		
		if(!this.level.isClientSide){
			fire(hitResult.getBlockPos().relative(hitResult.getDirection()));
		}
	}
	
	private void fire(BlockPos pos){
		Set<BlockPos> hits = new HashSet<>();
		place(pos, hits, 0, 9);
		hits.forEach(this::placeFire);
	}
	
	private void place(BlockPos pos, Set<BlockPos> visited, int cur, int max){
		if(cur >= max)
			return;
		if(visited.contains(pos))
			return;
		if(cur > 0 && !this.level.getBlockState(pos).isAir())
			return;
		
		visited.add(pos);
		place(pos.above(), visited, cur + 1, max);
		place(pos.below(), visited, cur + 1, max);
		place(pos.north(), visited, cur + 1, max);
		place(pos.east(), visited, cur + 1, max);
		place(pos.south(), visited, cur + 1, max);
		place(pos.west(), visited, cur + 1, max);
	}
	
	private void placeFire(BlockPos pos){
		if(!this.level.getBlockState(pos).isAir())
			return;
		
		BlockState fire = Blocks.FIRE.defaultBlockState();
		
		boolean up = false, north = false, east = false, south = false,
				west = false;
		if(this.level.getBlockState(pos.below()).isAir()){
			BlockPos abovePos = pos.above();
			BlockPos northPos = pos.north();
			BlockPos eastPos = pos.east();
			BlockPos southPos = pos.south();
			BlockPos westPos = pos.west();
			
			up = this.level.getBlockState(abovePos).isFlammable(this.level, abovePos, Direction.DOWN);
			north = this.level.getBlockState(northPos).isFlammable(this.level, northPos, Direction.SOUTH);
			east = this.level.getBlockState(eastPos).isFlammable(this.level, eastPos, Direction.WEST);
			south = this.level.getBlockState(southPos).isFlammable(this.level, southPos, Direction.NORTH);
			west = this.level.getBlockState(westPos).isFlammable(this.level, westPos, Direction.EAST);
			
			fire = fire.setValue(FireBlock.UP, up).setValue(FireBlock.NORTH, north).setValue(FireBlock.EAST, east).setValue(FireBlock.SOUTH, south).setValue(FireBlock.WEST, west);
		}
		
		this.level.setBlock(pos, fire, 3);
	}
}
