/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 * <p/>
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.core.statements;

import java.util.Collection;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

import buildcraft.BuildCraftCore;
import buildcraft.api.statements.IActionExternal;
import buildcraft.api.statements.IActionInternal;
import buildcraft.api.statements.IActionProvider;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.containers.IRedstoneStatementContainer;
import buildcraft.api.tiles.IControllable;

public class DefaultActionProvider implements IActionProvider {

    @Override
    public void addInternalActions(Collection<IActionInternal> res, IStatementContainer container) {
        if (container instanceof IRedstoneStatementContainer) {
            res.add(BuildCraftCore.actionRedstone);
        }
    }

    @Override
    public void addExternalActions(Collection<IActionExternal> res, EnumFacing side, TileEntity tile) {
        if (tile instanceof IControllable) {
            for (IControllable.Mode mode : IControllable.Mode.values()) {
                if (mode != IControllable.Mode.Unknown && ((IControllable) tile).acceptsControlMode(mode)) {
                    res.add(BuildCraftCore.actionControl[mode.ordinal()]);
                }
            }
        }
    }
}
