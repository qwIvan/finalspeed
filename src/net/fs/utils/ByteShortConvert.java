// Copyright (c) 2015 D1SM.net

package net.fs.utils;

public final class ByteShortConvert {
   
    public static void toByteArray(short i, byte[] b, int offset) {
    	 b[offset] = (byte) (i >> 8);
    	   b[offset + 1] = (byte) (i >> 0);
    }


    
    public static short toShort(byte[] b,int offset) { 
        return  (short) (((b[offset] << 8) | b[offset + 1] & 0xff)); 
    }


}
