import redis.clients.jedis.Jedis;

import java.util.Set;

/**
 * Created by Yusuf Khamis on 4/21/2017.
 */
public class RedisOperations {

    Jedis redisDB;

    public RedisOperations() {
        redisDB = new Jedis("localhost");
    }

    public boolean testRedis() {

        return redisDB.ping() == "PONG";
    }

    public void setItem(String key, String item) {
        redisDB.set(key, item);
    }

    public String getItem(String key) {

        return redisDB.get(key);
    }

    public void addToList(String item) {
        redisDB.sadd("emails", item);
    }

    public void removeFromList(String item) {
        redisDB.srem("emails", item);
    }

    public Set<String> allEmails() {

        return redisDB.smembers("emails");
    }
}
