package network;

public abstract class NetworkCallback{
   protected final int code; 

   public NetworkCallback(int code) {
        this.code = code;
    }

    public abstract void onSuccess(String resposta);
    public abstract void onFailure(String erro);
}