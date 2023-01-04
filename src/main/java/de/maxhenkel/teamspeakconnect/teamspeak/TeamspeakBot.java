package de.maxhenkel.teamspeakconnect.teamspeak;

import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3Config;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.api.event.*;
import com.github.theholywaffle.teamspeak3.api.reconnect.ReconnectStrategy;
import com.github.theholywaffle.teamspeak3.api.wrapper.ChannelInfo;
import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import com.github.theholywaffle.teamspeak3.api.wrapper.ClientInfo;
import de.maxhenkel.teamspeakconnect.Environment;
import de.maxhenkel.teamspeakconnect.Main;
import de.maxhenkel.teamspeakconnect.database.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class TeamspeakBot extends TS3EventAdapter {

    public static final Logger LOGGER = LogManager.getLogger("TeamspeakBot");

    private final String host;
    private final int queryPort;
    private final String queryUsername;
    private final String queryPassword;
    private final int virtualServerId;
    private final String botUsername;
    private final String botPassword;
    private final String botName;
    private final int botChannelId;

    private final Map<String, Integer> clientIdCache;
    private final Map<Integer, String> clientUIdCache;
    private final Map<Integer, Integer> clientChannelCache;

    private TS3Api api;

    public TeamspeakBot(String host, int queryPort, String queryUsername, String queryPassword, int virtualServerId, String botUsername, String botPassword, String botName, int botChannelId) {
        this.host = host;
        this.queryPort = queryPort;
        this.queryUsername = queryUsername;
        this.queryPassword = queryPassword;
        this.virtualServerId = virtualServerId;
        this.botUsername = botUsername;
        this.botPassword = botPassword;
        this.botName = botName;
        this.botChannelId = botChannelId;
        this.clientIdCache = new HashMap<>();
        this.clientUIdCache = new HashMap<>();
        this.clientChannelCache = new HashMap<>();
    }

    public void connect() {
        LOGGER.info("Connecting virtual server {} on {}:{} as {} with name {}", virtualServerId, host, queryPort, queryUsername, botName);

        TS3Config config = new TS3Config();
        config.setHost(host);
        config.setProtocol(TS3Query.Protocol.SSH);
        config.setQueryPort(queryPort);
        config.setReconnectStrategy(ReconnectStrategy.exponentialBackoff());
        config.setLoginCredentials(queryUsername, queryPassword);

        TS3Query query = new TS3Query(config);
        query.connect();

        api = query.getApi();
        api.selectVirtualServerById(virtualServerId, botName);
        api.login(botUsername, botPassword);
        try {
            api.setNickname(botName);
        } catch (Exception e) {
        }
        api.moveClient(api.whoAmI().getId(), botChannelId);

        LOGGER.info("Connected to TeamSpeak server");
        api.registerAllEvents();
        LOGGER.info("Registered teamspeak events");

        api.addTS3Listeners(this);
        LOGGER.info("Added listeners");

        for (Client client : api.getClients()) {
            if (!client.isRegularClient()) {
                continue;
            }
            clientIdCache.put(client.getUniqueIdentifier(), client.getId());
            clientUIdCache.put(client.getId(), client.getUniqueIdentifier());
            clientChannelCache.put(client.getId(), client.getChannelId());
        }
    }

    public String getBotChannelName() {
        ChannelInfo channel = api.getChannelInfo(api.whoAmI().getChannelId());
        if (channel == null) {
            return "N/A";
        }
        return channel.getName();
    }

    @Override
    public void onTextMessage(TextMessageEvent event) {
        String message = event.getMessage();

        if (!message.trim().toLowerCase().startsWith("!connect")) {
            return;
        }

        User user = Main.DATABASE.getOrCreateUser(event.getInvokerUniqueId());
        Integer authCode = Main.DATABASE.generateAuthCode(user.getId());
        api.sendPrivateMessage(event.getInvokerId(), "Your activation code is [b]%05d[/b]!\nThis code is valid only once and expires after some time.".formatted(authCode));
        api.sendPrivateMessage(event.getInvokerId(), "You can use the activation code for the following services:\nTelegram: %s\nDiscord: %s".formatted(Environment.TELEGRAM_BOT_URL, Environment.DISCORD_SERVER_URL));

        LOGGER.info("User {} ({}) generated an auth code", event.getInvokerName(), user.getId().toHexString());
    }

    @Override
    public void onClientMoved(ClientMovedEvent e) {
        if (!clientUIdCache.containsKey(e.getClientId())) {
            return;
        }
        clientChannelCache.put(e.getClientId(), e.getTargetChannelId());
    }

    @Override
    public void onClientJoin(ClientJoinEvent e) {
        ClientInfo clientInfo = api.getClientInfo(e.getClientId());
        if (clientInfo == null) {
            // This shouldn't happen
            return;
        }
        if (!clientInfo.isRegularClient()) {
            return;
        }
        clientChannelCache.put(e.getClientId(), clientInfo.getChannelId());

        clientIdCache.put(clientInfo.getUniqueIdentifier(), e.getClientId());
        clientUIdCache.put(e.getClientId(), clientInfo.getUniqueIdentifier());
    }

    @Override
    public void onClientLeave(ClientLeaveEvent e) {
        clientChannelCache.remove(e.getClientId());

        clientIdCache.values().removeIf(i -> i == e.getClientId());
        clientUIdCache.keySet().removeIf(i -> i == e.getClientId());
    }

    @Nullable
    public List<User> getClientsInChannel(String teamspeakUserId) {
        Integer clientId = clientIdCache.get(teamspeakUserId);
        if (clientId == null) {
            return null;
        }
        if (!clientChannelCache.containsKey(clientId)) {
            return null;
        }

        return clientChannelCache.entrySet().stream()
                .filter(e -> e.getValue().equals(clientChannelCache.get(clientId)))
                .map(Map.Entry::getKey)
                .map(clientUIdCache::get)
                .map(Main.DATABASE::getUserByTeamspeakId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

}
