package ru.tuma.portalpredictor.util;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

public final class BlockStateUtil {
    private BlockStateUtil() {}

    public static boolean isReplaceable(BlockState state) {
        return state.isAir() || state.getMaterial().isReplaceable();
    }

    public static boolean isSolid(BlockState state, BlockView world, BlockPos pos) {
        return state.isSolidBlock(world, pos);
    }
}
