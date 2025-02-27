/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 * <p/>
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.transport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import buildcraft.BuildCraftCore;
import buildcraft.BuildCraftTransport;
import buildcraft.core.lib.utils.BCStringUtils;
import buildcraft.transport.pipes.PipePowerWood;

@SideOnly(Side.CLIENT)
public final class PipeToolTipManager {

    private static final Map<Class<? extends Pipe<?>>, String> toolTips = new HashMap<>();

    static {
        if (!BuildCraftCore.hidePowerNumbers) {
            for (Map.Entry<Class<? extends Pipe<?>>, Integer> pipe : PipeTransportPower.powerMaximums.entrySet()) {
                if (PipePowerWood.class.isAssignableFrom(pipe.getKey())) {
                    continue;
                }

                PipeToolTipManager.addToolTip(pipe.getKey(), String.format("%d RF/t", pipe.getValue()));
            }
        }

        if (!BuildCraftCore.hideFluidNumbers) {
            for (Map.Entry<Class<? extends Pipe<?>>, Integer> pipe : PipeTransportFluids.fluidCapacities.entrySet()) {
                PipeToolTipManager.addToolTip(pipe.getKey(), String.format("%d mB/t", pipe.getValue()));
            }
        }
    }

    /** Deactivate constructor */
    private PipeToolTipManager() {}

    private static void addTipToList(String tipTag, List<String> tips) {
        if (BCStringUtils.canLocalize(tipTag)) {
            String localized = BCStringUtils.localize(tipTag);
            if (localized != null) {
                List<String> lines = BCStringUtils.newLineSplitter.splitToList(localized);
                tips.addAll(lines);
            }
        }
    }

    public static void addToolTip(Class<? extends Pipe<?>> pipe, String toolTip) {
        toolTips.put(pipe, toolTip);
    }

    public static List<String> getToolTip(Class<? extends Pipe<?>> pipe, boolean advanced) {
        List<String> tips = new ArrayList<>();
        addTipToList("tip." + pipe.getSimpleName(), tips);

        String tip = toolTips.get(pipe);
        if (tip != null) {
            tips.add(tip);
        }

        if (GuiScreen.isShiftKeyDown()) {
            addTipToList("tip.shift." + pipe.getSimpleName(), tips);
        }
        return tips;
    }
}
