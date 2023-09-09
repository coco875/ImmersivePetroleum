package flaxbeard.immersivepetroleum.common.util.sounds;

import java.util.HashSet;
import java.util.Set;

import flaxbeard.immersivepetroleum.ImmersivePetroleum;
import flaxbeard.immersivepetroleum.common.util.ResourceUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@Mod.EventBusSubscriber(modid = ImmersivePetroleum.MODID, bus = Bus.MOD)
public class IPSounds{
	static Set<SoundEvent> soundEvents = new HashSet<>();
	
	public final static SoundEvent FLARESTACK = register("flarestack_fire");
	public final static SoundEvent PROJECTOR = register("projector");
	
	static SoundEvent register(String name){
		ResourceLocation rl = ResourceUtils.ip(name);
		SoundEvent event = new SoundEvent(rl);
		soundEvents.add(event); // event.setRegistryName(rl)
		return event;
	}
	
	// TODO: make this work
	// @SubscribeEvent
	// public static void registerSounds(RegisterEvent.Register<SoundEvent> event){
	// 	ImmersivePetroleum.log.debug("Loading sounds.");
	// 	for(SoundEvent sound:soundEvents){
	// 		event.getRegistry().register(sound);
	// 	}
	// 	soundEvents.clear();
	// }
}
