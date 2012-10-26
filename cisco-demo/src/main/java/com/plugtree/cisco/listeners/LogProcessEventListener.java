package com.plugtree.cisco.listeners;

import java.io.PrintStream;

import org.drools.event.process.ProcessCompletedEvent;
import org.drools.event.process.ProcessEventListener;
import org.drools.event.process.ProcessNodeLeftEvent;
import org.drools.event.process.ProcessNodeTriggeredEvent;
import org.drools.event.process.ProcessStartedEvent;
import org.drools.event.process.ProcessVariableChangedEvent;

public class LogProcessEventListener implements ProcessEventListener {

	private final PrintStream out;

	public LogProcessEventListener() {
		this(System.out);
	}
	
	public LogProcessEventListener(PrintStream out) {
		this.out = out;
	}
	
	public void afterNodeLeft(ProcessNodeLeftEvent event) {
		out.println("AFTER NODE LEFT: " + event);
	}

	public void afterNodeTriggered(ProcessNodeTriggeredEvent event) {
		out.println("AFTER NODE TRIGGERED: " + event);
	}

	public void afterProcessCompleted(ProcessCompletedEvent event) {
		out.println("AFTER PROCESS COMPLETED: " + event);
	}

	public void afterProcessStarted(ProcessStartedEvent event) {
		out.println("AFTER PROCESS STARTED: " + event);
	}

	public void afterVariableChanged(ProcessVariableChangedEvent event) {
		out.println("AFTER VARIABLE CHANGED: " + event);
	}

	public void beforeNodeLeft(ProcessNodeLeftEvent event) {
		out.println("BEFORE NODE LEFT: " + event);
	}

	public void beforeNodeTriggered(ProcessNodeTriggeredEvent event) {
		out.println("BEFORE NODE TRIGGERED: " + event);
	}

	public void beforeProcessCompleted(ProcessCompletedEvent event) {
		out.println("BEFORE PROCESS COMPLETED: " + event);
	}

	public void beforeProcessStarted(ProcessStartedEvent event) {
		out.println("BEFORE PROCESS STARTED: " + event);
	}

	public void beforeVariableChanged(ProcessVariableChangedEvent event) {
		out.println("BEFORE VARIABLE CHANGE: " + event);
	}

}
