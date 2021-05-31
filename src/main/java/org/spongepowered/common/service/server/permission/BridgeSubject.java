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
package org.spongepowered.common.service.server.permission;

import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.context.Contextual;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.service.permission.SubjectReference;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.common.bridge.permissions.SubjectBridge;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Common Subject Implementation via SubjectBridge
 */
public interface BridgeSubject extends Subject {

    @Override
    default SubjectReference asSubjectReference() {
        return ((SubjectBridge) this).bridge$resolveReferenceOptional()
                .orElseThrow(() -> new IllegalStateException("No subject reference present for user " + this));
    }

    @Override
    default boolean isSubjectDataPersisted() {
        return ((SubjectBridge) this).bridge$resolveOptional()
                .map(Subject::isSubjectDataPersisted)
                .orElse(false);
    }

    @Override
    default Set<Context> activeContexts() {
        return ((SubjectBridge) this).bridge$resolveOptional()
                .map(Subject::activeContexts)
                .orElseGet(Collections::emptySet);
    }

    @Override
    default Optional<String> friendlyIdentifier() {
        return ((SubjectBridge) this).bridge$resolveOptional()
                .flatMap(Contextual::friendlyIdentifier);
    }

    @Override
    default SubjectCollection containingCollection() {
        return ((SubjectBridge) this).bridge$resolve().containingCollection();
    }

    @Override
    default SubjectData subjectData() {
        return ((SubjectBridge) this).bridge$resolve().subjectData();
    }

    @Override
    default SubjectData transientSubjectData() {
        return ((SubjectBridge) this).bridge$resolve().transientSubjectData();
    }

    @Override
    default Tristate permissionValue(final Set<Context> contexts, final String permission) {
        return ((SubjectBridge) this).bridge$resolveOptional()
                .map(subject -> subject.permissionValue(contexts, permission))
                .orElseGet(() -> ((SubjectBridge) this).bridge$permDefault(permission));
    }

    @Override
    default boolean hasPermission(final Set<Context> contexts, final String permission) {
        return ((SubjectBridge) this).bridge$resolveOptional()
                .map(subject -> {
                    final Tristate ret = subject.permissionValue(contexts, permission);
                    if (ret == Tristate.UNDEFINED) {
                        return ((SubjectBridge) this).bridge$permDefault(permission).asBoolean();
                    }
                    return ret.asBoolean();
                })
                .orElseGet(() -> ((SubjectBridge) this).bridge$permDefault(permission).asBoolean());
    }

    @Override
    default boolean hasPermission(final String permission) {
        // forwarded to the implementation in this class, and not the default
        // in the Subject interface so permission defaults can be applied
        return this.hasPermission(this.activeContexts(), permission);
    }

    @Override
    default boolean isChildOf(final Set<Context> contexts, final SubjectReference parent) {
        return ((SubjectBridge) this).bridge$resolve().isChildOf(contexts, parent);
    }

    @Override
    default boolean isChildOf(final SubjectReference parent) {
        return ((SubjectBridge) this).bridge$resolve().isChildOf(parent);
    }

    @Override
    default List<? extends SubjectReference> parents(final Set<Context> contexts) {
        return ((SubjectBridge) this).bridge$resolve().parents(contexts);
    }

    @Override
    default List<? extends SubjectReference> parents() {
        return ((SubjectBridge) this).bridge$resolve().parents();
    }

    @Override
    default Optional<String> option(final Set<Context> contexts, final String key) {
        return ((SubjectBridge) this).bridge$resolve().option(contexts, key);
    }

    @Override
    default Optional<String> option(final String key) {
        return ((SubjectBridge) this).bridge$resolve().option(key);
    }
}
