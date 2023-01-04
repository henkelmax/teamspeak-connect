package de.maxhenkel.teamspeakconnect.database;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import de.maxhenkel.teamspeakconnect.Environment;
import de.maxhenkel.teamspeakconnect.Main;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.types.ObjectId;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.lt;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;

public class Database {

    private final MongoClient mongoClient;
    private final MongoDatabase database;
    private final MongoCollection<User> users;
    private final Random random;

    public Database() {
        CodecRegistry registry = CodecRegistries.fromRegistries(CodecRegistries.fromCodecs(new User.UserCodec()), MongoClientSettings.getDefaultCodecRegistry());
        MongoClientOptions options = MongoClientOptions.builder().codecRegistry(registry).build();
        mongoClient = new MongoClient(new ServerAddress(Environment.DATABASE_URL), options);
        database = mongoClient.getDatabase(Environment.DATABASE_NAME);
        users = database.getCollection("users", User.class);
        random = new Random();

        Main.EXECUTOR.scheduleAtFixedRate(this::removeOutdatedAuthCodes, 5, 5, TimeUnit.MINUTES);
    }

    public User getOrCreateUser(String teamSpeakId) {
        User userByTeamspeakId = getUserByTeamspeakId(teamSpeakId);
        if (userByTeamspeakId != null) {
            return userByTeamspeakId;
        }

        User user = new User(teamSpeakId);
        users.insertOne(user);
        return user;
    }

    @Nullable
    public User getUserByTeamspeakId(String teamspeakId) {
        return users.find(eq("teamSpeakId", teamspeakId), User.class).first();
    }

    @Nullable
    public User getUserByTelegramId(Long telegramId) {
        return users.find(eq("telegramId", telegramId), User.class).first();
    }

    public boolean setTelegramId(ObjectId userId, Long telegramId) {
        User user = getUser(userId);
        if (user == null) {
            return false;
        }
        users.findOneAndUpdate(eq("_id", userId), set("telegramId", telegramId));
        return true;
    }

    @Nullable
    public User getUserByDiscordId(Long discordId) {
        return users.find(eq("discordId", discordId), User.class).first();
    }

    public boolean setDiscordId(ObjectId userId, Long discordId) {
        User user = getUser(userId);
        if (user == null) {
            return false;
        }
        users.findOneAndUpdate(eq("_id", userId), set("discordId", discordId));
        return true;
    }

    @Nullable
    public User getUser(ObjectId user) {
        return users.find(eq("_id", user), User.class).first();
    }

    @Nullable
    public Integer generateAuthCode(ObjectId userId) {
        User user = getUser(userId);
        if (user == null) {
            return null;
        }
        int code;
        while (true) {
            code = random.nextInt(100_000);
            User userWithCode = users.find(eq("authCode", code), User.class).first();
            if (userWithCode == null) {
                break;
            }
        }
        users.findOneAndUpdate(eq("_id", userId), combine(set("authCode", code), set("authCodeDate", new Date())));
        return code;
    }

    @Nullable
    public User getUserByAuthCode(int authCode) {
        return users.find(eq("authCode", authCode), User.class).first();
    }

    public void removeAuthCode(ObjectId userId) {
        users.findOneAndUpdate(eq("_id", userId), set("authCode", null));
    }

    public void removeOutdatedAuthCodes() {
        users.updateMany(
                lt("authCodeDate", new Date(System.currentTimeMillis() - Environment.AUTH_CODE_LIFETIME)),
                combine(set("authCode", null), set("authCodeDate", null))
        );
    }

}
