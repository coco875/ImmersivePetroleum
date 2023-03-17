package flaxbeard.immersivepetroleum.common;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import flaxbeard.immersivepetroleum.ImmersivePetroleum;
import flaxbeard.immersivepetroleum.api.reservoir.ReservoirIsland;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

/**
 * Manager for {@link RegionData}s
 * 
 * @author TwistedGate
 */
public class ReservoirRegionDataStorage extends SavedData{
	private static final Logger log = LogManager.getLogger(ImmersivePetroleum.MODID + "/RegionDataStorage");
	
	private static final String DATA_NAME = "ip-regions";
	private static final String REGIONDATA_FOLDER = "ipregions\\";
	
	private static ReservoirRegionDataStorage active_instance;
	public static ReservoirRegionDataStorage get(){
		return active_instance;
	}
	
	public static final void init(DimensionDataStorage dimData){
		active_instance = dimData.computeIfAbsent(t -> load(dimData, t), () -> {
			log.debug("Creating new ReservoirRegionDataStorage instance.");
			return new ReservoirRegionDataStorage(dimData);
		}, DATA_NAME);
	}
	
	// -----------------------------------------------------------------------------
	
	public static ReservoirRegionDataStorage load(DimensionDataStorage dimData, CompoundTag nbt){
		ReservoirRegionDataStorage storage = new ReservoirRegionDataStorage(dimData);
		storage.load(nbt);
		return storage;
	}
	
	/** Contains existing reservoir-region files */
	final Map<ColumnPos, RegionData> regions = new HashMap<>();
	final DimensionDataStorage dimData;
	public ReservoirRegionDataStorage(DimensionDataStorage dimData){
		this.dimData = dimData;
	}
	
	@Override
	public CompoundTag save(CompoundTag nbt){
		ListTag list = new ListTag();
		this.regions.forEach((key, entry) -> {
			CompoundTag tag = new CompoundTag();
			tag.putInt("x", key.x);
			tag.putInt("z", key.z);
			list.add(tag);
		});
		nbt.put("regions", list);
		
		log.debug("Saved regions file.");
		return nbt;
	}
	
	private void load(CompoundTag nbt){
		ListTag regions = nbt.getList("regions", Tag.TAG_COMPOUND);
		for(int i = 0;i < regions.size();i++){
			CompoundTag tag = regions.getCompound(i);
			int x = tag.getInt("x");
			int z = tag.getInt("z");
			
			ColumnPos rPos = new ColumnPos(x, z);
			RegionData rData = getOrCreateRegionDataFor(rPos);
			this.regions.put(rPos, rData);
		}
		
		log.debug("Loaded regions file.");
	}
	
	/** Marks itself and all regions as dirty. (Only to be used by {@link CommonEventHandler#onUnload(net.minecraftforge.event.world.WorldEvent.Unload)}) */
	public void markAllDirty(){
		setDirty();
		this.regions.values().forEach(RegionData::setDirty);
	}
	
	public void addIsland(ResourceKey<Level> dimensionKey, ReservoirIsland island){
		ColumnPos regionPos = toRegionCoords(island.getBoundingBox().getCenter());
		
		RegionData regionData = getOrCreateRegionDataFor(regionPos);
		synchronized(regionData.reservoirlist){
			if(!regionData.reservoirlist.containsEntry(dimensionKey, island)){
				regionData.reservoirlist.put(dimensionKey, island);
				island.setRegion(regionData);
				regionData.setDirty();
			}
		}
	}
	
	/** May only be called on the server-side. Returns null on client-side. */
	@Nullable
	public ReservoirIsland getIsland(Level world, BlockPos pos){
		return getIsland(world, new ColumnPos(pos));
	}
	
	/** May only be called on the server-side. Returns null on client-side. */
	@Nullable
	public ReservoirIsland getIsland(Level world, ColumnPos pos){
		if(world.isClientSide){
			return null;
		}
		
		RegionData regionData = getRegionDataFor(toRegionCoords(pos));
		return regionData != null ? regionData.get(world.dimension(), pos) : null;
	}
	
	public boolean existsAt(final ColumnPos pos){
		RegionData regionData = getRegionDataFor(toRegionCoords(pos));
		if(regionData != null){
			synchronized(regionData.reservoirlist){
				return regionData.reservoirlist.values().stream().anyMatch(island -> island.contains(pos));
			}
		}
		return false;
	}
	
	/** Utility method */
	public ColumnPos toRegionCoords(BlockPos pos){
		// 9 = SectionPos.blockToSectionCoord & ChunkPos.getRegionX
		return new ColumnPos(pos.getX() >> 9, pos.getZ() >> 9);
	}
	
	/** Utility method */
	public ColumnPos toRegionCoords(ColumnPos pos){
		// 9 = SectionPos.blockToSectionCoord & ChunkPos.getRegionX
		return new ColumnPos(pos.x >> 9, pos.z >> 9);
	}
	
	@Nullable
	public RegionData getRegionDataFor(BlockPos pos){
		return getRegionDataFor(toRegionCoords(pos));
	}
	
	@Nullable
	public RegionData getRegionDataFor(ColumnPos regionPos){
		RegionData ret = this.regions.getOrDefault(regionPos, null);
		return ret;
	}
	
	private RegionData getOrCreateRegionDataFor(ColumnPos regionPos){
		RegionData ret = this.regions.computeIfAbsent(regionPos, p -> {
			String fn = getRegionFileName(p);
			RegionData data = this.dimData.computeIfAbsent(t -> new RegionData(p, t), () -> new RegionData(p), fn);
			setDirty();
			log.debug("Created RegionData[{}, {}]", regionPos.x, regionPos.z);
			return data;
		});
		return ret;
	}
	
	private String getRegionFileName(ColumnPos regionPos){
		return REGIONDATA_FOLDER + regionPos.x + "_" + regionPos.z;
	}
	
	// -----------------------------------------------------------------------------
	
	/**
	 * Contains reservoirs within a particular region.
	 * 
	 * @author TwistedGate
	 */
	public static class RegionData extends SavedData{
		/** The current region this is assigned to. (Can be used as a sanity-check if need be) */
		public final ColumnPos regionPos;
		final Multimap<ResourceKey<Level>, ReservoirIsland> reservoirlist = ArrayListMultimap.create();
		RegionData(ColumnPos regionPos){
			this.regionPos = regionPos;
		}
		RegionData(ColumnPos regionPos, CompoundTag nbt){
			this.regionPos = regionPos;
			load(nbt);
		}
		
		@Override
		public void save(File pFile){
			if(!pFile.getParentFile().exists()){
				pFile.getParentFile().mkdirs();
			}
			super.save(pFile);
		}
		
		@Override
		public CompoundTag save(CompoundTag nbt){
			ListTag reservoirs = new ListTag();
			synchronized(this.reservoirlist){
				for(ResourceKey<Level> dimension:this.reservoirlist.keySet()){
					CompoundTag dim = new CompoundTag();
					dim.putString("dimension", dimension.location().toString());
					
					ListTag islands = new ListTag();
					for(ReservoirIsland island:this.reservoirlist.get(dimension)){
						islands.add(island.writeToNBT());
					}
					dim.put("islands", islands);
					
					reservoirs.add(dim);
				}
			}
			nbt.put("reservoirs", reservoirs);
			
			log.debug("RegionData[{}, {}] Saved.", this.regionPos.x, this.regionPos.z);
			return nbt;
		}
		
		private void load(CompoundTag nbt){
			ListTag reservoirs = nbt.getList("reservoirs", Tag.TAG_COMPOUND);
			if(!reservoirs.isEmpty()){
				synchronized(this.reservoirlist){
					for(int i = 0;i < reservoirs.size();i++){
						CompoundTag dim = reservoirs.getCompound(i);
						ResourceLocation rl = new ResourceLocation(dim.getString("dimension"));
						ResourceKey<Level> dimType = ResourceKey.create(Registry.DIMENSION_REGISTRY, rl);
						ListTag islands = dim.getList("islands", Tag.TAG_COMPOUND);
						
						List<ReservoirIsland> list = islands.stream()
								.map(inbt -> ReservoirIsland.readFromNBT((CompoundTag) inbt))
								.filter(o -> o != null)
								.collect(Collectors.toList());
						list.forEach(island -> island.setRegion(this));
						this.reservoirlist.putAll(dimType, list);
					}
				}
				log.debug("RegionData[{}, {}] Loaded.", this.regionPos.x, this.regionPos.z);
			}
		}
		
		public ReservoirIsland get(ResourceKey<Level> dimension, ColumnPos pos){
			synchronized(this.reservoirlist){
				for(ReservoirIsland island:this.reservoirlist.get(dimension)){
					if(island.contains(pos)){
						// There's no such thing as overlapping islands, so just return what was found directly
						return island;
					}
				}
				return null;
			}
		}
		
		/**
		 * @return {@link Multimap} of {@link ResourceKey<Level>}<{@link Level}>s to {@link ReservoirIsland}s
		 */
		public Multimap<ResourceKey<Level>, ReservoirIsland> getReservoirIslandList(){
			return this.reservoirlist;
		}
	}
}
