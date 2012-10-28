package com.plugtree.cisco.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.drools.agent.KnowledgeAgent;
import org.drools.agent.KnowledgeAgentConfiguration;
import org.drools.agent.KnowledgeAgentFactory;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.common.DroolsObjectOutputStream;
import org.drools.definition.KnowledgePackage;
import org.drools.event.knowledgeagent.KnowledgeBaseUpdatedEvent;
import org.drools.event.rule.DefaultKnowledgeAgentEventListener;
import org.drools.io.impl.ClassPathResource;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.process.ProcessInstance;
import org.drools.runtime.process.WorkflowProcessInstance;
import org.junit.Test;

import com.plugtree.cisco.workitems.CounterWorkItemHandler;
import com.plugtree.cisco.workitems.MessageWorkItemHandler;

public class KnowledgeAgentTest {

	private static Integer currentVersionNumber;
	
	@Test
	public void testKnowledgeAgent() throws Exception {
		Properties kagentProps = new Properties();
		//if the changeset defined a directory to scan, set this property to true
		kagentProps.setProperty("drools.agent.scanDirectories", "false");
		//wether to montiro changes in the knowledge base files defined in changeSet.xml
		kagentProps.setProperty("drools.agent.scanResources", "true");
		//wether to send events or not when you monitor for changes in the knowledge base files defined in changeSet.xml
		kagentProps.setProperty("drools.agent.monitorChangeSetEvents", "true");

		KnowledgeAgentConfiguration kagentConf = KnowledgeAgentFactory.newKnowledgeAgentConfiguration(kagentProps);
		KnowledgeAgent kagent = KnowledgeAgentFactory.newKnowledgeAgent("my-knowledge-agent", kagentConf);
		kagent.addEventListener(new DefaultKnowledgeAgentEventListener() {
			//this is fired even at the begining (at the first applyChangeSet(..) call 
			@Override
			public void knowledgeBaseUpdated(KnowledgeBaseUpdatedEvent event) {
				for (KnowledgePackage pkg : event.getKnowledgeBase().getKnowledgePackages()) {
					for (org.drools.definition.process.Process proc : pkg.getProcesses()) {
						if (proc.getId().startsWith("demo.my-user-defined-tasks-process")) {
							String versionNumberString = proc.getId().replace("demo.my-user-defined-tasks-process-v", "");
							Integer newVersionNumber = Integer.valueOf(versionNumberString);
							if (currentVersionNumber == null || newVersionNumber.intValue() > currentVersionNumber.intValue()) {
								currentVersionNumber = newVersionNumber;
							}
						}
					}
				}
			}
		});
		
		//it is going to monitor continuously if there are any changes in the files defined in the changeset
		//a guvnor dependent demo is not recommended due to the complexities involved in starting the demo elsewhere
		//be sure to change the path in the changeSet.xml file from 
		//<resource source="file:/opt/git/git_cisco/master/cisco-demo/src/test/resources/processes/binary.pkg" type="PKG"  />
		//to the one available in your machine.
		kagent.applyChangeSet(new ClassPathResource("changeSet.xml"));

		CounterWorkItemHandler.getInstance().restart();
		
		StatefulKnowledgeSession ksession = kagent.getKnowledgeBase().newStatefulKnowledgeSession();
		ksession.getWorkItemManager().registerWorkItemHandler("hello", new MessageWorkItemHandler("hello"));
		ksession.getWorkItemManager().registerWorkItemHandler("goodbyeV2", TestAsyncWorkItemHandler.getInstance());
		ksession.getWorkItemManager().registerWorkItemHandler("register", CounterWorkItemHandler.getInstance());

		
		assertNotNull(currentVersionNumber);
		assertTrue(currentVersionNumber == 2);
		String processId = "demo.my-user-defined-tasks-process-v" + currentVersionNumber;
		Map<String, Object> initData = new HashMap<String, Object>();
		initData.put("name", "Mariano");
		WorkflowProcessInstance processInstance = (WorkflowProcessInstance) ksession.startProcess(processId);
		
		assertEquals(processInstance.getState(), ProcessInstance.STATE_ACTIVE);
		ksession.getWorkItemManager().completeWorkItem(TestAsyncWorkItemHandler.getInstance().getItem().getId(), null);
		assertEquals(processInstance.getState(), ProcessInstance.STATE_COMPLETED);
		assertEquals(1, CounterWorkItemHandler.getInstance().getCount());
	}
	
	//helper method to create a binary.pkg file
	public static void main(String[] args) {
		org.drools.rule.Package pkg = new org.drools.rule.Package("binary");
		KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
		kbuilder.add(new ClassPathResource("processes/demo.my-user-defined-tasks-process-v1.bpmn2"), ResourceType.BPMN2);
		kbuilder.add(new ClassPathResource("processes/demo.my-user-defined-tasks-process-v2.bpmn2"), ResourceType.BPMN2);
		for (KnowledgePackage kpkg : kbuilder.getKnowledgePackages()) {
			for (org.drools.definition.process.Process proc : kpkg.getProcesses()) {
				pkg.addProcess(proc);
			}
		}
		
		try {
			File file = new File("/opt/git/git_cisco/master/cisco-demo/src/test/resources/processes/binary.pkg");
			if (!file.exists()) {
				file.createNewFile();
			}
			DroolsObjectOutputStream doos = new DroolsObjectOutputStream(new FileOutputStream(file));
			doos.writeObject(pkg);
			doos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
