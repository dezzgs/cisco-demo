package com.plugtree.cisco.workitems;

import java.io.PrintStream;
import java.util.Map;

import org.drools.runtime.process.WorkItem;
import org.drools.runtime.process.WorkItemHandler;
import org.drools.runtime.process.WorkItemManager;

public class LogWorkItemHandler implements WorkItemHandler {

	private final PrintStream out;
	
	public LogWorkItemHandler(PrintStream out) {
		this.out = out;
	}
	
	public void abortWorkItem(WorkItem item, WorkItemManager manager) {
		out.println("work item aborted: id " + item.getId() + " for process instance " + item.getProcessInstanceId() +" with data...");
		for (Map.Entry<String, Object> entry : item.getParameters().entrySet()) {
			out.println("-->" + entry.getKey() + ", " + entry.getValue());
		}
		manager.abortWorkItem(item.getId());
	}

	public void executeWorkItem(WorkItem item, WorkItemManager manager) {
		out.println("work item executed: id " + item.getId() + " for process instance " + item.getProcessInstanceId() +" with data...");
		for (Map.Entry<String, Object> entry : item.getParameters().entrySet()) {
			out.println("-->" + entry.getKey() + ", " + entry.getValue());
		}
		manager.completeWorkItem(item.getId(), item.getResults());
	}

}
