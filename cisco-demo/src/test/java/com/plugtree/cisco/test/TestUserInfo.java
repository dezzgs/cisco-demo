package com.plugtree.cisco.test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jbpm.task.Group;
import org.jbpm.task.OrganizationalEntity;
import org.jbpm.task.User;
import org.jbpm.task.UserInfo;

public 	class TestUserInfo implements UserInfo {

	@Override
	public String getDisplayName(OrganizationalEntity entity) {
		return entity.getId();
	}
	
	@Override
	public String getEmailForEntity(OrganizationalEntity entity) {
		return "mariano.demaio@plugtree.com";
	}
	
	@Override
	public String getLanguageForEntity(OrganizationalEntity entity) {
		return "en-UK";
	}
	
	@Override
	public Iterator<OrganizationalEntity> getMembersForGroup(Group group) {
		List<OrganizationalEntity> list = new ArrayList<OrganizationalEntity>();
		if (group != null && group.getId().equals("users")) {
			list.add(new User("mariano"));
			list.add(new User("salaboy"));
			list.add(new User("Administrator"));
		}
		return list.iterator();
	}
	@Override
	public boolean hasEmail(Group group) {
		return false;
	}
}
