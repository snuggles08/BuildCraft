/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 * <p/>
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Achievement;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInterModComms;
import net.minecraftforge.fml.common.event.FMLMissingMappingsEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;

import buildcraft.api.recipes.BuildcraftRecipeRegistry;
import buildcraft.core.BCRegistry;
import buildcraft.core.CompatHooks;
import buildcraft.core.DefaultProps;
import buildcraft.core.InterModComms;
import buildcraft.core.config.ConfigManager;
import buildcraft.core.lib.items.ItemBuildCraft;
import buildcraft.core.lib.network.base.ChannelHandler;
import buildcraft.core.lib.network.base.PacketHandler;
import buildcraft.silicon.BlockLaser;
import buildcraft.silicon.BlockLaserTable;
import buildcraft.silicon.ItemLaserTable;
import buildcraft.silicon.ItemRedstoneChipset;
import buildcraft.silicon.ItemRedstoneChipset.Chipset;
import buildcraft.silicon.SiliconGuiHandler;
import buildcraft.silicon.SiliconProxy;
import buildcraft.silicon.TileAdvancedCraftingTable;
import buildcraft.silicon.TileAssemblyTable;
import buildcraft.silicon.TileChargingTable;
import buildcraft.silicon.TileIntegrationTable;
import buildcraft.silicon.TileLaser;
import buildcraft.silicon.TileProgrammingTable;

@Mod(name = "BuildCraft Silicon", version = DefaultProps.VERSION, useMetadata = false, modid = "BuildCraft|Silicon",
        dependencies = DefaultProps.DEPENDENCY_CORE)
public class BuildCraftSilicon extends BuildCraftMod {
    @Mod.Instance("BuildCraft|Silicon")
    public static BuildCraftSilicon instance;

    public static ItemRedstoneChipset redstoneChipset;
    public static BlockLaser laserBlock;
    public static BlockLaserTable assemblyTableBlock;
    public static Item redstoneCrystal;

    public static Achievement timeForSomeLogicAchievement;
    public static Achievement tinglyLaserAchievement;

    public static float chipsetCostMultiplier = 1.0F;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent evt) {
        BuildCraftCore.mainConfigManager.register("power.chipsetCostMultiplier", 1.0D, "The cost multiplier for Chipsets",
                ConfigManager.RestartRequirement.GAME);
        BuildCraftCore.mainConfiguration.save();
        chipsetCostMultiplier = (float) BuildCraftCore.mainConfigManager.get("power.chipsetCostMultiplier").getDouble();

        if (BuildCraftCore.mainConfiguration.hasChanged()) {
            BuildCraftCore.mainConfiguration.save();
        }

        laserBlock = (BlockLaser) CompatHooks.INSTANCE.getBlock(BlockLaser.class);
        laserBlock.setUnlocalizedName("laserBlock");
        BCRegistry.INSTANCE.registerBlock(laserBlock, false);

        assemblyTableBlock = (BlockLaserTable) CompatHooks.INSTANCE.getBlock(BlockLaserTable.class);
        assemblyTableBlock.setUnlocalizedName("laserTableBlock");
        BCRegistry.INSTANCE.registerBlock(assemblyTableBlock, ItemLaserTable.class, false);

        redstoneChipset = new ItemRedstoneChipset();
        redstoneChipset.setUnlocalizedName("redstoneChipset");
        BCRegistry.INSTANCE.registerItem(redstoneChipset, false);
        redstoneChipset.registerItemStacks();

        redstoneCrystal = (new ItemBuildCraft()).setUnlocalizedName("redstoneCrystal");
        if (BCRegistry.INSTANCE.registerItem(redstoneCrystal, false)) {
            OreDictionary.registerOre("redstoneCrystal", new ItemStack(redstoneCrystal)); // Deprecated
            OreDictionary.registerOre("crystalRedstone", new ItemStack(redstoneCrystal));
        }

        SiliconProxy.proxy.preInit();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent evt) {
        channels = NetworkRegistry.INSTANCE.newChannel(DefaultProps.NET_CHANNEL_NAME + "-SILICON", new ChannelHandler(), new PacketHandler());

        NetworkRegistry.INSTANCE.registerGuiHandler(instance, new SiliconGuiHandler());
        BCRegistry.INSTANCE.registerTileEntity(TileLaser.class, "buildcraft.silicon.TileLaser", "net.minecraft.src.buildcraft.factory.TileLaser");
        BCRegistry.INSTANCE.registerTileEntity(TileAssemblyTable.class, "buildcraft.silicon.TileAssemblyTable",
                "net.minecraft.src.buildcraft.factory.TileAssemblyTable");
        BCRegistry.INSTANCE.registerTileEntity(TileAdvancedCraftingTable.class, "buildcraft.silicon.TileAdvancedCraftingTable",
                "net.minecraft.src.buildcraft.factory.TileAssemblyAdvancedWorkbench");
        BCRegistry.INSTANCE.registerTileEntity(TileIntegrationTable.class, "buildcraft.silicon.TileIntegrationTable",
                "net.minecraft.src.buildcraft.factory.TileIntegrationTable");
        BCRegistry.INSTANCE.registerTileEntity(TileChargingTable.class, "buildcraft.silicon.TileChargingTable",
                "net.minecraft.src.buildcraft.factory.TileChargingTable");
        BCRegistry.INSTANCE.registerTileEntity(TileProgrammingTable.class, "buildcraft.silicon.TileProgrammingTable",
                "net.minecraft.src.buildcraft.factory.TileProgrammingTable");

        timeForSomeLogicAchievement = BuildCraftCore.achievementManager.registerAchievement(new Achievement(
                "buildcraft|silicon:achievement.timeForSomeLogic", "timeForSomeLogicAchievement", 9, -2, assemblyTableBlock,
                BuildCraftCore.diamondGearAchievement));
        tinglyLaserAchievement = BuildCraftCore.achievementManager.registerAchievement(new Achievement("buildcraft|silicon:achievement.tinglyLaser",
                "tinglyLaserAchievement", 11, -2, laserBlock, timeForSomeLogicAchievement));

        if (BuildCraftCore.loadDefaultRecipes) {
            loadRecipes();
        }

        SiliconProxy.proxy.registerRenderers();
    }

    public static void loadRecipes() {

        // TABLES
        BCRegistry.INSTANCE.addCraftingRecipe(new ItemStack(laserBlock), "ORR", "DDR", "ORR", 'O', Blocks.obsidian, 'R', "dustRedstone", 'D',
                "gemDiamond");

        BCRegistry.INSTANCE.addCraftingRecipe(new ItemStack(laserBlock), "RRO", "RDD", "RRO", 'O', Blocks.obsidian, 'R', "dustRedstone", 'D',
                "gemDiamond");

        BCRegistry.INSTANCE.addCraftingRecipe(new ItemStack(laserBlock), "RRR", "RDR", "ODO", 'O', Blocks.obsidian, 'R', "dustRedstone", 'D',
                "gemDiamond");

        BCRegistry.INSTANCE.addCraftingRecipe(new ItemStack(laserBlock), "ODO", "RDR", "RRR", 'O', Blocks.obsidian, 'R', "dustRedstone", 'D',
                "gemDiamond");

        BCRegistry.INSTANCE.addCraftingRecipe(new ItemStack(assemblyTableBlock, 1, 0), "ODO", "ORO", "OGO", 'O', Blocks.obsidian, 'R', "dustRedstone",
                'D', "gemDiamond", 'G', "gearDiamond");

        BCRegistry.INSTANCE.addCraftingRecipe(new ItemStack(assemblyTableBlock, 1, 1), "OWO", "OCO", "ORO", 'O', Blocks.obsidian, 'W',
                Blocks.crafting_table, 'C', Blocks.chest, 'R', new ItemStack(redstoneChipset, 1, 0));

        BCRegistry.INSTANCE.addCraftingRecipe(new ItemStack(assemblyTableBlock, 1, 2), "OIO", "OCO", "OGO", 'O', Blocks.obsidian, 'I', "ingotGold",
                'C', new ItemStack(redstoneChipset, 1, 0), 'G', "gearDiamond");

        BCRegistry.INSTANCE.addCraftingRecipe(new ItemStack(assemblyTableBlock, 1, 3), "OIO", "OCO", "OGO", 'O', Blocks.obsidian, 'I', "dustRedstone",
                'C', new ItemStack(redstoneChipset, 1, 0), 'G', "gearGold");

        BCRegistry.INSTANCE.addCraftingRecipe(new ItemStack(assemblyTableBlock, 1, 4), "OCO", "ORO", "OGO", 'O', Blocks.obsidian, 'R', new ItemStack(
                redstoneChipset, 1, 0), 'C', "gemEmerald", 'G', "gearDiamond");

        // BCRegistry.INSTANCE.addCraftingRecipe(new ItemStack(assemblyTableBlock, 1, 5), "OWO", "ORO", "OGO", 'O',
        // Blocks.obsidian, 'W',
        // "craftingTableWood", 'G', "gearGold", 'R', new ItemStack(redstoneChipset, 1, 0));

        // BCRegistry.INSTANCE.addCraftingRecipe(new ItemStack(packagerBlock, 1, 0), " I ", "ICI", " P ", 'I',
        // "ingotIron", 'C', "craftingTableWood",
        // 'P', Blocks.piston);

        // CHIPSETS
        BuildcraftRecipeRegistry.assemblyTable.addRecipe("buildcraft:redstoneChipset", Math.round(100000 * chipsetCostMultiplier), Chipset.RED
                .getStack(), "dustRedstone");
        BuildcraftRecipeRegistry.assemblyTable.addRecipe("buildcraft:ironChipset", Math.round(200000 * chipsetCostMultiplier), Chipset.IRON
                .getStack(), "dustRedstone", "ingotIron");
        BuildcraftRecipeRegistry.assemblyTable.addRecipe("buildcraft:goldChipset", Math.round(400000 * chipsetCostMultiplier), Chipset.GOLD
                .getStack(), "dustRedstone", "ingotGold");
        BuildcraftRecipeRegistry.assemblyTable.addRecipe("buildcraft:diamondChipset", Math.round(800000 * chipsetCostMultiplier), Chipset.DIAMOND
                .getStack(), "dustRedstone", "gemDiamond");
        BuildcraftRecipeRegistry.assemblyTable.addRecipe("buildcraft:emeraldChipset", Math.round(1200000 * chipsetCostMultiplier), Chipset.EMERALD
                .getStack(), "dustRedstone", "gemEmerald");
        BuildcraftRecipeRegistry.assemblyTable.addRecipe("buildcraft:pulsatingChipset", Math.round(400000 * chipsetCostMultiplier), Chipset.PULSATING
                .getStack(2), "dustRedstone", Items.ender_pearl);
        BuildcraftRecipeRegistry.assemblyTable.addRecipe("buildcraft:quartzChipset", Math.round(600000 * chipsetCostMultiplier), Chipset.QUARTZ
                .getStack(), "dustRedstone", "gemQuartz");
        BuildcraftRecipeRegistry.assemblyTable.addRecipe("buildcraft:compChipset", Math.round(600000 * chipsetCostMultiplier), Chipset.COMP
                .getStack(), "dustRedstone", Items.comparator);

        // ROBOTS AND BOARDS
        BuildcraftRecipeRegistry.assemblyTable.addRecipe("buildcraft:redstoneCrystal", 10000000, new ItemStack(redstoneCrystal), new ItemStack(
                Blocks.redstone_block));
    }

    @Mod.EventHandler
    public void processIMCRequests(FMLInterModComms.IMCEvent event) {
        InterModComms.processIMC(event);
    }

    @Mod.EventHandler
    public void whiteListAppliedEnergetics(FMLInitializationEvent event) {
        FMLInterModComms.sendMessage("appliedenergistics2", "whitelist-spatial", TileLaser.class.getCanonicalName());
        FMLInterModComms.sendMessage("appliedenergistics2", "whitelist-spatial", TileAssemblyTable.class.getCanonicalName());
        FMLInterModComms.sendMessage("appliedenergistics2", "whitelist-spatial", TileAdvancedCraftingTable.class.getCanonicalName());
        FMLInterModComms.sendMessage("appliedenergistics2", "whitelist-spatial", TileIntegrationTable.class.getCanonicalName());
    }

    @Mod.EventHandler
    public void remap(FMLMissingMappingsEvent event) {
        for (FMLMissingMappingsEvent.MissingMapping mapping : event.get()) {
            if (mapping.name.equals("BuildCraft|Silicon:null")) {
                if (mapping.type == GameRegistry.Type.ITEM) {
                    mapping.remap(Item.getItemFromBlock(assemblyTableBlock));
                } else {
                    mapping.remap(assemblyTableBlock);
                }
            }

            // Silicon -> Robotics migration code
            if (mapping.type == GameRegistry.Type.ITEM) {
                if (mapping.name.equals("BuildCraft|Silicon:robot")) {
                    mapping.remap((Item) Item.itemRegistry.getObject(new ResourceLocation("BuildCraft|Robotics:robot")));
                } else if (mapping.name.equals("BuildCraft|Silicon:redstone_board")) {
                    mapping.remap((Item) Item.itemRegistry.getObject(new ResourceLocation("BuildCraft|Robotics:redstone_board")));
                } else if (mapping.name.equals("BuildCraft|Silicon:requester")) {
                    mapping.remap((Item) Item.itemRegistry.getObject(new ResourceLocation("BuildCraft|Robotics:requester")));
                } else if (mapping.name.equals("BuildCraft|Silicon:zonePlan")) {
                    mapping.remap((Item) Item.itemRegistry.getObject(new ResourceLocation("BuildCraft|Robotics:zonePlan")));
                }
            } else if (mapping.type == GameRegistry.Type.BLOCK) {
                if (mapping.name.equals("BuildCraft|Silicon:requester")) {
                    mapping.remap(Block.getBlockFromName("BuildCraft|Robotics:requester"));
                } else if (mapping.name.equals("BuildCraft|Silicon:zonePlan")) {
                    mapping.remap(Block.getBlockFromName("BuildCraft|Robotics:zonePlan"));
                }
            }
        }
    }
}
