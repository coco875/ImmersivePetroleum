package flaxbeard.immersivepetroleum.common.entity;

import flaxbeard.immersivepetroleum.ImmersivePetroleum;
import flaxbeard.immersivepetroleum.common.IPContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

public class MolotovItemEntity extends ThrowableItemProjectile{
	
	public static final EntityType<MolotovItemEntity> TYPE = createType();
	
	private static EntityType<MolotovItemEntity> createType(){
		EntityType<MolotovItemEntity> ret = EntityType.Builder.<MolotovItemEntity>of(MolotovItemEntity::new, MobCategory.MISC)
				.sized(0.25F, 0.25F)
				.clientTrackingRange(4)
				.updateInterval(10)
				.build(ImmersivePetroleum.MODID + "molotov");
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
			impact(this.level, new BlockPos(pResult.getLocation()));
			this.level.broadcastEntityEvent(this, (byte) 3);
			this.discard();
		}
	}
	
	private void impact(Level level, BlockPos pos){
		// TODO
	}
}
