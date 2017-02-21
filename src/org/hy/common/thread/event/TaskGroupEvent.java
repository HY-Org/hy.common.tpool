package org.hy.common.thread.event;

import java.util.Iterator;

import org.hy.common.thread.Task;

import org.hy.common.BaseEvent;





/**
 * 任务组的事件
 * 
 * 此类是个只读类，即只有 getter 方法。
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2012-04-28
 */
public class TaskGroupEvent extends BaseEvent
{
	
	private static final long serialVersionUID = -5418164359550922450L;
	
	/** 任务组中所有的任务 */
	protected Iterator<Task> iterator;
	
	
	
	public TaskGroupEvent(Object i_Source) 
	{
		super(i_Source);
	}
	
	
	
	/**
	 * 任务组中所有的任务
	 * 
	 * @return
	 */
	public Iterator<Task> getTasks()
	{
		return this.iterator;
	}
	
}
