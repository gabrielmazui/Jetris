package network.middleware;

import java.util.concurrent.ConcurrentHashMap;

public class RateLimitMiddleware extends Middleware {
    private final ConcurrentHashMap<String, Long> ipTimestamps = new ConcurrentHashMap<>();
    private long minMillisBetweenPackets;
    public RateLimitMiddleware(long delay){
        minMillisBetweenPackets = delay;
    }
    
    @Override
    public boolean check(String type, int code, int callbackCode, String body, String clientIp) {
        long currentTime = System.currentTimeMillis();
        Long lastTime = ipTimestamps.get(clientIp);

        if (lastTime != null && (currentTime - lastTime) < minMillisBetweenPackets) {
            return false;
        }

        ipTimestamps.put(clientIp, currentTime);
        return checkNext(type, code, callbackCode, body, clientIp);
    }
}