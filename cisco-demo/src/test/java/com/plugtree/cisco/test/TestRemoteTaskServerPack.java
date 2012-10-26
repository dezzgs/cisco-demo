package com.plugtree.cisco.test;

import org.jbpm.task.service.TaskClient;
import org.jbpm.task.service.TaskServer;

public class TestRemoteTaskServerPack {

	private final TaskServer taskServer;
	private final TaskClient taskClient;

	public TestRemoteTaskServerPack(TaskServer taskServer, TaskClient taskClient) {
		super();
		this.taskServer = taskServer;
		this.taskClient = taskClient;
	}

	public TaskServer getTaskServer() {
		return taskServer;
	}
	
	public TaskClient getTaskClient() {
		return taskClient;
	}
	
}
