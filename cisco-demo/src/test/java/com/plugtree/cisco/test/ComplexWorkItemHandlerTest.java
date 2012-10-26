package com.plugtree.cisco.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.process.ProcessInstance;
import org.drools.runtime.process.WorkflowProcessInstance;
import org.jbpm.workflow.instance.WorkflowRuntimeException;
import org.junit.Test;

import com.plugtree.cisco.workitems.DelayWorkItemHandler;
import com.plugtree.cisco.workitems.ExceptionWorkItemHandler;
import com.plugtree.cisco.workitems.MessageWorkItemHandler;

public class ComplexWorkItemHandlerTest {

	@Test
	public void testWorkItemsWithOutputs() throws Exception {
		StatefulKnowledgeSession ksession = JBPMUtil.initSimpleSession();
		
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task", TestAsyncWorkItemHandler.getInstance());
		
		Map<String, Object> initData = new HashMap<String, Object>();
		initData.put("dataone", "first value");
		WorkflowProcessInstance instance = (WorkflowProcessInstance) ksession.startProcess("demo.human-task-process", initData);
		
		assertEquals(instance.getState(), ProcessInstance.STATE_ACTIVE);
		
		Map<String, Object> results1 = new HashMap<String, Object>();
		results1.put("data2", "my value"); //the results match the data outputs of the task
		ksession.getWorkItemManager().completeWorkItem(TestAsyncWorkItemHandler.getInstance().getItem().getId(), results1);
		
		assertEquals(instance.getState(), ProcessInstance.STATE_ACTIVE);
		assertNotNull(instance.getVariable("datatwo")); //now it should be populated
		assertEquals("my value", instance.getVariable("datatwo")); //with the value of the data2 task output
		
		Map<String, Object> results2 = new HashMap<String, Object>();
		results2.put("data3", "my other value"); //the results match the data outputs of the task
		results2.put("data4", "lost value"); //since we don't assign this, it will be lost
		ksession.getWorkItemManager().completeWorkItem(TestAsyncWorkItemHandler.getInstance().getItem().getId(), results2);
		
		assertEquals(instance.getState(), ProcessInstance.STATE_COMPLETED);
		assertNotNull(instance.getVariable("datathree")); //now it should be populated
		assertEquals("my other value", instance.getVariable("datathree")); //with the value of the data3 task output
	}
	
	@Test
	public void testAsyncWorkItemHandlers() throws Exception {
		StatefulKnowledgeSession ksession = JBPMUtil.initSimpleSession();
		
		//registering work item handlers
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task", TestAsyncWorkItemHandler.getInstance());

		Map<String, Object> initData = new HashMap<String, Object>();
		initData.put("dataone", "1");
		initData.put("datatwo", "2");
		ProcessInstance pI = ksession.startProcess("demo.human-task-process", initData);
		
		assertEquals(pI.getState(), ProcessInstance.STATE_ACTIVE);
		
		//finishing task (letting the ksession know about it)
		ksession.getWorkItemManager().completeWorkItem(TestAsyncWorkItemHandler.getInstance().getItem().getId(), null);

		//because there are two tasks, the completeWorkItem should be called twice
		assertEquals(pI.getState(), ProcessInstance.STATE_ACTIVE);
		
		ksession.getWorkItemManager().completeWorkItem(TestAsyncWorkItemHandler.getInstance().getItem().getId(), null);
		
		assertEquals(ProcessInstance.STATE_COMPLETED, pI.getState());
	}
	
	@Test
	public void testHeavySyncWorkItemHandlers() throws Exception {
		StatefulKnowledgeSession ksession = JBPMUtil.initSimpleSession();
		
		//registering work item handlers
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task", new DelayWorkItemHandler(5000));
		
		Map<String, Object> initData = new HashMap<String, Object>();
		initData.put("dataone", "1");
		initData.put("datatwo", "2");
		ProcessInstance pI = ksession.startProcess("demo.human-task-process", initData);
		
		assertEquals(pI.getState(), ProcessInstance.STATE_COMPLETED);
	}
	
	@Test 
	public void testExceptionHappening() throws Exception {
		StatefulKnowledgeSession ksession = JBPMUtil.initSimpleSession();

		//registering work item handlers
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task", new ExceptionWorkItemHandler());
		
		try {
			ksession.startProcess("demo.human-task-process", new HashMap<String, Object>());
			fail("call to startProcess shouldn't succeed");
		} catch (WorkflowRuntimeException e) {
			Throwable actualException = e.getCause();
			assertTrue(actualException instanceof RuntimeException);
			assertEquals(ExceptionWorkItemHandler.ERR_MESSAGE, actualException.getMessage());
			e.printStackTrace();
		}
	}
	
	@Test
	public void testUserDefinedHandlers() throws Exception {
		StatefulKnowledgeSession ksession = JBPMUtil.initSimpleSession();
		
		//registering work item handlers
		ksession.getWorkItemManager().registerWorkItemHandler("hello", new MessageWorkItemHandler("hello"));
		ksession.getWorkItemManager().registerWorkItemHandler("goodbye", new MessageWorkItemHandler("goodbye"));
		
		Map<String, Object> initData = new HashMap<String, Object>();
		initData.put("name", "Mariano");
		ProcessInstance pI = ksession.startProcess("demo.my-user-defined-tasks-process-v1", initData);
		
		assertEquals(pI.getState(), ProcessInstance.STATE_COMPLETED);
	}
}
