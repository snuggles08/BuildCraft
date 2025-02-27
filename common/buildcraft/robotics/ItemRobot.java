/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 * <p/>
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.robotics;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import cofh.api.energy.IEnergyContainerItem;
import buildcraft.BuildCraftRobotics;
import buildcraft.api.boards.RedstoneBoardNBT;
import buildcraft.api.boards.RedstoneBoardRegistry;
import buildcraft.api.boards.RedstoneBoardRobotNBT;
import buildcraft.api.events.RobotEvent;
import buildcraft.api.robots.DockingStation;
import buildcraft.api.robots.EntityRobotBase;
import buildcraft.api.transport.pluggable.PipePluggable;
import buildcraft.core.BCCreativeTab;
import buildcraft.core.lib.items.ItemBuildCraft;
import buildcraft.core.lib.utils.BCStringUtils;
import buildcraft.core.lib.utils.NBTUtils;
import buildcraft.transport.BlockGenericPipe;
import buildcraft.transport.Pipe;

public class ItemRobot extends ItemBuildCraft implements IEnergyContainerItem {

    public ItemRobot() {
        super(BCCreativeTab.get("boards"));
        setMaxStackSize(1);
    }

    @Override
    public int getItemStackLimit(ItemStack stack) {
        NBTTagCompound cpt = getNBT(stack);
        RedstoneBoardRobotNBT boardNBT = getRobotNBT(cpt);

        if (boardNBT != RedstoneBoardRegistry.instance.getEmptyRobotBoard()) {
            return 1;
        } else {
            return 16;
        }
    }

    public EntityRobot createRobot(ItemStack stack, World world) {
        try {
            NBTTagCompound nbt = getNBT(stack);

            RedstoneBoardRobotNBT robotNBT = getRobotNBT(nbt);
            if (robotNBT == RedstoneBoardRegistry.instance.getEmptyRobotBoard()) {
                return null;
            }
            EntityRobot robot = new EntityRobot(world, robotNBT);
            robot.getBattery().setEnergy(getEnergy(nbt));

            return robot;
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    public static RedstoneBoardRobotNBT getRobotNBT(ItemStack stack) {
        return getRobotNBT(getNBT(stack));
    }

    public static int getEnergy(ItemStack stack) {
        return getEnergy(getNBT(stack));
    }

    public ResourceLocation getTextureRobot(ItemStack stack) {
        return getRobotNBT(stack).getRobotTexture();
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> list, boolean advanced) {
        NBTTagCompound cpt = getNBT(stack);
        RedstoneBoardRobotNBT boardNBT = getRobotNBT(cpt);

        if (boardNBT != RedstoneBoardRegistry.instance.getEmptyRobotBoard()) {
            boardNBT.addInformation(stack, player, list, advanced);

            int energy = getEnergy(cpt);
            int pct = energy * 100 / EntityRobotBase.MAX_ENERGY;
            String enInfo = pct + "% " + BCStringUtils.localize("tip.gate.charged");
            if (energy == EntityRobotBase.MAX_ENERGY) {
                enInfo = BCStringUtils.localize("tip.gate.fullcharge");
            } else if (energy == 0) {
                enInfo = BCStringUtils.localize("tip.gate.nocharge");
            }
            enInfo = (pct >= 80 ? EnumChatFormatting.GREEN : (pct >= 50 ? EnumChatFormatting.YELLOW : (pct >= 30 ? EnumChatFormatting.GOLD
                : (pct >= 20 ? EnumChatFormatting.RED : EnumChatFormatting.DARK_RED)))) + enInfo;
            list.add(enInfo);
        }
    }

    public static ItemStack createRobotStack(RedstoneBoardRobotNBT board, int energy) {
        ItemStack robot = new ItemStack(BuildCraftRobotics.robotItem);
        NBTTagCompound boardCpt = new NBTTagCompound();
        board.createBoard(boardCpt);
        NBTUtils.getItemData(robot).setTag("board", boardCpt);
        NBTUtils.getItemData(robot).setInteger("energy", energy);
        return robot;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item item, CreativeTabs par2CreativeTabs, List itemList) {
        itemList.add(createRobotStack(RedstoneBoardRegistry.instance.getEmptyRobotBoard(), 0));

        for (RedstoneBoardNBT boardNBT : RedstoneBoardRegistry.instance.getAllBoardNBTs()) {
            if (boardNBT instanceof RedstoneBoardRobotNBT) {
                RedstoneBoardRobotNBT robotNBT = (RedstoneBoardRobotNBT) boardNBT;
                itemList.add(createRobotStack(robotNBT, 0));
                itemList.add(createRobotStack(robotNBT, EntityRobotBase.MAX_ENERGY));
            }
        }
    }

    @Override
    public int receiveEnergy(ItemStack container, int maxReceive, boolean simulate) {
        NBTTagCompound cpt = getNBT(container);
        if (getRobotNBT(cpt) == RedstoneBoardRegistry.instance.getEmptyRobotBoard()) {
            return 0;
        }
        int currentEnergy = getEnergy(cpt);
        int energyReceived = Math.min(EntityRobotBase.MAX_ENERGY - currentEnergy, maxReceive);
        if (!simulate) {
            setEnergy(cpt, currentEnergy + energyReceived);
        }
        return energyReceived;
    }

    @Override
    public int extractEnergy(ItemStack container, int maxExtract, boolean simulate) {
        NBTTagCompound cpt = getNBT(container);
        if (getRobotNBT(cpt) == RedstoneBoardRegistry.instance.getEmptyRobotBoard()) {
            return 0;
        }
        int currentEnergy = getEnergy(cpt);
        int energyExtracted = Math.min(currentEnergy, maxExtract);
        if (!simulate) {
            setEnergy(cpt, currentEnergy - energyExtracted);
        }
        return energyExtracted;
    }

    @Override
    public int getEnergyStored(ItemStack container) {
        return getEnergy(container);
    }

    @Override
    public int getMaxEnergyStored(ItemStack container) {
        if (getRobotNBT(container) == RedstoneBoardRegistry.instance.getEmptyRobotBoard()) {
            return 0;
        }
        return EntityRobotBase.MAX_ENERGY;
    }

    @Override
    public boolean onItemUse(ItemStack currentItem, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY,
            float hitZ) {
        if (!world.isRemote) {
            Block b = world.getBlockState(pos).getBlock();
            if (!(b instanceof BlockGenericPipe)) {
                return false;
            }

            Pipe<?> pipe = BlockGenericPipe.getPipe(world, pos);
            if (pipe == null) {
                return false;
            }

            // BlockGenericPipe pipeBlock = (BlockGenericPipe) b;
            // BlockGenericPipe.RaytraceResult rayTraceResult = pipeBlock.doRayTrace(world, pos, player);

            PipePluggable pluggable = pipe.container.getPipePluggable(side);

            if (pluggable instanceof RobotStationPluggable) {
                RobotStationPluggable robotPluggable = (RobotStationPluggable) pluggable;
                DockingStation station = robotPluggable.getStation();

                if (!station.isTaken()) {
                    RedstoneBoardRobotNBT robotNBT = ItemRobot.getRobotNBT(currentItem);
                    if (robotNBT == RedstoneBoardRegistry.instance.getEmptyRobotBoard()) {
                        return true;
                    }

                    EntityRobot robot = ((ItemRobot) currentItem.getItem()).createRobot(currentItem, world);

                    RobotEvent.Place robotEvent = new RobotEvent.Place(robot, player);
                    MinecraftForge.EVENT_BUS.post(robotEvent);
                    if (robotEvent.isCanceled()) {
                        return true;
                    }

                    if (robot != null && robot.getRegistry() != null) {
                        robot.setUniqueRobotId(robot.getRegistry().getNextRobotId());

                        float px = pos.getX() + 0.5F + side.getFrontOffsetX() * 0.5F;
                        float py = pos.getY() + 0.5F + side.getFrontOffsetY() * 0.5F;
                        float pz = pos.getZ() + 0.5F + side.getFrontOffsetZ() * 0.5F;

                        robot.setPosition(px, py, pz);
                        station.takeAsMain(robot);
                        robot.dock(robot.getLinkedStation());
                        world.spawnEntityInWorld(robot);

                        if (!player.capabilities.isCreativeMode) {
                            player.getCurrentEquippedItem().stackSize--;
                        }
                    }
                }

                return true;
            }
        }
        return false;
    }

    private static NBTTagCompound getNBT(ItemStack stack) {
        NBTTagCompound cpt = NBTUtils.getItemData(stack);
        if (!cpt.hasKey("board")) {
            RedstoneBoardRegistry.instance.getEmptyRobotBoard().createBoard(cpt);
        }
        return cpt;
    }

    private static RedstoneBoardRobotNBT getRobotNBT(NBTTagCompound cpt) {
        NBTTagCompound boardCpt = cpt.getCompoundTag("board");
        return (RedstoneBoardRobotNBT) RedstoneBoardRegistry.instance.getRedstoneBoard(boardCpt);
    }

    private static int getEnergy(NBTTagCompound cpt) {
        return cpt.getInteger("energy");
    }

    private static void setEnergy(NBTTagCompound cpt, int energy) {
        cpt.setInteger("energy", energy);
    }
}
