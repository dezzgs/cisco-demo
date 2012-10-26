package com.plugtree.cisco.spring;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.drools.runtime.Environment;
import org.drools.runtime.EnvironmentName;
import org.jbpm.process.audit.NodeInstanceLog;
import org.jbpm.process.audit.ProcessInstanceLog;
import org.jbpm.process.audit.VariableInstanceLog;

/**
 * This class is essentially a very simple implementation of a service
 * that deals with ProcessInstanceLog entities.
 * 
 * It works with transaction management from environment variable
 * 
 */
public class SpringJPAProcessInstanceDbLog {

	private static Environment env;

    @SuppressWarnings("unchecked")
    public static List<ProcessInstanceLog> findProcessInstances() {
        EntityManager em = getEntityManager();
        boolean newTx = joinTransaction(em);
        List<ProcessInstanceLog> result = em.createQuery("FROM ProcessInstanceLog").getResultList();
        closeEntityManager(em, newTx);
        return result;
    }

    @SuppressWarnings("unchecked")
    public static List<ProcessInstanceLog> findProcessInstances(String processId) {
        EntityManager em = getEntityManager();
        boolean newTx = joinTransaction(em);
        List<ProcessInstanceLog> result = em
            .createQuery("FROM ProcessInstanceLog p WHERE p.processId = :processId")
                .setParameter("processId", processId).getResultList();
        closeEntityManager(em, newTx);
        return result;
    }

    @SuppressWarnings("unchecked")
    public static List<ProcessInstanceLog> findActiveProcessInstances(String processId) {
        EntityManager em = getEntityManager();
        boolean newTx = joinTransaction(em);
        List<ProcessInstanceLog> result = getEntityManager()
            .createQuery("FROM ProcessInstanceLog p WHERE p.processId = :processId AND p.end is null")
                .setParameter("processId", processId).getResultList();
        closeEntityManager(em, newTx);
        return result;
    }

    public static ProcessInstanceLog findProcessInstance(long processInstanceId) {
        EntityManager em = getEntityManager();
        boolean newTx = joinTransaction(em);
        ProcessInstanceLog result = (ProcessInstanceLog) getEntityManager()
            .createQuery("FROM ProcessInstanceLog p WHERE p.processInstanceId = :processInstanceId")
                .setParameter("processInstanceId", processInstanceId).getSingleResult();
        closeEntityManager(em, newTx);
        return result;
    }
    
    @SuppressWarnings("unchecked")
    public static List<NodeInstanceLog> findNodeInstances(long processInstanceId) {
        EntityManager em = getEntityManager();
        boolean newTx = joinTransaction(em);
        List<NodeInstanceLog> result = getEntityManager()
            .createQuery("FROM NodeInstanceLog n WHERE n.processInstanceId = :processInstanceId ORDER BY date")
                .setParameter("processInstanceId", processInstanceId).getResultList();
        closeEntityManager(em, newTx);
        return result;
    }

    @SuppressWarnings("unchecked")
    public static List<NodeInstanceLog> findNodeInstances(long processInstanceId, String nodeId) {
        EntityManager em = getEntityManager();
        boolean newTx = joinTransaction(em);
        List<NodeInstanceLog> result = getEntityManager()
            .createQuery("FROM NodeInstanceLog n WHERE n.processInstanceId = :processInstanceId AND n.nodeId = :nodeId ORDER BY date")
                .setParameter("processInstanceId", processInstanceId)
                .setParameter("nodeId", nodeId).getResultList();
        closeEntityManager(em, newTx);
        return result;
    }

    @SuppressWarnings("unchecked")
    public static List<VariableInstanceLog> findVariableInstances(long processInstanceId) {
        EntityManager em = getEntityManager();
        boolean newTx = joinTransaction(em);
        List<VariableInstanceLog> result = getEntityManager()
            .createQuery("FROM VariableInstanceLog v WHERE v.processInstanceId = :processInstanceId ORDER BY date")
                .setParameter("processInstanceId", processInstanceId).getResultList();
        closeEntityManager(em, newTx);
        return result;
    }

    @SuppressWarnings("unchecked")
    public static List<VariableInstanceLog> findVariableInstances(long processInstanceId, String variableId) {
        EntityManager em = getEntityManager();
        boolean newTx = joinTransaction(em);
        List<VariableInstanceLog> result = em
            .createQuery("FROM VariableInstanceLog v WHERE v.processInstanceId = :processInstanceId AND v.variableId = :variableId ORDER BY date")
                .setParameter("processInstanceId", processInstanceId)
                .setParameter("variableId", variableId).getResultList();
        closeEntityManager(em, newTx);
        return result;
    }

    @SuppressWarnings("unchecked")
    public static void clear() {
            EntityManager em = getEntityManager();
            boolean newTx = joinTransaction(em);
            
            List<ProcessInstanceLog> processInstances = em.createQuery("FROM ProcessInstanceLog").getResultList();
            for (ProcessInstanceLog processInstance: processInstances) {
                em.remove(processInstance);
            }
            List<NodeInstanceLog> nodeInstances = em.createQuery("FROM NodeInstanceLog").getResultList();
            for (NodeInstanceLog nodeInstance: nodeInstances) {
                em.remove(nodeInstance);
            }
            List<VariableInstanceLog> variableInstances = em.createQuery("FROM VariableInstanceLog").getResultList();
            for (VariableInstanceLog variableInstance: variableInstances) {
                em.remove(variableInstance);
            }           
            closeEntityManager(em, newTx);
    }
    private static void closeEntityManager(EntityManager em, boolean newTx) {
        em.flush(); // This saves any changes made
        em.clear(); // This makes sure that any returned entities are no longer attached to this entity manager/persistence context
        if (newTx) {
        	em.getTransaction().commit();
        }
    }

    private static boolean joinTransaction(EntityManager em) {
        EntityTransaction t = em.getTransaction();
        boolean newTx = false;
        if (!t.isActive()) {
        	t.begin();
        	newTx = true;
        }
        return newTx;
    }
    
    private static EntityManager getEntityManager() {
		return (EntityManager) env.get(EnvironmentName.CMD_SCOPED_ENTITY_MANAGER); 
    }

	public static void setEnvironment(Environment newEnv) {
		env = newEnv;
	}

}
