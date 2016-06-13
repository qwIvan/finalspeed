package net.fs.utils;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashSet;

public class LogOutputStream extends PrintStream{
	
	private HashSet<LogListener> listeners= new HashSet<>();
	
	private StringBuffer buffer=new StringBuffer();
	
	public LogOutputStream(OutputStream out) {
		super(out);
	}
	
	@Override
	public void write(byte[] buf, int off, int len) {
		super.write(buf, off, len);
		fireEvent(new String(buf, off, len));
	}
	
	private void fireEvent(String text){
		if(buffer!=null&&buffer.length()<10000){
			buffer.append(text);
		}
		for(LogListener listener:listeners){
			listener.onAppendContent(text);
		}
	}


}
