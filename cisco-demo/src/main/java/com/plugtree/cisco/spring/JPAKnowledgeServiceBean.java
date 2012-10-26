package com.plugtree.cisco.spring;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManagerFactory;

import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.base.MapGlobalResolver;
import org.drools.compiler.ProcessBuilderFactory;
import org.drools.event.process.ProcessEventListener;
import org.drools.event.rule.AgendaEventListener;
import org.drools.event.rule.WorkingMemoryEventListener;
import org.drools.marshalling.impl.ProcessMarshallerFactory;
import org.drools.persistence.jpa.KnowledgeStoreService;
import org.drools.runtime.Environment;
import org.drools.runtime.EnvironmentName;
import org.drools.runtime.KnowledgeSessionConfiguration;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.process.ProcessRuntimeFactory;
import org.drools.runtime.process.WorkItemHandler;
import org.jbpm.marshalling.impl.ProcessMarshallerFactoryServiceImpl;
import org.jbpm.process.audit.JPAWorkingMemoryDbLogger;
import org.jbpm.process.builder.ProcessBuilderFactoryServiceImpl;
import org.jbpm.process.instance.ProcessRuntimeFactoryServiceImpl;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;

public class JPAKnowledgeServiceBean implements InitializingBean {

	static {
		 ProcessBuilderFactory.setProcessBuilderFactoryService(new ProcessBuilderFactoryServiceImpl());
         ProcessMarshallerFactory.setProcessMarshallerFactoryService(new ProcessMarshallerFactoryServiceImpl());
         ProcessRuntimeFactory.setProcessRuntimeFactoryService(new ProcessRuntimeFactoryServiceImpl());
	}
	
	private KnowledgeBase kbase;
	private KnowledgeStoreService knowledgeStore;
	private Environment environment;
	private EntityManagerFactory entityManagerFactory;
	private AbstractPlatformTransactionManager transactionManager;
	private List<ProcessEventListener> processEventListeners = Collections.emptyList();
	private List<AgendaEventListener> agendaEventListeners = Collections.emptyList();
	private List<WorkingMemoryEventListener> workingMemoryEventListeners = Collections.emptyList();
	private Map<String, WorkItemHandler> workItemHandlers = Collections.emptyMap();
	private Map<String, String> sessionConfiguration = Collections.emptyMap();
	
	public StatefulKnowledgeSession newSession() {

		Environment env = createEnvironment();
		StatefulKnowledgeSession ksession = knowledgeStore.newStatefulKnowledgeSession(this.kbase, createSessionConf(), env);
		registerAgendaEventListeners(ksession);
		registerProcessEventListeners(ksession);
		registerWorkingMemoryEventListeners(ksession);
		registerWorkItemHandlers(ksession);
		new JPAWorkingMemoryDbLogger(ksession);
		SpringJPAProcessInstanceDbLog.setEnvironment(env);
		return ksession;
	}

	public StatefulKnowledgeSession loadSession(int sessionId) {
		StatefulKnowledgeSession ksession = knowledgeStore.loadStatefulKnowledgeSession(sessionId, this.kbase, createSessionConf(), createEnvironment());
		registerAgendaEventListeners(ksession);
		registerProcessEventListeners(ksession);
		registerWorkingMemoryEventListeners(ksession);
		registerWorkItemHandlers(ksession);
		return ksession;
	}

	
	public Environment createEnvironment() {
		Environment env = KnowledgeBaseFactory.newEnvironment();
		env.set(EnvironmentName.ENTITY_MANAGER_FACTORY, this.entityManagerFactory);
		env.set(EnvironmentName.TRANSACTION_MANAGER, this.transactionManager);
		env.set(EnvironmentName.GLOBALS, new MapGlobalResolver());
		return env;
	}
	
	private void registerWorkItemHandlers(StatefulKnowledgeSession ksession) {
		if (workItemHandlers != null && !workItemHandlers.isEmpty()) {
			for (Map.Entry<String, WorkItemHandler> entry : workItemHandlers.entrySet()) {
				ksession.getWorkItemManager().registerWorkItemHandler(entry.getKey(), entry.getValue());
			}
		}
	}

	private void registerAgendaEventListeners(StatefulKnowledgeSession ksession) {
		if (agendaEventListeners != null && !agendaEventListeners.isEmpty()) {
			for (AgendaEventListener listener : agendaEventListeners) {
				ksession.addEventListener(listener);
			}
		}
	}

	private void registerProcessEventListeners(StatefulKnowledgeSession ksession) {
		if (processEventListeners != null && !processEventListeners.isEmpty()) {
			for (ProcessEventListener listener : processEventListeners) {
				ksession.addEventListener(listener);
			}
		}
		//ksession.addEventListener(new JPAWorkingMemoryDbLogger(ksession));
	}	
	
	private void registerWorkingMemoryEventListeners(StatefulKnowledgeSession ksession) {
		if (workingMemoryEventListeners != null && !workingMemoryEventListeners.isEmpty()) {
			for (WorkingMemoryEventListener listener : workingMemoryEventListeners) {
				ksession.addEventListener(listener);
			}
		}
	}

	private KnowledgeSessionConfiguration createSessionConf() {
		if (sessionConfiguration != null && !sessionConfiguration.isEmpty()) {
			KnowledgeSessionConfiguration kconf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
			for (Map.Entry<String, String> entry : sessionConfiguration.entrySet()) {
				kconf.setProperty(entry.getKey(), entry.getValue());
			}
			return kconf;
		}
		return null;
	}

	public void afterPropertiesSet() throws Exception {
		if (kbase == null) {
			throw new InvalidPropertyException(this.getClass(), "kbase", "Cannot be null");
		}
		if (transactionManager == null) {
			throw new InvalidPropertyException(this.getClass(), "transactionManager", "Cannot be null");
		}
		if (entityManagerFactory == null) {
			throw new InvalidPropertyException(this.getClass(), "entityManagerFactory", "Cannot be null");
		}
		if (knowledgeStore == null) {
			throw new InvalidPropertyException(this.getClass(), "knowledgeStore", "Cannot be null");
		}
	}

	//public accessors
	
	public KnowledgeBase getKbase() {
		return kbase;
	}

	public void setKbase(KnowledgeBase kbase) {
		this.kbase = kbase;
	}

	public KnowledgeStoreService getKnowledgeStore() {
		return knowledgeStore;
	}

	public void setKnowledgeStore(KnowledgeStoreService knowledgeStore) {
		this.knowledgeStore = knowledgeStore;
	}

	public Environment getEnvironment() {
		return environment;
	}

	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	public EntityManagerFactory getEntityManagerFactory() {
		return entityManagerFactory;
	}

	public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}

	public AbstractPlatformTransactionManager getTransactionManager() {
		return transactionManager;
	}

	public void setTransactionManager(AbstractPlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public List<ProcessEventListener> getProcessEventListeners() {
		return processEventListeners;
	}

	public void setProcessEventListeners(List<ProcessEventListener> processEventListeners) {
		this.processEventListeners = processEventListeners;
	}

	public List<AgendaEventListener> getAgendaEventListeners() {
		return agendaEventListeners;
	}

	public void setAgendaEventListeners(List<AgendaEventListener> agendaEventListeners) {
		this.agendaEventListeners = agendaEventListeners;
	}

	public List<WorkingMemoryEventListener> getWorkingMemoryEventListeners() {
		return workingMemoryEventListeners;
	}

	public void setWorkingMemoryEventListeners(List<WorkingMemoryEventListener> workingMemoryEventListeners) {
		this.workingMemoryEventListeners = workingMemoryEventListeners;
	}

	public Map<String, WorkItemHandler> getWorkItemHandlers() {
		return workItemHandlers;
	}

	public void setWorkItemHandlers(Map<String, WorkItemHandler> workItemHandlers) {
		this.workItemHandlers = workItemHandlers;
	}

	public Map<String, String> getSessionConfiguration() {
		return sessionConfiguration;
	}

	public void setSessionConfiguration(Map<String, String> sessionConfiguration) {
		this.sessionConfiguration = sessionConfiguration;
	}
}
