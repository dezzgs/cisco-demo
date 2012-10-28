package com.plugtree.cisco.workitems;

import org.drools.process.instance.WorkItemHandler;
import org.drools.runtime.process.WorkItem;
import org.drools.runtime.process.WorkItemManager;

/**
 * Implements a counter of how many times this handler is called
 * 
 * @author mariano.demaio@plugtree.com
 *
 */
public class CounterWorkItemHandler implements WorkItemHandler {

	private int count;
	
	private static final CounterWorkItemHandler INSTANCE = new CounterWorkItemHandler();
	
	public static CounterWorkItemHandler getInstance() {
		return INSTANCE;
	}
	
	private CounterWorkItemHandler() {
	}
	
	public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
		synchronized (this) {
			++count;
		}
		manager.completeWorkItem(workItem.getId(), workItem.getResults());
	}

	public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
	}

	public int getCount() {
		return count;
	}
	
	public synchronized void restart() {
		count = 0;
	}
}
