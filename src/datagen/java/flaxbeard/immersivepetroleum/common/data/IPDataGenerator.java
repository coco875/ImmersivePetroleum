package flaxbeard.immersivepetroleum.common.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import blusunrize.immersiveengineering.common.blocks.multiblocks.StaticTemplateManager;
import flaxbeard.immersivepetroleum.ImmersivePetroleum;
import flaxbeard.immersivepetroleum.common.data.loot.IPLootGenerator;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = ImmersivePetroleum.MODID, bus = Bus.MOD)
public class IPDataGenerator{
	public static final Logger log = LogManager.getLogger(ImmersivePetroleum.MODID + "/DataGenerator");
	
	@SubscribeEvent
	public static void generate(GatherDataEvent event){
		DataGenerator generator = event.getGenerator();
		ExistingFileHelper exhelper = event.getExistingFileHelper();
		StaticTemplateManager.EXISTING_HELPER = exhelper;
		
		if(event.includeServer()){
			IPBlockTags blockTags = new IPBlockTags(generator, exhelper);
			generator.addProvider(event.includeServer(), blockTags);
			generator.addProvider(event.includeServer(), new IPItemTags(generator, blockTags, exhelper));
			generator.addProvider(event.includeServer(), new IPFluidTags(generator, exhelper));
			generator.addProvider(event.includeServer(), new IPLootGenerator(generator));
			generator.addProvider(event.includeServer(), new IPRecipes(generator));
			generator.addProvider(event.includeServer(), new IPAdvancements(generator, exhelper));
		}
		
		if(event.includeClient()){
			generator.addProvider(event.includeServer(), new IPBlockStates(generator, exhelper));
			generator.addProvider(event.includeServer(), new IPItemModels(generator, exhelper));
		}
	}
}
