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
package org.spongepowered.common.data.provider.item.stack;

import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PlayerHeadItem;
import org.spongepowered.api.data.Keys;
import org.spongepowered.common.data.provider.DataProviderRegistrator;
import org.spongepowered.common.profile.SpongeGameProfile;
import org.spongepowered.common.util.Constants;

public final class SkullItemStackData {

    private SkullItemStackData() {
    }

    // @formatter:off
    public static void register(final DataProviderRegistrator registrator) {
        registrator
                .asMutable(ItemStack.class)
                    .create(Keys.GAME_PROFILE)
                        .get(h -> {
                            final CompoundTag tag = h.getTagElement(Constants.Item.Skull.ITEM_SKULL_OWNER);
                            final GameProfile mcProfile = tag == null ? null : NbtUtils.readGameProfile(tag);
                            return mcProfile == null ? null : SpongeGameProfile.of(mcProfile);
                        })
                        .set((h, v) -> {
                            final com.mojang.authlib.GameProfile mcProfile = SpongeGameProfile.toMcProfile(v);
                            final CompoundTag tag = NbtUtils.writeGameProfile(new CompoundTag(), mcProfile);
                            h.addTagElement(Constants.Item.Skull.ITEM_SKULL_OWNER, tag);
                        })
                        .delete(h -> h.removeTagKey(Constants.Item.Skull.ITEM_SKULL_OWNER))
                        .supports(h -> h.getItem() instanceof PlayerHeadItem);
    }
    // @formatter:on
}
