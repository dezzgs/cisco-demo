package com.plugtree.cisco.listeners;

import java.io.PrintStream;

import org.drools.event.rule.ObjectInsertedEvent;
import org.drools.event.rule.ObjectRetractedEvent;
import org.drools.event.rule.ObjectUpdatedEvent;
import org.drools.event.rule.WorkingMemoryEventListener;

public class LogWMEventListener implements WorkingMemoryEventListener {

	private final PrintStream out;

	public LogWMEventListener() {
		this(System.out);
	}
	
	public LogWMEventListener(PrintStream out) {
		this.out = out;
	}
	
	public void objectInserted(ObjectInsertedEvent event) {
		out.println("OBJECT INSERTED: " + event);
	}

	public void objectUpdated(ObjectUpdatedEvent event) {
		out.println("OBJECT UPDATED: " + event);
	}

	public void objectRetracted(ObjectRetractedEvent event) {
		out.println("OBJECT RETRACTED: " + event);
	}

}
