package net.ericsonj.verilog.decorations;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.StringTokenizer;
// import java.lang.String;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.ericsonj.verilog.FileFormat;
import net.ericsonj.verilog.StyleImp;
// import net.ericsonj.verilog.AlignConsecutive

/**
 *
 * @author Ericson Joseph <ericsonjoseph@gmail.com>
 *
 * Create on Feb 18, 2019 10:37:47 PM
 */
public class ModuleAlign implements StyleImp {

    public enum COMMENT_STATE {
        BLOCK_COMMENT,
        LINE_COMMNET
    }

    private String keyWord;

    public ModuleAlign() {
        this("module");
    }

    public ModuleAlign(String keyWord) {
        this.keyWord = keyWord;
    }

    private LinkedHashMap<String, String> commnets = new LinkedHashMap<>();

    @Override
    public void applyStyle(FileFormat format, LinkedList<String> buffer) {

        String align = format.getSetting().getStringValue("ModuleAlign", "BAS_Align");

//        if (align.equals("BAS_Align")) {
//            process(buffer);
//        }
        int startModuleLine = getIdxLineMatches(buffer, "[ ]*" + keyWord + "[ ]+[a-zA-Z0-9-_,;&$# ]*.*", 0);
        if (startModuleLine == -1) {
            return;
        }
        int endModuleLine = 0;
        endModuleLine = getIdxLineMatches(buffer, ".*[)][ ]*[;][ ]*[//|/*]*.*", startModuleLine);
        if (endModuleLine == -1) {
            return;
        }
        // System.out.println("cccccc");
        while (true) {
            removeComments(buffer, startModuleLine, endModuleLine);

            String moduleDef = getModuleInLine(buffer, startModuleLine, endModuleLine);
            // System.out.println(moduleDef);

            LinkedList<String> resp = BASAlign(moduleDef);

            int commentAlign = getMostLargeLineSize(resp);

            for (int i = 0; i < resp.size(); i++) {
                String line = resp.get(i);
                String[] words = line.split(" ");
                String lastWord = words[words.length - 1];
                if (commnets.containsKey(lastWord)) {
                    LinkedList<String> lines = getCommentAlign(line, commentAlign, commnets.get(lastWord));
                    resp.remove(i);
                    resp.addAll(i, lines);
                } else {
                    if (lastWord.matches("[^ ]+[)];")) {
                        String newWordKey = lastWord.replace(");", "");
                        if (commnets.containsKey(newWordKey)) {
                            LinkedList<String> lines = getCommentAlign(line, commentAlign, commnets.get(newWordKey));
                            resp.remove(i);
                            resp.addAll(i, lines);
                        }
                    }
                }
                // System.out.println(resp.get(i));
            }

            replaceInBuffer(buffer, startModuleLine, endModuleLine, resp);
            startModuleLine = getIdxLineMatches(buffer, "((?! if | for | while | case | always ).)*[ ]*#*\\(", endModuleLine); // 匹配实例
            if (startModuleLine == -1) {
                return;
            }
            // System.out.println("aaaaaa");
            endModuleLine = getIdxLineMatches(buffer, ".*[)][ ]*[;].*", startModuleLine);
            if (endModuleLine == -1) {
                return;
            }
            // System.out.println("start" + startModuleLine);
            // System.out.println("end" + endModuleLine);
        }
        // return;

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

    private String getModuleInLine(LinkedList<String> buffer, int startModuleLine, int endModuleLine) {
        StringBuilder sb = new StringBuilder();

        int startIndet = getIndent(buffer.get(startModuleLine));
        // sb.append("\n");

        for (int i = startModuleLine; i < endModuleLine + 1; i++) {
            sb.append(buffer.get(i).trim());
            sb.append(" ");
        }

        String moduleDef = sb.toString().trim();

        moduleDef = orderLine(moduleDef);

        moduleDef = indent(startIndet, moduleDef);

        return moduleDef;

    }

    private void replaceInBuffer(LinkedList<String> buffer, int startModuleLine, int endModuleLine, LinkedList<String> bufferSrc) {
        int linesRemove = endModuleLine - startModuleLine + 1;
        for (int i = 0; i < linesRemove; i++) {
            buffer.remove(startModuleLine);
        }
        
        buffer.addAll(startModuleLine, bufferSrc);
        // System.out.println(buffer.toString());
    }

    private String indent(int indent, String line) {
        StringBuilder sb = new StringBuilder(line);
        for (int i = 0; i < indent; i++) {
            sb.insert(0, " ");
        }
        // sb.insert(0, "\t");
        return sb.toString();
    }

    private int getIndent(String line) {
        int indent = 0;
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == ' ') {
                indent++;
            } else {
                break;
            }
        }
        return indent;
    }

    private LinkedList<String> BASAlign(String moduleInLine) {
        // System.out.println(moduleInLine);
        LinkedList<String> resp = new LinkedList<>();
        
        boolean moduleWithParam = moduleInLine.contains("#(");

        boolean is_ins = !(moduleInLine.matches("^module .*"));

        int indentSize = 0;
        int initParamBrackt = 0;
        int endParamBrackt = 0;
        int initBracket = 0;
        int endBracket = 0;

        if (moduleWithParam) {

            initParamBrackt = moduleInLine.indexOf("#(");
            if (is_ins) {
                endParamBrackt = moduleInLine.indexOf("))") + 1;
            } else {
                endParamBrackt = moduleInLine.indexOf(")");
            }
            initBracket = moduleInLine.indexOf("(", moduleWithParam ? endParamBrackt : 0);
            endBracket = moduleInLine.lastIndexOf(")");

            resp.add(moduleInLine.substring(0, initParamBrackt + 2)); // 将#(添加
            // resp.add("xixixi");

            String paramArgs = moduleInLine.substring(initParamBrackt + 2, endParamBrackt); // 获取参数

            StringTokenizer st = new StringTokenizer(paramArgs, ",");
            int count = st.countTokens();
            for (int i = 0; i < count; i++) {
                String arg = st.nextToken();
                arg = arg.trim();
                if (is_ins) { 
                    if (i == count - 1) {
                        resp.addLast(indent(8, arg));
                        break;
                    }
                    resp.add(indent(8, arg + ","));
                } else {
                    String[] arg_array = arg.split("=");
                    arg = "";
                    for (int j = 0; j < arg_array.length; j++) {
                        arg_array[j] = arg_array[j].trim();
                    }
                    StringBuilder arg_space = new StringBuilder();
                    for (int j = 0; j < 24-arg_array[0].length(); j++) {
                        arg_space.append(" ");
                    }
                    if (arg_array.length == 1) {
                        arg = arg_array[0];
                    } else {
                        arg = arg_array[0] + arg_space + "=        " + arg_array[1];
                    }
                    if (i == count - 1) {
                        resp.addLast(indent(4, arg));
                        break;
                    }
                    resp.add(indent(4, arg + ","));
                }
                // System.out.println(arg.toString());
            }
            indentSize = initParamBrackt;

        } else {
            initBracket = moduleInLine.indexOf("(");
            endBracket = moduleInLine.lastIndexOf(")");
        }
        
        // ports align
        int endParamLine = 0;
        if (is_ins) {
            if (moduleWithParam) {
                resp.add(indent(4, moduleInLine.substring(moduleWithParam ? endParamBrackt : 0, initBracket + 1).trim()));
                endParamLine = resp.size() - 1;
                indentSize++;
            } else {
                resp.add(moduleInLine.substring(0, initBracket + 1));
                indentSize = initBracket;
            }
        } else {
            if (moduleWithParam) {
                
                resp.add(indent(0, moduleInLine.substring(moduleWithParam ? endParamBrackt : 0, initBracket + 1).trim()));
                endParamLine = resp.size() - 1;
                indentSize++;
            } else {
                resp.add(moduleInLine.substring(0, initBracket + 1));
                indentSize = initBracket;
            }
        }
        
        // System.out.println(initBracket);
        // System.out.println(endBracket);
        String moduleArgs = moduleInLine.substring(initBracket + 1, endBracket);
        if (moduleArgs.isEmpty()) {
            resp.set(endParamLine, resp.get(endParamLine) + moduleArgs + moduleInLine.substring(endBracket));
            return resp;
        }

        StringTokenizer st = new StringTokenizer(moduleArgs, ",");
        int count = st.countTokens();
        // if (count == 1) {
        //     resp.set(endParamLine, resp.get(endParamLine) + moduleArgs + moduleInLine.substring(endBracket));
        //     return resp;
        // }
        for (int i = 0; i < count; i++) {
            String arg = st.nextToken();
            arg = arg.trim();
            boolean in_or_out, wire_or_reg; // true is input ,false is output
            boolean have_type, have_width;
            in_or_out = false;
            have_type = false;
            have_width = false;
            wire_or_reg = false;
            // int part_num = 2;
            // String[] arg_array = new(part_num);
            StringBuilder arg_builder = new StringBuilder();
            if (is_ins) {
                if (i == count - 1) {
                    resp.addLast(indent(8, arg) + "\n" + indent(4, moduleInLine.substring(endBracket)));
                    break;
                }
                resp.add(indent(8, arg + ","));
            } else {
                if (arg.matches("^[(input)(output)].*")) { // 包含input或者output端口
                    if (arg.matches("^input.*")) {
                        in_or_out = true;
                    } else {
                        in_or_out = false;
                    }
                    arg_builder.append(in_or_out ? "input" : "output");
                    // arg_builder.append("    ");
                }
                if (arg.matches("(.*[ ]reg[ \\[].*)|(.*[ ]wire[ \\[].*)")) { // reg or wire
                    if (arg.matches(".*[ ]reg[ \\[].*")) {
                        wire_or_reg = false;
                    } else {
                        wire_or_reg = true;
                    }
                    int spaces_len = 12 - arg_builder.length();
                    for (int j = 0; j < spaces_len; j++) {
                        arg_builder.append(" ");
                    }
                    arg_builder.append(wire_or_reg ? "wire" : "reg");
                }
                if (arg.matches(".*\\[.*\\].*")) { // [bitwidth]
                    int spaces_len = 24 - arg_builder.length();
                    for (int j = 0; j < spaces_len; j++) {
                        arg_builder.append(" ");
                    }
                    int start_k = arg.indexOf("[");
                    int end_k = arg.indexOf("]");
                    arg_builder.append(arg.substring(start_k, end_k+1));
                }
                int spaces_len = 55 - arg_builder.length();
                for (int j = 0; j < spaces_len; j++) {
                    arg_builder.append(" ");
                }
                arg_builder.append(arg.substring(arg.lastIndexOf(" "), arg.length()));
                arg = arg_builder.toString();
                if (i == count - 1) {
                    resp.addLast(indent(4, arg) + "\n" + moduleInLine.substring(endBracket));
                    break;
                }
                resp.add(indent(4, arg + ","));
            }           
        }
        
        return resp;

    }

    private void removeComments(LinkedList<String> buffer, int startModuleLine, int endModuleLine) {

        COMMENT_STATE commentState = COMMENT_STATE.LINE_COMMNET;
        String blockComment = "";
        String blockKey = "";

        for (int i = startModuleLine; i < endModuleLine + 1; i++) {
            String line = buffer.get(i);
            line = orderLine(line);

            switch (commentState) {
                case LINE_COMMNET:
                    if (line.matches(".*/\\*.*\\*/")) {
                        Pattern p = Pattern.compile(".*[ ](.*)[ ](/\\*.*\\*/)");
                        Matcher m = p.matcher(line);
                        if (m.find()) {
                            String key = m.group(1);
                            String comment = m.group(2);
                            commnets.put(key, comment);
                        }
                    } else if (line.matches(".*//.*")) {
                        Pattern p = Pattern.compile(".*[ ](.*)[ ](//.*)");
                        Matcher m = p.matcher(line);
                        if (m.find()) {
                            String key = m.group(1);
                            String comment = m.group(2);
                            commnets.put(key, comment);
                        }
                    } else if (line.matches(".*/\\*.*")) {
                        Pattern p = Pattern.compile(".*[ ](.*)[ ](/\\*.*)");
                        Matcher m = p.matcher(line);
                        if (m.find()) {
                            String key = m.group(1);
                            String comment = m.group(2);
                            blockComment += comment;
                            blockKey = key;
                            commnets.put(key, comment);
                            commentState = COMMENT_STATE.BLOCK_COMMENT;
                        }
                    }
                    break;
                case BLOCK_COMMENT:
                    if (line.matches(".*\\*/")) {
                        blockComment += "\\" + line;
                        commnets.put(blockKey, blockComment);
                        commentState = COMMENT_STATE.LINE_COMMNET;
                    } else {
                        blockComment += "\\" + line;
                        line = "";
                    }
                    break;
                default:
                    throw new AssertionError(commentState.name());
            }

            line = line.replaceAll("/\\*.*", "");
            line = line.replaceAll("//.*", "");
            line = line.replaceAll(".*\\*/", "");
            buffer.remove(i);
            buffer.add(i, line);
        }
    }

    private String orderLine(String line) { // 修改括号
        int startIndet = getIndent(line);
        String orderLine = line.replaceAll("[#][ ]*[(]", "#(");
        orderLine = orderLine.replaceAll("[ ]+", " ");
        orderLine = orderLine.replaceAll("[(][ ]*", "(");
        orderLine = orderLine.replaceAll("[ ]*[)]", ")");
        orderLine = orderLine.replaceAll("[)][ ]*[;]", ");");
        orderLine = orderLine.replaceAll("[ ]*[,][ ]*", ", ");
        // orderLine = orderLine.replaceAll("[)][\\s]*[(]", ")(");
        orderLine = indent(startIndet, orderLine.trim());
        return orderLine;
    }

    private int getMostLargeLineSize(LinkedList<String> buffer) {
        int size = 0;
        for (String line : buffer) {
            if (line.length() > size) {
                size = line.length();
            }
        }
        return size;
    }

    private LinkedList<String> getCommentAlign(String line, int lineAlign, String comment) {
        LinkedList<String> lines = new LinkedList<>();
        int spaces = lineAlign - line.length() + 1;
        if (spaces == -1) {
            lines.add(line);
            return lines;
        }
        // System.out.println(line);
        StringTokenizer st = new StringTokenizer(comment, "\\");
        int count = st.countTokens();
        for (int j = 0; j < count; j++) {
            StringBuilder sb = new StringBuilder();
            if (j == 0) {
                sb.append(line);
            } else {
                for (int i = 0; i < line.length(); i++) {
                    sb.append(" ");
                }
            }
            for (int i = 0; i < spaces; i++) {
                sb.append(" ");
            }
            sb.append(st.nextToken());
            lines.add(sb.toString());
        }
        // System.out.println(lines.toString());
        return lines;

    }

}
