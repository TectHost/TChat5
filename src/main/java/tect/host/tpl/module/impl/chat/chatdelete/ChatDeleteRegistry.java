package tect.host.tpl.module.impl.chat.chatdelete;

import net.kyori.adventure.chat.SignedMessage;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

final class ChatDeleteRegistry {

    record Entry(@Nullable SignedMessage signedMessage, @NonNull Set<UUID> recipients, long expiresAtMillis) {}

    private final int maxSize;
    private final long ttlMillis;
    private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<String> insertionOrder = new ConcurrentLinkedQueue<>();
    private final AtomicLong sequence = new AtomicLong();

    ChatDeleteRegistry(int maxSize, long ttlSeconds) {
        this.maxSize = maxSize;
        this.ttlMillis = ttlSeconds * 1000L;
    }

    @NonNull String register(@Nullable SignedMessage signedMessage, @NonNull Set<UUID> recipients) {
        purgeSome();

        String id = Long.toString(sequence.incrementAndGet(), 36);
        entries.put(id, new Entry(signedMessage, recipients, System.currentTimeMillis() + ttlMillis));
        insertionOrder.add(id);

        while (insertionOrder.size() > maxSize) {
            String evicted = insertionOrder.poll();
            if (evicted != null) entries.remove(evicted);
        }

        return id;
    }

    @Nullable Entry consume(@NonNull String id) {
        Entry entry = entries.remove(id);
        if (entry == null) return null;
        if (entry.expiresAtMillis() < System.currentTimeMillis()) return null;
        return entry;
    }

    private void purgeSome() {
        long now = System.currentTimeMillis();
        for (int i = 0; i < 5; i++) {
            String headId = insertionOrder.peek();
            if (headId == null) break;
            Entry entry = entries.get(headId);
            if (entry == null) { insertionOrder.poll(); continue; }
            if (entry.expiresAtMillis() >= now) break;
            insertionOrder.poll();
            entries.remove(headId);
        }
    }
}