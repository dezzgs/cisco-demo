package com.plugtree.cisco.test;

import java.util.ArrayList;
import java.util.List;

import org.jbpm.task.service.UserGroupCallback;

public class TestUserGroupCallback implements UserGroupCallback {

	private boolean amongValidUsers(String userId) {
		return userId.equals("mariano") || userId.equals("salaboy") || userId.equals("Administrator");
	}
	
	@Override
	public List<String> getGroupsForUser(String userId, List<String> groupIds,
			List<String> allExistingGroupIds) {
		List<String> list = new ArrayList<String>();
		if (userId != null && amongValidUsers(userId)) {
			list.add("users");
			if (userId.equals("Administrator")) {
				list.add("bosses");
			}
		}
		return list;
	}
	
	@Override
	public boolean existsUser(String userId) {
		return userId != null && amongValidUsers(userId);
	}
	
	@Override
	public boolean existsGroup(String groupId) {
		return groupId != null && ("users".equals(groupId) || "bosses".equals(groupId));
	}
}
