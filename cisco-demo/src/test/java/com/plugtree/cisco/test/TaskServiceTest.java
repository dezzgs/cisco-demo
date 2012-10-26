package com.plugtree.cisco.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.drools.SystemEventListener;
import org.drools.SystemEventListenerFactory;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.process.ProcessInstance;
import org.drools.runtime.process.WorkflowProcessInstance;
import org.jbpm.process.workitem.wsht.GenericHTWorkItemHandler;
import org.jbpm.task.Status;
import org.jbpm.task.query.TaskSummary;
import org.jbpm.task.service.ContentData;
import org.jbpm.task.service.TaskService;
import org.jbpm.task.service.UserGroupCallbackManager;
import org.jbpm.task.service.local.LocalTaskService;
import org.jbpm.task.utils.ContentMarshallerContext;
import org.jbpm.task.utils.ContentMarshallerHelper;
import org.junit.Test;

public class TaskServiceTest {
	
	@Test
	public void testLocalTaskService() throws Exception {
		
		//start taskService 
		SystemEventListener systemEventListener = SystemEventListenerFactory.getSystemEventListener();
		UserGroupCallbackManager.getInstance().setCallback(new TestUserGroupCallback());
		EntityManagerFactory emf = Persistence.createEntityManagerFactory("org.jbpm.task");
		TaskService internalTaskService = new TaskService(emf, systemEventListener);
		//the default escalated deadline handler will try to send an email.
		//te test one we created only prints the escalation through system.out
		TestEscalatedDeadlineHandler deadlineHandler = new TestEscalatedDeadlineHandler();
		TestUserInfo userInfo = new TestUserInfo();
		deadlineHandler.setUserInfo(userInfo);
		internalTaskService.setEscalatedDeadlineHandler(deadlineHandler);
		internalTaskService.setUserinfo(userInfo);
		LocalTaskService taskService = new LocalTaskService(internalTaskService);
		
		StatefulKnowledgeSession ksession = JBPMUtil.initSimpleSession();
		
		//use human-task-process with GenericHTWorkItemHandler and LocalTaskService
		GenericHTWorkItemHandler humanTaskHandler = new GenericHTWorkItemHandler(ksession);
		humanTaskHandler.setClient(taskService);
		humanTaskHandler.setIpAddress("ommit"); //quickfix to generic connect issue
		humanTaskHandler.setPort(20); //quickfix to generic connect issue
		humanTaskHandler.setLocal(true);
		
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task", humanTaskHandler);
		
		Map<String, Object> initData = new HashMap<String, Object>();
		initData.put("dataone", "first value");
		WorkflowProcessInstance instance = (WorkflowProcessInstance) ksession.startProcess("demo.human-task-process", initData);
		assertEquals(ProcessInstance.STATE_ACTIVE, instance.getState());
		
		List<TaskSummary> tasks = taskService.getTasksOwned("salaboy", "en-UK");
		assertNotNull(tasks);
		assertEquals(1, tasks.size());
		
		TaskSummary firstTask = tasks.iterator().next();
		assertEquals(Status.Reserved, firstTask.getStatus());
		taskService.start(firstTask.getId(), "salaboy");
		
		Map<String, Object> results1 = new HashMap<String, Object>();
		results1.put("data2", "second value");
		ContentData outputData1 = ContentMarshallerHelper.marshal(results1, new ContentMarshallerContext(), ksession.getEnvironment());
		taskService.complete(firstTask.getId(), "salaboy", outputData1);
		//up to here, all direct interaction is handled through task service
		// the handler is in charge of getting to the next task
		
		assertEquals(ProcessInstance.STATE_ACTIVE, instance.getState());
		
		assertNotNull(instance.getVariable("datatwo"));
		assertEquals(instance.getVariable("datatwo"), "second value");
		
		List<String> groupIds = new ArrayList<String>();
		groupIds.add("users");
		List<TaskSummary> groupTasks = taskService.getTasksAssignedAsPotentialOwner("mariano", groupIds, "en-UK");
		assertNotNull(groupTasks);
		assertEquals(1, groupTasks.size());
		
		TaskSummary secondTask = groupTasks.iterator().next();
		assertEquals(Status.Ready, secondTask.getStatus());
		taskService.claim(secondTask.getId(), "mariano", groupIds);
		taskService.start(secondTask.getId(), "mariano");

		Map<String, Object> results2 = new HashMap<String, Object>();
		results2.put("data3", "third value");
		ContentData outputData2 = ContentMarshallerHelper.marshal(results2, new ContentMarshallerContext(), ksession.getEnvironment());
		taskService.complete(secondTask.getId(), "mariano", outputData2);
		
		//now that the second task is completed, the process is completed as well
		assertEquals(ProcessInstance.STATE_COMPLETED, instance.getState());
		assertNotNull(instance.getVariable("datathree"));
		assertEquals(instance.getVariable("datathree"), "third value");
		
	}
}
