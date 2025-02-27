/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 * <p/>
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.core.list;

import io.netty.buffer.ByteBuf;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;

import buildcraft.BuildCraftCore;
import buildcraft.core.ItemList;
import buildcraft.core.lib.gui.BuildCraftContainer;
import buildcraft.core.lib.network.command.CommandWriter;
import buildcraft.core.lib.network.command.ICommandReceiver;
import buildcraft.core.lib.network.command.PacketCommand;
import buildcraft.core.lib.utils.NetworkUtils;

public class ContainerList extends BuildCraftContainer implements ICommandReceiver {
    public ListHandler.Line[] lines;
    private EntityPlayer player;

    public ContainerList(EntityPlayer iPlayer) {
        super(iPlayer, iPlayer.inventory.getSizeInventory());

        player = iPlayer;

        lines = ListHandler.getLines(player.getCurrentEquippedItem());

        for (int sy = 0; sy < 3; sy++) {
            for (int sx = 0; sx < 9; sx++) {
                addSlotToContainer(new Slot(player.inventory, sx + sy * 9 + 9, 8 + sx * 18, 103 + sy * 18));
            }
        }

        for (int sx = 0; sx < 9; sx++) {
            addSlotToContainer(new Slot(player.inventory, sx, 8 + sx * 18, 161));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }

    public void setStack(final int lineIndex, final int slotIndex, final ItemStack stack) {
        lines[lineIndex].setStack(slotIndex, stack);
        ListHandler.saveLines(player.getCurrentEquippedItem(), lines);

        if (player.worldObj.isRemote) {
            BuildCraftCore.instance.sendToServer(new PacketCommand(this, "setStack", new CommandWriter() {
                @Override
                public void write(ByteBuf data) {
                    data.writeByte(lineIndex);
                    data.writeByte(slotIndex);
                    NetworkUtils.writeStack(data, stack);
                }
            }));
        }
    }

    public void switchButton(final int lineIndex, final int button) {
        lines[lineIndex].toggleOption(button);
        ListHandler.saveLines(player.getCurrentEquippedItem(), lines);

        if (player.worldObj.isRemote) {
            BuildCraftCore.instance.sendToServer(new PacketCommand(this, "switchButton", new CommandWriter() {
                @Override
                public void write(ByteBuf data) {
                    data.writeByte(lineIndex);
                    data.writeByte(button);
                }
            }));
        }
    }

    public void setLabel(final String text) {
        ItemList.saveLabel(player.getCurrentEquippedItem(), text);

        if (player.worldObj.isRemote) {
            BuildCraftCore.instance.sendToServer(new PacketCommand(this, "setLabel", new CommandWriter() {
                @Override
                public void write(ByteBuf data) {
                    NetworkUtils.writeUTF(data, text);
                }
            }));
        }
    }

    @Override
    public void receiveCommand(String command, Side side, Object sender, ByteBuf stream) {
        if (side.isServer()) {
            if ("setLabel".equals(command)) {
                setLabel(NetworkUtils.readUTF(stream));
            } else if ("switchButton".equals(command)) {
                switchButton(stream.readUnsignedByte(), stream.readUnsignedByte());
            } else if ("setStack".equals(command)) {
                setStack(stream.readUnsignedByte(), stream.readUnsignedByte(), NetworkUtils.readStack(stream));
            }
        }
    }
}
