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
package org.spongepowered.common.event.tracking.phase.packet;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundTeleportToEntityPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.accessor.network.protocol.game.ServerboundInteractPacketAccessor;
import org.spongepowered.common.event.tracking.IPhaseState;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.phase.packet.drag.DragInventoryAddSlotState;
import org.spongepowered.common.event.tracking.phase.packet.drag.DragInventoryStartState;
import org.spongepowered.common.event.tracking.phase.packet.drag.MiddleDragInventoryStopState;
import org.spongepowered.common.event.tracking.phase.packet.drag.PrimaryDragInventoryStopState;
import org.spongepowered.common.event.tracking.phase.packet.drag.SecondaryDragInventoryStopState;
import org.spongepowered.common.event.tracking.phase.packet.inventory.BasicInventoryPacketState;
import org.spongepowered.common.event.tracking.phase.packet.inventory.CloseWindowState;
import org.spongepowered.common.event.tracking.phase.packet.inventory.CreativeInventoryPacketState;
import org.spongepowered.common.event.tracking.phase.packet.inventory.DoubleClickInventoryState;
import org.spongepowered.common.event.tracking.phase.packet.inventory.DropInventoryState;
import org.spongepowered.common.event.tracking.phase.packet.inventory.DropItemInsideWindowState;
import org.spongepowered.common.event.tracking.phase.packet.inventory.DropItemOutsideWindowState;
import org.spongepowered.common.event.tracking.phase.packet.inventory.DropItemWithHotkeyState;
import org.spongepowered.common.event.tracking.phase.packet.inventory.EnchantItemPacketState;
import org.spongepowered.common.event.tracking.phase.packet.inventory.MiddleInventoryClickState;
import org.spongepowered.common.event.tracking.phase.packet.inventory.OpenInventoryState;
import org.spongepowered.common.event.tracking.phase.packet.inventory.PlaceRecipePacketState;
import org.spongepowered.common.event.tracking.phase.packet.inventory.PrimaryInventoryClickState;
import org.spongepowered.common.event.tracking.phase.packet.inventory.PrimaryInventoryShiftClick;
import org.spongepowered.common.event.tracking.phase.packet.inventory.SecondaryInventoryClickState;
import org.spongepowered.common.event.tracking.phase.packet.inventory.SecondaryInventoryShiftClickState;
import org.spongepowered.common.event.tracking.phase.packet.inventory.SetCarriedItemState;
import org.spongepowered.common.event.tracking.phase.packet.inventory.SwapHandItemsState;
import org.spongepowered.common.event.tracking.phase.packet.inventory.SwitchHotbarNumberPressState;
import org.spongepowered.common.event.tracking.phase.packet.player.AnimationPacketState;
import org.spongepowered.common.event.tracking.phase.packet.player.AttackEntityPacketState;
import org.spongepowered.common.event.tracking.phase.packet.player.IgnoredPacketState;
import org.spongepowered.common.event.tracking.phase.packet.player.InteractAtEntityPacketState;
import org.spongepowered.common.event.tracking.phase.packet.player.InteractEntityPacketState;
import org.spongepowered.common.event.tracking.phase.packet.player.InteractionPacketContext;
import org.spongepowered.common.event.tracking.phase.packet.player.InteractionPacketState;
import org.spongepowered.common.event.tracking.phase.packet.player.InvalidPacketState;
import org.spongepowered.common.event.tracking.phase.packet.player.MovementPacketState;
import org.spongepowered.common.event.tracking.phase.packet.player.PacketCommandState;
import org.spongepowered.common.event.tracking.phase.packet.player.PlaceBlockPacketState;
import org.spongepowered.common.event.tracking.phase.packet.player.PlayerCommandPhaseContext;
import org.spongepowered.common.event.tracking.phase.packet.player.StopSleepingPacketState;
import org.spongepowered.common.event.tracking.phase.packet.player.UnknownPacketState;
import org.spongepowered.common.event.tracking.phase.packet.player.UseItemPacketState;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.util.Preconditions;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public final class PacketPhase {

    public static final class General {

        public static final IPhaseState<BasicPacketContext> UNKNOWN = new UnknownPacketState();
        static final IPhaseState<BasicPacketContext> MOVEMENT = new MovementPacketState();
        static final IPhaseState<InteractionPacketContext> INTERACTION = new InteractionPacketState();
        static final IPhaseState<BasicPacketContext> IGNORED = new IgnoredPacketState();
        static final IPhaseState<BasicPacketContext> INTERACT_ENTITY = new InteractEntityPacketState();
        static final IPhaseState<BasicPacketContext> ATTACK_ENTITY = new AttackEntityPacketState();
        static final IPhaseState<BasicPacketContext> INTERACT_AT_ENTITY = new InteractAtEntityPacketState();
        static final IPhaseState<BasicPacketContext> CREATIVE_INVENTORY = new CreativeInventoryPacketState();
        static final IPhaseState<BasicPacketContext> PLACE_BLOCK = new PlaceBlockPacketState();
        static final IPhaseState<BasicPacketContext> REQUEST_RESPAWN = new BasicPacketState();
        static final IPhaseState<BasicPacketContext> USE_ITEM = new UseItemPacketState();
        static final IPhaseState<BasicPacketContext> INVALID = new InvalidPacketState();
        static final IPhaseState<BasicPacketContext> START_RIDING_JUMP = new BasicPacketState();
        static final IPhaseState<BasicPacketContext> ANIMATION = new AnimationPacketState();
        static final IPhaseState<BasicPacketContext> PRESS_SHIFT_KEY = new BasicPacketState();
        static final IPhaseState<BasicPacketContext> RELEASE_SHIFT_KEY = new BasicPacketState();
        static final IPhaseState<BasicPacketContext> START_SPRINTING = new BasicPacketState();
        static final IPhaseState<BasicPacketContext> STOP_SPRINTING = new BasicPacketState();
        static final IPhaseState<BasicPacketContext> STOP_SLEEPING = new StopSleepingPacketState();
        static final IPhaseState<BasicPacketContext> TAB_COMPLETE = new BasicPacketState();
        static final CloseWindowState CLOSE_WINDOW = new CloseWindowState();
        public static final IPhaseState<BasicPacketContext> UPDATE_SIGN = new BasicPacketState();
        static final IPhaseState<BasicPacketContext> STOP_RIDING_JUMP = new BasicPacketState();
        static final IPhaseState<BasicPacketContext> HANDLED_EXTERNALLY = new UnknownPacketState();
        static final IPhaseState<BasicPacketContext> START_FALL_FLYING = new BasicPacketState();
        static final IPhaseState<PlayerCommandPhaseContext> CHAT_COMMAND = new PacketCommandState();
    }

    public static final class Inventory {
        static final BasicInventoryPacketState INVENTORY = new BasicInventoryPacketState();
        static final BasicInventoryPacketState PRIMARY_INVENTORY_CLICK = new PrimaryInventoryClickState();
        static final BasicInventoryPacketState SECONDARY_INVENTORY_CLICK = new SecondaryInventoryClickState();
        static final BasicInventoryPacketState MIDDLE_INVENTORY_CLICK = new MiddleInventoryClickState();
        static final BasicInventoryPacketState DROP_ITEM_OUTSIDE_WINDOW = new DropItemOutsideWindowState(
                Constants.Networking.MODE_CLICK | Constants.Networking.BUTTON_PRIMARY | Constants.Networking.BUTTON_SECONDARY | Constants.Networking.CLICK_OUTSIDE_WINDOW);
        static final BasicInventoryPacketState DROP_ITEM_INSIDE_WINDOW = new DropItemInsideWindowState();
        static final BasicInventoryPacketState DROP_ITEM_WITH_HOTKEY = new DropItemWithHotkeyState();
        static final BasicInventoryPacketState DROP_ITEM_OUTSIDE_WINDOW_NOOP = new DropItemOutsideWindowState(
                Constants.Networking.MODE_DROP | Constants.Networking.BUTTON_PRIMARY | Constants.Networking.BUTTON_SECONDARY | Constants.Networking.CLICK_OUTSIDE_WINDOW);
        static final BasicInventoryPacketState DROP_ITEMS = new BasicInventoryPacketState();
        static final BasicInventoryPacketState DROP_INVENTORY = new DropInventoryState();
        static final BasicInventoryPacketState SWITCH_HOTBAR_NUMBER_PRESS = new SwitchHotbarNumberPressState();
        static final BasicInventoryPacketState PRIMARY_INVENTORY_SHIFT_CLICK = new PrimaryInventoryShiftClick();
        static final BasicInventoryPacketState SECONDARY_INVENTORY_SHIFT_CLICK = new SecondaryInventoryShiftClickState();
        static final BasicInventoryPacketState DOUBLE_CLICK_INVENTORY = new DoubleClickInventoryState();

        static final BasicInventoryPacketState PRIMARY_DRAG_INVENTORY_START = new DragInventoryStartState("PRIMARY_DRAG_INVENTORY_START", Constants.Networking.DRAG_MODE_PRIMARY_BUTTON);
        static final BasicInventoryPacketState SECONDARY_DRAG_INVENTORY_START = new DragInventoryStartState("SECONDARY_DRAG_INVENTORY_START", Constants.Networking.DRAG_MODE_SECONDARY_BUTTON);
        static final BasicInventoryPacketState MIDDLE_DRAG_INVENTORY_START = new DragInventoryStartState("MIDDLE_DRAG_INVENTORY_START", Constants.Networking.DRAG_MODE_MIDDLE_BUTTON);

        static final BasicInventoryPacketState PRIMARY_DRAG_INVENTORY_ADDSLOT = new DragInventoryAddSlotState("PRIMARY_DRAG_INVENTORY_ADD_SLOT", Constants.Networking.DRAG_MODE_PRIMARY_BUTTON);
        static final BasicInventoryPacketState SECONDARY_DRAG_INVENTORY_ADDSLOT = new DragInventoryAddSlotState("SECONDARY_DRAG_INVENTORY_ADD_SLOT", Constants.Networking.DRAG_MODE_SECONDARY_BUTTON);
        static final BasicInventoryPacketState MIDDLE_DRAG_INVENTORY_ADDSLOT = new DragInventoryAddSlotState("MIDDLE_DRAG_INVENTORY_ADD_SLOT", Constants.Networking.DRAG_MODE_MIDDLE_BUTTON);

        static final BasicInventoryPacketState PRIMARY_DRAG_INVENTORY_STOP = new PrimaryDragInventoryStopState();
        static final BasicInventoryPacketState SECONDARY_DRAG_INVENTORY_STOP = new SecondaryDragInventoryStopState();
        static final BasicInventoryPacketState MIDDLE_DRAG_INVENTORY_STOP = new MiddleDragInventoryStopState();

        static final BasicInventoryPacketState SET_CARRIED_ITEM = new SetCarriedItemState();
        static final BasicInventoryPacketState OPEN_INVENTORY = new OpenInventoryState();
        static final BasicInventoryPacketState ENCHANT_ITEM = new EnchantItemPacketState();
        static final BasicInventoryPacketState SWAP_HAND_ITEMS = new SwapHandItemsState();

        static final BasicInventoryPacketState PLACE_RECIPE = new PlaceRecipePacketState();

        static final ImmutableList<BasicInventoryPacketState> VALUES = ImmutableList.<BasicInventoryPacketState>builder()
                .add(Inventory.INVENTORY)
                .add(Inventory.PRIMARY_INVENTORY_CLICK)
                .add(Inventory.SECONDARY_INVENTORY_CLICK)
                .add(Inventory.MIDDLE_INVENTORY_CLICK)
                .add(Inventory.DROP_ITEM_OUTSIDE_WINDOW)
                .add(Inventory.DROP_ITEM_INSIDE_WINDOW)
                .add(Inventory.DROP_ITEM_OUTSIDE_WINDOW_NOOP)
                .add(Inventory.DROP_ITEMS)
                .add(Inventory.DROP_INVENTORY)
                .add(Inventory.SWITCH_HOTBAR_NUMBER_PRESS)
                .add(Inventory.PRIMARY_INVENTORY_SHIFT_CLICK)
                .add(Inventory.SECONDARY_INVENTORY_SHIFT_CLICK)
                .add(Inventory.DOUBLE_CLICK_INVENTORY)

                .add(Inventory.PRIMARY_DRAG_INVENTORY_START)
                .add(Inventory.SECONDARY_DRAG_INVENTORY_START)
                .add(Inventory.MIDDLE_DRAG_INVENTORY_START)

                .add(Inventory.PRIMARY_DRAG_INVENTORY_ADDSLOT)
                .add(Inventory.SECONDARY_DRAG_INVENTORY_ADDSLOT)
                .add(Inventory.MIDDLE_DRAG_INVENTORY_ADDSLOT)

                .add(Inventory.PRIMARY_DRAG_INVENTORY_STOP)
                .add(Inventory.SECONDARY_DRAG_INVENTORY_STOP)
                .add(Inventory.MIDDLE_DRAG_INVENTORY_STOP)

                .add(Inventory.SET_CARRIED_ITEM)
                .add(Inventory.OPEN_INVENTORY)
                .add(Inventory.ENCHANT_ITEM)
                .build();

    }


    private final Map<Class<? extends Packet<?>>, Function<Packet<?>, IPhaseState<? extends PacketContext<?>>>> packetTranslationMap = new IdentityHashMap<>();

    // General use methods

    boolean isPacketInvalid(final Packet<?> packetIn, final ServerPlayer packetPlayer, final IPhaseState<? extends PacketContext<?>> packetState) {
        return ((PacketState<?>) packetState).isPacketIgnored(packetIn, packetPlayer);
    }

    IPhaseState<? extends PacketContext<?>> getStateForPacket(final Packet<?> packet) {
        final Function<Packet<?>, IPhaseState<? extends PacketContext<?>>> packetStateFunction = this.packetTranslationMap.get(packet.getClass());
        if (packetStateFunction != null) {
            return packetStateFunction.apply(packet);
        }
        return PacketPhase.General.UNKNOWN;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public PhaseContext<?> populateContext(final Packet<?> packet, final ServerPlayer entityPlayerMP, final IPhaseState<?> state, final PhaseContext<?> context) {
        Objects.requireNonNull(packet, "Packet cannot be null!");
        Preconditions.checkArgument(!context.isComplete(), "PhaseContext cannot be marked as completed!");
        ((PacketState) state).populateContext(entityPlayerMP, packet, (PacketContext) context);
        return context;
    }

    // Inventory packet specific methods

    private static BasicInventoryPacketState fromWindowPacket(final ServerboundContainerClickPacket windowPacket) {
        final int mode = 0x01 << 9 << windowPacket.getClickType().ordinal();
        final int packed = windowPacket.getButtonNum();
        final int unpacked = mode == Constants.Networking.MODE_DRAG ? (0x01 << 6 << (packed >> 2 & 3)) | (0x01 << 3 << (packed & 3)) : (0x01 << (packed & 3));

        final BasicInventoryPacketState inventory = PacketPhase.fromState(PacketPhase.clickType(windowPacket.getSlotNum()) | mode | unpacked);
        if (inventory == PacketPhase.Inventory.INVENTORY) {
            SpongeCommon.logger().warn(String.format("Unable to find InventoryPacketState handler for click window packet: %s", windowPacket));
        }
        return inventory;
    }


    private static int clickType(final int slotId) {
        return (slotId == Constants.Networking.MAGIC_CLICK_OUTSIDE_SURVIVAL
                || slotId == Constants.Networking.MAGIC_CLICK_OUTSIDE_CREATIVE)
                ? Constants.Networking.CLICK_OUTSIDE_WINDOW : Constants.Networking.CLICK_INSIDE_WINDOW;
    }


    private static BasicInventoryPacketState fromState(final int state) {
        for (final BasicInventoryPacketState inventory : PacketPhase.Inventory.VALUES) {
            if (inventory.matches(state)) {
                return inventory;
            }
        }
        return PacketPhase.Inventory.INVENTORY;
    }

    // General methods

    public static PacketPhase getInstance() {
        return PacketPhase.Holder.INSTANCE;
    }

    @SuppressWarnings("WeakerAccess")
    PacketPhase() {
        this.setupPacketToStateMapping();
    }

    private static final class Holder {
        static final PacketPhase INSTANCE = new PacketPhase();
    }


    private void setupPacketToStateMapping() {
        this.packetTranslationMap.put(ServerboundKeepAlivePacket.class, packet -> PacketPhase.General.IGNORED);
        this.packetTranslationMap.put(ServerboundChatPacket.class, packet -> PacketPhase.General.CHAT_COMMAND);
        this.packetTranslationMap.put(ServerboundInteractPacket.class, packet -> {
            final ServerboundInteractPacket useEntityPacket = (ServerboundInteractPacket) packet;
            final ServerboundInteractPacket.ActionType action = ((ServerboundInteractPacketAccessor) useEntityPacket).accessor$action().getType();
            if (action == ServerboundInteractPacket.ActionType.INTERACT) {
                return PacketPhase.General.INTERACT_ENTITY;
            } else if (action == ServerboundInteractPacket.ActionType.ATTACK) {
                return PacketPhase.General.ATTACK_ENTITY;
            } else if (action == ServerboundInteractPacket.ActionType.INTERACT_AT) {
                return PacketPhase.General.INTERACT_AT_ENTITY;
            } else {
                return PacketPhase.General.INVALID;
            }
        });
        this.packetTranslationMap.put(ServerboundMovePlayerPacket.class, packet -> PacketPhase.General.MOVEMENT);
        this.packetTranslationMap.put(ServerboundMovePlayerPacket.Pos.class, packet -> PacketPhase.General.MOVEMENT);
        this.packetTranslationMap.put(ServerboundMovePlayerPacket.Rot.class, packet -> PacketPhase.General.MOVEMENT);
        this.packetTranslationMap.put(ServerboundMovePlayerPacket.PosRot.class, packet -> PacketPhase.General.MOVEMENT);
        this.packetTranslationMap.put(ServerboundPlayerActionPacket.class, packet -> {
            final ServerboundPlayerActionPacket playerDigging = (ServerboundPlayerActionPacket) packet;
            final ServerboundPlayerActionPacket.Action action = playerDigging.getAction();
            final IPhaseState<? extends PacketContext<?>> state = PacketPhase.INTERACTION_ACTION_MAPPINGS.get(action);
            return state == null ? PacketPhase.General.UNKNOWN : state;
        });
        this.packetTranslationMap.put(ServerboundUseItemOnPacket.class, packet -> General.PLACE_BLOCK);
        this.packetTranslationMap.put(ServerboundUseItemPacket.class, packet -> PacketPhase.General.USE_ITEM);
        this.packetTranslationMap.put(ServerboundSetCarriedItemPacket.class, packet -> PacketPhase.Inventory.SET_CARRIED_ITEM);
        this.packetTranslationMap.put(ServerboundSwingPacket.class, packet -> PacketPhase.General.ANIMATION);
        this.packetTranslationMap.put(ServerboundPlayerCommandPacket.class, packet -> {
            final ServerboundPlayerCommandPacket playerAction = (ServerboundPlayerCommandPacket) packet;
            final ServerboundPlayerCommandPacket.Action action = playerAction.getAction();
            return PacketPhase.PLAYER_ACTION_MAPPINGS.get(action);
        });
        this.packetTranslationMap.put(ServerboundPlayerInputPacket.class, packet -> PacketPhase.General.HANDLED_EXTERNALLY);
        this.packetTranslationMap.put(ServerboundContainerClosePacket.class, packet -> PacketPhase.General.CLOSE_WINDOW);
        this.packetTranslationMap.put(ServerboundContainerClickPacket.class, packet -> PacketPhase.fromWindowPacket((ServerboundContainerClickPacket) packet));
        this.packetTranslationMap.put(ServerboundSetCreativeModeSlotPacket.class, packet -> PacketPhase.General.CREATIVE_INVENTORY);
        this.packetTranslationMap.put(ServerboundContainerButtonClickPacket.class, packet -> PacketPhase.Inventory.ENCHANT_ITEM);
        this.packetTranslationMap.put(ServerboundSignUpdatePacket.class, packet -> PacketPhase.General.UPDATE_SIGN);
        this.packetTranslationMap.put(ServerboundPlayerAbilitiesPacket.class, packet -> PacketPhase.General.IGNORED);
        this.packetTranslationMap.put(ServerboundCommandSuggestionPacket.class, packet -> PacketPhase.General.TAB_COMPLETE);
        this.packetTranslationMap.put(ServerboundClientCommandPacket.class, packet -> {
            final ServerboundClientCommandPacket clientStatus = (ServerboundClientCommandPacket) packet;
            final ServerboundClientCommandPacket.Action status = clientStatus.getAction();
            if (status == ServerboundClientCommandPacket.Action.PERFORM_RESPAWN) {
                return PacketPhase.General.REQUEST_RESPAWN;
            }
            return PacketPhase.General.IGNORED;
        });
        this.packetTranslationMap.put(ServerboundCustomPayloadPacket.class, packet -> PacketPhase.General.HANDLED_EXTERNALLY);
        this.packetTranslationMap.put(ServerboundTeleportToEntityPacket.class, packet -> PacketPhase.General.IGNORED);
        this.packetTranslationMap.put(ServerboundResourcePackPacket.class, packet -> PacketPhase.General.HANDLED_EXTERNALLY);
        this.packetTranslationMap.put(ServerboundPlaceRecipePacket.class, packet -> PacketPhase.Inventory.PLACE_RECIPE);
    }

    private static final ImmutableMap<ServerboundPlayerCommandPacket.Action, IPhaseState<? extends PacketContext<?>>> PLAYER_ACTION_MAPPINGS = ImmutableMap.<ServerboundPlayerCommandPacket.Action, IPhaseState<? extends PacketContext<?>>>builder()
            .put(ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY, PacketPhase.General.PRESS_SHIFT_KEY)
            .put(ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY, PacketPhase.General.RELEASE_SHIFT_KEY)
            .put(ServerboundPlayerCommandPacket.Action.STOP_SLEEPING, PacketPhase.General.STOP_SLEEPING)
            .put(ServerboundPlayerCommandPacket.Action.START_SPRINTING, PacketPhase.General.START_SPRINTING)
            .put(ServerboundPlayerCommandPacket.Action.STOP_SPRINTING, PacketPhase.General.STOP_SPRINTING)
            .put(ServerboundPlayerCommandPacket.Action.START_RIDING_JUMP, PacketPhase.General.START_RIDING_JUMP)
            .put(ServerboundPlayerCommandPacket.Action.STOP_RIDING_JUMP, PacketPhase.General.STOP_RIDING_JUMP)
            .put(ServerboundPlayerCommandPacket.Action.OPEN_INVENTORY, PacketPhase.Inventory.OPEN_INVENTORY)
            .put(ServerboundPlayerCommandPacket.Action.START_FALL_FLYING, PacketPhase.General.START_FALL_FLYING)
            .build();

    private static final ImmutableMap<ServerboundPlayerActionPacket.Action, IPhaseState<? extends PacketContext<?>>> INTERACTION_ACTION_MAPPINGS = ImmutableMap.<ServerboundPlayerActionPacket.Action, IPhaseState<? extends PacketContext<?>>>builder()
            .put(ServerboundPlayerActionPacket.Action.DROP_ITEM, Inventory.DROP_ITEM_WITH_HOTKEY)
            .put(ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS, PacketPhase.Inventory.DROP_ITEM_WITH_HOTKEY)
            .put(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, PacketPhase.General.INTERACTION)
            .put(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, PacketPhase.General.INTERACTION)
            .put(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, PacketPhase.General.INTERACTION)
            .put(ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM, PacketPhase.General.INTERACTION)
            .put(ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, PacketPhase.Inventory.SWAP_HAND_ITEMS)
            .build();

}
