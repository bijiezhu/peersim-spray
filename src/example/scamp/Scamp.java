package example.scamp;

import example.scamp.old.ScampMessageOld;
import peersim.cdsim.CDProtocol;
import peersim.cdsim.CDState;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.transport.Transport;

import java.util.List;

/**
 * Created by julian on 01/02/15.
 */
public abstract class Scamp implements Linkable, EDProtocol, CDProtocol, example.PeerSamplingService {

    // =================== static fields ==================================
    // ====================================================================


    /**
     * Parameter "c" of Scamp . Defaults to 0.
     *
     * @config
     */
    private static final String PAR_C = "c";

    private static final String SCAMP_PROT = "0";

    /**
     * Time-to-live for indirection. Defaults to -1.
     *
     * @config
     */
    private static final String PAR_INDIRTTL = "indirectionTTL";

    /**
     * Lease timeout. If negative, there is no lease mechanism. Defaults to -1.
     *
     * @config
     */
    private static final String PAR_LEASE_MAX = "leaseTimeoutMax";

    private static final String PAR_LEASE_MIN = "leaseTimeoutMin";

    private static final String PAR_TRANSPORT = "transport";

    /**
     * c
     */
    protected static int c;

    /**
     * indirection TTL
     */
    protected static int indirTTL;

    /**
     * lease timeout
     */
    protected static long leaseTimeoutMin;
    protected static long leaseTimeoutMax;

    protected final int tid;

    public static int pid;

    /**
     * to support the lease mechanism. with randomised resubmission it would
     * not be necessary.
     */
    public long birthDate;

    //protected Map<Long, Node> inView;
    //protected Map<Long, Node> outView;

    public View inView;
    public View partialView;

    public long randomLeaseTimeout;


    //private List<Node> outViewList;
    //private List<Node> inViewList;


    public Scamp(String n) {
        Scamp.c = Configuration.getInt(n + "." + PAR_C, 0);
        Scamp.indirTTL = Configuration.getInt(n + "." + PAR_INDIRTTL, -1);
        Scamp.leaseTimeoutMax = Configuration.getInt(n + "." + PAR_LEASE_MAX, -1);
        Scamp.leaseTimeoutMin = Configuration.getInt(n + "." + PAR_LEASE_MIN, -1);
        this.tid = Configuration.getPid(n + "." + PAR_TRANSPORT);
        pid = Configuration.lookupPid(SCAMP_PROT);
        inView = new View();
        partialView = new View();
        birthDate = CommonState.getTime();
        this.randomLeaseTimeout = CDState.r.nextLong(leaseTimeoutMax - leaseTimeoutMin) + leaseTimeoutMin;
        System.out.println("Lease:" + this.randomLeaseTimeout);


        System.out.println("indirTTL:" + indirTTL);
    }

    public Object clone() {
        Scamp p = null;
        try {
            p = (Scamp) super.clone();
        } catch (CloneNotSupportedException e) {

        }
        p.partialView = new View();
        p.inView = new View();
        p.randomLeaseTimeout = CDState.r.nextLong(leaseTimeoutMax - leaseTimeoutMin) + leaseTimeoutMin;
        System.out.println("Lease:" + p.randomLeaseTimeout);
        return p;
    }

    /*
     * P U B L I C  I N T E R F A C E
     */

    @Override
    public int degree() {
        return this.partialView.length();
    }

    @Override
    public boolean contains(Node neighbor) {
        return this.partialView.contains(neighbor);
    }

    @Override
    public void pack() {

    }

    @Override
    public void onKill() {

    }

    @Override
    public String debug() {
        StringBuilder sb = new StringBuilder();
        sb.append("{In:");
        sb.append(this.inView);
        sb.append(" Out:");
        sb.append(this.partialView);
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean addNeighbor(Node n) {
        return this.addToOutView(n);
    }

    @Override
    public Node getNeighbor(int i) {
        return this.partialView.list().get(i);
    }

    @Override
    public List<Node> getPeers() {
        return this.partialView.list();
    }

    public boolean isExpired() {
        long currentTime = CommonState.getTime();
        return (currentTime - this.birthDate) > this.randomLeaseTimeout;
    }

    @Override
    public void nextCycle(Node node, int protocolID) {
        subNextCycle(node, protocolID);
    }

    @Override
    public void processEvent(Node node, int pid, Object event) {
        ScampMessageOld message = (ScampMessageOld) event;
        switch (message.type) {
            default:
                subProcessEvent(node, pid, message);
                break;
        }
    }

    protected abstract void subNextCycle(Node node, int protocolID);

    protected abstract void subProcessEvent(Node node, int pid, ScampMessageOld message);

    public abstract void acceptSubscription(Node acceptor, Node subscriber);

    protected boolean addToOutView(Node n) {
        if (this.partialView.contains(n)) {
            this.partialView.updateBirthdate(n);
            return false;
        } else {
            this.partialView.add(n);
            return true;
        }
    }

    public boolean addToInView(Node n) {
        if (this.inView.contains(n)) {
            this.inView.updateBirthdate(n);
            return false;
        } else {
            this.inView.add(n);
            return true;
        }
    }

    public boolean p() {
        return CDState.r.nextDouble() < 1.0 / (1.0 + this.degree());
    }

    public void send(Node me, Node destination, ScampMessageOld m) {
        Transport tr = (Transport) me.getProtocol(tid);
        tr.send(me, destination, m, pid);
    }

    /**
     * This node will act as a contact node forwarding the subscription to nodes
     * from its view and c other random nodes.
     *
     * @param n the contact node
     * @param s the subscribing node
     */
    public static void subscribe(Node n, Node s) {
        ((Scamp) n.getProtocol(pid)).startSubscribe(n, s);
    }

    public abstract void startSubscribe(Node me, Node s);


}
