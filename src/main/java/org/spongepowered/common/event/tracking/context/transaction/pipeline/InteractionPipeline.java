package org.spongepowered.common.event.tracking.context.transaction.pipeline;

import net.minecraft.world.InteractionResult;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.context.transaction.ResultingTransactionBySideEffect;
import org.spongepowered.common.event.tracking.context.transaction.TransactionalCaptureSupplier;
import org.spongepowered.common.event.tracking.context.transaction.effect.ProcessingSideEffect;

import java.util.Objects;

public record InteractionPipeline<@NonNull A extends ProcessingSideEffect.Args>(
    A args,
    InteractionResult defaultResult,
    ProcessingSideEffect<InteractionPipeline<A>, InteractionResult, A, InteractionResult> effect,
    TransactionalCaptureSupplier transactor
) {

    public InteractionResult processInteraction(final PhaseContext<?> context) {
        var result = this.defaultResult;
        final ResultingTransactionBySideEffect<InteractionPipeline<A>, InteractionResult, A, InteractionResult> resultingEffect = new ResultingTransactionBySideEffect<>(this.effect);
        try (final var ignored = context.getTransactor().pushEffect(resultingEffect)) {
            final var effectResult = this.effect.processSideEffect(this, result, this.args);
            if (effectResult.hasResult) {
                final @Nullable InteractionResult resultingState = effectResult.resultingState;
                result = Objects.requireNonNullElse(resultingState, result);
            }
        }
        return result;
    }
}
