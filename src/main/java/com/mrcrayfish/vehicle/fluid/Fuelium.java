package com.mrcrayfish.vehicle.fluid;

import com.mrcrayfish.vehicle.Reference;
import com.mrcrayfish.vehicle.init.ModFluids;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.IFluidState;
import net.minecraft.state.StateContainer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.ForgeFlowingFluid;

/**
 * Author: MrCrayfish
 */
public abstract class Fuelium extends ForgeFlowingFluid
{
    public Fuelium()
    {
        super(new Properties(() -> ModFluids.FUELIUM, () -> ModFluids.FLOWING_FUELIUM, FluidAttributes.builder(new ResourceLocation(Reference.MOD_ID, "block/fuelium_still"), new ResourceLocation(Reference.MOD_ID, "block/fuelium_flowing")).density(900).viscosity(900)));
    }

    public static class Source extends Fuelium
    {
        public Source()
        {
            this.setRegistryName(new ResourceLocation(Reference.MOD_ID, "fuelium"));
        }

        @Override
        public boolean isSource(IFluidState state)
        {
            return true;
        }

        @Override
        public int getLevel(IFluidState p_207192_1_)
        {
            return 8;
        }
    }

    public static class Flowing extends Fuelium
    {
        public Flowing()
        {
            this.setRegistryName(new ResourceLocation(Reference.MOD_ID, "flowing_fuelium"));
        }

        @Override
        protected void fillStateContainer(StateContainer.Builder<Fluid, IFluidState> builder)
        {
            super.fillStateContainer(builder);
            builder.add(LEVEL_1_8);
        }

        @Override
        public int getLevel(IFluidState state)
        {
            return state.get(LEVEL_1_8);
        }

        @Override
        public boolean isSource(IFluidState state)
        {
            return false;
        }
    }
}