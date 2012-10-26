package com.plugtree.cisco.workitems;

import org.drools.runtime.process.WorkItem;
import org.drools.runtime.process.WorkItemHandler;
import org.drools.runtime.process.WorkItemManager;

/**
 * Waits for a given time before finishing a task.
 * The purpose is to show how a syncrhonic heavy work item affects 
 * performance on the jBPM engine and why asynchronous handlers is the
 * best option.
 * 
 * @author mariano.demaio@plugtree.com
 *
 */
public class DelayWorkItemHandler implements WorkItemHandler {

	private final long waitTime;
	
	public DelayWorkItemHandler(long waitTime) {
		this.waitTime = waitTime;
	}
	
	public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
		System.out.println("waiting for " + (waitTime / 1000) + " secs...");
		try {
		    Thread.sleep(waitTime);
		} catch (InterruptedException e) {
			System.out.println("interrupted");
		}
		System.out.println("done waiting");
		manager.completeWorkItem(workItem.getId(), null);
	}

	public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
		//do nothing
	}

}
