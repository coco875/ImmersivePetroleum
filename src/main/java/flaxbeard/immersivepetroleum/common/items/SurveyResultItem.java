package flaxbeard.immersivepetroleum.common.items;

import java.util.List;
import java.util.Locale;

import javax.annotation.Nonnull;

import flaxbeard.immersivepetroleum.common.util.survey.ISurveyInfo;
import flaxbeard.immersivepetroleum.common.util.survey.IslandInfo;
import flaxbeard.immersivepetroleum.common.util.survey.SurveyScan;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;

/**
 * @author TwistedGate
 */
public class SurveyResultItem extends IPItemBase{
	public SurveyResultItem(){
		super(new Item.Properties().stacksTo(1));
	}
	
	@Override
	@Nonnull
	public Component getName(@Nonnull ItemStack stack){
		String selfKey = getDescriptionId(stack);
		return Component.translatable(selfKey).withStyle(ChatFormatting.GOLD);
	}
	
	@Override
	public void appendHoverText(ItemStack stack, Level worldIn, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flagIn){
		if(stack.hasTag() && stack.getTag() != null){
			ISurveyInfo info = ISurveyInfo.from(stack);
			
			if(info instanceof SurveyScan scan){
				if(scan.getUuid() == null){
					tooltip.add(Component.literal("SORRY, IM FAULTY!").withStyle(ChatFormatting.RED));
					tooltip.add(Component.literal("YOU'LL HAVE TO TOSS ME!").withStyle(ChatFormatting.RED));
					return;
				}
				
				tooltip.add(Component.translatable("desc.immersivepetroleum.flavour.surveytool.holdme"));
				
				if(flagIn == TooltipFlag.Default.ADVANCED){
					tooltip.add(Component.literal("ID: " + (scan.getUuid() != null ? scan.getUuid().toString() : "Null")));
					tooltip.add(Component.literal("dSize: " + (scan.getData() != null ? scan.getData().length : "Null")));
				}
			}
			
			if(info instanceof IslandInfo islandInfo){
				int expected = islandInfo.getExpected();
				long amount = islandInfo.getAmount();
				byte percentage = islandInfo.getStatus();
				FluidStack fs = islandInfo.getFluidStack();
				
				if(islandInfo.getFluidStack() == FluidStack.EMPTY){
					tooltip.add(Component.literal("SORRY, IM FAULTY!").withStyle(ChatFormatting.RED));
					tooltip.add(Component.literal("YOU'LL HAVE TO TOSS ME!").withStyle(ChatFormatting.RED));
					return;
				}
				
				tooltip.add(Component.translatable(fs.getTranslationKey()).withStyle(ChatFormatting.DARK_GRAY));
				tooltip.add(Component.translatable("desc.immersivepetroleum.info.survey_result.amount", String.format(Locale.ENGLISH, "%,.3f", amount / 1000D), percentage).withStyle(ChatFormatting.DARK_GRAY));
				tooltip.add(Component.translatable("desc.immersivepetroleum.info.survey_result.expected", expected).withStyle(ChatFormatting.DARK_GRAY));
				
			}
			
			if(info != null){
				int x = info.getX();
				int z = info.getZ();
				
				tooltip.add(Component.translatable("desc.immersivepetroleum.flavour.surveytool.location", x, z).withStyle(ChatFormatting.DARK_GRAY));
			}
		}
	}
}
