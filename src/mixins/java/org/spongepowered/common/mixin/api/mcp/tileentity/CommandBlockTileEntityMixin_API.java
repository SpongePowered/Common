/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.mixin.api.mcp.tileentity;

import org.spongepowered.api.block.entity.CommandBlock;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.adventure.SpongeAdventure;
import org.spongepowered.common.util.Constants;

import net.minecraft.tileentity.CommandBlockLogic;
import net.minecraft.tileentity.CommandBlockTileEntity;

import java.util.Set;

@Mixin(CommandBlockTileEntity.class)
public abstract class CommandBlockTileEntityMixin_API extends TileEntityMixin_API implements CommandBlock {

    @Shadow public abstract CommandBlockLogic getCommandBlockLogic();

    @Override
    public void execute() {
        this.getCommandBlockLogic().trigger(this.world);
    }

    @Override
    public String getName() {
        return this.getCommandBlockLogic().getName().getString();
    }

    @Override
    @SuppressWarnings("deprecated")
    public DataContainer toContainer() {
        DataContainer container = super.toContainer();
        container.set(Constants.TileEntity.CommandBlock.STORED_COMMAND, this.getCommandBlockLogic().getCommand());
        container.set(Constants.TileEntity.CommandBlock.SUCCESS_COUNT, this.getCommandBlockLogic().getSuccessCount());
        container.set(Constants.TileEntity.CUSTOM_NAME, this.getCommandBlockLogic().getName());
        container.set(Constants.TileEntity.CommandBlock.DOES_TRACK_OUTPUT, this.getCommandBlockLogic().shouldReceiveErrors());
        if (this.getCommandBlockLogic().shouldReceiveErrors()) {
            container.set(Constants.TileEntity.CommandBlock.TRACKED_OUTPUT, SpongeAdventure.legacySection(SpongeAdventure.asAdventure(this.getCommandBlockLogic().getLastOutput())));
        }
        return container;
    }

    @Override
    protected Set<Value.Immutable<?>> api$getVanillaValues() {
        final Set<Value.Immutable<?>> values = super.api$getVanillaValues();

        values.add(this.storedCommand().asImmutable());
        values.add(this.successCount().asImmutable());
        values.add(this.doesTrackOutput().asImmutable());

        this.lastOutput().map(Value::asImmutable).ifPresent(values::add);

        return values;
    }

}
