package org.hy.common.thread;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.hy.common.Date;
import org.hy.common.Help;
import org.hy.common.thread.event.TaskGroupEvent;
import org.hy.common.thread.event.TaskGroupListener;

import org.hy.common.thread.event.DefaultTaskGroupEvent;





/**
 * 任务组
 * 
 * <O>指线程自有的缓存的对象类型
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2012-04-28
 *           V2.0  2017-02-21  添加：停止尚未绑定线程开始执行的任务。对于已绑定线程执行的任务不生效。
 */
public class TaskGroup 
{
	/** 最后一个创建的任务组 */
	private static TaskGroup                     $LastTaskGroup = null;
	
	
	
	/** 任务组名称 */
	private String                               taskGroupName;
	
	/** 任务组中的任务列表 */
	private List<Task<?>>                        taskList;
	
	/** 组中完成任务的数量 */
	private int                                  finishSize;
	
	/** 自定义事件的监听器集合 */
	private Collection<TaskGroupListener>        taskGroupListeners;
	
	/** 事件对象 */
	private DefaultTaskGroupEvent                taskGroupEvent;
	
	/** 
	 * 任务组和任务组中所有任务是否都完成了
	 * 即，注册监听者都执行完 finishAllTask(...) 方法了，返回true。
	 **/
	private boolean                              taskGroupIsFinish;
	
	/** 
	 * 任务组中所有任务是否都完成了
	 * 即，注册监听者都执行 finishAllTask(...) 方法之前，就返回true了。
	 **/
	private boolean                              tasksIsFinish;
	
	/** 上一个任务组对象 */
	private TaskGroup                            upperTaskGroup;
	
	
	
	/**
	 * 设置上一个任务组对象
	 * 
	 * @param i_TaskGroup
	 */
	private synchronized static void setUpperTaskGroup(TaskGroup i_TaskGroup)
	{
		i_TaskGroup.upperTaskGroup = $LastTaskGroup;
		$LastTaskGroup             = i_TaskGroup;
	}
	
	
	
	/**
	 * 构造器
	 * 
	 * @param i_TaskGroupName  任务组名称
	 */
	public TaskGroup(String i_TaskGroupName)
	{
		if ( Help.isNull(i_TaskGroupName) )
		{
			throw new NullPointerException("Task group name is null.");
		}
		
		this.taskGroupName     = i_TaskGroupName;
		this.taskList          = new ArrayList<Task<?>>();
		this.finishSize        = 0;
		this.taskGroupIsFinish = false;
		this.tasksIsFinish     = false;
		
		setUpperTaskGroup(this);
	}
	
	
	
	/**
	 * 启动所有任务。
	 * 
	 * 说是启动不如说它是"装载"，将每个任务 put 到任务池中，排队顺次执行。
	 */
	public void startupAllTask()
	{
		int v_Size = this.taskList.size();
		
		if ( v_Size <= 0 )
		{
			return;
		}
		
		
		this.taskGroupIsFinish = false;
		this.tasksIsFinish     = false;
		this.finishSize        = 0;
		this.taskGroupEvent = new DefaultTaskGroupEvent(this);
		this.taskGroupEvent.setSize(v_Size);
		this.taskGroupEvent.setTasks(this.taskList.iterator());
		
		this.fireStartupAllTaskListener(this.taskGroupEvent);
		
		
		for (int v_Index=0; v_Index<v_Size; v_Index++)
		{
			TaskPool.putTask(this.taskList.get(v_Index));
		}
	}
	
	
	
	/**
	 * 停止尚未绑定线程开始执行的任务。对于已绑定线程执行的任务不生效。
	 * 
	 * @author      ZhengWei(HY)
	 * @createDate  2017-02-21
	 * @version     v1.0
	 *
	 */
	public void stopTasksNoExecute()
	{
	    if ( Help.isNull(this.taskList) )
	    {
	        return;
	    }
	    
	    for (Task<?> v_Task : this.taskList)
	    {
	        v_Task.stopTasksNoExecute();
	        this.taskFinish(v_Task);
	    }
	}
	
	
	
	/**
	 * 报告每个任务完成。
	 * 
	 * 当每个任务执行完成时，即在 Task.finishTask() 方法中，会自动调用此方法。
	 * 
	 * @param i_Task
	 */
	public synchronized void taskFinish(Task<?> i_Task)
	{
		// 任务组已标记完成，就不在接收每个任务的报告了。
		if ( this.isTasksFinish() )
		{
			return;
		}
		
		
		int v_Size = this.taskList.size();
		
		this.plusFinishSize();
		// System.out.println(i_Task.getTaskName() + "  " + i_Task.getThreadNo() + "  " + this.finishSize);
		
		
		if ( this.finishSize > 0 && this.finishSize >= v_Size )
		{
		    this.tasksIsFinish = true;
		    
		    if ( this.taskGroupEvent != null )
		    {
    			this.taskGroupEvent.setCompleteSize(this.finishSize);
    			this.taskGroupEvent.setTasks(this.taskList.iterator());
    			this.taskGroupEvent.setEndTime(new Date());
    			
    			try
    			{
    				this.fireFinishAllTaskListener(this.taskGroupEvent);
    			}
    			catch (Exception exce)
    			{
    				exce.printStackTrace();
    			}
		    }
			
			this.taskGroupIsFinish = true;
			this.finishSize        = 0;
		}
	}
	
	
	
	/**
	 * 完成任务数++（加同步功能的）。
	 * 
	 * 因为加了synchronized关键字，但调用此方法的 taskFinish() 没有同步功能。
	 * 所以，当有多个任务同时完成时，即同时调用 taskFinish() 方法时，为并发的，
	 * 但，当遇到 plusFinishSize() 是就会出现堵塞，并发变成串行。
	 * 
	 * 这样就能保证并发任务时，只触发一次"任务组"整体完成事件。
	 */
	private synchronized void plusFinishSize()
	{
		this.finishSize++;
	}
	
	
	
	/**
	 * 添加任务
	 * 
	 * @param i_Task
	 */
	public void addTask(Task<?> i_Task)
	{
		if ( i_Task == null )
		{
			return;
		}
		
		i_Task.setTaskGroup(this);
		this.taskList.add(i_Task);
	}
	
	
	
	/**
	 * 添加任务，并执行任务
	 * 
	 * @author      ZhengWei(HY)
	 * @createDate  2017-02-22
	 * @version     v1.0
	 *
	 * @param i_Task
	 */
	public void addTaskAndStart(Task<?> i_Task)
    {
	    this.addTask(i_Task);
        TaskPool.putTask(i_Task);
    }
	
	
	
	/**
	 * 获取任务
	 * 
	 * @param i_Index  小标从零开始
	 * @return
	 */
	public Task<?> getTask(int i_Index)
	{
		return this.taskList.get(i_Index);
	}
	
	
	
	/**
	 * 获取任务组中任务数量
	 * 
	 * @return
	 */
	public int size()
	{
		return this.taskList.size();
	}
	
	
	
	/**
	 * 任务组和任务组中所有任务是否都完成了
	 * 
	 * 即，注册监听者都执行完 finishAllTask(...) 方法了，返回true。
	 * 
	 * @return
	 */
	public boolean isTaskGroupFinish()
	{
		return this.taskGroupIsFinish;
	}
	
	
	
	/** 
	 * 任务组中所有任务是否都完成了
	 * 即，注册监听者都执行 finishAllTask(...) 方法之前，就返回true了。
	 **/
	public synchronized boolean isTasksFinish()
	{
		return this.tasksIsFinish;
	}
	
	
	
	/**
	 * 获取上一个任务组对象
	 * @return
	 */
	public TaskGroup getUpperTaskGroup()
	{
		return this.upperTaskGroup;
	}
	
	
	
	/**
	 * 清空任务组中所有的任务
	 */
	public synchronized void clear()
	{
		for (int v_Index=this.taskList.size()-1; v_Index>=0; v_Index--)
		{
			Task<?> v_Task = null;
			
			try
			{
				v_Task= this.taskList.remove(v_Index);
				
				// 2012-07-23 不能有此句，它会引发 this.taskFinish 的调用，造成异常 
				// v_Task.finishTask();
				
				v_Task.finalize();
				
				v_Task = null;
			}
			catch (Exception exce)
			{
				exce.printStackTrace();
			}
			catch (Throwable t)
			{
				t.printStackTrace();
			}
			finally
			{
				v_Task = null;
			}
		}
	}
	
	
	
	/**
	 * 注册事件
	 * 
	 * @param e
	 */
	public void addTaskGroupListener(TaskGroupListener e)
	{
		if ( this.taskGroupListeners == null )
		{
			this.taskGroupListeners = new HashSet<TaskGroupListener>();
		}
		
		this.taskGroupListeners.add(e);
	}
	
	
	
	/**
	 * 移除事件
	 * 
	 * @param e
	 */
	public void removeTaskGroupListener(TaskGroupListener e)
	{
		if ( this.taskGroupListeners == null )
		{
			return;
		}
		
		this.taskGroupListeners.remove(e);
	}
	
	
	
	/**
	 * 触发启用任务组所有任务的事件
	 * 
	 * @param i_Event
	 */
	protected void fireStartupAllTaskListener(TaskGroupEvent i_Event)
	{
		if ( this.taskGroupListeners == null )
		{
			return;
		}
		
		notifyStartupAllTaskListeners(i_Event);
	}
	
	
	
	/**
	 * 触发任务组中任务都完成后的事件
	 * 
	 * @param i_Event
	 */
	protected void fireFinishAllTaskListener(TaskGroupEvent i_Event)
	{
		if ( this.taskGroupListeners == null )
		{
			return;
		}
		
		notifyFinishAllTaskListeners(i_Event);
	}
	
	
	
	/**
	 * 通知所有注册启用任务组所有任务事件监听的对象
	 * 
	 * @param i_Event
	 */
	private void notifyStartupAllTaskListeners(TaskGroupEvent i_Event)
	{
		Iterator<TaskGroupListener> v_Iter = this.taskGroupListeners.iterator(); 

		while ( v_Iter.hasNext() ) 
		{
			TaskGroupListener v_Listener = v_Iter.next();

			v_Listener.startupAllTask(i_Event);
		}
	}
	
	
	
	/**
	 * 通知所有注册任务组中任务都完成后事件监听的对象
	 * 
	 * @param i_Event
	 */
	private void notifyFinishAllTaskListeners(TaskGroupEvent i_Event)
	{
		Iterator<TaskGroupListener> v_Iter = this.taskGroupListeners.iterator(); 

		while ( v_Iter.hasNext() ) 
		{
			TaskGroupListener v_Listener = v_Iter.next();

			v_Listener.finishAllTask(i_Event);
		}
	}
	
	
	
	public String getTaskGroupName() 
	{
		return taskGroupName;
	}



	public void setTaskGroupName(String taskGroupName) 
	{
		this.taskGroupName = taskGroupName;
	}
	
}
