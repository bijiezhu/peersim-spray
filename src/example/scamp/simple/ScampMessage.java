package example.scamp.simple;

import peersim.core.Node;

/**
 * Created by julian on 29/01/15.
 */
public class ScampMessage {

    public enum Type {
        Subscribe,
        Unsubscribe,
        ForwardSubscription,
        AcceptedSubscription
    }

    public final Type type;
    public final Node sender;
    public final Node subscriber;
    public int ttl;

    public ScampMessage(Node n, Type t, Node s) {
        this.ttl = 250;
        this.type = t;
        this.sender = n;
        this.subscriber = s;
    }

    public boolean isValid() {
        return this.ttl > 0;
    }

    public void reduceTTL() {
        this.ttl -= 1;
    }

    @Override
    public String toString(){
        return "Msg: {" + sender.getID() + "-> s:" + subscriber.getID() + " t:" + type + "}";
    }

}
