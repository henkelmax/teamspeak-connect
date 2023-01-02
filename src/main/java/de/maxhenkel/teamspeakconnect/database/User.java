package de.maxhenkel.teamspeakconnect.database;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.DocumentCodec;
import org.bson.codecs.EncoderContext;
import org.bson.types.ObjectId;

import javax.annotation.Nullable;
import java.util.Date;

public class User {

    private final ObjectId id;
    private final Date creationDate;
    private String teamSpeakId;
    @Nullable
    private Long telegramId;
    @Nullable
    private Integer authCode;
    @Nullable
    private Date authCodeDate;

    public User(ObjectId id, Date creationDate, String teamSpeakId) {
        this.id = id;
        this.creationDate = creationDate;
        this.teamSpeakId = teamSpeakId;
    }

    public User(String teamSpeakId) {
        this(ObjectId.get(), new Date(), teamSpeakId);
    }

    public ObjectId getId() {
        return id;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public String getTeamSpeakId() {
        return teamSpeakId;
    }

    @Nullable
    public Long getTelegramId() {
        return telegramId;
    }

    public void setTelegramId(@Nullable Long telegramId) {
        this.telegramId = telegramId;
    }

    @Nullable
    public Integer getAuthCode() {
        return authCode;
    }

    public void setAuthCode(@Nullable Integer authCode) {
        this.authCode = authCode;
    }

    @Nullable
    public Date getAuthCodeDate() {
        return authCodeDate;
    }

    public void setAuthCodeDate(@Nullable Date authCodeDate) {
        this.authCodeDate = authCodeDate;
    }

    public static class UserCodec implements Codec<User> {

        private final Codec<Document> documentCodec;

        public UserCodec() {
            this.documentCodec = new DocumentCodec();
        }

        @Override
        public void encode(BsonWriter writer, User value, EncoderContext encoderContext) {
            if (value != null) {
                Document document = new Document();
                document.append("_id", value.getId());
                document.append("creationDate", value.getCreationDate());
                document.append("teamSpeakId", value.getTeamSpeakId());
                if (value.getAuthCode() != null) {
                    document.append("authCode", value.getAuthCode());
                }
                if (value.getAuthCodeDate() != null) {
                    document.append("authCodeDate", value.getAuthCodeDate());
                }
                if (value.getTelegramId() != null) {
                    document.append("telegramId", value.getTelegramId());
                }
                documentCodec.encode(writer, document, encoderContext);
            }
        }

        @Override
        public User decode(BsonReader reader, DecoderContext decoderContext) {
            Document document = documentCodec.decode(reader, decoderContext);
            User user = new User(document.getObjectId("_id"), document.getDate("creationDate"), document.getString("teamSpeakId"));
            if (document.containsKey("authCode")) {
                user.setAuthCode(document.getInteger("authCode"));
            }
            if (document.containsKey("authCodeDate")) {
                user.setAuthCodeDate(document.getDate("authCodeDate"));
            }
            if (document.containsKey("telegramId")) {
                user.setTelegramId(document.getLong("telegramId"));
            }
            return user;
        }

        @Override
        public Class<User> getEncoderClass() {
            return User.class;
        }
    }

}
