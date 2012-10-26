package com.plugtree.cisco.workitems;

import org.drools.runtime.process.WorkItem;
import org.drools.runtime.process.WorkItemHandler;
import org.drools.runtime.process.WorkItemManager;

public class ExceptionWorkItemHandler implements WorkItemHandler {

	public static final String ERR_MESSAGE = "workItemHandler called. Exception throwed";
	
	public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
		throw new RuntimeException(ERR_MESSAGE);
	}

	public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
		// do nothing
	}

}
