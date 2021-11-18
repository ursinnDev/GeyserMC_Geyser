/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.connector.entity;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.MetadataType;
import com.github.steveice10.mc.protocol.data.game.entity.type.EntityType;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.geysermc.connector.entity.factory.BaseEntityFactory;
import org.geysermc.connector.entity.factory.EntityFactory;
import org.geysermc.connector.registry.Registries;

import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;

/**
 * Represents data for an entity. This includes properties such as height and width, as well as the list of entity
 * metadata translators needed to translate the properties sent from the server. The translators are structured in such
 * a way that inserting a new one (for example in version updates) is convenient.
 *
 * @param <T> the entity type this definition represents
 */
public record EntityDefinition<T extends Entity>(EntityFactory<T> factory, EntityType entityType, int bedrockId, String identifier,
                                                 float width, float height, float offset, List<EntityMetadataTranslator<? super T, ?>> translators) {

    public static <T extends Entity> Builder<T> inherited(BaseEntityFactory<T> factory, EntityDefinition<? super T> parent) {
        return inherited((EntityFactory<T>) factory, parent);
    }

    public static <T extends Entity> Builder<T> inherited(EntityFactory<T> factory, EntityDefinition<? super T> parent) {
        return new Builder<>(factory, parent.entityType, parent.bedrockId, parent.identifier, parent.width, parent.height, parent.offset, new ObjectArrayList<>(parent.translators));
    }

    public static <T extends Entity> Builder<T> builder(EntityFactory<T> factory) {
        return new Builder<>(factory);
    }

    @Setter
    @Accessors(fluent = true, chain = true)
    public static class Builder<T extends Entity> {
        private final EntityFactory<T> factory;
        private EntityType type;
        private int bedrockId;
        private String identifier;
        private float width;
        private float height;
        private float offset;
        private final List<EntityMetadataTranslator<? super T, ?>> translators;

        private Builder(EntityFactory<T> factory) {
            this.factory = factory;
            translators = new ObjectArrayList<>();
        }

        public Builder(EntityFactory<T> factory, EntityType type, int bedrockId, String identifier, float width, float height, float offset, List<EntityMetadataTranslator<? super T, ?>> translators) {
            this.factory = factory;
            this.type = type;
            this.bedrockId = bedrockId;
            this.identifier = identifier;
            this.width = width;
            this.height = height;
            this.offset = offset;
            this.translators = translators;
        }

        /**
         * Sets the height and width as one value
         */
        public Builder<T> heightAndWidth(float value) {
            height = value;
            width = value;
            return this;
        }

        /**
         * Resets the identifier as well
         */
        public Builder<T> type(EntityType type) {
            this.type = type;
            identifier = null;
            return this;
        }

        public <U> Builder<T> addTranslator(MetadataType type, BiConsumer<T, EntityMetadata<U>> translateFunction) {
            translators.add(new EntityMetadataTranslator<>(type, translateFunction));
            return this;
        }

        public Builder<T> addTranslator(EntityMetadataTranslator<T, ?> translator) {
            translators.add(translator);
            return this;
        }

        public EntityDefinition<T> build() {
            return build(true);
        }

        /**
         * @param register whether to register this entity in the Registries for entity types. Generally this should be
         *                 set to false if we're not expecting this entity to spawn from the network.
         */
        public EntityDefinition<T> build(boolean register) {
            if (identifier == null && type != null) {
                identifier = "minecraft:" + type.name().toLowerCase(Locale.ROOT);
            }
            EntityDefinition<T> definition = new EntityDefinition<>(factory, type, bedrockId, identifier, width, height, offset, translators);
            if (register && definition.entityType() != null) {
                Registries.ENTITY_DEFINITIONS.get().putIfAbsent(definition.entityType(), definition);
                Registries.JAVA_ENTITY_IDENTIFIERS.get().putIfAbsent(definition.identifier(), definition);
            }
            return definition;
        }
    }
}
