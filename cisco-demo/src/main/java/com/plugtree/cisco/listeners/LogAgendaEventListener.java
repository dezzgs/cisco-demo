package com.plugtree.cisco.listeners;

import java.io.PrintStream;

import org.drools.event.rule.ActivationCancelledEvent;
import org.drools.event.rule.ActivationCreatedEvent;
import org.drools.event.rule.AfterActivationFiredEvent;
import org.drools.event.rule.AgendaEventListener;
import org.drools.event.rule.AgendaGroupPoppedEvent;
import org.drools.event.rule.AgendaGroupPushedEvent;
import org.drools.event.rule.BeforeActivationFiredEvent;
import org.drools.event.rule.RuleFlowGroupActivatedEvent;
import org.drools.event.rule.RuleFlowGroupDeactivatedEvent;

public class LogAgendaEventListener implements AgendaEventListener {

	private final PrintStream out;

	public LogAgendaEventListener() {
		this(System.out);
	}
	
	public LogAgendaEventListener(PrintStream out) {
		this.out = out;
	}
	
	public void activationCreated(ActivationCreatedEvent event) {
		out.println("BEFORE ACTIVATION CREATED: " + event);
	}

	public void activationCancelled(ActivationCancelledEvent event) {
		out.println("BEFORE ACTIVATION CANCELLED: " + event);
	}

	public void beforeActivationFired(BeforeActivationFiredEvent event) {
		out.println("BEFORE ACTIVATION FIRED: " + event);
	}

	public void afterActivationFired(AfterActivationFiredEvent event) {
		out.println("AFTER ACTIVATION FIRED: " + event);
	}

	public void agendaGroupPopped(AgendaGroupPoppedEvent event) {
		out.println("AGENDA GROUP POPPED: " + event);
	}

	public void agendaGroupPushed(AgendaGroupPushedEvent event) {
		out.println("AGENDA GROUP PUSHED: " + event);
	}

	public void beforeRuleFlowGroupActivated(RuleFlowGroupActivatedEvent event) {
		out.println("BEFORE RULEFLOW GROUP ACTIVATED: " + event);
	}

	public void afterRuleFlowGroupActivated(RuleFlowGroupActivatedEvent event) {
		out.println("AFTER RULEFLOW GROUP ACTIVATED: " + event);
	}

	public void beforeRuleFlowGroupDeactivated(RuleFlowGroupDeactivatedEvent event) {
		out.println("BEFORE RULEFLOW GROUP DEACTIVATED: " + event);
	}

	public void afterRuleFlowGroupDeactivated(RuleFlowGroupDeactivatedEvent event) {
		out.println("AFTER RULEFLOW GROUP DEACTIVATED: " + event);
	}

}
