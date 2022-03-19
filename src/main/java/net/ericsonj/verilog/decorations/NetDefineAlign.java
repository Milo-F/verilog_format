package net.ericsonj.verilog.decorations;

import net.ericsonj.verilog.FileFormat;
import net.ericsonj.verilog.StyleImp;
import java.util.LinkedList;

// align reg/wire defination 
public class NetDefineAlign implements StyleImp {
    
    @Override
    public void applyStyle (FileFormat format, LinkedList<String> buffer) {
        System.out.println("aaaaaaaaaaaaaaaaa");
        // int start = 0;
        // int end = 0;
        int location = 0;
        for (int j = 0; j < buffer.size(); j++) {
            String line = buffer.get(j);
            LinkedList<String> resp = new LinkedList<String>();
            line = line.trim();
            line = line.replaceAll("[ ]*,[ ]*", ",");
            StringBuilder line_builder = new StringBuilder("    ");
            if (line.matches("(^reg[ ].*)|(^wire[ ].*)")) {
                System.out.println(line);
                boolean reg_or_wire = false;
                if (line.matches("^(reg).*")) {
                    reg_or_wire = true;
                } else {
                    reg_or_wire = false;
                }
                if (reg_or_wire) {
                    line_builder.append("reg");
                } else {
                    line_builder.append("wire");
                }
                location = 28 - line_builder.length();
                for (int i = 0; i < location; i++) {
                    line_builder.append(" ");
                }
                if (line.matches(".*\\[.*].*")) {
                    line_builder.append(line.substring(line.indexOf("["), line.indexOf("]") + 1));
                }
                location = 59 - line_builder.length();
                for (int i = 0; i < location; i++) {
                    line_builder.append(" ");
                }
                line_builder.append(line.substring(line.lastIndexOf(" "), line.length()));
                resp.add(line_builder.toString());
                // start = getIdxLineMatches(buffer, line, end);
                // System.out.println(line_builder);
                // System.out.println(start + "/" + end);
                // if (start == -1) {
                //     break;
                // }
                // end = getIdxLineMatches(buffer, ".*[;][ ]*", start);
                // if (end == -1) {
                //     break;
                // }
                replaceInBuffer(buffer, j, j, resp);
            }
            // start = end + 1;
        }
    }
    private int getIdxLineMatches(LinkedList<String> buffer, String regex, int offset) {
        for (int i = offset; i < buffer.size(); i++) {
            String line = buffer.get(i);
            if (line.matches(regex)) {
                return i;
            }
        }
        return -1;
    }
    private void replaceInBuffer(LinkedList<String> buffer, int startModuleLine, int endModuleLine, LinkedList<String> bufferSrc) {
        int linesRemove = endModuleLine - startModuleLine + 1;
        for (int i = 0; i < linesRemove; i++) {
            buffer.remove(startModuleLine);
        }
        
        buffer.addAll(startModuleLine, bufferSrc);
        // System.out.println(buffer.toString());
    }


}