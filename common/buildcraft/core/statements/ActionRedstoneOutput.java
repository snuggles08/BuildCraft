/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 * <p/>
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.core.statements;

import net.minecraft.util.EnumFacing;

import buildcraft.api.statements.IActionInternal;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.containers.IRedstoneStatementContainer;
import buildcraft.api.statements.containers.ISidedStatementContainer;
import buildcraft.core.lib.utils.BCStringUtils;

public class ActionRedstoneOutput extends BCStatement implements IActionInternal {

    protected ActionRedstoneOutput(String s) {
        // Used by fader output
        super(s);
    }

    public ActionRedstoneOutput() {
        super("buildcraft:redstone.output", "buildcraft.redstone.output");
        setBuildCraftLocation("core", "triggers/action_redstoneoutput");
    }

    @Override
    public String getDescription() {
        return BCStringUtils.localize("gate.action.redstone.signal");
    }

    @Override
    public IStatementParameter createParameter(int index) {
        IStatementParameter param = null;

        if (index == 0) {
            param = new StatementParameterRedstoneGateSideOnly();
        }

        return param;
    }

    @Override
    public int maxParameters() {
        return 1;
    }

    protected boolean isSideOnly(IStatementParameter[] parameters) {
        if (parameters != null && parameters.length >= (getRGSOSlot() + 1) && parameters[getRGSOSlot()] instanceof StatementParameterRedstoneGateSideOnly) {
            return ((StatementParameterRedstoneGateSideOnly) parameters[getRGSOSlot()]).isOn;
        }

        return false;
    }

    @Override
    public void actionActivate(IStatementContainer source, IStatementParameter[] parameters) {
        if (source instanceof IRedstoneStatementContainer) {
            EnumFacing side = null;
            if (source instanceof ISidedStatementContainer && isSideOnly(parameters)) {
                side = ((ISidedStatementContainer) source).getSide();
            }
            ((IRedstoneStatementContainer) source).setRedstoneOutput(side, getSignalLevel(parameters));
        }
    }

    protected int getRGSOSlot() { return 0; }

    protected int getSignalLevel(IStatementParameter[] parameters) {
        return 15;
    }
}
