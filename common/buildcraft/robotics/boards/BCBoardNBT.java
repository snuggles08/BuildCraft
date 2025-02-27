package buildcraft.robotics.boards;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

import buildcraft.api.boards.RedstoneBoardRobot;
import buildcraft.api.boards.RedstoneBoardRobotNBT;
import buildcraft.api.robots.EntityRobotBase;
import buildcraft.core.lib.utils.BCStringUtils;

public class BCBoardNBT extends RedstoneBoardRobotNBT {
    public static final Map<String, BCBoardNBT> REGISTRY = new HashMap<>();
    private final ResourceLocation texture;
    private final String id, upperName, boardType;
    private final Constructor<? extends RedstoneBoardRobot> boardInit;

    public BCBoardNBT(String id, String name, Class<? extends RedstoneBoardRobot> board, String boardType) {
        this.id = id;
        this.boardType = boardType;
        this.upperName = name.substring(0, 1).toUpperCase() + name.substring(1);
        this.texture = new ResourceLocation("buildcraftrobotics:entities/robot_" + name);

        Constructor<? extends RedstoneBoardRobot> boardInitLocal;
        try {
            boardInitLocal = board.getConstructor(EntityRobotBase.class);
        } catch (Exception e) {
            e.printStackTrace();
            boardInitLocal = null;
        }
        this.boardInit = boardInitLocal;

        REGISTRY.put(name, this);
    }

    @Override
    public String getID() {
        return id;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced) {
        list.add(EnumChatFormatting.BOLD + BCStringUtils.localize("buildcraft.boardRobot" + upperName));
        list.add(BCStringUtils.localize("buildcraft.boardRobot" + upperName + ".desc"));
    }

    @Override
    public RedstoneBoardRobot create(EntityRobotBase robot) {
        try {
            return boardInit.newInstance(robot);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public ResourceLocation getRobotTexture() {
        return texture;
    }

    @Override
    public String getItemModelLocation() {
        return "buildcraftrobotics:board/" + boardType;
    }

    @Override
    public String getDisplayName() {
        return BCStringUtils.localize("buildcraft.boardRobot" + upperName);
    }
}
