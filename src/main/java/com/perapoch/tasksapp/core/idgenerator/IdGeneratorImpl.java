package com.perapoch.tasksapp.core.idgenerator;

import com.perapoch.tasksapp.storage.db.KeyValueStore;

import java.util.concurrent.atomic.AtomicLong;

public class IdGeneratorImpl implements IdGenerator {

    private final String name;
    private final int rangeSize;

    private volatile IdRange range;
    private AtomicLong currentId;
    private final KeyValueStore<String, IdRange> idGeneratorStore;

    public IdGeneratorImpl(String name, int rangeSize, KeyValueStore<String, IdRange> idGeneratorStore) {
        this.name = name;
        this.rangeSize = rangeSize;
        this.idGeneratorStore = idGeneratorStore;
    }

    @Override
    public long newId() {
        if (range == null || currentId.get() > range.to()) {
            synchronized (this) {
                if (range == null || currentId.get() > range.to()) {
                    loadNewRange();
                }
            }
        }
        long nextId = currentId.getAndIncrement();
        if (nextId <= range.to()) {
            return nextId;
        }
        return newId();
    }

    private void loadNewRange() {
        range = idGeneratorStore.update(name, idRange -> {
            if (idRange == null) {
                return new IdRange(1, rangeSize);
            }
            return idRange.nextRange(rangeSize);
        });
        currentId = new AtomicLong(range.from());
    }

    public void reset() {
        synchronized (this) {
            range = null;
            if (currentId != null) {
                currentId.set(0L);
            }
        }
    }
}
