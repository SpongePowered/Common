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
package org.spongepowered.common.util;

import net.minecraft.world.damagesource.DamageSources;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.common.SpongeCommon;

public final class SpongeDamageSourceFactory implements DamageSource.Factory {

    private DamageSources damageSources;

    private DamageSources damageSources() {
        if (this.damageSources == null) {
            this.damageSources = new DamageSources(SpongeCommon.vanillaRegistryAccess());
        }
        return damageSources;
    }

    @Override
    public DamageSource drowning() {
        return (DamageSource) this.damageSources().drown();
    }

    @Override
    public DamageSource dryout() {
        return (DamageSource) this.damageSources().dryOut();
    }

    @Override
    public DamageSource falling() {
        return (DamageSource) this.damageSources().fall();
    }

    @Override
    public DamageSource fireTick() {
        return (DamageSource) this.damageSources().onFire();
    }

    @Override
    public DamageSource generic() {
        return (DamageSource) this.damageSources().generic();
    }

    @Override
    public DamageSource magic() {
        return (DamageSource) this.damageSources().magic();
    }

    @Override
    public DamageSource starvation() {
        return (DamageSource) this.damageSources().starve();
    }

    @Override
    public DamageSource voidSource() {
        return (DamageSource) this.damageSources().fellOutOfWorld();
    }

    @Override
    public DamageSource wither() {
        return (DamageSource) this.damageSources().wither();
    }
}
