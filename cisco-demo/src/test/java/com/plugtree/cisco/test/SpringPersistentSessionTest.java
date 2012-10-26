package com.plugtree.cisco.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.drools.WorkingMemory;
import org.drools.command.impl.CommandBasedStatefulKnowledgeSession;
import org.drools.command.impl.KnowledgeCommandContext;
import org.drools.event.ActivationCancelledEvent;
import org.drools.event.ActivationCreatedEvent;
import org.drools.event.AfterActivationFiredEvent;
import org.drools.event.AgendaGroupPoppedEvent;
import org.drools.event.AgendaGroupPushedEvent;
import org.drools.event.BeforeActivationFiredEvent;
import org.drools.event.RuleFlowGroupActivatedEvent;
import org.drools.event.RuleFlowGroupDeactivatedEvent;
import org.drools.impl.StatefulKnowledgeSessionImpl;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.process.ProcessInstance;
import org.drools.runtime.process.WorkflowProcessInstance;
import org.jbpm.process.audit.ProcessInstanceLog;
import org.jbpm.process.workitem.wsht.GenericHTWorkItemHandler;
import org.jbpm.task.Status;
import org.jbpm.task.query.TaskSummary;
import org.jbpm.task.service.ContentData;
import org.jbpm.task.service.local.LocalTaskService;
import org.jbpm.task.utils.ContentMarshallerContext;
import org.jbpm.task.utils.ContentMarshallerHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import com.plugtree.cisco.spring.JPAKnowledgeServiceBean;
import com.plugtree.cisco.spring.SpringJPAProcessInstanceDbLog;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:config-spring.xml")
@Transactional
@TransactionConfiguration(defaultRollback=true)
public class SpringPersistentSessionTest {

	@Resource(name="knowledgeProvider")
	private JPAKnowledgeServiceBean knowledgeProvider;
	@Resource(name="taskService")
	private LocalTaskService taskService;
	
	@Test
	public void testJPASessionForProcessWithRules() {
		final StatefulKnowledgeSession ksession = knowledgeProvider.newSession();
		//you need to create this kind of listener to make sure business rule tasks are executed
		final org.drools.event.AgendaEventListener agendaEventListener = new org.drools.event.AgendaEventListener() {
            public void activationCreated(ActivationCreatedEvent event, WorkingMemory workingMemory){
                ksession.fireAllRules();
            }
            public void afterRuleFlowGroupActivated(RuleFlowGroupActivatedEvent event, WorkingMemory workingMemory) {
                workingMemory.fireAllRules();
            }
            public void activationCancelled(ActivationCancelledEvent event, WorkingMemory workingMemory){ }
            public void beforeActivationFired(BeforeActivationFiredEvent event, WorkingMemory workingMemory) { }
            public void afterActivationFired(AfterActivationFiredEvent event, WorkingMemory workingMemory) { }
            public void agendaGroupPopped(AgendaGroupPoppedEvent event, WorkingMemory workingMemory) { }
            public void agendaGroupPushed(AgendaGroupPushedEvent event, WorkingMemory workingMemory) { }
            public void beforeRuleFlowGroupActivated(RuleFlowGroupActivatedEvent event, WorkingMemory workingMemory) { }
            public void beforeRuleFlowGroupDeactivated(RuleFlowGroupDeactivatedEvent event, WorkingMemory workingMemory) { }
            public void afterRuleFlowGroupDeactivated(RuleFlowGroupDeactivatedEvent event, WorkingMemory workingMemory) { }
        };
        //adding it is a bit dirty for the time being, but it works:
        //NOTE: this is the current way to do it when you use a persisted session
        ( (StatefulKnowledgeSessionImpl) ((KnowledgeCommandContext) ((CommandBasedStatefulKnowledgeSession) ksession)
        		.getCommandService().getContext()).getStatefulKnowledgesession() )
        		.session.addEventListener(agendaEventListener);
        
        //if any rules in your knowledge package should fire process instantiations, you should call fireAllRules() as a
        //first step after creating your session
        
        Map<String, Object> inputData1 = new HashMap<String, Object>();
        inputData1.put("input", "100");
        WorkflowProcessInstance instance1 = (WorkflowProcessInstance) ksession.startProcess("demo.rule-process", inputData1);
        
        assertEquals(ProcessInstance.STATE_COMPLETED, instance1.getState());
        assertNotNull(instance1.getVariable("processVar"));
        assertEquals("message", instance1.getVariable("processVar"));
        
        Map<String, Object> inputData2 = new HashMap<String, Object>();
        inputData2.put("input", "99");
        WorkflowProcessInstance instance2 = (WorkflowProcessInstance) ksession.startProcess("demo.rule-process", inputData2);

        assertEquals(ProcessInstance.STATE_COMPLETED, instance2.getState());
        assertNotNull(instance2.getVariable("processVar"));
        assertEquals("error", instance2.getVariable("processVar"));
        
        Map<String, Object> inputData3 = new HashMap<String, Object>();
        WorkflowProcessInstance instance3 = (WorkflowProcessInstance) ksession.startProcess("demo.rule-process", inputData3);

        assertEquals(ProcessInstance.STATE_COMPLETED, instance3.getState());
        assertNotNull(instance3.getVariable("processVar"));
        assertEquals("error", instance3.getVariable("processVar"));
        
	}
	
	@Test
	public void testPersistentSessionConfiguredThroughSpring() throws Exception {
		StatefulKnowledgeSession ksession = knowledgeProvider.newSession();
		//JPAProcessInstanceDbLog.setEnvironment(knowledgeProvider.createEnvironment());
		
		//registering work item handlers and listeners is still needed because it's a runtime configuration
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task", TestAsyncWorkItemHandler.getInstance());
		
		Map<String, Object> initData = new HashMap<String, Object>();
		initData.put("dataone", "1");
		initData.put("datatwo", "2");
		ProcessInstance pI = ksession.startProcess("demo.human-task-process", initData);
		
		assertEquals(pI.getState(), ProcessInstance.STATE_ACTIVE);
		
		int sessionId = ksession.getId();
		
		//dispose of session to free memory
		//ksession.dispose(); //TODO there seems to be issues regarding using a spring transaction manager and disposing a session
		                      //     for the moment disposal should be done after processes is already finished
		
		StatefulKnowledgeSession ksession2 = knowledgeProvider.loadSession(sessionId);
		ksession2.getWorkItemManager().registerWorkItemHandler("Human Task", TestAsyncWorkItemHandler.getInstance());
		
		//validate variables are still there
		WorkflowProcessInstance wpI = (WorkflowProcessInstance) ksession2.getProcessInstance(pI.getId());
		assertEquals(wpI.getVariable("dataone"), "1");

		ksession2.getWorkItemManager().completeWorkItem(TestAsyncWorkItemHandler.getInstance().getItem().getId(), null);
		assertEquals(pI.getState(), ProcessInstance.STATE_ACTIVE);
		ksession2.getWorkItemManager().completeWorkItem(TestAsyncWorkItemHandler.getInstance().getItem().getId(), null);
		assertEquals(wpI.getState(), ProcessInstance.STATE_COMPLETED);
		
		//ksession2.dispose(); //TODO there seems to be issues regarding using a spring transaction manager and disposing a session
                               //     for the moment disposal should be done after processes is already finished
		
		StatefulKnowledgeSession ksession3 = knowledgeProvider.loadSession(sessionId);
		
		//assert only active instances are accesible through the session
		Collection<ProcessInstance> activePIs = ksession3.getProcessInstances();
		assertTrue(activePIs.isEmpty());

		List<ProcessInstanceLog> completedProcesses = SpringJPAProcessInstanceDbLog.findProcessInstances();
		assertNotNull(completedProcesses);
		assertEquals(1, completedProcesses.size());
		//ksession3.dispose();
	}
	
	@Test
	public void testLocalTaskService() throws Exception {
		
		StatefulKnowledgeSession ksession = knowledgeProvider.newSession();
		
		//use human-task-process with GenericHTWorkItemHandler and LocalTaskService
		GenericHTWorkItemHandler humanTaskHandler = new GenericHTWorkItemHandler(ksession);
		humanTaskHandler.setClient(taskService);
		humanTaskHandler.setIpAddress("ommit"); //quickfix to generic connect issue
		humanTaskHandler.setPort(20); //quickfix to generic connect issue
		
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
		
		Thread.sleep(100); //there's a small race condition inside the process for some reason
		
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

		Thread.sleep(100); //there's a small race condition inside the process for some reason

		//now that the second task is completed, the process is completed as well
		assertEquals(ProcessInstance.STATE_COMPLETED, instance.getState());
		assertNotNull(instance.getVariable("datathree"));
		assertEquals(instance.getVariable("datathree"), "third value");
		
	}


}
