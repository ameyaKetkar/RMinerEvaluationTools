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

package org.apache.giraph.comm.messages.primitives.long_id;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.apache.giraph.bsp.CentralizedServiceWorker;
import org.apache.giraph.conf.ImmutableClassesGiraphConfiguration;
import org.apache.giraph.factories.MessageValueFactory;
import org.apache.giraph.graph.Vertex;
import org.apache.giraph.partition.Partition;
import org.apache.giraph.partition.PartitionOwner;
import org.apache.giraph.utils.VertexIdIterator;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Writable;

import java.util.List;

/**
 * Special message store to be used when ids are LongWritable and no combiner
 * is used.
 * Uses fastutil primitive maps in order to decrease number of objects and
 * get better performance.
 *
 * @param <M> message type
 * @param <L> list type
 */
public abstract class LongAbstractListMessageStore<M extends Writable,
  L extends List> extends LongAbstractMessageStore<M, L> {
  /**
   * Map used to store messages for nascent vertices i.e., ones
   * that did not exist at the start of current superstep but will get
   * created because of sending message to a non-existent vertex id
   */
  private final
  Int2ObjectOpenHashMap<Long2ObjectOpenHashMap<L>> nascentMap;

  /**
   * Constructor
   *
   * @param messageValueFactory Factory for creating message values
   * @param service             Service worker
   * @param config              Hadoop configuration
   */
  public LongAbstractListMessageStore(
      MessageValueFactory<M> messageValueFactory,
      CentralizedServiceWorker<LongWritable, Writable, Writable> service,
      ImmutableClassesGiraphConfiguration<LongWritable,
          Writable, Writable> config) {
    super(messageValueFactory, service, config);
    populateMap();

    // create map for vertex ids (i.e., nascent vertices) not known yet
    nascentMap = new Int2ObjectOpenHashMap<>();
    for (int partitionId : service.getPartitionStore().getPartitionIds()) {
      nascentMap.put(partitionId, new Long2ObjectOpenHashMap<L>());
    }
  }

  /**
   * Populate the map with all vertexIds for each partition
   */
  private void populateMap() { // TODO - can parallelize?
    // populate with vertex ids already known
    service.getPartitionStore().startIteration();
    while (true) {
      Partition partition = service.getPartitionStore().getNextPartition();
      if (partition == null) {
        break;
      }
      Long2ObjectOpenHashMap<L> partitionMap = map.get(partition.getId());
      for (Object obj : partition) {
        Vertex vertex = (Vertex) obj;
        LongWritable vertexId = (LongWritable) vertex.getId();
        partitionMap.put(vertexId.get(), createList());
      }
      service.getPartitionStore().putPartition(partition);
    }
  }

  /**
   * Create an instance of L
   * @return instance of L
   */
  protected abstract L createList();

  /**
   * Get list for the current vertexId
   *
   * @param iterator vertexId iterator
   * @return list for current vertexId
   */
  protected L getList(
    VertexIdIterator<LongWritable> iterator) {
    PartitionOwner owner =
        service.getVertexPartitionOwner(iterator.getCurrentVertexId());
    long vertexId = iterator.getCurrentVertexId().get();
    int partitionId = owner.getPartitionId();
    Long2ObjectOpenHashMap<L> partitionMap = map.get(partitionId);
    if (!partitionMap.containsKey(vertexId)) {
      synchronized (nascentMap) {
        // assumption: not many nascent vertices are created
        // so overall synchronization is negligible
        Long2ObjectOpenHashMap<L> nascentPartitionMap =
          nascentMap.get(partitionId);
        if (nascentPartitionMap.get(vertexId) == null) {
          nascentPartitionMap.put(vertexId, createList());
        }
        return nascentPartitionMap.get(vertexId);
      }
    }
    return partitionMap.get(vertexId);
  }

  @Override
  public void finalizeStore() {
    for (int partitionId : nascentMap.keySet()) {
      // nascent vertices are present only in nascent map
      map.get(partitionId).putAll(nascentMap.get(partitionId));
    }
    nascentMap.clear();
  }

  // TODO - discussion
  /*
  some approaches for ensuring correctness with parallel inserts
  - current approach: uses a small extra bit of memory by pre-populating
  map & pushes everything map cannot handle to nascentMap
  at the beginning of next superstep compute a single threaded finalizeStore is
  called (so little extra memory + 1 sequential finish ops)
  - used striped parallel fast utils instead (unsure of perf)
  - use concurrent map (every get gets far slower)
  - use reader writer locks (unsure of perf)
  (code looks something like underneath)

      private final ReadWriteLock rwl = new ReentrantReadWriteLock();
      rwl.readLock().lock();
      L list = partitionMap.get(vertexId);
      if (list == null) {
        rwl.readLock().unlock();
        rwl.writeLock().lock();
        if (partitionMap.get(vertexId) == null) {
          list = createList();
          partitionMap.put(vertexId, list);
        }
        rwl.readLock().lock();
        rwl.writeLock().unlock();
      }
      rwl.readLock().unlock();
  - adopted from the article
    http://docs.oracle.com/javase/1.5.0/docs/api/java/util/concurrent/locks/\
    ReentrantReadWriteLock.html
   */
}
