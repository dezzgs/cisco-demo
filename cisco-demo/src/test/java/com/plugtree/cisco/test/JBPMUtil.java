package com.plugtree.cisco.test;

import java.util.Properties;

import javax.naming.Context;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.SystemEventListenerFactory;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderError;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.io.impl.ClassPathResource;
import org.drools.persistence.jpa.JPAKnowledgeService;
import org.drools.runtime.Environment;
import org.drools.runtime.EnvironmentName;
import org.drools.runtime.KnowledgeSessionConfiguration;
import org.drools.runtime.StatefulKnowledgeSession;
import org.easymock.EasyMock;
import org.jbpm.task.service.TaskClient;
import org.jbpm.task.service.TaskServer;
import org.jbpm.task.service.jms.JMSTaskClientConnector;
import org.jbpm.task.service.jms.JMSTaskClientHandler;
import org.jbpm.task.service.jms.JMSTaskServer;
import org.jbpm.task.service.local.LocalTaskService;
import org.jbpm.task.service.mina.MinaTaskClientConnector;
import org.jbpm.task.service.mina.MinaTaskClientHandler;
import org.jbpm.task.service.mina.MinaTaskServer;

public class JBPMUtil {

	private static KnowledgeBase createKnowledgeBase() {
		//validation of contents
		KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
		kbuilder.add(new ClassPathResource("processes/demo.my-user-defined-tasks-process-v1.bpmn2"), ResourceType.BPMN2);
		kbuilder.add(new ClassPathResource("processes/demo.my-user-defined-tasks-process-v2.bpmn2"), ResourceType.BPMN2);
		kbuilder.add(new ClassPathResource("processes/demo.human-task-process.bpmn2"), ResourceType.BPMN2);
		kbuilder.add(new ClassPathResource("processes/demo.human-task-process-v2.bpmn2"), ResourceType.BPMN2);
		kbuilder.add(new ClassPathResource("processes/demo.new-sub-process.bpmn2"), ResourceType.BPMN2);
		kbuilder.add(new ClassPathResource("processes/demo.new-main-process.bpmn2"), ResourceType.BPMN2);
		kbuilder.add(new ClassPathResource("processes/demo.rule-process.bpmn2"), ResourceType.BPMN2);
		kbuilder.add(new ClassPathResource("processes/demo.timer-events-process.bpmn2"), ResourceType.BPMN2);
		kbuilder.add(new ClassPathResource("processes/demo.signal-events-process.bpmn2"), ResourceType.BPMN2);
		kbuilder.add(new ClassPathResource("processes/demo.signal-events-process-startup.bpmn2"), ResourceType.BPMN2);
		kbuilder.add(new ClassPathResource("rules/my-rules.drl"), ResourceType.DRL);
		
		if (kbuilder.hasErrors()) {
			for (KnowledgeBuilderError error : kbuilder.getErrors()) {
				System.err.println(error);
			}
			throw new RuntimeException("Coudln't parse bpmn files");
		}
		
		//knowledge base (definitions of knowledge) created with valid contents
		KnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase();
		kbase.addKnowledgePackages(kbuilder.getKnowledgePackages());
		return kbase;
	}

	public static StatefulKnowledgeSession initSimpleSession() {
		KnowledgeBase kbase = createKnowledgeBase();
		//knowledge session (runtime of knowledge) created from valid knowledge base
		return kbase.newStatefulKnowledgeSession();
	}
	
	public static StatefulKnowledgeSession initSimpleSession(KnowledgeSessionConfiguration ksessionConf) {
		KnowledgeBase kbase = createKnowledgeBase();
		//knowledge session (runtime of knowledge) created from valid knowledge base
		return kbase.newStatefulKnowledgeSession(ksessionConf, KnowledgeBaseFactory.newEnvironment());
	}
	
	public static StatefulKnowledgeSession initJPASession() {
		KnowledgeBase kbase = createKnowledgeBase();
		
		//creation of persistence context
		EntityManagerFactory emf = Persistence.createEntityManagerFactory("org.jbpm.persistence.jpa");
		Environment env = KnowledgeBaseFactory.newEnvironment();
		env.set(EnvironmentName.ENTITY_MANAGER_FACTORY, emf);
		
		//with drools-spring in the classpath, the initial go-to option for transaction managers is DroolsSpringTransactionManager
		
		//env.set(EnvironmentName.TRANSACTION, ut);
		//PersistenceContextManager pcm = new JpaProcessPersTTistenceContextManager(env);
		//env.set(EnvironmentName.PERSISTENCE_CONTEXT_MANAGER, pcm);
		
		//creation of session config
		Properties sessionProperties = new Properties();
		sessionProperties.put("drools.processInstanceManagerFactory", "org.jbpm.persistence.processinstance.JPAProcessInstanceManagerFactory");
		sessionProperties.put("drools.processSignalManagerFactory", "org.jbpm.persistence.processinstance.JPASignalManagerFactory");
		KnowledgeSessionConfiguration conf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration(sessionProperties);
		
		return JPAKnowledgeService.newStatefulKnowledgeSession(kbase, conf, env);
	}
	
	public static StatefulKnowledgeSession loadJPASession(int sessionId) {
		if (sessionId <= 0) {
			return null;
		}
	
		KnowledgeBase kbase = createKnowledgeBase();
		
		//creation of persistence context
		EntityManagerFactory emf = Persistence.createEntityManagerFactory("org.jbpm.persistence.jpa");
		Environment env = KnowledgeBaseFactory.newEnvironment();
		env.set(EnvironmentName.ENTITY_MANAGER_FACTORY, emf);
		//env.set(EnvironmentName.TRANSACTION_MANAGER, new JtaTransactionManagertm);
		//env.set(EnvironmentName.TRANSACTION, ut);
		//PersistenceContextManager pcm = new JpaProcessPersistenceContextManager(env);
		//env.set(EnvironmentName.PERSISTENCE_CONTEXT_MANAGER, pcm);
		
		//creation of session config
		Properties sessionProperties = new Properties();
		sessionProperties.put("drools.processInstanceManagerFactory", "org.jbpm.persistence.processinstance.JPAProcessInstanceMnaagerFactory");
		sessionProperties.put("drools.processSignalManagerFactory", "org.jbpm.persistence.processinstance.JPASignalManagerFactory");
		KnowledgeSessionConfiguration conf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration(sessionProperties);
		
		return JPAKnowledgeService.loadStatefulKnowledgeSession(sessionId, kbase, conf, env); 
	}

	private static org.jbpm.task.service.TaskService initInternalTaskService() {
		EntityManagerFactory emf = Persistence.createEntityManagerFactory("org.jbpm.task");
		org.jbpm.task.service.TaskService internalTaskService = 
			new org.jbpm.task.service.TaskService(emf, SystemEventListenerFactory.getSystemEventListener());
		return internalTaskService;
	}
	
	public static LocalTaskService initLocalTaskService() {
		return new LocalTaskService(initInternalTaskService());
	}
	
	public static TestRemoteTaskServerPack initMinaTaskService() throws Exception {
		TaskServer server = new MinaTaskServer(initInternalTaskService(), 9123, "127.0.0.1"); //default connection properties
		server.start();
		TaskClient client = new TaskClient(new MinaTaskClientConnector(JBPMUtil.class.getName(), 
				new MinaTaskClientHandler(SystemEventListenerFactory.getSystemEventListener())));
		return new TestRemoteTaskServerPack(server, client);
	}

	public static TestRemoteTaskServerPack initJMSTaskService() throws Exception {
		//start a JMS service through mocked JNDI
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false");
        Context context = EasyMock.createMock(Context.class);
        EasyMock.expect(context.lookup("ConnectionFactory")).andReturn(factory).anyTimes();
        EasyMock.replay(context);
        
        //establish connnection parameters for server
        Properties serverProperties = new Properties();
        serverProperties.setProperty("JMSTaskServer.connectionFactory", "ConnectionFactory");
        serverProperties.setProperty("JMSTaskServer.transacted", "true");
        serverProperties.setProperty("JMSTaskServer.acknowledgeMode", "AUTO_ACKNOWLEDGE");
        serverProperties.setProperty("JMSTaskServer.queueName", "tasksQueue");
        serverProperties.setProperty("JMSTaskServer.responseQueueName", "tasksResponseQueue");
		TaskServer server = new JMSTaskServer(initInternalTaskService(), serverProperties, context);
		server.start();

		//establish connection parameters for client
        Properties clientProperties = new Properties();
        clientProperties.setProperty("JMSTaskClient.connectionFactory", "ConnectionFactory");
        clientProperties.setProperty("JMSTaskClient.transactedQueue", "true");
        clientProperties.setProperty("JMSTaskClient.acknowledgeMode", "AUTO_ACKNOWLEDGE");
        clientProperties.setProperty("JMSTaskClient.queueName", "tasksQueue");
        clientProperties.setProperty("JMSTaskClient.responseQueueName", "tasksResponseQueue");
		
		TaskClient client = new TaskClient(new JMSTaskClientConnector(JBPMUtil.class.getName(), 
				new JMSTaskClientHandler(SystemEventListenerFactory.getSystemEventListener()), clientProperties, context));
		return new TestRemoteTaskServerPack(server, client);
	}
	
}
