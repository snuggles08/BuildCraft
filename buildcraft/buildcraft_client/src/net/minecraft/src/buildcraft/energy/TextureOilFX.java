package net.minecraft.src.buildcraft.energy;

import org.lwjgl.opengl.GL11;
import net.minecraft.src.BuildCraftCore;
import net.minecraft.src.BuildCraftEnergy;
import net.minecraft.src.RenderEngine;
import net.minecraft.src.TextureFX;

public class TextureOilFX extends TextureFX
{
	
	private int int_numPixels = 256;
	private int int_size = 16;
	private int int_sizeMinus1 = 0xF;
	
    public TextureOilFX()
    {
        super(BuildCraftEnergy.oilMoving.blockIndexInTexture);
        
		try {
			Class sizeClass = Class
					.forName("com.pclewis.mcpatcher.mod.TileSize");

			int_numPixels = sizeClass.getDeclaredField("int_numPixels").getInt(
					sizeClass);
			int_size = sizeClass.getDeclaredField("int_size").getInt(sizeClass);
			int_sizeMinus1 = sizeClass.getDeclaredField("int_sizeMinus1")
					.getInt(sizeClass);
		} catch (Throwable t) {

		}
        
        field_1158_g = new float[int_numPixels];
        field_1157_h = new float[int_numPixels];
        field_1156_i = new float[int_numPixels];
        field_1155_j = new float[int_numPixels];
        tickCounter = 0;
    }
    
	public void bindImage(RenderEngine renderengine) {
		GL11.glBindTexture(3553 /*GL_TEXTURE_2D*/ /* GL_TEXTURE_2D */,
//				 ModLoader
//					.getMinecraftInstance().renderEngine.getTexture(BuildCraftCore.customBuildCraftTexture)
				renderengine.getTexture(BuildCraftCore.customBuildCraftTexture)
				);
	}

    public void onTick()
    {
        tickCounter++;
        for(int i = 0; i < int_size; i++)
        {
            for(int k = 0; k < int_size; k++)
            {
                float f = 0.0F;
                for(int j1 = i - 1; j1 <= i + 1; j1++)
                {
                    int k1 = j1 & int_sizeMinus1;
                    int i2 = k & int_sizeMinus1;
                    f += field_1158_g[k1 + i2 * int_size];
                }

                field_1157_h[i + k * int_size] = f / 3.3F + field_1156_i[i + k * int_size] * 0.8F;
            }

        }

        for(int j = 0; j < int_size; j++)
        {
            for(int l = 0; l < int_size; l++)
            {
                field_1156_i[j + l * int_size] += field_1155_j[j + l * int_size] * 0.05F;
                if(field_1156_i[j + l * int_size] < 0.0F)
                {
                    field_1156_i[j + l * int_size] = 0.0F;
                }
                field_1155_j[j + l * int_size] -= 0.1F;
                if(Math.random() < 0.050000000000000003D)
                {
                    field_1155_j[j + l * int_size] = 0.5F;
                }
            }

        }

        float af[] = field_1157_h;
        field_1157_h = field_1158_g;
        field_1158_g = af;
        for(int i1 = 0; i1 < int_numPixels; i1++)
        {
            float f1 = field_1158_g[i1];
            if(f1 > 1.0F)
            {
                f1 = 1.0F;
            }
            if(f1 < 0.0F)
            {
                f1 = 0.0F;
            }
            float f2 = f1 * f1;
            int l1 = (int)(10F + f2 * 21F);
            int j2 = (int)(50F + f2 * 64F);
            int k2 = 255;
            if(anaglyphEnabled)
            {
                int i3 = (l1 * 30 + j2 * 59 + k2 * 11) / 100;
                int j3 = (l1 * 30 + j2 * 70) / 100;
                int k3 = (l1 * 30 + k2 * 70) / 100;
                l1 = i3;
                j2 = j3;
                k2 = k3;
            }
            
            imageData[i1 * 4 + 0] = (byte)l1;
            imageData[i1 * 4 + 1] = (byte)l1;
            imageData[i1 * 4 + 2] = (byte)l1;
            imageData[i1 * 4 + 3] = /*(byte)l2*/(byte)255;
        }

    }

    protected float field_1158_g[];
    protected float field_1157_h[];
    protected float field_1156_i[];
    protected float field_1155_j[];
    private int tickCounter;
}
