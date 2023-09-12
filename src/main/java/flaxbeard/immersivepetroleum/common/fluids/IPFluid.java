package flaxbeard.immersivepetroleum.common.fluids;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;

import com.google.common.collect.ImmutableList;

import flaxbeard.immersivepetroleum.ImmersivePetroleum;
import flaxbeard.immersivepetroleum.common.IPRegisters;
import flaxbeard.immersivepetroleum.common.util.ResourceUtils;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.wrappers.FluidBucketWrapper;
import net.minecraftforge.registries.RegistryObject;


public class IPFluid extends FlowingFluid{
	public static final List<IPFluidEntry> FLUIDS = new ArrayList<>();
	
	public static IPFluidEntry makeFluid(String name, int density, int viscosity, boolean isGas){
		IPFluidEntry entry = IPFluidEntry.make(name, builder(density, viscosity, isGas));
		return entry;
	}
	
	public static <B extends IPFluidBlock> IPFluidEntry makeFluid(String name, int density, int viscosity, boolean isGas, BiFunction<IPFluidEntry, Block.Properties, B> blockFactory){
		IPFluidEntry entry = IPFluidEntry.make(name, blockFactory, builder(density, viscosity, isGas));
		return entry;
	}
	
	public static <S extends IPFluid> IPFluidEntry makeFluidF(String name, int density, int viscosity, boolean isGas, Function<IPFluidEntry, S> fluidFactory){
		IPFluidEntry entry = IPFluidEntry.make(name, fluidFactory, builder(density, viscosity, isGas));
		return entry;
	}
	
	private static IPFluidEntry staticEntry;
	private static IPFluid makeFluid(Function<IPFluidEntry, ? extends IPFluid> make, IPFluid.IPFluidEntry entry){
		staticEntry = entry;
		IPFluid ret = make.apply(entry);
		staticEntry = null;
		return ret;
	}
	
	protected final IPFluidEntry entry;

	protected IPFluid(IPFluidEntry entry){
		this.entry = entry;
	}
	
	public static IPFluidEntry makeFluid(String name, Function<IPFluidEntry, IPFluid> factory){
		return makeFluid(name, factory, IPFluidBlock::new);
	}
	
	public static IPFluidEntry makeFluid(String name, Function<IPFluidEntry, IPFluid> factory, Function<IPFluidEntry, Block> blockFactory){
		Mutable<IPFluidEntry> entry = new MutableObject<>();

		FluidType.Properties properties = FluidType.Properties.create()
					.sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_EMPTY);
		RegistryObject<FluidType> fluid_types = IPRegisters.registerFluidType(name, () -> new FluidType(properties));
		
		entry.setValue(new IPFluidEntry(
				name,
				IPRegisters.registerFluid(name, () -> factory.apply(entry.getValue()).setForgeFluidType(fluid_types)),
				IPRegisters.registerFluid(name+"_flowing", () -> new IPFluidFlowing(entry.getValue().still.get()).setForgeFluidType(fluid_types)),
				IPRegisters.registerBlock(name, () -> blockFactory.apply(entry.getValue())),
				IPRegisters.registerItem(name+"_bucket", () -> new IPBucketItem(entry.getValue().still()))
		));
		FLUIDS.add(entry.getValue());
		return entry.getValue();
	}
	
	public static RegistryObject<FluidType> FORGEFLUIDTYPE;
	
	// TODO add texture
	@Override
	protected void createFluidStateDefinition(Builder<Fluid, FluidState> builder){
		super.createFluidStateDefinition(builder);
		for(Property<?> p:(entry == null ? staticEntry : entry).properties()){
			builder.add(p);
		}
	}

	@Nonnull
	public FluidType getFluidType(){
		return FORGEFLUIDTYPE.get();
	}
	// @Override
	// @Nonnull
	// protected FluidType createAttributes(){
	// 	FluidType.Properties builder = FluidType.builder(this.stillTexture, this.flowingTexture)
	// 			.overlay(this.stillTexture)
	// 			.sound(SoundEvents.BUCKET_FILL, SoundEvents.BUCKET_EMPTY);
		
	// 	if(this.buildAttributes != null)
	// 		this.buildAttributes.accept(builder);
		
	// 	return builder.build(this);
	// }


	public IPFluid setForgeFluidType(RegistryObject<FluidType> type){
		FORGEFLUIDTYPE = type;
		return this;
	}
	
	@Override
	protected void beforeDestroyingBlock(@Nonnull LevelAccessor arg0, @Nonnull BlockPos arg1, @Nonnull BlockState arg2){
	}
	
	@Override
	protected boolean canConvertToSource(){
		return false;
	}
	
	@Override
	@Nonnull
	public Fluid getFlowing(){
		return this.entry.flowing.get();
	}
	
	@Override
	@Nonnull
	public Fluid getSource(){
		return this.entry.source.get();
	}
	
	@Override
	@Nonnull
	public Item getBucket(){
		return this.entry.bucket.get();
	}
	
	@Override
	public FluidType getFluidType(){
		return this.entry.type.get();
	}
	
	@Override
	protected int getDropOff(@Nonnull LevelReader arg0){
		return 1;
	}
	
	@Override
	protected int getSlopeFindDistance(@Nonnull LevelReader arg0){
		return 4;
	}
	
	@Override
	protected boolean canBeReplacedWith(FluidState fluidState, BlockGetter blockReader, BlockPos pos, Fluid fluid, Direction direction){
		return direction == Direction.DOWN && !isSame(fluid);
	}
	
	@Override
	public int getTickDelay(@Nonnull LevelReader p_205569_1_){
		return 5;
	}
	
	@Override
	protected float getExplosionResistance(){
		return 100;
	}
	
	@Override
	protected @Nonnull BlockState createLegacyBlock(@Nonnull FluidState state){
		return this.entry.block.get().defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
	}
	
	@Override
	public boolean isSource(FluidState state){
		return state.is(getSource());
	}
	
	@Override
	public int getAmount(@Nonnull FluidState state){
		return isSource(state) ? 8 : state.getValue(LEVEL);
	}
	
	@Override
	public boolean isSame(Fluid fluidIn){
		return fluidIn == getSource() || fluidIn == getFlowing();
	}
	
	private static Consumer<FluidType.Properties> builder(int density, int viscosity, boolean isGas){
		// Apparently the gaseous stuff aint there anymore. RIP :(
		return builder -> builder.viscosity(viscosity).density(density);
	}
	
	// STATIC CLASSES
	
	public record IPFluidEntry(RegistryObject<IPFluid> source, RegistryObject<IPFluid> flowing, RegistryObject<IPFluidBlock> block, RegistryObject<BucketItem> bucket, RegistryObject<FluidType> type, List<Property<?>> properties){
		
		public Fluid get(){
			return source().get();
		}
		
		protected static IPFluidEntry make(String name){
			return make(name, 0, null);
		}
		
		protected static IPFluidEntry make(String name, @Nullable Consumer<FluidType.Properties> buildAttributes){
			return make(name, 0, IPFluid::new, IPFluid.Flowing::new, IPFluid.IPFluidBlock::new, buildAttributes, ImmutableList.of());
		}
		
		protected static IPFluidEntry make(String name, int burnTime, @Nullable Consumer<FluidType.Properties> buildAttributes){
			return make(name, burnTime, IPFluid::new, IPFluid.Flowing::new, IPFluid.IPFluidBlock::new, buildAttributes, ImmutableList.of());
		}
		
		protected static <B extends IPFluidBlock> IPFluidEntry make(String name, BiFunction<IPFluidEntry, BlockBehaviour.Properties, B> makeBlock, @Nullable Consumer<FluidType.Properties> buildAttributes){
			return make(name, IPFluid::new, IPFluid.Flowing::new, makeBlock, buildAttributes, ImmutableList.of());
		}
		
		protected static <S extends IPFluid> IPFluidEntry make(String name, Function<IPFluidEntry, S> makeSource, @Nullable Consumer<FluidType.Properties> buildAttributes){
			return make(name, makeSource, IPFluid.Flowing::new, IPFluid.IPFluidBlock::new, buildAttributes, ImmutableList.of());
		}
		
		protected static <S extends IPFluid, F extends IPFluid, B extends IPFluidBlock> IPFluidEntry make(
			String name,
			Function<IPFluidEntry, S> makeSource, Function<IPFluidEntry, F> makeFlowing, BiFunction<IPFluidEntry, BlockBehaviour.Properties, B> makeBlock,
			@Nullable Consumer<FluidType.Properties> buildAttributes, List<Property<?>> properties
		){
			return make(name, 0, makeSource, makeFlowing, makeBlock, buildAttributes, properties);
		}
		
		protected static <S extends IPFluid, F extends IPFluid, B extends IPFluidBlock> IPFluidEntry make(
			String name, int burnTime,
			Function<IPFluidEntry, S> makeSource, Function<IPFluidEntry, F> makeFlowing, BiFunction<IPFluidEntry, BlockBehaviour.Properties, B> makeBlock,
			@Nullable Consumer<FluidType.Properties> buildAttributes, List<Property<?>> properties
		){
			FluidType.Properties builder = FluidType.Properties.create();
			if(buildAttributes != null){
				buildAttributes.accept(builder);
			}
			
			RegistryObject<FluidType> type = IPRegisters.FLUID_TYPE.register(name, () -> typeWithTexture(builder, name));
			
			Mutable<IPFluidEntry> thisMutable = new MutableObject<>();
			
			RegistryObject<IPFluid> source = IPRegisters.registerFluid(name, () -> IPFluid.makeFluid(makeSource, thisMutable.getValue()));
			RegistryObject<IPFluid> flow = IPRegisters.registerFluid(name + "_flowing", () -> IPFluid.makeFluid(makeFlowing, thisMutable.getValue()));
			RegistryObject<IPFluidBlock> block = IPRegisters.registerBlock(name + "_fluid_block", () -> makeBlock.apply(thisMutable.getValue(), Properties.copy(Blocks.WATER)));
			RegistryObject<BucketItem> bucket = IPRegisters.registerItem(name + "_bucket", () -> new IPBucketItem(source, burnTime));
			
			IPFluidEntry entry = new IPFluidEntry(source, flow, block, bucket, type, properties);
			thisMutable.setValue(entry);
			FLUIDS.add(entry);
			return entry;
		}
		
		private static FluidType typeWithTexture(FluidType.Properties builder, String name){
			final ResourceLocation stillTexture = ResourceUtils.ip("block/fluid/" + name + "_still");
			final ResourceLocation flowTexture = ResourceUtils.ip("block/fluid/" + name + "_flow");
			
			return new FluidType(builder){
				@Override
				public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer){
					consumer.accept(new IClientFluidTypeExtensions(){
						@Override
						public ResourceLocation getStillTexture(){
							return stillTexture;
						}
						
						@Override
						public ResourceLocation getFlowingTexture(){
							return flowTexture;
						}
					});
				}
			};
		}
	}
	
	public static class IPFluidBlock extends LiquidBlock{
		private static IPFluidEntry staticEntry;
		
		protected final IPFluidEntry entry;
		@Nullable
		private MobEffect effect;
		private int duration;
		private int level;
		public IPFluidBlock(IPFluidEntry entry, Block.Properties props){
			super(entry.source(), Util.make(props, p -> staticEntry = entry));
			this.entry = entry;
			staticEntry = null;
		}
		
		public void setEffect(@Nullable MobEffect effect, int duration, int level){
			this.effect = effect;
			this.duration = duration;
			this.level = level;
		}
		
		@Override
		protected void createBlockStateDefinition(Builder<Block, BlockState> builder){
			super.createBlockStateDefinition(builder);
			for(Property<?> p:(this.entry == null ? staticEntry : this.entry).properties())
				builder.add(p);
		}
		
		@Override
		public FluidState getFluidState(BlockState pState){
			FluidState state = super.getFluidState(pState);
			for(Property<?> prop:state.getProperties()){
				if(state.hasProperty(prop))
					state = copyValue(prop, state, pState);
			}
			return state;
		}
		
		@SuppressWarnings("deprecation")
		@Override
		public void entityInside(BlockState pState, Level pLevel, BlockPos pPos, Entity pEntity){
			super.entityInside(pState, pLevel, pPos, pEntity);
			if(this.effect != null && pEntity instanceof LivingEntity living)
				living.addEffect(new MobEffectInstance(this.effect, this.duration, this.level));
		}
		
		public static <S extends StateHolder<?, S>, P extends Comparable<P>> S copyValue(Property<P> prop, S oldState, StateHolder<?, ?> from){
			return oldState.setValue(prop, from.getValue(prop));
		}
	}
	
	public static class IPBucketItem extends BucketItem{
		private static final Item.Properties PROPS = new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET).tab(ImmersivePetroleum.creativeTab);
		
		private int burnTime;
		public IPBucketItem(Supplier<? extends Fluid> fluid, int burnTime){
			super(fluid, PROPS);
			this.burnTime = burnTime;
		}
		
		public IPBucketItem(Supplier<? extends Fluid> fluid, Function<Item.Properties, Item.Properties> props){
			super(fluid, props.apply(PROPS));
		}
		
		@Override
		public ICapabilityProvider initCapabilities(@Nonnull ItemStack stack, @Nullable CompoundTag nbt){
			return new FluidBucketWrapper(stack);
		}
		
		@Override
		public int getBurnTime(ItemStack itemStack, @org.jetbrains.annotations.Nullable RecipeType<?> recipeType){
			return this.burnTime;
		}
	}
	
	public static class Flowing extends IPFluid{
		public Flowing(IPFluid.IPFluidEntry entry){
			super(entry);
		}
		
		@Override
		protected void createFluidStateDefinition(@Nonnull Builder<Fluid, FluidState> builder){
			super.createFluidStateDefinition(builder);
			builder.add(LEVEL);
		}
	}
}
