/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 * <p/>
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.core.lib.fluids;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.google.common.collect.ForwardingList;
import io.netty.buffer.ByteBuf;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Provides a simple way to save+load and send+recieve data for any number of tanks. This also attempts to fill all of
 * the tanks one by one via the {@link #fill(EnumFacing, FluidStack, boolean)} and
 * {@link #drain(EnumFacing, FluidStack, boolean)} methods. */
public class TankManager<T extends Tank> extends ForwardingList<T> implements IFluidHandler, INBTSerializable<NBTTagCompound> {

    private List<T> tanks = new ArrayList<>();

    public TankManager() {}

    public TankManager(T... tanks) {
        addAll(Arrays.asList(tanks));
    }

    @Override
    protected List<T> delegate() {
        return tanks;
    }

    @Override
    public int fill(EnumFacing from, FluidStack resource, boolean doFill) {
        for (Tank tank : tanks) {
            int used = tank.fill(resource, doFill);
            if (used > 0) {
                return used;
            }
        }
        return 0;
    }

    @Override
    public FluidStack drain(EnumFacing from, FluidStack resource, boolean doDrain) {
        if (resource == null) {
            return null;
        }
        for (Tank tank : tanks) {
            if (!resource.isFluidEqual(tank.getFluid())) {
                continue;
            }
            FluidStack drained = tank.drain(resource.amount, doDrain);
            if (drained != null && drained.amount > 0) {
                return drained;
            }
        }
        return null;
    }

    @Override
    public FluidStack drain(EnumFacing from, int maxDrain, boolean doDrain) {
        for (Tank tank : tanks) {
            FluidStack drained = tank.drain(maxDrain, doDrain);
            if (drained != null && drained.amount > 0) {
                return drained;
            }
        }
        return null;
    }

    @Override
    public boolean canFill(EnumFacing from, Fluid fluid) {
        return true;
    }

    @Override
    public boolean canDrain(EnumFacing from, Fluid fluid) {
        return true;
    }

    @Override
    public FluidTankInfo[] getTankInfo(EnumFacing from) {
        FluidTankInfo[] info = new FluidTankInfo[size()];
        for (int i = 0; i < size(); i++) {
            info[i] = get(i).getInfo();
        }
        return info;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        for (Tank t : tanks) {
            nbt.setTag(t.getTankName(), t.serializeNBT());
        }
        return nbt;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        for (Tank t : tanks) {
            t.deserializeNBT(nbt.getCompoundTag(t.getTankName()));
        }
    }

    @Deprecated
    public void writeToNBT(NBTTagCompound data) {
        for (Tank tank : tanks) {
            tank.writeToNBT(data);
        }
    }

    @Deprecated
    public void readFromNBT(NBTTagCompound data) {
        for (Tank tank : tanks) {
            tank.readFromNBT(data);
        }
    }

    public void writeData(ByteBuf data) {
        PacketBuffer packet = new PacketBuffer(data);
        for (Tank tank : tanks) {
            FluidStack fluidStack = tank.getFluid();
            if (fluidStack != null && fluidStack.getFluid() != null) {
                packet.writeString(fluidStack.getFluid().getName());
                packet.writeInt(fluidStack.amount);
                packet.writeInt(fluidStack.getFluid().getColor(fluidStack));
            } else {
                packet.writeString("~");
            }
        }
    }

    @SideOnly(Side.CLIENT)
    public void readData(ByteBuf data) {
        PacketBuffer packet = new PacketBuffer(data);
        for (Tank tank : tanks) {
            String fluidId = packet.readStringFromBuffer(40);
            if (FluidRegistry.getFluid(fluidId) != null) {
                tank.setFluid(new FluidStack(FluidRegistry.getFluid(fluidId), data.readInt()));
                tank.colorRenderCache = data.readInt();
            } else {
                tank.setFluid(null);
                tank.colorRenderCache = 0xFFFFFF;
            }
        }
    }
}
