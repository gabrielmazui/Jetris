package network.middleware;

public abstract class Middleware {
    private Middleware next;

    public static Middleware link(Middleware first, Middleware... chain) {
        Middleware head = first;
        for (Middleware nextInChain : chain) {
            head.next = nextInChain;
            head = nextInChain;
        }
        return first;
    }

    public abstract boolean check(String type, int code, int callbackCode, String body, String clientIp);

    protected boolean checkNext(String type, int code, int callbackCode, String body, String clientIp) {
        if (next == null) {
            return true;
        }
        return next.check(type, code, callbackCode, body, clientIp);
    }
}