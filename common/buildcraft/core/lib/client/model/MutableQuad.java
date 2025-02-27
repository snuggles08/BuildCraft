package buildcraft.core.lib.client.model;

import java.util.Arrays;

import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.client.renderer.vertex.VertexFormatElement.EnumType;
import net.minecraft.client.renderer.vertex.VertexFormatElement.EnumUsage;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;

import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

public class MutableQuad {
    public static final VertexFormat ITEM_LMAP = new VertexFormat(DefaultVertexFormats.ITEM);
    public static final VertexFormat ITEM_BLOCK_PADDING = new VertexFormat();

    // Baked Quad array indices
    public static final int X = 0;
    public static final int Y = 1;
    public static final int Z = 2;
    public static final int SHADE = 3;
    public static final int U = 4;
    public static final int V = 5;
    /** Represents either the normal (for items) or lightmap (for blocks) */
    public static final int UNUSED = 6;

    static {
        ITEM_LMAP.addElement(DefaultVertexFormats.TEX_2S);

        ITEM_BLOCK_PADDING.addElement(DefaultVertexFormats.POSITION_3F);
        ITEM_BLOCK_PADDING.addElement(DefaultVertexFormats.COLOR_4UB);
        ITEM_BLOCK_PADDING.addElement(DefaultVertexFormats.TEX_2F);
        ITEM_BLOCK_PADDING.addElement(new VertexFormatElement(0, EnumType.INT, EnumUsage.PADDING, 1));
    }

    public static MutableQuad create(BakedQuad quad, VertexFormat format) {
        int[] data = quad.getVertexData();
        int stride = data.length / 4;
        MutableQuad mutable = new MutableQuad(quad.getTintIndex(), quad.getFace());
        for (int v = 0; v < 4; v++) {
            MutableVertex mutableVertex = mutable.getVertex(v);
            float x = fromBits(data[stride * v + X]);
            float y = fromBits(data[stride * v + Y]);
            float z = fromBits(data[stride * v + Z]);
            mutableVertex.positionf(x, y, z);
            mutableVertex.colouri(data[stride * v + SHADE]);
            float texU = fromBits(data[stride * v + U]);
            float texV = fromBits(data[stride * v + V]);
            mutableVertex.texf(texU, texV);

            if (format == DefaultVertexFormats.BLOCK) {
                int lightmap = data[stride * v + UNUSED];
                mutableVertex.lighti(lightmap);
            } else if (format == DefaultVertexFormats.ITEM) {
                int normal = data[stride * v + UNUSED];
            }
        }
        return mutable;
    }

    /** Creates a mutable quad as a copy of the given {@link BakedQuad}. This assumes the baked quad uses the format
     * {@link DefaultVertexFormats#BLOCK} or {@link DefaultVertexFormats#ITEM}, but ignores the lightmap value. (This
     * uses Mutable
     * 
     * @param quad
     * @return */
    public static MutableQuad create(BakedQuad quad) {
        return create(quad, ITEM_BLOCK_PADDING);
    }

    public static float fromBits(int bits) {
        return Float.intBitsToFloat(bits);
    }

    private final MutableVertex[] verticies = new MutableVertex[4];
    private int tintIndex = -1;
    private EnumFacing face = null;

    public MutableQuad(int tintIndex, EnumFacing face) {
        this.tintIndex = tintIndex;
        this.face = face;
        for (int v = 0; v < 4; v++) {
            verticies[v] = new MutableVertex();
        }
    }

    public MutableQuad(VertexFormat format, float[][][] data, int tintIndex, EnumFacing face) {
        this(tintIndex, face);
        for (int v = 0; v < 4; v++) {
            verticies[v].setData(data[v], format);
        }
    }

    public MutableQuad(MutableQuad from) {
        this.tintIndex = from.tintIndex;
        this.face = from.face;
        for (int i = 0; i < 4; i++)
            this.verticies[i] = new MutableVertex(from.verticies[i]);
    }

    public MutableQuad setTint(int tint) {
        tintIndex = tint;
        return this;
    }

    public int getTint() {
        return tintIndex;
    }

    public MutableQuad setFace(EnumFacing face) {
        this.face = face;
        return this;
    }

    public EnumFacing getFace() {
        return face;
    }

    public UnpackedBakedQuad toUnpacked() {
        return toUnpacked(ITEM_LMAP);
    }

    public UnpackedBakedQuad toUnpacked(VertexFormat format) {
        float[][][] data = new float[4][][];
        for (int vertex = 0; vertex < 4; vertex++) {
            float[][] fromData = verticies[vertex].getData(format);
            data[vertex] = new float[fromData.length][];
            for (int element = 0; element < fromData.length; element++) {
                data[vertex][element] = new float[fromData[element].length];
                for (int d = 0; d < fromData[element].length; d++) {
                    data[vertex][element][d] = fromData[element][d];
                }
            }
        }
        return new UnpackedBakedQuad(data, tintIndex, face, format);
    }

    public void render(WorldRenderer wr) {
        for (MutableVertex v : verticies) {
            v.render(wr);
        }
    }

    public MutableVertex getVertex(int v) {
        return verticies[v & 0b11];
    }

    public Vector3f getCalculatedNormal() {
        Point3f[] positions = { getVertex(0).position(), getVertex(1).position(), getVertex(2).position() };

        Vector3f a = new Vector3f(positions[1]);
        a.sub(positions[0]);

        Vector3f b = new Vector3f(positions[2]);
        b.sub(positions[0]);

        Vector3f c = new Vector3f();
        c.cross(a, b);
        return c;
    }

    public void setCalculatedNormal() {
        normalv(getCalculatedNormal());
    }

    public static float diffuseLight(Vector3f normal) {
        return diffuseLight(normal.x, normal.y, normal.z);
    }

    public static float diffuseLight(float x, float y, float z) {
        boolean up = y >= 0;

        float xx = x * x;
        float yy = y * y;
        float zz = z * z;

        float t = xx + yy + zz;
        float light = (xx * 0.6f + zz * 0.8f) / t;

        float yyt = yy / t;
        if (!up) yyt *= 0.5;
        light += yyt;

        return light;
    }

    public float getCalculatedDiffuse() {
        return diffuseLight(getCalculatedNormal());
    }

    public void setCalculatedDiffuse() {
        float diffuse = getCalculatedDiffuse();
        colourf(diffuse, diffuse, diffuse, 1);
    }

    /** Inverts this quad's normal so that it will render in the opposite direction. You will need to recall diffusion
     * calculations if you had previously calculated the diffuse. */
    public MutableQuad invertNormal() {
        MutableVertex[] newArray = new MutableVertex[4];
        newArray[0] = verticies[3];
        newArray[1] = verticies[2];
        newArray[2] = verticies[1];
        newArray[3] = verticies[0];
        for (int i = 0; i < 4; i++)
            verticies[i] = newArray[i].invertNormal();
        return this;
    }

    /* A lot of delegate functions here. The actual documentation should be per-vertex. */
    // @formatter:off
    /** @see MutableVertex#normalv(Vector3f) */ public void normalv(Vector3f vec) {Arrays.stream(verticies).forEach(v -> v.normalv(vec));}
    public void normalf(float x, float y, float z) {Arrays.stream(verticies).forEach(v -> v.normalf(x, y, z));}

    public void colourv(Vector4f vec) {Arrays.stream(verticies).forEach(v -> v.colourv(vec));};
    public void colourf(float r, float g, float b, float a) {Arrays.stream(verticies).forEach(v -> v.colourf(r,g,b,a));}
    public void colouri(int rgba) {Arrays.stream(verticies).forEach(v -> v.colouri(rgba));}
    public void colouri(int r, int g, int b, int a) {Arrays.stream(verticies).forEach(v -> v.colouri(r, g, b, a));}

    public void lightv(Vector2f vec) {for (MutableVertex v : verticies) v.lightv(vec);}
    public void lightf(float block, float sky) {for (MutableVertex v : verticies) v.lightf(block, sky);}
    public void lighti(int combined) {for (MutableVertex v : verticies) v.lighti(combined);}
    public void lighti(int block, int sky) {for (MutableVertex v : verticies) v.lighti(block, sky);}

    public void transform(Matrix4f transformation) {for (MutableVertex v : verticies) v.transform(transformation);}
    // @formatter:on

    @Override
    public String toString() {
        return "MutableQuad [verticies=" + vToS() + ", tintIndex=" + tintIndex + ", face=" + face + "]";
    }

    private String vToS() {
        StringBuilder builder = new StringBuilder();
        for (MutableVertex v : verticies) {
            builder.append(v.toString() + "\n");
        }
        return builder.toString();
    }
}
