import re
import shutil
import os

# before need to patch build.gradle
# replace
#

# get all java files
java_files = []
for root, dirs, files in os.walk("src"):
    for file in files:
        if file.endswith(".java"):
            java_files.append(os.path.join(root, file))

print(java_files)

function_patch = []

# patch function
@function_patch.append
def rename_WorldEvent(chr: str)->str:
    return chr.replace("WorldEvent", "LevelEvent")

@function_patch.append
def change_getItemStackLimit(chr: str)->str:
    return chr.replace("getItemStackLimit", "getMaxStackSize")

@function_patch.append
def change_ThingComponent(chr: str)->str:
    thingcomponent_replacement = ["TranslatableComponent","TextComponent","KeybindComponent"]
    if any(x in chr for x in thingcomponent_replacement):
        if "import net.minecraft.network.chat.Component;" in chr:
            for x in thingcomponent_replacement:
                chr = chr.replace(f"import net.minecraft.network.chat.{x};\n", "")
        else:
            for x in thingcomponent_replacement:
                chr = chr.replace(f"import net.minecraft.network.chat.{x};", "import net.minecraft.network.chat.Component;")
        chr = chr.replace("new TranslatableComponent", "Component.translatable")
        chr = chr.replace("new TextComponent", "Component.literal")
        chr = chr.replace("new KeybindComponent", "Component.keybind")
    chr = chr.replace("TranslatableComponent", "Component")
    chr = chr.replace("TextComponent", "Component")
    chr = chr.replace("KeybindComponent", "Component")
    return chr

@function_patch.append
def fix_change_fluid_api(chr: str)->str:
    change = {
        "FluidAttributes.Builder": "FluidType.Properties",
        "FluidAttributes": "FluidType",
    }
    # for more info, see
    # https://forge.gemwire.uk/wiki/User:ChampionAsh5357/Sandbox/Fluids_API
    for x in change:
        chr = chr.replace(x, change[x])
    return chr

# not perfect, but it works
@function_patch.append
def change_getRegistryName(chr: str)->str:
    all_var = re.findall(r"(\w+)\.getRegistryName\(\)", chr)
    if len(all_var) == 0:
        return chr
    print(all_var)
    for x in all_var:
        type_ = re.findall(rf"(\w+) {x}(?!\w)", chr)[0]
        print(type_, x)
        match type_:
            case "Block" | "StairBlock":
                chr = chr.replace(f"{x}.getRegistryName()", f"ForgeRegistries.BLOCKS.getKey({x})")
            case "Item":
                chr = chr.replace(f"{x}.getRegistryName()", f"ForgeRegistries.ITEMS.getKey({x})")
            case "Fluid":
                chr = chr.replace(f"{x}.getRegistryName()", f"ForgeRegistries.FLUIDS.getKey({x})")
            case "Biome":
                chr = chr.replace(f"{x}.getRegistryName()", f" level.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY).getKey({x})")
    
    return chr

@function_patch.append
def change_asthing(chr: str)->str:
    chr = re.sub(r"(\w+)\.asItem\(\)\.getRegistryName\(\)", r"ForgeRegistries.ITEMS.getKey(\1.asItem())", chr)
    chr = re.sub(r"(\w+)\.getItem\(\)\.getRegistryName\(\)", r"ForgeRegistries.ITEMS.getKey(\1.getItem())", chr)
    chr = re.sub(r"(\w+)\.getFluid\(\)\.getRegistryName\(\)", r"ForgeRegistries.BLOCKS.getKey(\1.getFluid())", chr)
    return chr

@function_patch.append
def change_OBJLoaderBuilder(chr: str)->str:
    return chr.replace("OBJLoaderBuilder", "ObjModelBuilder")

@function_patch.append
def change_some_imports(chr: str)->str:
    return chr.replace("import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;", "import net.minecraftforge.data.event.GatherDataEvent;")

@function_patch.append
def some_generator_change(chr: str)->str:
    all_var = re.findall(r"DataGenerator (\w+) = event\.getGenerator\(\);", chr)
    if len(all_var) == 0:
        return chr
    event_variable_name = re.findall(r"GatherDataEvent (\w+)", chr)[0]
    print(all_var)
    for x in all_var:
        chr = re.sub(rf"{x}.addProvider\(([\w (,)]+)\)", rf"{x}.addProvider({event_variable_name}.includeServer(), \1)", chr)
    return chr

# TODO: modify ColumnPos
# ColumnPos(pos) -> ColumnPos(pos.getX(), pos.getZ())
# pos.x -> ColumnPos.getX(pos.toLong())
# pos.z -> ColumnPos.getZ(pos.toLong())

# TODO: modify Ressource
# Ressource.getInputStream -> Resource.open

# TODO: modify getStillTexture
# ForgeRegistries.FLUIDS.getKey(still).getStillTexture() -> IClientFluidTypeExtensions.of(still).getStillTexture()

# TODO: modify event.getType() == RenderGuiOverlayEvent.ElementType.TEXT
# RenderGuiOverlayEvent.Post -> CustomizeGuiOverlayEvent.DebugText
# remove event.getType() == RenderGuiOverlayEvent.ElementType.TEXT

# TODO: modify ForgeModelBakery.addSpecialModel
# ForgeModelBakery.addSpecialModel -> ModelBakery.addSpecialModel
# register the function
# ModelEvent.RegisterAdditional event in arg
# ForgeModelBakery.addSpecialModel -> event.register

@function_patch.append
def change_detectCullableFaces(chr: str)->str:
    return chr.replace("detectCullableFaces", "automaticCulling")

@function_patch.append
def change_RenderGameOverlayEvent(chr: str)->str:
    return chr.replace("RenderGameOverlayEvent", "RenderGuiOverlayEvent")

@function_patch.append
def change_getMatrixStack(chr: str)->str:
    return chr.replace("getMatrixStack", "getPoseStack")

# not sure
@function_patch.append
def change_DynamicFluidContainerModelBuilder(chr: str)->str:
    return chr.replace("BucketModelBuilder","DynamicFluidContainerModelBuilder")

@function_patch.append
def change_EmptyModelData(chr: str)->str:
    return chr.replace("EmptyModelData.INSTANCE", "ModelData.EMPTY")

@function_patch.append
def change_TextComponent_Empty(chr: str)->str:
    return chr.replace("TextComponent.EMPTY", "Component.nullToEmpty(null)")

@function_patch.append
def change_RenderBlockOverlayEvent(chr: str)->str:
    return chr.replace("RenderBlockOverlayEvent", "RenderBlockScreenEffectEvent")

@function_patch.append
def change_ModelRegistryEvent(chr: str)->str:
    chr = chr.replace("ModelRegistryEvent;", "ModelEvent.RegisterGeometryLoaders;")
    chr = chr.replace("ModelRegistryEvent", "RegisterGeometryLoaders")
    return chr

@function_patch.append
def change_fluid_getAttributes(chr: str)->str:
    return re.sub(r"(\w+)\.getAttributes\(\)", r"IClientFluidTypeExtensions.of(\1)", chr)

@function_patch.append
def change_nodrops(chr: str)->str:
    return chr.replace("noDrops", "noLootTable")

@function_patch.append
def change_WorldTickEvent(chr: str)->str:
    return chr.replace("WorldTickEvent", "LevelTickEvent")

@function_patch.append
def change_getWorld(chr: str)->str:
    return chr.replace("getWorld", "getLevel")

@function_patch.append
def change_getEntityLiving(chr: str)->str:
    return chr.replace("getEntityLiving", "getEntity")

@function_patch.append
def change_getLevelObj(chr: str)->str:
    return chr.replace("getLevelObj", "getLevel")

@function_patch.append
def change_ParticleFactoryRegisterEvent(chr: str)->str:
    return chr.replace("ParticleFactoryRegisterEvent", "RegisterParticleProvidersEvent")

@function_patch.append
def change_event_RegistryEvent(chr: str)->str:
    chr = chr.replace("event.RegistryEvent", "registries.RegisterEvent")
    chr = chr.replace("RegistryEvent", "RegisterEvent")
    return chr

@function_patch.append
def change_ForgeRegistries(chr: str)->str:
    chr = chr.replace("ForgeRegistries.BLOCK_ENTITIES", "ForgeRegistries.BLOCK_ENTITY_TYPES")
    chr = chr.replace("ForgeRegistries.ENTITIES", "ForgeRegistries.ENTITY_TYPES")
    chr = chr.replace("ForgeRegistries.CONTAINERS", "ForgeRegistries.MENU_TYPES")
    return chr

@function_patch.append
def change_EffectRenderer(chr: str)->str:
    chr = chr.replace("net.minecraftforge.client.EffectRenderer", "net.minecraftforge.client.extensions.common.IClientMobEffectExtensions")
    return chr.replace("EffectRenderer", "IClientMobEffectExtensions")

@function_patch.append
def change_MouseScrollEvent(chr: str)->str:
    return chr.replace("MouseScrollEvent", "MouseScrollingEvent")

@function_patch.append
def change_KeyInputEvent(chr: str)->str:
    return chr.replace("KeyInputEvent", "Key")

@function_patch.append
def change_IModelData(chr: str)->str:
    return chr.replace("IModelData", "ModelData")

# TODO: depht change registry event
# register(evt.getRegistry(), new WispParticleType(), "wisp");
# register(evt.getRegistry(), new SparkleParticleType(), "sparkle");
# ->
# evt.register(ForgeRegistries.Keys.PARTICLE_TYPES, helper -> {
#   helper.register(new ResourceLocation(LibMisc.MOD_ID, "wisp"), new WispParticleType());
#   helper.register(new ResourceLocation(LibMisc.MOD_ID, "sparkle"), new SparkleParticleType());
# });
# 

# immmersive engeering only
# IReadOnPlacement -> IPlacementInteraction

# maybe readOnPlacement is onBEPlaced

for file in java_files:
    print(f"Processing {file}")
    with open(file, "r") as f:
        content = f.read()
    for x in function_patch:
        content = x(content)
    with open(file, "w") as f:
        f.write(content)