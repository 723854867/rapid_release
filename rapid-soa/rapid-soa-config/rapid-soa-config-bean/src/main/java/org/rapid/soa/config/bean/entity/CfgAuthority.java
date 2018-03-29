package org.rapid.soa.config.bean.entity;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.rapid.core.bean.model.Identifiable;

/**
 * <pre>
 * 权限关系表
 * tid、type、authId 三者做唯一索引
 * </pre>
 * 
 * @author lynn
 */
public class CfgAuthority implements Identifiable<Integer> {

	private static final long serialVersionUID = -1449282195019172095L;

	@Id
	@GeneratedValue
	private int id;
	private int tid;
	private int type;
	private int authId;
	private int created;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getTid() {
		return tid;
	}

	public void setTid(int tid) {
		this.tid = tid;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getAuthId() {
		return authId;
	}

	public void setAuthId(int authId) {
		this.authId = authId;
	}

	public int getCreated() {
		return created;
	}

	public void setCreated(int created) {
		this.created = created;
	}

	@Override
	public Integer identity() {
		return this.id;
	}
}
