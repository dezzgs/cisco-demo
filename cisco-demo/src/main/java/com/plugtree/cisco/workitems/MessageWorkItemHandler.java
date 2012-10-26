package com.plugtree.cisco.workitems;

import org.drools.runtime.process.WorkItem;
import org.drools.runtime.process.WorkItemHandler;
import org.drools.runtime.process.WorkItemManager;

public class MessageWorkItemHandler implements WorkItemHandler {
	
	private final String message;
	
	public MessageWorkItemHandler(String message) {
		if (message == null) {
			throw new IllegalArgumentException("message should not be null");
		}
		this.message = message;
	}
	
	public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
		String parsedMessage = this.message + " " + workItem.getParameter("name") + "!";
		System.out.println(parsedMessage);
		
		manager.completeWorkItem(workItem.getId(), null);
	}

	public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
		//do nothing
	}

}
