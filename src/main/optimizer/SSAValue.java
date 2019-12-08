
package main.optimizer;

public class SSAValue {

    private int version;

    public SSAValue(int v) {
        version = v;
    }

    public int getVersion() {
        return version;
    }

    public void changeVersion(int id) {
        version = id;
    }

    public SSAValue clone() {
        return new SSAValue(version);
    }

    @Override
    public String toString() {
        return Integer.toString(version);
    }

}
