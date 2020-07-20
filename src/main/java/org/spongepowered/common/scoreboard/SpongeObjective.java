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
package org.spongepowered.common.scoreboard;

import com.google.common.collect.Maps;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.minecraft.scoreboard.ScoreCriteria;
import net.minecraft.scoreboard.ScoreObjective;
import org.spongepowered.api.scoreboard.Score;
import org.spongepowered.api.scoreboard.Scoreboard;
import org.spongepowered.api.scoreboard.criteria.Criterion;
import org.spongepowered.api.scoreboard.objective.Objective;
import org.spongepowered.api.scoreboard.objective.displaymode.ObjectiveDisplayMode;
import org.spongepowered.api.scoreboard.objective.displaymode.ObjectiveDisplayModes;
import org.spongepowered.common.accessor.scoreboard.ScoreAccessor;
import org.spongepowered.common.accessor.scoreboard.ScoreObjectiveAccessor;
import org.spongepowered.common.accessor.scoreboard.ScoreboardAccessor;
import org.spongepowered.common.adventure.SpongeAdventure;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class SpongeObjective implements Objective {

    private Map<net.minecraft.scoreboard.Scoreboard, ScoreObjective> objectives = new HashMap<>();

    private String name;
    private Component displayName;
    private Criterion criterion;
    private ObjectiveDisplayMode displayMode;
    private Map<Component, Score> scores = new HashMap<>();

    public SpongeObjective(final String name, final Criterion criterion) {
        this.name = name;
        this.displayName = SpongeAdventure.legacy(LegacyComponentSerializer.SECTION_CHAR, name);
        this.displayMode = ObjectiveDisplayModes.INTEGER.get();
        this.criterion = criterion;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Component getDisplayName() {
        return this.displayName;
    }

    @Override
    public void setDisplayName(final Component displayName) throws IllegalArgumentException {
        this.displayName = displayName;
        this.updateDisplayName();
    }

    private void updateDisplayName() {
        for (final ScoreObjective objective: this.objectives.values()) {
            objective.setDisplayName(SpongeAdventure.asVanilla(this.displayName));
        }
    }

    @Override
    public Criterion getCriterion() {
        return this.criterion;
    }

    @Override
    public ObjectiveDisplayMode getDisplayMode() {
        return this.displayMode;
    }

    @Override
    public void setDisplayMode(final ObjectiveDisplayMode displayMode) {
        this.displayMode = displayMode;
        this.updateDisplayMode();

    }

    @SuppressWarnings("ConstantConditions")
    private void updateDisplayMode() {
        for (final ScoreObjective objective: this.objectives.values()) {
            objective.setRenderType((ScoreCriteria.RenderType) (Object) this.displayMode);
        }
    }

    @Override
    public Map<Component, Score> getScores() {
        return new HashMap<>(this.scores);
    }

    @Override
    public boolean hasScore(final Component name) {
        return this.scores.containsKey(name);
    }

    @Override
    public void addScore(final Score score) throws IllegalArgumentException {
        if (this.scores.containsKey(score.getName())) {
            throw new IllegalArgumentException(String.format("A score with the name %s already exists!",
                    SpongeAdventure.legacySection(score.getName())));
        }
        this.scores.put(score.getName(), score);

        final SpongeScore spongeScore = (SpongeScore) score;
        for (final ScoreObjective objective: this.objectives.values()) {
            this.addScoreToScoreboard(((ScoreObjectiveAccessor) objective).accessor$getScoreboard(), spongeScore.getScoreFor(objective));
        }
    }

    public void updateScores(final net.minecraft.scoreboard.Scoreboard scoreboard) {
        final ScoreObjective objective = this.getObjectiveFor(scoreboard);

        for (final Score score: this.getScores().values()) {
            final SpongeScore spongeScore = (SpongeScore) score;
            this.addScoreToScoreboard(scoreboard, spongeScore.getScoreFor(objective));
        }
    }

    private void addScoreToScoreboard(final net.minecraft.scoreboard.Scoreboard scoreboard, final net.minecraft.scoreboard.Score score) {
        final String name = score.getPlayerName();
        final Map<ScoreObjective, net.minecraft.scoreboard.Score> scoreMap = ((ScoreboardAccessor) scoreboard).accessor$getEntitiesScoreObjectives()
            .computeIfAbsent(name, k -> Maps.newHashMap());

        scoreMap.put(((ScoreAccessor) score).accessor$getObjective(), score);

        // Trigger refresh
        ((ScoreAccessor) score).accessor$setForceUpdate(true);
        score.setScorePoints(((ScoreAccessor) score).accessor$getScorePoints());
    }

    @Override
    public Optional<Score> getScore(final Component name) {
        return Optional.ofNullable(this.scores.get(name));
    }

    @Override
    public Score getOrCreateScore(final Component name) {
        if (this.scores.containsKey(name)) {
            return this.scores.get(name);
        }

        final SpongeScore score = new SpongeScore(name);
        this.addScore(score);
        return score;
    }

    @Override
    public boolean removeScore(final Score spongeScore) {
        final String name = ((SpongeScore) spongeScore).legacyName;

        if (!this.scores.containsKey(spongeScore.getName())) {
            return false;
        }

        for (final ScoreObjective objective: this.objectives.values()) {
            final net.minecraft.scoreboard.Scoreboard scoreboard = ((ScoreObjectiveAccessor) objective).accessor$getScoreboard();


            final Map<?, ?> map = ((ScoreboardAccessor) scoreboard).accessor$getEntitiesScoreObjectives().get(name);

            if (map != null) {
                final net.minecraft.scoreboard.Score score = (net.minecraft.scoreboard.Score) map.remove(objective);


                if (map.size() < 1) {
                    final Map<?, ?> map1 = ((ScoreboardAccessor) scoreboard).accessor$getEntitiesScoreObjectives().remove(name);

                    if (map1 != null) {
                        scoreboard.onPlayerRemoved(name);
                    }
                } else if (score != null) {
                    scoreboard.onPlayerScoreRemoved(name, objective);
                }
            }
            ((SpongeScore) spongeScore).removeScoreFor(objective);
        }

        this.scores.remove(spongeScore.getName());
        return true;
    }

    @Override
    public boolean removeScore(final Component name) {
        final Optional<Score> score = this.getScore(name);
        return score.filter(this::removeScore).isPresent();
    }

    @SuppressWarnings("ConstantConditions")
    public ScoreObjective getObjectiveFor(final net.minecraft.scoreboard.Scoreboard scoreboard) {
        if (this.objectives.containsKey(scoreboard)) {
            return this.objectives.get(scoreboard);
        }
        final ScoreObjective objective = new ScoreObjective(scoreboard, this.name, (ScoreCriteria) this.criterion,
            SpongeAdventure.asVanilla(this.displayName), (ScoreCriteria.RenderType) (Object) this.displayMode);
        this.objectives.put(scoreboard, objective);
        return objective;
    }

    public void removeObjectiveFor(final net.minecraft.scoreboard.Scoreboard scoreboard) {
        if (this.objectives.remove(scoreboard) == null) {
            throw new IllegalStateException("Attempting to remove an objective without an entry!");
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<Scoreboard> getScoreboards() {
        return (Set<Scoreboard>) (Set<?>) new HashSet<>(this.objectives.keySet());
    }

    public Collection<ScoreObjective> getObjectives() {
        return this.objectives.values();
    }
}
