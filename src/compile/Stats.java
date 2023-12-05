package compile;

public class Stats {
    int endorseCounter;
    int trustsCounter;
    int bypassLocksCounter;
    int acquireLockCounter;
    int releaseLockCounter;

    public void endorse() {
        ++endorseCounter;
    }
    public void endorse(int size) {
        endorseCounter += size;
    }

    public void trusts() {
        ++trustsCounter;
    }

    public void bypassLocks() {
        ++bypassLocksCounter;
    }

    public void acquireLock() {
        ++acquireLockCounter;
    }

    public void releaseLock() {
        ++releaseLockCounter;
    }

    public int endorseCount() {
        return endorseCounter;
    }

    public int dynamicCallCount() {
        return trustsCounter + bypassLocksCounter + acquireLockCounter + releaseLockCounter;
    }
}
