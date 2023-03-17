package flaxbeard.immersivepetroleum.common;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import flaxbeard.immersivepetroleum.ImmersivePetroleum;
import flaxbeard.immersivepetroleum.api.crafting.LubricatedHandler;
import flaxbeard.immersivepetroleum.api.crafting.LubricatedHandler.LubricatedTileInfo;
import flaxbeard.immersivepetroleum.api.reservoir.ReservoirIsland;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

public class IPSaveData extends SavedData{
	public static final String dataName = "ImmersivePetroleum-SaveData";
	
	private static IPSaveData INSTANCE;
	
	public IPSaveData(){
		INSTANCE = this;
	}
	
	public IPSaveData(CompoundTag nbt){
		INSTANCE = this;
		
		ListTag lubricatedList = nbt.getList("lubricated", Tag.TAG_COMPOUND);
		LubricatedHandler.lubricatedTiles.clear();
		for(int i = 0;i < lubricatedList.size();i++){
			CompoundTag tag = lubricatedList.getCompound(i);
			LubricatedTileInfo info = new LubricatedTileInfo(tag);
			LubricatedHandler.lubricatedTiles.add(info);
		}
		
		// TODO Backwards compability. Remove in 1.19.x!
		ListTag reservoirs;
		if(nbt.contains("reservoirs", Tag.TAG_LIST) && !(reservoirs = nbt.getList("reservoirs", Tag.TAG_COMPOUND)).isEmpty()){
			final ReservoirRegionDataStorage storage = ReservoirRegionDataStorage.get();
			
			ImmersivePetroleum.log.debug("[ReservoirIslands]: Reading...");
			for(int i = 0;i < reservoirs.size();i++){
				CompoundTag dim = reservoirs.getCompound(i);
				ResourceLocation rl = new ResourceLocation(dim.getString("dimension"));
				ResourceKey<Level> dimType = ResourceKey.create(Registry.DIMENSION_REGISTRY, rl);
				ListTag islands = dim.getList("islands", Tag.TAG_COMPOUND);
				
				List<ReservoirIsland> list = islands.stream().map(inbt -> ReservoirIsland.readFromNBT((CompoundTag) inbt)).filter(o -> o != null).collect(Collectors.toList());
				list.forEach(island -> {
					storage.addIsland(dimType, island);
				});
				ImmersivePetroleum.log.debug("[ReservoirIslands]: Read {} islands for dim {}", list.size(), dimType.toString());
			}
		}
	}
	
	@Override
	@Nonnull
	public CompoundTag save(@Nonnull CompoundTag nbt){
		/*
		ListTag reservoirs = new ListTag();
		synchronized(ReservoirHandler.getReservoirIslandList()){
			for(ResourceKey<Level> dimension:ReservoirHandler.getReservoirIslandList().keySet()){
				CompoundTag dim = new CompoundTag();
				dim.putString("dimension", dimension.location().toString());
				
				ListTag islands = new ListTag();
				for(ReservoirIsland island:ReservoirHandler.getReservoirIslandList().get(dimension)){
					islands.add(island.writeToNBT());
				}
				dim.put("islands", islands);
				
				reservoirs.add(dim);
			}
		}
		nbt.put("reservoirs", reservoirs);
		*/
		
		ListTag lubricatedList = new ListTag();
		for(LubricatedTileInfo info:LubricatedHandler.lubricatedTiles){
			if(info != null){
				CompoundTag tag = info.writeToNBT();
				lubricatedList.add(tag);
			}
		}
		nbt.put("lubricated", lubricatedList);
		
		return nbt;
	}
	
	public static void markDirty(){
		if(INSTANCE != null){
			INSTANCE.setDirty();
		}
	}
}
