package net.flamgop.util;

import java.io.OutputStream;
import java.io.PrintStream;

public class QuietPrint extends PrintStream {
    public QuietPrint(OutputStream out) {
        super(out);
    }


    @Override
    public void print(String s) {
        if (s == null) return;
        if (s.toLowerCase().contains("packet")) return;
        super.print(s);
    }
}
