package tect.host.tpl.module.impl.chat.chatdelete;

import net.kyori.adventure.chat.SignedMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import tect.host.tpl.config.ConfigFile;
import tect.host.tpl.context.MessageContext;
import tect.host.tpl.module.ModuleContext;
import tect.host.tpl.module.type.ChatModule;
import tect.host.tpl.util.ColorUtil;
import tect.host.tpl.util.Utils;
import tect.host.tpl.util.logging.DebugLogger;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class ChatDeleteModule implements ChatModule {

    private static final String ID = "chat-delete";
    public static final String PERMISSION = "tchat.admin.command.chatdelete";

    private final ModuleContext moduleContext;
    private final DebugLogger log;
    private ConfigFile configFile;

    private volatile ChatDeleteRegistry registry;
    private String prefixText;

    public ChatDeleteModule(@NonNull ModuleContext moduleContext) {
        this.moduleContext = moduleContext;
        this.log = moduleContext.getDebugLogger();
    }

    @Override
    public void onEnable() {
        configFile = moduleContext.createConfigFile("chatdelete.yml", "modules");
        configFile.register();
        load();
    }

    @Override
    public void onReload() {
        configFile.reload();
        load();
    }

    private void load() {
        ChatDeleteConfig config = new ChatDeleteConfig(configFile);
        registry = new ChatDeleteRegistry(config.getCacheSize(), config.getCacheTtlSeconds());
        prefixText = config.getPrefixText();
        log.debug("chat-delete: config reloaded (cache-size=%d, cache-ttl=%ds)".formatted(config.getCacheSize(), config.getCacheTtlSeconds()));
    }

    @Override
    public void process(@NonNull MessageContext ctx) {
        Set<? extends Player> recipients = ctx.getRecipients();
        if (recipients == null || recipients.isEmpty()) return;

        boolean anyEligibleViewer = false;
        for (Player recipient : recipients) {
            if (Utils.hasPerms(recipient, PERMISSION)) { anyEligibleViewer = true; break; }
        }
        if (!anyEligibleViewer) return;

        Set<UUID> recipientIds = new LinkedHashSet<>(recipients.size());
        for (Player recipient : recipients) recipientIds.add(recipient.getUniqueId());

        SignedMessage signedMessage = ctx.getSignedMessage();
        // A non-null SignedMessage does not mean the message is actually
        // deletable, canDelete() is false for chat that arrived without a
        // valid signature (offline-mode servers, a proxy not forwarding chat
        // session keys, or enforce-secure-profile disabled).
        // The prefix still shows so the button doesn't flicker in and out,
        // but clicking it will report back that it couldn't be removed from the client.
        if (signedMessage == null || !signedMessage.canDelete()) {
            log.debug("chat-delete: message from %s is not deletable (insecure chat/invalid signature, offline-mode, proxy without signature forwarding, or enforce-secure-profile disabled)".formatted(ctx.getPlayer().getName()));
        }

        String id = registry.register(signedMessage, recipientIds);
        Component prefix = ColorUtil.deserialize(prefixText.replace("%id%", id));

        log.debug("chat-delete: id=%s registered for message from %s (%d recipients)".formatted(id, ctx.getPlayer().getName(), recipientIds.size()));

        ctx.addViewerDecorator((viewer, rendered) -> {
            if (!Utils.hasPerms(viewer, PERMISSION)) return rendered;
            return prefix.append(rendered);
        });
    }

    /** Attempts to delete a previously registered message */
    public @NonNull ChatDeleteResult deleteMessage(@NonNull String id) {
        ChatDeleteRegistry.Entry entry = registry.consume(id);
        if (entry == null) {
            log.debug("chat-delete: id=%s not found (expired, already deleted, or invalid)".formatted(id));
            return ChatDeleteResult.NOT_FOUND;
        }

        SignedMessage signedMessage = entry.signedMessage();
        if (signedMessage == null || !signedMessage.canDelete()) {
            log.debug("chat-delete: id=%s had no valid signature (canDelete=false), cannot delete it from the client".formatted(id));
            return ChatDeleteResult.UNSIGNED;
        }

        int deleted = 0, offline = 0;
        for (UUID uuid : entry.recipients()) {
            Player recipient = Bukkit.getPlayer(uuid);
            if (recipient != null) {
                recipient.deleteMessage(signedMessage);
                deleted++;
            } else {
                offline++;
            }
        }

        log.debug("chat-delete: id=%s deleted for %d connected clients (%d already disconnected)".formatted(id, deleted, offline));
        return ChatDeleteResult.DELETED;
    }

    @Override
    public @NonNull String getId() { return ID; }
}