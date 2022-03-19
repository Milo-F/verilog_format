package net.ericsonj.verilog.decorations;

import net.ericsonj.verilog.FileFormat;
import net.ericsonj.verilog.StyleImp;
import java.util.LinkedList;

// align reg/wire defination 
public class NetDefineAlign implements StyleImp {
    
    @Override
    public void applyStyle (FileFormat format, LinkedList<String> buffer) {
        System.out.println("aaaaaaaaaaaaaaaaa");
    }

}