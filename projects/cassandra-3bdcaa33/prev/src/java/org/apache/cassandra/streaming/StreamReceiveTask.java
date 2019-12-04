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
package org.apache.cassandra.streaming;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.cassandra.concurrent.NamedThreadFactory;
import org.apache.cassandra.config.Schema;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.Keyspace;
import org.apache.cassandra.db.lifecycle.LifecycleTransaction;
import org.apache.cassandra.db.compaction.OperationType;
import org.apache.cassandra.io.sstable.format.SSTableReader;
import org.apache.cassandra.io.sstable.format.SSTableWriter;
import org.apache.cassandra.utils.Pair;

import org.apache.cassandra.utils.concurrent.Refs;

/**
 * Task that manages receiving files for the session for certain ColumnFamily.
 */
public class StreamReceiveTask extends StreamTask
{
    private static final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("StreamReceiveTask"));

    // number of files to receive
    private final int totalFiles;
    // total size of files to receive
    private final long totalSize;

    // Transaction tracking new files received
    public final LifecycleTransaction txn;

    // true if task is done (either completed or aborted)
    private boolean done = false;

    //  holds references to SSTables received
    protected Collection<SSTableWriter> sstables;

    public StreamReceiveTask(StreamSession session, UUID cfId, int totalFiles, long totalSize)
    {
        super(session, cfId);
        this.totalFiles = totalFiles;
        this.totalSize = totalSize;
        // this is an "offline" transaction, as we currently manually expose the sstables once done;
        // this should be revisited at a later date, so that LifecycleTransaction manages all sstable state changes
        this.txn = LifecycleTransaction.offline(OperationType.STREAM, Schema.instance.getCFMetaData(cfId));
        this.sstables = new ArrayList<>(totalFiles);
    }

    /**
     * Process received file.
     *
     * @param sstable SSTable file received.
     */
    public synchronized void received(SSTableWriter sstable)
    {
        if (done)
            return;

        assert cfId.equals(sstable.metadata.cfId);

        sstables.add(sstable);
        if (sstables.size() == totalFiles)
        {
            done = true;
            executor.submit(new OnCompletionRunnable(this));
        }
    }

    public int getTotalNumberOfFiles()
    {
        return totalFiles;
    }

    public long getTotalSize()
    {
        return totalSize;
    }

    private static class OnCompletionRunnable implements Runnable
    {
        private final StreamReceiveTask task;

        public OnCompletionRunnable(StreamReceiveTask task)
        {
            this.task = task;
        }

        public void run()
        {
            Pair<String, String> kscf = Schema.instance.getCF(task.cfId);
            if (kscf == null)
            {
                // schema was dropped during streaming
                for (SSTableWriter writer : task.sstables)
                    writer.abort();
                task.sstables.clear();
                task.txn.abort();
                return;
            }
            ColumnFamilyStore cfs = Keyspace.open(kscf.left).getColumnFamilyStore(kscf.right);

            List<SSTableReader> readers = new ArrayList<>();
            for (SSTableWriter writer : task.sstables)
                readers.add(writer.finish(true));
            task.txn.finish();
            task.sstables.clear();

            try (Refs<SSTableReader> refs = Refs.ref(readers))
            {
                // add sstables and build secondary indexes
                cfs.addSSTables(readers);
                cfs.indexManager.maybeBuildSecondaryIndexes(readers, cfs.indexManager.allIndexesNames());
            }

            task.session.taskCompleted(task);
        }
    }

    /**
     * Abort this task.
     * If the task already received all files and
     * {@link org.apache.cassandra.streaming.StreamReceiveTask.OnCompletionRunnable} task is submitted,
     * then task cannot be aborted.
     */
    public synchronized void abort()
    {
        if (done)
            return;

        done = true;
        for (SSTableWriter writer : sstables)
            writer.abort();
        txn.abort();
        sstables.clear();
    }
}
