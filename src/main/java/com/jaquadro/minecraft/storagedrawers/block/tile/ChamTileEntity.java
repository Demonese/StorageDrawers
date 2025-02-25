package com.jaquadro.minecraft.storagedrawers.block.tile;

import com.jaquadro.minecraft.storagedrawers.StorageDrawers;
import com.jaquadro.minecraft.storagedrawers.block.tile.tiledata.TileDataShim;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.extensions.IForgeBlockEntity;

import java.util.ArrayList;
import java.util.List;

public class ChamTileEntity extends BlockEntity implements IForgeBlockEntity
{
    private CompoundTag failureSnapshot;
    private List<TileDataShim> fixedShims;
    private List<TileDataShim> portableShims;

    public ChamTileEntity (BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
        super(tileEntityTypeIn, pos, state);
    }

    public boolean hasDataPacket () {
        return true;
    }

    public boolean dataPacketRequiresRenderUpdate () {
        return false;
    }

    public void injectData (TileDataShim shim) {
        if (fixedShims == null)
            fixedShims = new ArrayList<TileDataShim>();
        fixedShims.add(shim);
    }

    public void injectPortableData (TileDataShim shim) {
        if (portableShims == null)
            portableShims = new ArrayList<TileDataShim>();
        portableShims.add(shim);
    }

    @Override
    public final void load (CompoundTag tag) {
        super.load(tag);

        //failureSnapshot = null;

        //try {
            readFixed(tag);
            readPortable(tag);
        //}
        //catch (Throwable t) {
        //    trapLoadFailure(t, tag);
        //}
    }

    public final void read (CompoundTag tag) {
        load(tag);
    }

    /*@Override
    public final CompoundTag save (CompoundTag tag) {
        super.save(tag);

        if (failureSnapshot != null) {
            restoreLoadFailure(tag);
            return tag;
        }

        try {
            tag = writeFixed(tag);
            tag = writePortable(tag);
        }
        catch (Throwable t) {
            StorageDrawers.log.error("Tile Save Failure.", t);
        }

        return tag;
    }*/

    @Override
    protected void saveAdditional (CompoundTag tag) {
        tag = writeFixed(tag);
        writePortable(tag);
    }

    public void readPortable (CompoundTag tag) {
        if (portableShims != null) {
            for (TileDataShim shim : portableShims)
                shim.read(tag);
        }
    }

    public CompoundTag writePortable (CompoundTag tag) {
        if (portableShims != null) {
            for (TileDataShim shim : portableShims)
                tag = shim.write(tag);
        }

        return tag;
    }

    protected void readFixed (CompoundTag tag) {
        if (fixedShims != null) {
            for (TileDataShim shim : fixedShims)
                shim.read(tag);
        }
    }

    protected CompoundTag writeFixed (CompoundTag tag) {
        if (fixedShims != null) {
            for (TileDataShim shim : fixedShims)
                tag = shim.write(tag);
        }

        return tag;
    }

    private void trapLoadFailure (Throwable t, CompoundTag tag) {
        failureSnapshot = tag.copy();
        StorageDrawers.log.error("Tile Load Failure.", t);
    }

    private void restoreLoadFailure (CompoundTag tag) {
        for (String key : failureSnapshot.getAllKeys()) {
            if (!tag.contains(key))
                tag.put(key, failureSnapshot.get(key).copy());
        }
    }

    protected boolean loadDidFail () {
        return failureSnapshot != null;
    }

    @Override
    public final CompoundTag getUpdateTag () {
        CompoundTag tag = this.saveWithoutMetadata();
        //save(tag);

        return tag;
    }

    @Override
    public final ClientboundBlockEntityDataPacket getUpdatePacket () {
        return hasDataPacket() ? ClientboundBlockEntityDataPacket.create(this) : null;
    }

    @Override
    public final void onDataPacket (Connection net, ClientboundBlockEntityDataPacket pkt) {
        if (pkt != null && pkt.getTag() != null)
            read(pkt.getTag());

        if (dataPacketRequiresRenderUpdate() && getLevel().isClientSide) {
            BlockState state = getLevel().getBlockState(getBlockPos());
            getLevel().sendBlockUpdated(getBlockPos(), state, state, 3);
        }
    }

    /**
     * Calls server to sync data with client, update neighbors, and cause a delayed render update.
     */
    public void markBlockForUpdate () {
        if (getLevel() != null && !getLevel().isClientSide) {
            BlockState state = getLevel().getBlockState(worldPosition);
            getLevel().sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    public void markBlockForUpdateClient () {
        if (getLevel() != null && getLevel().isClientSide) {
            BlockState state = getLevel().getBlockState(worldPosition);
            getLevel().sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    /**
     * Causes immediate render update when called client-side, or delayed render update when called server-side.
     * Does not sync tile data or notify neighbors of any state change.
     */
    public void markBlockForRenderUpdate () {
        if (getLevel() == null)
            return;

        //if (getWorld().isRemote)
        //    getWorld().markBlockRangeForRenderUpdate(pos, pos);
        //else {
        BlockState state = getLevel().getBlockState(worldPosition);
        getLevel().sendBlockUpdated(worldPosition, state, state, 2);
        //}
    }
}
