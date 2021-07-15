package com.mrcrayfish.vehicle.network.message;

import com.mrcrayfish.vehicle.Config;
import com.mrcrayfish.vehicle.block.VehicleCrateBlock;
import com.mrcrayfish.vehicle.common.VehicleRegistry;
import com.mrcrayfish.vehicle.crafting.VehicleRecipe;
import com.mrcrayfish.vehicle.crafting.VehicleRecipes;
import com.mrcrayfish.vehicle.entity.EngineTier;
import com.mrcrayfish.vehicle.entity.EngineType;
import com.mrcrayfish.vehicle.entity.IWheelType;
import com.mrcrayfish.vehicle.entity.PoweredVehicleEntity;
import com.mrcrayfish.vehicle.entity.VehicleEntity;
import com.mrcrayfish.vehicle.entity.WheelType;
import com.mrcrayfish.vehicle.inventory.container.WorkstationContainer;
import com.mrcrayfish.vehicle.item.EngineItem;
import com.mrcrayfish.vehicle.item.WheelItem;
import com.mrcrayfish.vehicle.tileentity.WorkstationTileEntity;
import com.mrcrayfish.vehicle.util.InventoryUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

/**
 * Author: MrCrayfish
 */
public class MessageCraftVehicle implements IMessage<MessageCraftVehicle>
{
    private String vehicleId;
    private BlockPos pos;

    public MessageCraftVehicle() {}

    public MessageCraftVehicle(String vehicleId, BlockPos pos)
    {
        this.vehicleId = vehicleId;
        this.pos = pos;
    }

    @Override
    public void encode(MessageCraftVehicle message, PacketBuffer buffer)
    {
        buffer.writeUtf(message.vehicleId, 128);
        buffer.writeBlockPos(message.pos);
    }

    @Override
    public MessageCraftVehicle decode(PacketBuffer buffer)
    {
        return new MessageCraftVehicle(buffer.readUtf(128), buffer.readBlockPos());
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void handle(MessageCraftVehicle message, Supplier<NetworkEvent.Context> supplier)
    {
        supplier.get().enqueueWork(() ->
        {
            ServerPlayerEntity player = supplier.get().getSender();
            if(player != null)
            {
                World world = player.level;
                if(!(player.containerMenu instanceof WorkstationContainer))
                    return;

                WorkstationContainer workstation = (WorkstationContainer) player.containerMenu;
                if(!workstation.getPos().equals(message.pos))
                    return;

                ResourceLocation entityId = new ResourceLocation(message.vehicleId);
                if(Config.SERVER.disabledVehicles.get().contains(entityId.toString()))
                    return;

                EntityType<?> entityType = ForgeRegistries.ENTITIES.getValue(entityId);
                if(entityType == null)
                    return;

                if(!VehicleRegistry.getRegisteredVehicles().contains(entityType))
                    return;

                VehicleRecipe recipe = VehicleRecipes.getRecipe(entityType, world);
                if(recipe == null)
                    return;

                for(ItemStack stack : recipe.getMaterials())
                {
                    if(!InventoryUtil.hasItemStack(player, stack))
                    {
                        return;
                    }
                }

                VehicleEntity vehicle = null;
                EngineType engineType = EngineType.NONE;
                Entity entity = entityType.create(world);
                if(entity instanceof VehicleEntity)
                {
                    vehicle = (VehicleEntity) entity;
                }
                if(entity instanceof PoweredVehicleEntity)
                {
                    PoweredVehicleEntity entityPoweredVehicle = (PoweredVehicleEntity) entity;
                    engineType = entityPoweredVehicle.getProperties().getEngineType();

                    WorkstationTileEntity workstationTileEntity = workstation.getTileEntity();
                    ItemStack engine = workstationTileEntity.getItem(1);
                    if(engine.isEmpty() || !(engine.getItem() instanceof EngineItem))
                    {
                        return;
                    }

                    EngineType engineType2 = ((EngineItem) engine.getItem()).getEngineType();
                    if(entityPoweredVehicle.getProperties().getEngineType() != EngineType.NONE && entityPoweredVehicle.getProperties().getEngineType() != engineType2)
                    {
                        return;
                    }

                    if(entityPoweredVehicle.canChangeWheels())
                    {
                        ItemStack wheel = workstationTileEntity.getInventory().get(2);
                        if(!(wheel.getItem() instanceof WheelItem))
                        {
                            return;
                        }
                    }
                }

                if(vehicle == null)
                {
                    return;
                }

                for(ItemStack stack : recipe.getMaterials())
                {
                    InventoryUtil.removeItemStack(player, stack);
                }

                WorkstationTileEntity workstationTileEntity = workstation.getTileEntity();

                /* Gets the color based on the dye */
                int color = VehicleEntity.DYE_TO_COLOR[0];
                if(vehicle.canBeColored())
                {
                    ItemStack dyeStack = workstationTileEntity.getInventory().get(0);
                    if(dyeStack.getItem() instanceof DyeItem)
                    {
                        DyeItem dyeItem = (DyeItem) dyeStack.getItem();
                        color = dyeItem.getDyeColor().getColorValue();
                        workstationTileEntity.getInventory().set(0, ItemStack.EMPTY);
                    }
                }

                EngineTier engineTier = EngineTier.IRON;
                if(engineType != EngineType.NONE)
                {
                    ItemStack engine = workstationTileEntity.getInventory().get(1);
                    if(engine.getItem() instanceof EngineItem)
                    {
                        EngineItem engineItem = (EngineItem) engine.getItem();
                        engineTier = engineItem.getEngineTier();
                        workstationTileEntity.getInventory().set(1, ItemStack.EMPTY);
                    }
                }

                ItemStack wheelStack = ItemStack.EMPTY;
                if(vehicle instanceof PoweredVehicleEntity && ((PoweredVehicleEntity) vehicle).canChangeWheels())
                {
                    ItemStack workstationWheelStack = workstationTileEntity.getInventory().get(2);
                    if(workstationWheelStack.getItem() instanceof WheelItem)
                    {
                        wheelStack = workstationWheelStack.copy();
                        workstationTileEntity.getInventory().set(2, ItemStack.EMPTY);
                    }
                }

                ItemStack stack = VehicleCrateBlock.create(entityId, color, engineTier, wheelStack);
                world.addFreshEntity(new ItemEntity(world, message.pos.getX() + 0.5, message.pos.getY() + 1.125, message.pos.getZ() + 0.5, stack));
            }
        });
        supplier.get().setPacketHandled(true);
    }
}
