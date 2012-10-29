package com.plugtree.cisco.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.naming.Context;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.drools.SystemEventListener;
import org.drools.SystemEventListenerFactory;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.process.ProcessInstance;
import org.drools.runtime.process.WorkflowProcessInstance;
import org.easymock.EasyMock;
import org.jbpm.process.workitem.wsht.AsyncGenericHTWorkItemHandler;
import org.jbpm.process.workitem.wsht.AsyncMinaHTWorkItemHandler;
import org.jbpm.process.workitem.wsht.GenericHTWorkItemHandler;
import org.jbpm.task.AsyncTaskService;
import org.jbpm.task.Status;
import org.jbpm.task.query.TaskSummary;
import org.jbpm.task.service.ContentData;
import org.jbpm.task.service.TaskClient;
import org.jbpm.task.service.TaskService;
import org.jbpm.task.service.UserGroupCallbackManager;
import org.jbpm.task.service.jms.JMSTaskClientConnector;
import org.jbpm.task.service.jms.JMSTaskClientHandler;
import org.jbpm.task.service.jms.JMSTaskServer;
import org.jbpm.task.service.local.LocalTaskService;
import org.jbpm.task.service.mina.AsyncMinaTaskClient;
import org.jbpm.task.service.mina.MinaTaskServer;
import org.jbpm.task.service.responsehandlers.BlockingTaskOperationResponseHandler;
import org.jbpm.task.service.responsehandlers.BlockingTaskSummaryResponseHandler;
import org.jbpm.task.utils.ContentMarshallerContext;
import org.jbpm.task.utils.ContentMarshallerHelper;
import org.junit.Test;

public class TaskServiceTest {

	private TaskService startInternalTaskService() {
		//start taskService 
		SystemEventListener systemEventListener = SystemEventListenerFactory.getSystemEventListener();
		UserGroupCallbackManager.getInstance().setCallback(new TestUserGroupCallback());
		EntityManagerFactory emf = Persistence.createEntityManagerFactory("org.jbpm.task");
		TaskService internalTaskService = new TaskService(emf, systemEventListener);
		//the default escalated deadline handler will try to send an email.
		//the test one we created only prints the escalation through system.out
		TestEscalatedDeadlineHandler deadlineHandler = new TestEscalatedDeadlineHandler();
		TestUserInfo userInfo = new TestUserInfo();
		deadlineHandler.setUserInfo(userInfo);
		internalTaskService.setEscalatedDeadlineHandler(deadlineHandler);
		internalTaskService.setUserinfo(userInfo);
		return internalTaskService;
	}
	
	private int getAvailablePort() {
		int taskPort = 9123; //initial port is default mina port
		boolean portOpen = false;
        ServerSocket socket = null;
        while (!portOpen) {
            try {
                socket = new ServerSocket(taskPort);
                socket.setReuseAddress(true);
                portOpen = true;
            } catch (IOException e) {
                taskPort++;//increase until we find a port open
                System.out.println("Port used. Trying with new port: " + taskPort);
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) { }
                }
            }
        }
        return taskPort;
	}
	
	@Test
	public void testLocalTaskService() throws Exception {
		
		TaskService internalTaskService = startInternalTaskService();
		
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

	@Test
	public void testMinaTaskService() throws Exception {
		
		TaskService internalTaskService = startInternalTaskService();
		
		int availablePort = getAvailablePort();
		MinaTaskServer taskServer = new MinaTaskServer(internalTaskService, availablePort);
		taskServer.start();
		AsyncTaskService taskClient = new AsyncMinaTaskClient();
		
		StatefulKnowledgeSession ksession = JBPMUtil.initSimpleSession();
		
		//use human-task-process with AsyncMinaHTWorkItemHandler and MinaTaskServer / TaskClient
		AsyncMinaHTWorkItemHandler humanTaskHandler = new AsyncMinaHTWorkItemHandler(ksession);
		humanTaskHandler.setClient(taskClient);
		humanTaskHandler.setIpAddress("127.0.0.1");
		humanTaskHandler.setPort(availablePort); //quickfix to generic connect issue
		
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task", humanTaskHandler);
		
		Map<String, Object> initData = new HashMap<String, Object>();
		initData.put("dataone", "first value");
		WorkflowProcessInstance instance = (WorkflowProcessInstance) ksession.startProcess("demo.human-task-process", initData);
		assertEquals(ProcessInstance.STATE_ACTIVE, instance.getState());

		final long waitTime = 7000;
		
		BlockingTaskSummaryResponseHandler responseHandler1 = new BlockingTaskSummaryResponseHandler();
		taskClient.getTasksOwned("salaboy", "en-UK", responseHandler1);
		List<TaskSummary> tasks = responseHandler1.getResults();
		assertNotNull(tasks);
		assertEquals(1, tasks.size());
		
		TaskSummary firstTask = tasks.iterator().next();
		assertEquals(Status.Reserved, firstTask.getStatus());

		BlockingTaskOperationResponseHandler responseHandler2 = new BlockingTaskOperationResponseHandler();
		taskClient.start(firstTask.getId(), "salaboy", responseHandler2);
		responseHandler2.waitTillDone(waitTime);
		
		Map<String, Object> results1 = new HashMap<String, Object>();
		results1.put("data2", "second value");
		ContentData outputData1 = ContentMarshallerHelper.marshal(results1, new ContentMarshallerContext(), ksession.getEnvironment());
		BlockingTaskOperationResponseHandler responseHandler3 = new BlockingTaskOperationResponseHandler();
		taskClient.complete(firstTask.getId(), "salaboy", outputData1, responseHandler3);
		responseHandler3.waitTillDone(waitTime);
		
		Thread.sleep(500); //there's a race condition here between the thread that completes the task and the thread that updates the ksession
		
		//up to here, all direct interaction is handled through task service asynchrohously
		// the handler is in charge of getting to the next task
		assertEquals(ProcessInstance.STATE_ACTIVE, instance.getState());
		
		assertNotNull(instance.getVariable("datatwo"));
		assertEquals(instance.getVariable("datatwo"), "second value");
		
		List<String> groupIds = new ArrayList<String>();
		groupIds.add("users");
		BlockingTaskSummaryResponseHandler responseHandler4 = new BlockingTaskSummaryResponseHandler();
		taskClient.getTasksAssignedAsPotentialOwner("mariano", groupIds, "en-UK", responseHandler4);
		List<TaskSummary> groupTasks = responseHandler4.getResults();
		assertNotNull(groupTasks);
		assertEquals(1, groupTasks.size());
		
		TaskSummary secondTask = groupTasks.iterator().next();
		assertEquals(Status.Ready, secondTask.getStatus());
		BlockingTaskOperationResponseHandler responseHandler5 = new BlockingTaskOperationResponseHandler();
		taskClient.claim(secondTask.getId(), "mariano", groupIds, responseHandler5);
		responseHandler5.waitTillDone(waitTime);
		BlockingTaskOperationResponseHandler responseHandler6 = new BlockingTaskOperationResponseHandler();
		taskClient.start(secondTask.getId(), "mariano", responseHandler6);
		responseHandler6.waitTillDone(waitTime);

		Map<String, Object> results2 = new HashMap<String, Object>();
		results2.put("data3", "third value");
		ContentData outputData2 = ContentMarshallerHelper.marshal(results2, new ContentMarshallerContext(), ksession.getEnvironment());
		BlockingTaskOperationResponseHandler responseHandler7 = new BlockingTaskOperationResponseHandler();
		taskClient.complete(secondTask.getId(), "mariano", outputData2, responseHandler7);
		responseHandler7.waitTillDone(waitTime);

		Thread.sleep(500); //there's a race condition here between the thread that completes the task and the thread that updates the ksession
		
		//now that the second task is completed, the process is completed as well
		assertEquals(ProcessInstance.STATE_COMPLETED, instance.getState());
		assertNotNull(instance.getVariable("datathree"));
		assertEquals(instance.getVariable("datathree"), "third value");
		
		taskServer.stop();

	}

	@Test
	public void testJMSTaskService() throws Exception {
		
		TaskService internalTaskService = startInternalTaskService();
		
		ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false");
		Context context = EasyMock.createMock(Context.class);
        EasyMock.expect(context.lookup("ConnectionFactory")).andReturn(factory).anyTimes();
        EasyMock.replay(context);

        Properties serverConnProperties = new Properties();
        serverConnProperties.setProperty("JMSTaskServer.connectionFactory", "ConnectionFactory");
        serverConnProperties.setProperty("JMSTaskServer.transacted", "true");
        serverConnProperties.setProperty("JMSTaskServer.acknowledgeMode", "AUTO_ACKNOWLEDGE");
        serverConnProperties.setProperty("JMSTaskServer.queueName", "tasksQueue");
        serverConnProperties.setProperty("JMSTaskServer.responseQueueName", "tasksResponseQueue");

		JMSTaskServer taskServer = new JMSTaskServer(internalTaskService, serverConnProperties, context);
		Thread thread = new Thread(taskServer);
		thread.start();

        Properties clientConnProperties = new Properties();
        clientConnProperties.setProperty("JMSTaskClient.connectionFactory", "ConnectionFactory");
        clientConnProperties.setProperty("JMSTaskClient.transactedQueue", "true");
        clientConnProperties.setProperty("JMSTaskClient.acknowledgeMode", "AUTO_ACKNOWLEDGE");
        clientConnProperties.setProperty("JMSTaskClient.queueName", "tasksQueue");
        clientConnProperties.setProperty("JMSTaskClient.responseQueueName", "tasksResponseQueue");

		AsyncTaskService taskClient = new TaskClient(new JMSTaskClientConnector("client-jms", 
				new JMSTaskClientHandler(SystemEventListenerFactory.getSystemEventListener()), 
				clientConnProperties, context));
		
		StatefulKnowledgeSession ksession = JBPMUtil.initSimpleSession();
		
		//use human-task-process with AsyncMinaHTWorkItemHandler and MinaTaskServer / TaskClient
		AsyncGenericHTWorkItemHandler humanTaskHandler = new AsyncGenericHTWorkItemHandler(ksession);
		
		humanTaskHandler.setClient(taskClient);
		humanTaskHandler.setIpAddress("127.0.0.1"); //quickfix to generic connect issue
		humanTaskHandler.setPort(20); //quickfix to generic connect issue
		
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task", humanTaskHandler);
		
		Map<String, Object> initData = new HashMap<String, Object>();
		initData.put("dataone", "first value");
		WorkflowProcessInstance instance = (WorkflowProcessInstance) ksession.startProcess("demo.human-task-process", initData);
		assertEquals(ProcessInstance.STATE_ACTIVE, instance.getState());

		final long waitTime = 7000;
		
		BlockingTaskSummaryResponseHandler responseHandler1 = new BlockingTaskSummaryResponseHandler();
		taskClient.getTasksOwned("salaboy", "en-UK", responseHandler1);
		List<TaskSummary> tasks = responseHandler1.getResults();
		assertNotNull(tasks);
		assertEquals(1, tasks.size());
		
		TaskSummary firstTask = tasks.iterator().next();
		assertEquals(Status.Reserved, firstTask.getStatus());

		BlockingTaskOperationResponseHandler responseHandler2 = new BlockingTaskOperationResponseHandler();
		taskClient.start(firstTask.getId(), "salaboy", responseHandler2);
		responseHandler2.waitTillDone(waitTime);
		
		Map<String, Object> results1 = new HashMap<String, Object>();
		results1.put("data2", "second value");
		ContentData outputData1 = ContentMarshallerHelper.marshal(results1, new ContentMarshallerContext(), ksession.getEnvironment());
		BlockingTaskOperationResponseHandler responseHandler3 = new BlockingTaskOperationResponseHandler();
		taskClient.complete(firstTask.getId(), "salaboy", outputData1, responseHandler3);
		responseHandler3.waitTillDone(waitTime);
		
		Thread.sleep(500); //there's a race condition here between the thread that completes the task and the thread that updates the ksession
		
		//up to here, all direct interaction is handled through task service asynchrohously
		// the handler is in charge of getting to the next task
		assertEquals(ProcessInstance.STATE_ACTIVE, instance.getState());
		
		assertNotNull(instance.getVariable("datatwo"));
		assertEquals(instance.getVariable("datatwo"), "second value");
		
		List<String> groupIds = new ArrayList<String>();
		groupIds.add("users");
		BlockingTaskSummaryResponseHandler responseHandler4 = new BlockingTaskSummaryResponseHandler();
		taskClient.getTasksAssignedAsPotentialOwner("mariano", groupIds, "en-UK", responseHandler4);
		List<TaskSummary> groupTasks = responseHandler4.getResults();
		assertNotNull(groupTasks);
		assertEquals(1, groupTasks.size());
		
		TaskSummary secondTask = groupTasks.iterator().next();
		assertEquals(Status.Ready, secondTask.getStatus());
		BlockingTaskOperationResponseHandler responseHandler5 = new BlockingTaskOperationResponseHandler();
		taskClient.claim(secondTask.getId(), "mariano", groupIds, responseHandler5);
		responseHandler5.waitTillDone(waitTime);
		BlockingTaskOperationResponseHandler responseHandler6 = new BlockingTaskOperationResponseHandler();
		taskClient.start(secondTask.getId(), "mariano", responseHandler6);
		responseHandler6.waitTillDone(waitTime);

		Map<String, Object> results2 = new HashMap<String, Object>();
		results2.put("data3", "third value");
		ContentData outputData2 = ContentMarshallerHelper.marshal(results2, new ContentMarshallerContext(), ksession.getEnvironment());
		BlockingTaskOperationResponseHandler responseHandler7 = new BlockingTaskOperationResponseHandler();
		taskClient.complete(secondTask.getId(), "mariano", outputData2, responseHandler7);
		responseHandler7.waitTillDone(waitTime);

		Thread.sleep(500); //there's a race condition here between the thread that completes the task and the thread that updates the ksession
		
		//now that the second task is completed, the process is completed as well
		assertEquals(ProcessInstance.STATE_COMPLETED, instance.getState());
		assertNotNull(instance.getVariable("datathree"));
		assertEquals(instance.getVariable("datathree"), "third value");
		
		taskServer.stop();

	}

}
