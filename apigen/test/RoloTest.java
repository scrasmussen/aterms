package test;

import test.rolodex.*;
import test.rolodex.rolodex.*;
import test.rolodex.roloList.*;
import test.rolodex.phoneNumber.*;
import aterm.pure.PureFactory;

public class RoloTest {

    private RolodexFactory factory;
    static private String directory;

    public RoloTest(RolodexFactory factory) {
        this.factory = factory;
    }

    public void run() throws jjtraveler.VisitFailure, java.io.IOException {
        RoloList list =
            factory.RoloListFromTerm(factory.getPureFactory().readFromFile(directory + "/rolodex.trm"));

        Collector c = new Collector();
        jjtraveler.Visitor tester = new jjtraveler.BottomUp(c);
        tester.visit(list);

        testAssert(c.concatenation.equals("123"), "bottom-up");
    }

    public final static void main(String[] args)
        throws jjtraveler.VisitFailure, java.io.IOException {
        RoloTest test = new RoloTest(new RolodexFactory(new PureFactory()));
        directory = args[0];

        test.run();
        return;
    }

    void testAssert(boolean b, String name) {
        if (!b) {
            throw new RuntimeException("Test " + name + " failed!");
        }
    }
}

class Collector extends Fwd {
    public String concatenation = "";

    public Collector() {
        super(new jjtraveler.Identity());
    }

    public void visit_PhoneNumber_Voice(Voice v) {
        concatenation = concatenation + v.getVoice();
    }
}
