package buildcraft.transport.client.model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.EnumFacing.AxisDirection;
import net.minecraft.util.Vec3;

import buildcraft.BuildCraftTransport;
import buildcraft.api.transport.PipeWire;
import buildcraft.core.CoreConstants;
import buildcraft.core.lib.client.model.BCModelHelper;
import buildcraft.core.lib.client.model.BuildCraftBakedModel;
import buildcraft.core.lib.client.model.IModelCache;
import buildcraft.core.lib.client.model.ModelCache;
import buildcraft.core.lib.client.model.ModelCacheBuilder;
import buildcraft.core.lib.client.model.ModelCacheMultipleSame;
import buildcraft.core.lib.client.model.MutableQuad;
import buildcraft.core.lib.utils.Utils;
import buildcraft.transport.PipeRenderState;

import javax.vecmath.Vector3f;

public class PipeModelCacheWire {
    public static final IModelCache<PipeWireKey> cacheAll;
    public static final ModelCache<PipeWireKeySingle> cacheSingle;

    private static EnumMap<PipeWire, Vec3> wirePosMap = Maps.newEnumMap(PipeWire.class);
    private static EnumMap<PipeWire, AxisDirection[]> wireDirectionMap = Maps.newEnumMap(PipeWire.class);
    private static final double WIRE_WIDTH = 0.05;
    // Offset all wires very slightly out of the pipe
    private static final double WIRE_OFFSET = 0.001;

    static {
        AxisDirection neg = AxisDirection.NEGATIVE;
        AxisDirection pos = AxisDirection.POSITIVE;

        wireDirectionMap.put(PipeWire.RED, new AxisDirection[] { neg, pos, neg });
        wireDirectionMap.put(PipeWire.BLUE, new AxisDirection[] { pos, pos, pos });
        wireDirectionMap.put(PipeWire.GREEN, new AxisDirection[] { pos, neg, neg });
        wireDirectionMap.put(PipeWire.YELLOW, new AxisDirection[] { neg, neg, pos });

        wirePosMap.put(PipeWire.RED, getOffset(PipeWire.RED));
        wirePosMap.put(PipeWire.BLUE, getOffset(PipeWire.BLUE));
        wirePosMap.put(PipeWire.GREEN, getOffset(PipeWire.GREEN));
        wirePosMap.put(PipeWire.YELLOW, getOffset(PipeWire.YELLOW));

        cacheSingle = new ModelCacheBuilder<>("pipe.wire.single", PipeModelCacheWire::generate).setMaxSize(1003).enableGL(
                DefaultVertexFormats.POSITION_TEX).setKeepMutable(false).build();
        // new ModelCache<>("pipe.wire.single", 1000, PipeModelCacheWire::generate);
        cacheAll = new ModelCacheMultipleSame<>("pipe.wire.all", PipeWireKey::getKeys, cacheSingle);
    }

    private static Vec3 getOffset(PipeWire wire) {
        double min = CoreConstants.PIPE_MIN_POS - WIRE_WIDTH;
        double max = CoreConstants.PIPE_MAX_POS;

        // Offset each wire slightly differently to avoid z-fighting between wires
        int multiple = wire.ordinal() + 1;
        double offset = WIRE_OFFSET * multiple;
        double inset = -offset;

        AxisDirection[] axis = wireDirectionMap.get(wire);
        boolean[] axisPos = new boolean[3];
        for (int i = 0; i < 3; i++) {
            axisPos[i] = axis[i] == AxisDirection.POSITIVE;
        }

        Vec3 base = new Vec3(axisPos[0] ? max : min, axisPos[1] ? max : min, axisPos[2] ? max : min);
        return base.addVector(axisPos[0] ? inset : offset, axisPos[1] ? inset : offset, axisPos[2] ? inset : offset);
    }

    private static ImmutableList<MutableQuad> generate(PipeWireKeySingle key) {
        PipeWire wire = key.type;
        Vec3 pos = wirePosMap.get(wire);

        boolean isLit = key.on;
        // BCLog.logger.info("generate[" + wire + ", " + isLit + ", " + key.connections + "]");

        TextureAtlasSprite sprite = BuildCraftTransport.instance.wireIconProvider.getIcon(wire, isLit);

        List<MutableQuad> unprocessed = new ArrayList<>();

        Vec3 center = pos;
        Vec3 centerSize = new Vec3(WIRE_WIDTH, WIRE_WIDTH, WIRE_WIDTH);
        AxisDirection[] directions = wireDirectionMap.get(wire);
        int numFaces = 0;

        for (EnumFacing face : EnumFacing.values()) {
            boolean positive = face.getAxisDirection() == AxisDirection.POSITIVE;
            Axis axis = face.getAxis();
            AxisDirection wireCenter = directions[axis.ordinal()];
            if (key.connections.contains(face)) {
                if (wireCenter == face.getAxisDirection()) {
                    numFaces++;
                }
                numFaces++;
                Vec3 start = pos;
                Vec3 end = pos.add(centerSize);
                if (positive) {
                    start = Utils.withValue(start, axis, Utils.getValue(start, axis) + WIRE_WIDTH);
                    end = Utils.withValue(end, axis, 1);
                } else {
                    start = Utils.withValue(start, axis, 0);
                    end = Utils.withValue(end, axis, Utils.getValue(end, axis) - WIRE_WIDTH);
                }
                renderCuboid(unprocessed, start, end.subtract(start), sprite);
            } else {
                boolean anyOther = false;
                for (EnumFacing face2 : EnumFacing.values()) {
                    if (face2.getOpposite() == face) {
                        continue;
                    }
                    anyOther |= key.connections.contains(face2);
                }
                if (anyOther) {
                    continue;
                }
                Vec3 start = pos;
                Vec3 end = pos.add(centerSize);
                if (positive) {
                    start = Utils.withValue(start, axis, Utils.getValue(start, axis) + WIRE_WIDTH);
                    end = Utils.withValue(end, axis, CoreConstants.PIPE_MAX_POS);
                } else {
                    start = Utils.withValue(start, axis, CoreConstants.PIPE_MIN_POS);
                    end = Utils.withValue(end, axis, Utils.getValue(end, axis) - WIRE_WIDTH);
                }
                Vec3 size = end.subtract(start);
                if (size.lengthVector() > WIRE_WIDTH * 2) {
                    renderCuboid(unprocessed, start, size, sprite);
                }
            }
        }
        if (numFaces != 1) {
            renderCuboid(unprocessed, center, centerSize, sprite);
        }

        ImmutableList.Builder<MutableQuad> builder = ImmutableList.builder();

        for (MutableQuad quad : unprocessed) {
            if (isLit) quad.lightf(1, 0);
            quad.colourf(1, 1, 1, 1);
            // quad.setCalculatedDiffuse();
            builder.add(quad);
        }

        return builder.build();
    }

    private static void renderCuboid(List<MutableQuad> quads, Vec3 min, Vec3 size, TextureAtlasSprite sprite) {
        Vec3 radius = Utils.multiply(size, 0.5);
        Vector3f radiusF = Utils.convertFloat(radius);
        Vector3f center = Utils.convertFloat(min.add(radius));
        for (EnumFacing face : EnumFacing.values()) {
            float[] uvs = new float[4];
            int neg = (face.ordinal() - 2) % 6;
            if (neg < 0) {
                neg += 6;
            }
            EnumFacing uFace = EnumFacing.VALUES[neg];
            EnumFacing vFace = EnumFacing.VALUES[(face.ordinal() + 2) % 6];
            if (face.getAxis() == Axis.Z) {
                EnumFacing holder = uFace;
                uFace = vFace;
                vFace = holder;
            }
            uvs[BuildCraftBakedModel.U_MIN] = sprite.getMinU();
            uvs[BuildCraftBakedModel.U_MAX] = sprite.getInterpolatedU(Utils.getValue(size, uFace.getAxis()) * 16);
            uvs[BuildCraftBakedModel.V_MIN] = sprite.getMinV();
            uvs[BuildCraftBakedModel.V_MAX] = sprite.getInterpolatedV(Utils.getValue(size, vFace.getAxis()) * 16);
            BCModelHelper.appendQuads(quads, BCModelHelper.createFace(face, center, radiusF, uvs));
        }
    }

    public static final class PipeWireKey {
        public final ImmutableSet<PipeWireKeySingle> keys;
        private final int hash;

        public PipeWireKey(PipeRenderState state) {
            ImmutableSet.Builder<PipeWireKeySingle> set = ImmutableSet.builder();
            for (PipeWire wire : PipeWire.VALUES) {
                if (!state.wireMatrix.hasWire(wire)) continue;
                EnumSet<EnumFacing> connections = EnumSet.noneOf(EnumFacing.class);
                for (EnumFacing face : EnumFacing.values()) {
                    if (state.wireMatrix.isWireConnected(wire, face)) connections.add(face);
                }
                set.add(new PipeWireKeySingle(wire, state.wireMatrix.isWireLit(wire), connections));
            }
            keys = set.build();
            hash = keys.hashCode();
        }

        public static ImmutableSet<PipeWireKeySingle> getKeys(PipeWireKey key) {
            return key.keys;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            PipeWireKey other = (PipeWireKey) obj;
            if (keys == null) {
                if (other.keys != null) return false;
            } else if (!keys.equals(other.keys)) return false;
            return true;
        }
    }

    public static final class PipeWireKeySingle {
        public final PipeWire type;
        public final boolean on;
        public final EnumSet<EnumFacing> connections;
        private final int hash;

        public PipeWireKeySingle(PipeWire type, boolean on, EnumSet<EnumFacing> connections) {
            this.type = type;
            this.on = on;
            this.connections = connections;
            hash = Objects.hash(type, on, connections);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            PipeWireKeySingle other = (PipeWireKeySingle) obj;
            if (on != other.on) return false;
            if (type != other.type) return false;
            return connections.containsAll(other.connections) && other.connections.containsAll(connections);
        }
    }
}
