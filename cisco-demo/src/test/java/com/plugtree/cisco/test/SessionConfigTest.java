package com.plugtree.cisco.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.drools.WorkingMemory;
import org.drools.event.ActivationCancelledEvent;
import org.drools.event.ActivationCreatedEvent;
import org.drools.event.AfterActivationFiredEvent;
import org.drools.event.AgendaGroupPoppedEvent;
import org.drools.event.AgendaGroupPushedEvent;
import org.drools.event.BeforeActivationFiredEvent;
import org.drools.event.RuleFlowGroupActivatedEvent;
import org.drools.event.RuleFlowGroupDeactivatedEvent;
import org.drools.event.process.ProcessEventListener;
import org.drools.event.rule.AgendaEventListener;
import org.drools.event.rule.WorkingMemoryEventListener;
import org.drools.impl.StatefulKnowledgeSessionImpl;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.process.ProcessInstance;
import org.drools.runtime.process.WorkflowProcessInstance;
import org.junit.Test;

import com.plugtree.cisco.listeners.LogAgendaEventListener;
import com.plugtree.cisco.listeners.LogProcessEventListener;
import com.plugtree.cisco.listeners.LogWMEventListener;
import com.plugtree.cisco.workitems.CounterWorkItemHandler;
import com.plugtree.cisco.workitems.LogWorkItemHandler;
import com.plugtree.cisco.workitems.MessageWorkItemHandler;

public class SessionConfigTest {

	@Test
	public void testSessionForProcessWithRules() {
		final StatefulKnowledgeSession ksession = JBPMUtil.initSimpleSession();
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
        ((StatefulKnowledgeSessionImpl) ksession).session.addEventListener(agendaEventListener);
        ksession.addEventListener(new LogAgendaEventListener());
        ksession.addEventListener(new LogWMEventListener());
        ksession.addEventListener(new LogProcessEventListener());

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
	public void testConfigurations() throws Exception {
		StatefulKnowledgeSession ksession = JBPMUtil.initSimpleSession();
		
		//registering work item handlers
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task", new LogWorkItemHandler(System.out));
		
		//registering listeners for process events
		ProcessEventListener processEventListener = new LogProcessEventListener(System.out);
		ksession.addEventListener(processEventListener);
		
		//registering listeners for rules' agenda events
		AgendaEventListener agendaEventListener = new LogAgendaEventListener(System.out);
		ksession.addEventListener(agendaEventListener);
		
		//registering listeners for working memory events
		WorkingMemoryEventListener wmEventListener = new LogWMEventListener(System.out);
		ksession.addEventListener(wmEventListener);
		
		Map<String, Object> initData = new HashMap<String, Object>();
		initData.put("dataone", "1");
		initData.put("datatwo", "2");
		ProcessInstance pI = ksession.startProcess("demo.human-task-process", initData);
		
		assertEquals(pI.getState(), ProcessInstance.STATE_COMPLETED);
	}
	
	/*
	populateOutputOfATask
	LocalTaskService
	*/
	
	@Test
	public void testTwoVersionsOfSameProcess() throws Exception {
		StatefulKnowledgeSession ksession = JBPMUtil.initSimpleSession();
		
		//registering work item handlers
		ksession.getWorkItemManager().registerWorkItemHandler("hello", new MessageWorkItemHandler("hello"));
		ksession.getWorkItemManager().registerWorkItemHandler("goodbye", new MessageWorkItemHandler("goodbye"));
		ksession.getWorkItemManager().registerWorkItemHandler("goodbyeV2", TestAsyncWorkItemHandler.getInstance());
		ksession.getWorkItemManager().registerWorkItemHandler("register", CounterWorkItemHandler.getInstance());
		
		Map<String, Object> initData = new HashMap<String, Object>();
		initData.put("name", "Mariano");
		ProcessInstance instance1 = ksession.startProcess("demo.my-user-defined-tasks-process-v1", initData);
		ProcessInstance instance2 = ksession.startProcess("demo.my-user-defined-tasks-process-v2", initData);
		
		//both process versions have different handlers after hello
		assertEquals(instance1.getState(), ProcessInstance.STATE_COMPLETED);
		assertEquals(instance2.getState(), ProcessInstance.STATE_ACTIVE);
		ksession.getWorkItemManager().completeWorkItem(TestAsyncWorkItemHandler.getInstance().getItem().getId(), null);
		assertEquals(instance2.getState(), ProcessInstance.STATE_COMPLETED);
		
		//"register" handler is only called from instance2
		assertEquals(1, CounterWorkItemHandler.getInstance().getCount());
		
	}
	
	@Test
	public void testSubProcess() throws Exception {
		StatefulKnowledgeSession ksession = JBPMUtil.initSimpleSession();
		
		Map<String, Object> initData = new HashMap<String, Object>();
		initData.put("data1", "my initial data 1");
		initData.put("data3", "my initial data 3");
		
		ProcessInstance instance = ksession.startProcess("demo.new-main-process", initData);
		
		assertEquals(instance.getState(), ProcessInstance.STATE_COMPLETED);
	}
}
