package com.plugtree.cisco.test;

import java.util.HashSet;
import java.util.Set;

import org.jbpm.task.Content;
import org.jbpm.task.Deadline;
import org.jbpm.task.EmailNotification;
import org.jbpm.task.EmailNotificationHeader;
import org.jbpm.task.OrganizationalEntity;
import org.jbpm.task.Task;
import org.jbpm.task.service.DefaultEscalatedDeadlineHandler;
import org.jbpm.task.service.TaskService;

public class TestEscalatedDeadlineHandler extends DefaultEscalatedDeadlineHandler {

	private static Set<Long> notifiedTaskIds = new HashSet<Long>(); 
	
	@Override
	public void executeEscalatedDeadline(Task task, Deadline deadline, Content content, TaskService service) {
		notifiedTaskIds.add(task.getId());
		super.executeEscalatedDeadline(task, deadline, content, service);
	}
	
	@Override
    public void executeEmailNotification(EmailNotification notification, Task task, Content content) {
		System.out.println("Escalated deadline for Task: " + task.getNames().iterator().next().getText());
		System.out.println("EmailNotification: " + notification.getId());
		for (EmailNotificationHeader header : notification.getEmailHeaders().values()) {
			System.out.println("-->from: " + header.getFrom());
			System.out.println("-->subject: " + header.getSubject());
			System.out.println("-->body: " + header.getBody()); 
		}
		for (OrganizationalEntity entity : notification.getRecipients()) {
			System.out.println("-->to: " + getUserInfo().getEmailForEntity(entity));;
		}
	}
	
	public static Set<Long> getNotifiedTaskIds() {
		return notifiedTaskIds;
	}

}
