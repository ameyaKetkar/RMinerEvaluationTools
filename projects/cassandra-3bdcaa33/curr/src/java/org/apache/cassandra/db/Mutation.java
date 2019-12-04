/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cassandra.db;

import java.io.IOException;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.config.Schema;
import org.apache.cassandra.db.rows.SerializationHelper;
import org.apache.cassandra.db.partitions.*;
import org.apache.cassandra.io.IVersionedSerializer;
import org.apache.cassandra.io.util.DataInputPlus;
import org.apache.cassandra.io.util.DataOutputPlus;
import org.apache.cassandra.net.MessageOut;
import org.apache.cassandra.net.MessagingService;
import org.apache.cassandra.service.StorageService;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO convert this to a Builder pattern instead of encouraging M.add directly,
// which is less-efficient since we have to keep a mutable HashMap around
public class Mutation implements IMutation
{
    public static final MutationSerializer serializer = new MutationSerializer();
    private static final Logger logger = LoggerFactory.getLogger(Mutation.class);

    public static final String FORWARD_TO = "FWD_TO";
    public static final String FORWARD_FROM = "FWD_FRM";

    // todo this is redundant
    // when we remove it, also restore SerializationsTest.testMutationRead to not regenerate new Mutations each test
    private final String keyspaceName;

    private final DecoratedKey key;
    // map of column family id to mutations for that column family.
    private final Map<UUID, PartitionUpdate> modifications;

    // Time at which this mutation was instantiated
    public final long createdAt = System.currentTimeMillis();
    public Mutation(String keyspaceName, DecoratedKey key)
    {
        this(keyspaceName, key, new HashMap<UUID, PartitionUpdate>());
    }

    public Mutation(PartitionUpdate update)
    {
        this(update.metadata().ksName, update.partitionKey(), Collections.singletonMap(update.metadata().cfId, update));
    }

    protected Mutation(String keyspaceName, DecoratedKey key, Map<UUID, PartitionUpdate> modifications)
    {
        this.keyspaceName = keyspaceName;
        this.key = key;
        this.modifications = modifications;
    }

    public Mutation copy()
    {
        Mutation copy = new Mutation(keyspaceName, key, new HashMap<>(modifications));
        return copy;
    }

    public String getKeyspaceName()
    {
        return keyspaceName;
    }

    public Collection<UUID> getColumnFamilyIds()
    {
        return modifications.keySet();
    }

    public DecoratedKey key()
    {
        return key;
    }

    public Collection<PartitionUpdate> getPartitionUpdates()
    {
        return modifications.values();
    }

    public PartitionUpdate getPartitionUpdate(UUID cfId)
    {
        return modifications.get(cfId);
    }

    public Mutation add(PartitionUpdate update)
    {
        assert update != null;
        PartitionUpdate prev = modifications.put(update.metadata().cfId, update);
        if (prev != null)
            // developer error
            throw new IllegalArgumentException("Table " + update.metadata().cfName + " already has modifications in this mutation: " + prev);
        return this;
    }

    public PartitionUpdate get(CFMetaData cfm)
    {
        return modifications.get(cfm.cfId);
    }

    public boolean isEmpty()
    {
        return modifications.isEmpty();
    }

    /**
     * Creates a new mutation that merges all the provided mutations.
     *
     * @param mutations the mutations to merge together. All mutation must be
     * on the same keyspace and partition key. There should also be at least one
     * mutation.
     * @return a mutation that contains all the modifications contained in {@code mutations}.
     *
     * @throws IllegalArgumentException if not all the mutations are on the same
     * keyspace and key.
     */
    public static Mutation merge(List<Mutation> mutations)
    {
        assert !mutations.isEmpty();

        if (mutations.size() == 1)
            return mutations.get(0);

        Set<UUID> updatedTables = new HashSet<>();
        String ks = null;
        DecoratedKey key = null;
        for (Mutation mutation : mutations)
        {
            updatedTables.addAll(mutation.modifications.keySet());
            if (ks != null && !ks.equals(mutation.keyspaceName))
                throw new IllegalArgumentException();
            if (key != null && !key.equals(mutation.key))
                throw new IllegalArgumentException();
            ks = mutation.keyspaceName;
            key = mutation.key;
        }

        List<PartitionUpdate> updates = new ArrayList<>(mutations.size());
        Map<UUID, PartitionUpdate> modifications = new HashMap<>(updatedTables.size());
        for (UUID table : updatedTables)
        {
            for (Mutation mutation : mutations)
            {
                PartitionUpdate upd = mutation.modifications.get(table);
                if (upd != null)
                    updates.add(upd);
            }

            if (updates.isEmpty())
                continue;

            modifications.put(table, updates.size() == 1 ? updates.get(0) : PartitionUpdate.merge(updates));
            updates.clear();
        }
        return new Mutation(ks, key, modifications);
    }

    /*
     * This is equivalent to calling commit. Applies the changes to
     * to the keyspace that is obtained by calling Keyspace.open().
     */
    public void apply()
    {
        Keyspace ks = Keyspace.open(keyspaceName);
        ks.apply(this, ks.getMetadata().params.durableWrites);
    }

    public void applyUnsafe()
    {
        Keyspace.open(keyspaceName).apply(this, false);
    }

    public MessageOut<Mutation> createMessage()
    {
        return createMessage(MessagingService.Verb.MUTATION);
    }

    public MessageOut<Mutation> createMessage(MessagingService.Verb verb)
    {
        return new MessageOut<>(verb, this, serializer);
    }

    public long getTimeout()
    {
        return DatabaseDescriptor.getWriteRpcTimeout();
    }

    public String toString()
    {
        return toString(false);
    }

    public String toString(boolean shallow)
    {
        StringBuilder buff = new StringBuilder("Mutation(");
        buff.append("keyspace='").append(keyspaceName).append('\'');
        buff.append(", key='").append(ByteBufferUtil.bytesToHex(key.getKey())).append('\'');
        buff.append(", modifications=[");
        if (shallow)
        {
            List<String> cfnames = new ArrayList<String>(modifications.size());
            for (UUID cfid : modifications.keySet())
            {
                CFMetaData cfm = Schema.instance.getCFMetaData(cfid);
                cfnames.add(cfm == null ? "-dropped-" : cfm.cfName);
            }
            buff.append(StringUtils.join(cfnames, ", "));
        }
        else
        {
            buff.append("\n  ").append(StringUtils.join(modifications.values(), "\n  ")).append("\n");
        }
        return buff.append("])").toString();
    }

    public Mutation without(UUID cfId)
    {
        Mutation mutation = new Mutation(keyspaceName, key);
        for (Map.Entry<UUID, PartitionUpdate> entry : modifications.entrySet())
            if (!entry.getKey().equals(cfId))
                mutation.add(entry.getValue());
        return mutation;
    }

    public static class MutationSerializer implements IVersionedSerializer<Mutation>
    {
        public void serialize(Mutation mutation, DataOutputPlus out, int version) throws IOException
        {
            if (version < MessagingService.VERSION_20)
                out.writeUTF(mutation.getKeyspaceName());

            /* serialize the modifications in the mutation */
            int size = mutation.modifications.size();

            if (version < MessagingService.VERSION_30)
            {
                ByteBufferUtil.writeWithShortLength(mutation.key().getKey(), out);
                out.writeInt(size);
            }
            else
            {
                out.writeVInt(size);
            }

            assert size > 0;
            for (Map.Entry<UUID, PartitionUpdate> entry : mutation.modifications.entrySet())
                PartitionUpdate.serializer.serialize(entry.getValue(), out, version);
        }

        public Mutation deserialize(DataInputPlus in, int version, SerializationHelper.Flag flag) throws IOException
        {
            String keyspaceName = null; // will always be set from cf.metadata but javac isn't smart enough to see that
            if (version < MessagingService.VERSION_20)
                keyspaceName = in.readUTF();

            DecoratedKey key = null;
            int size;
            if (version < MessagingService.VERSION_30)
            {
                key = StorageService.getPartitioner().decorateKey(ByteBufferUtil.readWithShortLength(in));
                size = in.readInt();
            }
            else
            {
                size = (int)in.readVInt();
            }

            assert size > 0;

            if (size == 1)
                return new Mutation(PartitionUpdate.serializer.deserialize(in, version, flag, key));

            Map<UUID, PartitionUpdate> modifications = new HashMap<>(size);
            PartitionUpdate update = null;
            for (int i = 0; i < size; ++i)
            {
                update = PartitionUpdate.serializer.deserialize(in, version, flag, key);
                modifications.put(update.metadata().cfId, update);
            }

            if (keyspaceName == null)
                keyspaceName = update.metadata().ksName;
            if (key == null)
                key = update.partitionKey();

            return new Mutation(keyspaceName, key, modifications);
        }

        public Mutation deserialize(DataInputPlus in, int version) throws IOException
        {
            return deserialize(in, version, SerializationHelper.Flag.FROM_REMOTE);
        }

        public long serializedSize(Mutation mutation, int version)
        {
            int size = 0;

            if (version < MessagingService.VERSION_20)
                size += TypeSizes.sizeof(mutation.getKeyspaceName());

            if (version < MessagingService.VERSION_30)
            {
                int keySize = mutation.key().getKey().remaining();
                size += TypeSizes.sizeof((short) keySize) + keySize;
                size += TypeSizes.sizeof(mutation.modifications.size());
            }
            else
            {
                size += TypeSizes.sizeofVInt(mutation.modifications.size());
            }

            for (Map.Entry<UUID, PartitionUpdate> entry : mutation.modifications.entrySet())
                size += PartitionUpdate.serializer.serializedSize(entry.getValue(), version);

            return size;
        }
    }
}
