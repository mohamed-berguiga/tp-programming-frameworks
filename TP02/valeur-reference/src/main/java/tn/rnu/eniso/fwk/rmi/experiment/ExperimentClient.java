package tn.rnu.eniso.fwk.rmi.experiment;

import java.rmi.Naming;

public class ExperimentClient {
    public static void main(String[] args) throws Exception {
        String url = args.length > 0 ? args[0] : "rmi://localhost:1099/probe";
        Probe api = (Probe) Naming.lookup(url);
        run(api);
    }

    static String[] run(Probe api) throws Exception {
        Box b = new Box(1);
        int r = api.bump(b);
        String line1 = r + " " + b.n;
        Box x = api.last();
        Box y = api.last();
        String line2 = String.valueOf(x == y);
        String line3 = String.valueOf(x.equals(y));
        System.out.println("r + \" \" + b.n                 -> " + line1);
        System.out.println("api.last() == api.last()      -> " + line2);
        System.out.println("api.last().equals(api.last()) -> " + line3 + " (equals not overridden)");
        System.out.println("api.last().n                  -> " + x.n);
        return new String[]{line1, line2, line3};
    }
}
