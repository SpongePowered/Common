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
package org.spongepowered.common.text.action;

import static com.google.common.base.Preconditions.checkState;

import net.minecraft.entity.EntityList;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.event.HoverEvent;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.HoverAction;
import org.spongepowered.common.entity.SpongeEntityType;
import org.spongepowered.common.text.SpongeTexts;

import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

public abstract class SpongeHoverTextAction<R> extends SpongeTextAction<R> implements HoverAction<R> {

    @Nullable private HoverEvent event;

    SpongeHoverTextAction(R result) {
        super(result);
    }

    @Override
    public final Text toText() {
        return SpongeTexts.toText(this.asEvent().getValue());
    }

    public final HoverEvent asEvent() {
        if (this.event == null) {
            this.event = this.createEvent();
        }
        return this.event;
    }

    abstract HoverEvent createEvent();

    public static final class SpongeShowText extends SpongeHoverTextAction<Text> implements HoverAction.ShowText {

        SpongeShowText(Text result) {
            super(result);
        }

        @Override
        HoverEvent createEvent() {
            return new HoverEvent(HoverEvent.Action.SHOW_TEXT, SpongeTexts.toComponent(this.result));
        }

        public static class Builder implements HoverAction.ShowText.Builder {

            @Nullable private Text text;

            @Override
            public HoverAction.ShowText.Builder text(Text text) {
                this.text = text;
                return this;
            }

            @Override
            public HoverAction.ShowText.Builder from(HoverAction.ShowText value) {
                this.text = value.getResult();
                return this;
            }

            @Override
            public HoverAction.ShowText.Builder reset() {
                this.text = null;
                return this;
            }

            @Override
            public HoverAction.ShowText build() {
                checkState(this.text != null, "text not set");
                return new SpongeShowText(this.text);
            }
        }
    }

    public static final class SpongeShowItem extends SpongeHoverTextAction<ItemStackSnapshot> implements HoverAction.ShowItem {

        SpongeShowItem(ItemStackSnapshot result) {
            super(result);
        }

        @Override
        HoverEvent createEvent() {
            final ItemStack item = (ItemStack) this.result.createStack();
            final NBTTagCompound compound = new NBTTagCompound();
            item.writeToNBT(compound);
            return new HoverEvent(HoverEvent.Action.SHOW_ITEM, new TextComponentString(compound.toString()));
        }

        public static class Builder implements HoverAction.ShowItem.Builder {

            @Nullable private ItemStackSnapshot item;

            @Override
            public HoverAction.ShowItem.Builder item(ItemStackSnapshot item) {
                this.item = item;
                return this;
            }

            @Override
            public HoverAction.ShowItem.Builder from(HoverAction.ShowItem value) {
                this.item = value.getResult();
                return this;
            }

            @Override
            public HoverAction.ShowItem.Builder reset() {
                this.item = null;
                return this;
            }

            @Override
            public HoverAction.ShowItem build() {
                checkState(this.item != null, "item not set");
                return new SpongeShowItem(this.item);
            }
        }
    }

    public static final class SpongeShowEntity extends SpongeHoverTextAction<HoverAction.ShowEntity.Ref> implements HoverAction.ShowEntity {

        SpongeShowEntity(HoverAction.ShowEntity.Ref result) {
            super(result);
        }

        @Override
        HoverEvent createEvent() {
            NBTTagCompound compound = new NBTTagCompound();
            compound.setString("id", this.result.getUniqueId().toString());
            compound.setString("name", this.result.getName());
            if (this.result.getType().isPresent()) {
                compound.setString("type", EntityList.getKey(((SpongeEntityType) this.result.getType().get()).entityClass).toString());
            }
            return new HoverEvent(HoverEvent.Action.SHOW_ENTITY, new TextComponentString(compound.toString()));
        }

        public static class Builder implements HoverAction.ShowEntity.Builder {

            @Nullable private HoverAction.ShowEntity.Ref ref;

            @Override
            public HoverAction.ShowEntity.Builder entity(Entity entity, String name) {
                this.ref = new Ref(entity.getUniqueId(), name, entity.getType());
                return this;
            }

            @Override
            public ShowEntity.Builder entity(ShowEntity.Ref ref) {
                this.ref = ref;
                return this;
            }

            @Override
            public HoverAction.ShowEntity.Builder from(HoverAction.ShowEntity value) {
                this.ref = value.getResult();
                return this;
            }

            @Override
            public HoverAction.ShowEntity.Builder reset() {
                this.ref = null;
                return this;
            }

            @Override
            public HoverAction.ShowEntity build() {
                checkState(this.ref != null, "ref not set");
                return new SpongeShowEntity(this.ref);
            }
        }

        public static class Ref implements HoverAction.ShowEntity.Ref {

            private final UUID uniqueId;
            private final String name;
            @Nullable private final EntityType type;

            Ref(UUID uniqueId, String name, @Nullable EntityType type) {
                this.uniqueId = uniqueId;
                this.name = name;
                this.type = type;
            }

            @Override
            public UUID getUniqueId() {
                return this.uniqueId;
            }

            @Override
            public String getName() {
                return this.name;
            }

            @Override
            public Optional<EntityType> getType() {
                return Optional.ofNullable(this.type);
            }

            public static class Builder implements HoverAction.ShowEntity.Ref.Builder {

                @Nullable private UUID uniqueId;
                @Nullable private String name;
                @Nullable private EntityType type;

                @Override
                public Ref.Builder uniqueId(UUID uniqueId) {
                    this.uniqueId = uniqueId;
                    return this;
                }

                @Override
                public Ref.Builder name(String name) {
                    this.name = name;
                    return this;
                }

                @Override
                public Ref.Builder type(@Nullable EntityType type) {
                    this.type = type;
                    return this;
                }

                @Override
                public Ref.Builder from(HoverAction.ShowEntity.Ref value) {
                    this.uniqueId = value.getUniqueId();
                    this.name = value.getName();
                    this.type = value.getType().orElse(null);
                    return this;
                }

                @Override
                public Ref.Builder reset() {
                    this.uniqueId = null;
                    this.name = null;
                    this.type = null;
                    return this;
                }

                @Override
                public Ref build() {
                    checkState(this.uniqueId != null, "unique id not set");
                    checkState(this.name != null, "name not set");
                    return new Ref(this.uniqueId, this.name, this.type);
                }
            }
        }
    }
}
