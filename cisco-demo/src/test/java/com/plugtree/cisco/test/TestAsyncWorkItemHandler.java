package com.plugtree.cisco.test;

import java.util.Map;

import org.drools.runtime.process.WorkItem;
import org.drools.runtime.process.WorkItemHandler;
import org.drools.runtime.process.WorkItemManager;

/**
 * This is a simple async work item handler. It self stores the work item ID
 * that needs to be completed to continue a process.
 * 
 * @author mariano.demaio@plugtree.com
 *
 */
public class TestAsyncWorkItemHandler implements WorkItemHandler {

	private WorkItem item = null;

	private static final TestAsyncWorkItemHandler INSTANCE = new TestAsyncWorkItemHandler(); 
	
	public static TestAsyncWorkItemHandler getInstance() {
		return INSTANCE;
	}
	 
	private TestAsyncWorkItemHandler() {
	}
	
	public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
		this.item = workItem;
		if (workItem.getParameters() != null) {
			System.out.println("TestAsyncWorkItemHandler parameters:");
			for (Map.Entry<String, Object> entry : workItem.getParameters().entrySet()) {
				System.out.println("--->" + entry.getKey() + ": " + entry.getValue());
			}
		}
	}

	public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
		//do nothing
	}
	
	public WorkItem getItem() {
		WorkItem result = item;
		item = null;
		return result;
	}

}
