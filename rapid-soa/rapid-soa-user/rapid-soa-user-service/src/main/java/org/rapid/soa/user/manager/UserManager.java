package org.rapid.soa.user.manager;

import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.codec.digest.DigestUtils;
import org.rapid.core.Assert;
import org.rapid.core.bean.model.code.Code;
import org.rapid.soa.core.bean.entity.UserDevice;
import org.rapid.soa.core.bean.entity.UserInfo;
import org.rapid.soa.user.bean.entity.UserInvitation;
import org.rapid.soa.user.bean.entity.UserRole;
import org.rapid.soa.user.bean.entity.Username;
import org.rapid.soa.user.bean.enums.UserCode;
import org.rapid.soa.user.bean.enums.UsernameType;
import org.rapid.soa.user.bean.info.LoginInfo;
import org.rapid.soa.user.bean.model.query.DeviceQuery;
import org.rapid.soa.user.bean.model.query.RoleQuery;
import org.rapid.soa.user.bean.model.query.UsernameQuery;
import org.rapid.soa.user.bean.request.AuthRequest;
import org.rapid.soa.user.bean.request.RegisterRequest;
import org.rapid.soa.user.bean.request.UnauthRequest;
import org.rapid.soa.user.dao.UserDeviceDao;
import org.rapid.soa.user.dao.UserInfoDao;
import org.rapid.soa.user.dao.UserInvitationDao;
import org.rapid.soa.user.dao.UserRoleDao;
import org.rapid.soa.user.dao.UsernameDao;
import org.rapid.soa.user.internal.EntityGenerator;
import org.rapid.util.Consts;
import org.rapid.util.bean.Pair;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UserManager {

	@Resource
	private UserRoleDao userRoleDao;
	@Resource
	private UserInfoDao userInfoDao;
	@Resource
	private UsernameDao usernameDao;
	@Resource
	private UserDeviceDao userDeviceDao;
	@Resource
	private UserInvitationDao userInvitationDao;

	@Transactional
	public Pair<Long, UserInvitation> register(RegisterRequest request) {
		UserInfo user = EntityGenerator.newUserInfo(request.getPassword());
		userInfoDao.insert(user);
		Username username = EntityGenerator.newUsername(user, request.getUsername(), request.getUsernameType());
		usernameDao.insert(username);
		Pair<Long, UserInvitation> pair = new Pair<Long, UserInvitation>();
		if (null != request.getInviter()) {
			UserInfo invitor = userInfoDao.getByKey(request.getInviter());
			Assert.notNull(UserCode.INVITOR_NOT_EXIST, invitor);
			UserInvitation invitation = EntityGenerator.newUserInvitation(invitor, user);
			userInvitationDao.insert(invitation);
			pair.setValue(invitation);
		}
		pair.setKey(user.getId());
		return pair;
	}
	
	@Transactional
	public Pair<LoginInfo, UserDevice> login(UserDevice device, String pwd) {
		UserInfo user = userInfoDao.getByKey(device.getUid());
		String cpwd = DigestUtils.md5Hex(pwd + Consts.Symbol.UNDERLINE + user.getSalt());
		Assert.isTrue(UserCode.LOGIN_PWD_ERROR, cpwd.equalsIgnoreCase(pwd));
		DeviceQuery query = new DeviceQuery();
		query.uid(user.getId()).type(device.getType());
		UserDevice odevice = userDeviceDao.queryUnique(query);
		Pair<LoginInfo, UserDevice> pair = new Pair<LoginInfo, UserDevice>();
		if (null != odevice) {							// 已经有同类型的设备登录了
			userDeviceDao.deleteByKey(odevice.getId());
			if (!odevice.getId().equals(device.getId())) 
				pair.setValue(odevice);
		}
		userDeviceDao.insert(device);
		pair.setKey(new LoginInfo(user.getId(), device.getToken()));
		return pair;
	}
	
	@Transactional
	public long auth(AuthRequest request, boolean root) {
		UserInfo user = user(request.getUid());
		Assert.notNull(UserCode.USER_NOT_EIXST, user);
		UserRole parent = null;
		if (!root) {
			RoleQuery query = new RoleQuery();
			query.uid(request.getUser().getId()).roleId(request.getRoleId());
			parent = userRoleDao.queryUnique(query);
			Assert.notNull(UserCode.USER_ROLE_NOT_EIXST, parent);
		}
		if (null != parent)
			userRoleDao.insertUpdate(parent.getTrunk(), parent.getRight());
		UserRole role = EntityGenerator.newUserRole(request, parent);
		userRoleDao.insert(role);
		return role.getId();
	}
	
	@Transactional
	public void unauth(UnauthRequest request, boolean root) { 
		UserRole role = userRoleDao.getByKey(request.getId());
		Assert.notNull(UserCode.USER_ROLE_NOT_EIXST, role);
		if (!root) {
			int parent = userRoleDao.parent(role.getTrunk(), role.getLeft(), role.getRight(), request.getUser().getId());
			Assert.isTrue(Code.FORBID, parent == 1);
		}
		userRoleDao.unauth(role);
		userRoleDao.deleteUpdate(role, role.getRight() - role.getLeft() + 1);
	}
	
	public UserInfo user(long uid) {
		return userInfoDao.getByKey(uid);
	}
	
	public UserDevice device(String id) {
		return userDeviceDao.getByKey(id);
	}
	
	public List<UserRole> roles(long uid) {
		return userRoleDao.queryList(new RoleQuery().uid(uid));
	}
	
	public Username username(UsernameType type, String username) {
		UsernameQuery query = new UsernameQuery();
		query.username(username).type(type);
		return usernameDao.queryUnique(query);
	}
}
