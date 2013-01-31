package com.ghelius.narodmon;

public class JSONEncoder {
    public static String encode(String s){
        if(s==null)
            return null;
        StringBuffer stringBuffer = new StringBuffer();
        escapeCharacters(s, stringBuffer);
        return stringBuffer.toString();
    }

    static void escapeCharacters(String s, StringBuffer stringBuffer) {
        for(int i=0;i<s.length();i++){
            char ch=s.charAt(i);
            switch(ch){
                case '"':
                    stringBuffer.append("\\\"");
                    break;
                case '\\':
                    stringBuffer.append("\\\\");
                    break;
                case '\b':
                    stringBuffer.append("\\b");
                    break;
                case '\f':
                    stringBuffer.append("\\f");
                    break;
                case '\n':
                    stringBuffer.append("\\n");
                    break;
                case '\r':
                    stringBuffer.append("\\r");
                    break;
                case '\t':
                    stringBuffer.append("\\t");
                    break;
                case '/':
                    stringBuffer.append("\\/");
                    break;
                default:
                    String ss=Integer.toHexString(ch);
                    stringBuffer.append("\\u");
                    for(int k=0;k<4-ss.length();k++){
                        stringBuffer.append('0');
                    }
                    stringBuffer.append(ss.toUpperCase());
            }
        }
    }
}
