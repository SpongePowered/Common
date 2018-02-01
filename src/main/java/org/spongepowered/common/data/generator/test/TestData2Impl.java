package org.spongepowered.common.data.generator.test;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.merge.MergeFunction;
import org.spongepowered.api.data.value.BaseValue;
import org.spongepowered.api.data.value.immutable.ImmutableValue;
import org.spongepowered.api.data.value.mutable.OptionalValue;
import org.spongepowered.api.data.value.mutable.Value;
import org.spongepowered.common.data.generator.InternalCopies;
import org.spongepowered.common.data.value.immutable.ImmutableSpongeOptionalValue;
import org.spongepowered.common.data.value.immutable.ImmutableSpongeValue;
import org.spongepowered.common.data.value.mutable.SpongeOptionalValue;
import org.spongepowered.common.data.value.mutable.SpongeValue;

import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

public class TestDataImpl implements TestData    {

    // Static values will be injected through reflection

    static Key<Value<String>> key$my_string;
    static Key<Value<Integer>> key$my_int;
    static Key<OptionalValue<Double>> key$my_opt_double;
    static ImmutableSet<Key<?>> keys;

    // TODO: Inline strings?
    static String default_value$my_string;
    // TODO: Inline primitives
    static int default_value$my_int;
    static Optional<Double> default_value$my_opt_double;

    private String value$my_string = default_value$my_string;
    private int value$my_int = default_value$my_int;
    private Optional<Double> value$my_opt_double = default_value$my_opt_double;

    @Override
    public Optional<TestData> fill(DataHolder dataHolder, MergeFunction overlap) {
        final Optional<TestData> optData = dataHolder.get(TestData.class);
        if (optData.isPresent()) {
            final TestDataImpl data = (TestDataImpl) overlap.merge(this, optData.get());
            this.value$my_string = data.value$my_string;
            this.value$my_int = data.value$my_int;
        }
        return Optional.of(this);
    }

    @Override
    public Optional<TestData> from(DataContainer container) {
        if (container.contains(key$my_string.getQuery())) {
            this.value$my_string = container.getString(key$my_string.getQuery()).get();
        }
        if (container.contains(key$my_int.getQuery())) {
            this.value$my_int = container.getInt(key$my_int.getQuery()).get();
        }
        if (container.contains(key$my_opt_double.getQuery())) {
            this.value$my_int = container.getInt(key$my_int.getQuery()).get();
        }
        return Optional.of(this);
    }

    @Override
    public <E> TestData set(Key<? extends BaseValue<E>> key, E value) {
        checkNotNull(key, "key");
        checkNotNull(value, "value");
        if ((Key) key == key$my_string) {
            this.value$my_string = (String) InternalCopies.copy(value);
            return this;
        }
        if ((Key) key == key$my_int) {
            this.value$my_int = (Integer) value; // No internal copy for primitives
            return this;
        }
        if ((Key) key == key$my_opt_double) {
            this.value$my_opt_double = InternalCopies.copy((Optional<Double>) value);
            return this;
        }
        return this;
    }

    @Override
    public <E> Optional<E> get(Key<? extends BaseValue<E>> key) {
        checkNotNull(key, "key");
        if ((Key) key == key$my_string) {
            return Optional.of((E) InternalCopies.copy(this.value$my_string));
        }
        if ((Key) key == key$my_int) {
            return Optional.of((E) (Object) this.value$my_int); // No internal copy for primitives
        }
        if ((Key) key == key$my_opt_double) {
            return Optional.of((E) InternalCopies.copy(this.value$my_opt_double));
        }
        return Optional.empty();
    }

    @Override
    public <E, V extends BaseValue<E>> Optional<V> getValue(Key<V> key) {
        checkNotNull(key, "key");
        if (key == key$my_string) {
            return Optional.of((V) new SpongeValue<>(key$my_string, default_value$my_string, InternalCopies.copy(this.value$my_string)));
        }
        if (key == key$my_int) {
            return Optional.of((V) new SpongeValue<>(key$my_int, default_value$my_int, this.value$my_int));
        }
        if (key == key$my_opt_double) {
            return Optional.of((V) new SpongeOptionalValue<>(key$my_opt_double, default_value$my_opt_double, InternalCopies.copy(this.value$my_opt_double)));
        }
        return Optional.empty();
    }

    @Override
    public boolean supports(Key<?> key) {
        checkNotNull(key, "key");
        return keys.contains(key);
    }

    @Override
    public TestData copy() {
        TestDataImpl copy = new TestDataImpl();
        copy.value$my_int = this.value$my_int;
        copy.value$my_string = this.value$my_string;
        copy.value$my_opt_double = this.value$my_opt_double;
        return copy;
    }

    @Override
    public Set<Key<?>> getKeys() {
        return keys;
    }

    @Override
    public Set<ImmutableValue<?>> getValues() {
        ImmutableSet.Builder<ImmutableValue<?>> values = ImmutableSet.builder();
        values.add(new ImmutableSpongeValue<>(key$my_string, default_value$my_string, this.value$my_string));
        values.add(new ImmutableSpongeValue<>(key$my_int, default_value$my_int, this.value$my_int));
        values.add(new ImmutableSpongeOptionalValue<>(key$my_opt_double, default_value$my_opt_double, this.value$my_opt_double));
        return values.build();
    }

    @Override
    public ImmutableTestData asImmutable() {
        Immutable immutable = new Immutable();
        immutable.value$my_string = this.value$my_string;
        immutable.value$my_int = this.value$my_int;
        immutable.value$my_opt_double = this.value$my_opt_double;
        return immutable;
    }

    @Override
    public int getContentVersion() {
        return 1;
    }

    @Override
    public DataContainer toContainer() {
        DataContainer dataContainer = DataContainer.createNew();
        dataContainer.set(key$my_string, this.value$my_string);
        dataContainer.set(key$my_int, this.value$my_int);
        if (this.value$my_opt_double.isPresent()) {
            dataContainer.set(key$my_opt_double.getQuery(), this.value$my_opt_double.get());
        }
        return dataContainer;
    }

    @Override
    public String getString() {
        return this.value$my_string;
    }

    @Override
    public Integer getInteger() {
        return this.value$my_int;
    }

    @Override
    public int getInt() {
        return this.value$my_int;
    }

    @Override
    public void setInt(int value) {
        this.value$my_int = value;
    }

    @Override
    public void setInt(Integer value) {
        this.value$my_int = checkNotNull(value);
    }

    @Override
    public Optional<Double> getDouble() {
        return this.value$my_opt_double;
    }

    @Nullable
    @Override
    public Double getNullableDouble() {
        return this.value$my_opt_double.orElse(null);
    }

    @Override
    public void setDouble(@Nullable Double value) {
        this.value$my_opt_double = Optional.ofNullable(value);
    }

    @Override
    public void setDouble(Optional<Double> value) {
        this.value$my_opt_double = checkNotNull(value);
    }

    @Override
    public void setDouble(double value) {
        this.value$my_opt_double = Optional.of(value);
    }

    public static class Immutable implements ImmutableTestData {

        private String value$my_string;
        private int value$my_int;
        private Optional<Double> value$my_opt_double;

        @Override
        public TestData asMutable() {
            TestDataImpl mutable = new TestDataImpl();
            mutable.value$my_string = this.value$my_string;
            mutable.value$my_int = this.value$my_int;
            return mutable;
        }

        @Override
        public int getContentVersion() {
            return 1;
        }

        @Override
        public DataContainer toContainer() {
            return DataContainer.createNew()
                    .set(key$my_string, this.value$my_string)
                    .set(key$my_int, this.value$my_int);
        }

        @Override
        public <E> Optional<E> get(Key<? extends BaseValue<E>> key) {
            checkNotNull(key, "key");
            if ((Key) key == key$my_string) {
                return Optional.of((E) InternalCopies.copy(this.value$my_string));
            }
            if ((Key) key == key$my_int) {
                return Optional.of((E) (Object) this.value$my_int);
            }
            return Optional.empty();
        }

        @Override
        public <E, V extends BaseValue<E>> Optional<V> getValue(Key<V> key) {
            checkNotNull(key, "key");
            if (key == key$my_string) {
                return Optional.of((V) new ImmutableSpongeValue<>(key$my_string, default_value$my_string, this.value$my_string));
            }
            if (key == key$my_int) {
                return Optional.of((V) new ImmutableSpongeValue<>(key$my_int, default_value$my_int, this.value$my_int));
            }
            return Optional.empty();
        }

        @Override
        public boolean supports(Key<?> key) {
            checkNotNull(key, "key");
            return keys.contains(key);
        }

        @Override
        public Set<Key<?>> getKeys() {
            return keys;
        }

        @Override
        public Set<ImmutableValue<?>> getValues() {
            ImmutableSet.Builder<ImmutableValue<?>> values = ImmutableSet.builder();
            values.add(new ImmutableSpongeValue<>(key$my_string, default_value$my_string, this.value$my_string));
            values.add(new ImmutableSpongeValue<>(key$my_int, default_value$my_int, this.value$my_int));
            return values.build();
        }
    }
}
