package com.hbm.blocks.deco;

import com.hbm.blocks.BlockBase;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockDecoSign extends BlockBase {

    // ✅ Хитбокс: высота 0.5 (полублок). Меняй второе 0.5 на 1.0/3.0 если нужна полная коллизия
    protected static final AxisAlignedBB SIGN_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.5D, 1.0D);

    public BlockDecoSign(String registryName, float hardness, float resistance) {
        super(Material.ROCK, registryName); // BlockBase уже делает setTranslationKey + setRegistryName
        this.setHardness(hardness);
        this.setResistance(resistance);
        // CreativeTab ставится в регистрации через .setCreativeTab(), как у тебя в проекте
    }

    // ✅ Хитбокс для рендера и рейкастов
    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return SIGN_AABB;
    }

    // ✅ Прозрачность для света и рендера
    @Override
    public boolean isOpaqueCube(IBlockState state) { return false; }

    @Override
    public boolean isFullCube(IBlockState state) { return false; }

    // ✅ Включаем кастомный рендер (OBJ модель)
    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

}