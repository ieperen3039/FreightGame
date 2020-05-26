package NG.Network;

/**
 * @author Geert van Ieperen created on 2-5-2020.
 */
public class SpecialNetworkNode extends NetworkNode {
    private final NetworkPosition source;

    public SpecialNetworkNode(NetworkPosition source) {
        this.source = source;
    }

    @Override
    public boolean isNetworkCritical() {
        // this is the point of being special
        return true;
    }

    @Override
    public String toString() {
        return "SpecialNode{" + source + '}';
    }
}
