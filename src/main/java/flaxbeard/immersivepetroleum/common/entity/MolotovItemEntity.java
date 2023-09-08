package flaxbeard.immersivepetroleum.common.entity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import flaxbeard.immersivepetroleum.ImmersivePetroleum;
import flaxbeard.immersivepetroleum.common.IPContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class MolotovItemEntity extends ThrowableItemProjectile{	
	public static EntityType<MolotovItemEntity> createType(){
		EntityType<MolotovItemEntity> ret = EntityType.Builder.<MolotovItemEntity> of(MolotovItemEntity::new, MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10).build(ImmersivePetroleum.MODID + "molotov");
		// ret.setRegistryName(ImmersivePetroleum.MODID, "molotov");
		return ret;
	}
	
	public MolotovItemEntity(Level world, LivingEntity living){
		this(IPContent.Entity.MOLOTOV.get(), world, living);
	}
	
	public MolotovItemEntity(Level world, LivingEntity living, double x, double y, double z){
		this(IPContent.Entity.MOLOTOV.get(), world, living);
		setPos(x, y, z);
		this.xo = x;
		this.yo = y;
		this.zo = z;
	}
	
	private MolotovItemEntity(EntityType<MolotovItemEntity> type, Level world){
		super(type, world);
		this.blocksBuilding = true;
	}
	
	private MolotovItemEntity(EntityType<MolotovItemEntity> type, Level world, LivingEntity living){
		super(type, living, world);
		this.blocksBuilding = true;
	}
	
	@Override
	protected Item getDefaultItem(){
		return IPContent.Items.MOLOTOV_LIT.get();
	}
	
	@Override
	public void tick(){
		super.tick();
		
		if(this.level.isClientSide){
			double m = 0.125;
			double x = getX() + (m - m * 2 * this.random.nextDouble());
			double y = getY() + (m - m * 2 * this.random.nextDouble());
			double z = getZ() + (m - m * 2 * this.random.nextDouble());
			this.level.addParticle(ParticleTypes.FLAME, x, y, z, 0, 0, 0);
		}
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
		if(this.level.getBlockState(pos).getMaterial().isLiquid()){
			this.level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, this.level.getRandom().nextFloat() * 0.4F + 0.8F);
			return;
		}
		
		this.level.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 0.3F, 0.7F);
		
		Set<BlockPos> hits = new HashSet<>();
		scanArea(pos, pos, hits, 9);
		if(hits.isEmpty())
			return;
		hits.forEach(this::placeFire);
		
		this.level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.NEUTRAL, 1.0F, 1.0F);
		
		AABB bounds = AABB.ofSize(new Vec3(pos.getX(), pos.getY(), pos.getZ()), 6.5, 6.5, 6.5);
		List<LivingEntity> list = this.level.getEntitiesOfClass(LivingEntity.class, bounds);
		if(list.isEmpty())
			return;
		
		if(getOwner() instanceof Player player){
			final DamageSource src = DamageSource.playerAttack(player);
			list.forEach(e -> e.hurt(src, 1.0F));
		}
		
		if(getOwner() instanceof LivingEntity living){
			living.setLastHurtMob(list.get(list.size() - 1));
		}
	}
	
	private void scanArea(BlockPos start, BlockPos pos, Set<BlockPos> visited, int radiusSqr){
		BlockPos dif = pos.subtract(start);
		int sqr = dif.getX() * dif.getX() + dif.getY() * dif.getY() + dif.getZ() * dif.getZ();
		if(sqr > radiusSqr)
			return;
		if(pos != start && !this.level.getBlockState(pos).isAir())
			return;
		if(visited.contains(pos))
			return;
		
		visited.add(pos);
		scanArea(start, pos.above(), visited, radiusSqr);
		scanArea(start, pos.below(), visited, radiusSqr);
		scanArea(start, pos.north(), visited, radiusSqr);
		scanArea(start, pos.east(), visited, radiusSqr);
		scanArea(start, pos.south(), visited, radiusSqr);
		scanArea(start, pos.west(), visited, radiusSqr);
	}
	
	private void placeFire(BlockPos pos){
		if(!this.level.getBlockState(pos).isAir())
			return;
		
		BlockState fire = Blocks.FIRE.defaultBlockState();
		
		boolean up = false;
		boolean north = false;
		boolean east = false;
		boolean south = false;
		boolean west = false;
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
