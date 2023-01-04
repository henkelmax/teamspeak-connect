package de.maxhenkel.teamspeakconnect.ratelimit;

import de.maxhenkel.teamspeakconnect.database.User;
import org.bson.types.ObjectId;

import java.util.HashMap;
import java.util.Map;

public class RateLimiter {

    private final int capRate;
    private final int maxRate;
    private final Map<ObjectId, Rate> rates;

    public RateLimiter(int maxRate) {
        this.maxRate = maxRate;
        this.capRate = maxRate * 2;
        rates = new HashMap<>();
    }

    public void updateRates() {
        rates.values().forEach(rate -> rate.rate = Math.max(0, rate.rate - 100));
        long time = System.currentTimeMillis();
        rates.entrySet().removeIf(entry -> entry.getValue().lastUpdate + 60L * 60L * 1000L < time);
    }

    /**
     * @param user   the user
     * @param points the points to add
     * @return true if the user is allowed to do the action
     */
    public boolean addRate(User user, int points) {
        return addRate(user.getId(), points);
    }

    /**
     * @param id     the id of the user
     * @param points the points to add
     * @return true if the user is allowed to do the action
     */
    public boolean addRate(ObjectId id, int points) {
        Rate rate = rates.getOrDefault(id, new Rate());
        rate.rate = Math.min(rate.rate + points, capRate);
        rate.lastUpdate = System.currentTimeMillis();
        rates.put(id, rate);
        return rate.rate <= maxRate;
    }

    /**
     * @param id the id of the user
     * @return true if the user is allowed to do the action
     */
    public boolean checkRate(ObjectId id) {
        if (!rates.containsKey(id)) {
            return true;
        }
        return rates.get(id).rate <= maxRate;
    }

    private static class Rate {
        long lastUpdate;
        int rate;

        public Rate() {
            this.lastUpdate = System.currentTimeMillis();
            this.rate = 0;
        }
    }

}
