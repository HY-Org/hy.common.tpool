package org.hy.common.thread;

import java.util.Hashtable;
import java.util.Map;

import org.hy.common.Help;





/**
 * 对线程池中的活动线程，分任务类型的统计其活动线程数
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2011-11-30
 */
public class TaskTypeTotal 
{
	/**
	 * 统计数据的保存对象
	 * 
	 * Map.Key   为任务类型
	 * Map.Value 为任务对象
	 */
	private Map<String ,TaskTypeTotalInfo>  totalMap;
	
	
	
	public TaskTypeTotal()
	{
		this.totalMap = new Hashtable<String ,TaskTypeTotalInfo>();
	}
	
	
	/**
	 * 获取任务类型的个数
	 * 
	 * @return
	 */
	public int getTaskTypeCount()
	{
		return this.totalMap.size();
	}
	
	
	/**
	 * 获取某一任务类型的活动线程数
	 * 
	 * @param i_TaskType
	 * @return
	 */
	public int getActiveCount(String i_TaskType)
	{
		if ( Help.isNull(i_TaskType) )
		{
			throw new NullPointerException("Task Type is null");
		}
		
		if ( this.totalMap.containsKey(i_TaskType) )
		{
			return this.totalMap.get(i_TaskType).getActiveCount();
		}
		else
		{
			return 0;
		}
	}
	
	
	/**
	 * 活动的任务类型数++
	 * 
	 * @param i_TaskType
	 */
	public void active(String i_TaskType)
	{
		if ( Help.isNull(i_TaskType) )
		{
			throw new NullPointerException("Task Type is null");
		}
		
		this.taskTypeTotalChange(i_TaskType, 1);
	}
	
	
	/**
	 * 活动的任务类型数--
	 * 
	 * @param i_TaskType
	 */
	public void rest(String i_TaskType)
	{
		if ( Help.isNull(i_TaskType) )
		{
			throw new NullPointerException("Task Type is null");
		}
		
		this.taskTypeTotalChange(i_TaskType, -1);
	}
	
	
	/**
	 *  统一用此方法设置统计数据
	 * 
	 * @param i_TaskType     任务类型
	 * @param i_OperateType  设置类型 >= 1 为：添加； 其它为减少
	 */
	private synchronized void taskTypeTotalChange(String i_TaskType ,int i_OperateType)
	{
		if ( i_OperateType >= 1 )
		{
			if ( this.totalMap.containsKey(i_TaskType) )
			{
				this.totalMap.get(i_TaskType).putActive();
			}
			else
			{
				this.totalMap.put(i_TaskType ,new TaskTypeTotalInfo(i_TaskType));
			}
		}
		else
		{
			this.totalMap.get(i_TaskType).outActive();
		}
	}
	
	
	
	/**
	 * 内部类。分任务类型的统计的元数据信息
	 */
	class TaskTypeTotalInfo
	{
		/** 任务类型 */
		private String taskType;
		
		/** 活动的线程数 */
		private int    activeCount;
		
		
		
		public TaskTypeTotalInfo(String i_TaskType)
		{
			this.taskType    = i_TaskType;
			this.activeCount = 1;
		}
		
		
		public String getTaskType()
		{
			return this.taskType;
		}
		
		
		public int getActiveCount()
		{
			return this.activeCount;
		}
		
		
		/**
		 * 活动数加1
		 */
		public void putActive()
		{
			this.setActiveCount(1);
		}
		
		
		/**
		 * 活动数减1
		 */
		public void outActive()
		{
			this.setActiveCount(-1);
		}
		
		
		/**
		 * 统一用此方法设置活动数
		 * 
		 * @param i_OperateType  设置类型 >= 1 为：添加； 其它为减少
		 */
		private synchronized void setActiveCount(int i_OperateType)
		{
			if ( i_OperateType >= 1 )
			{
				this.activeCount++;
			}
			else
			{
				this.activeCount--;
			}
		}
		
	}
	
}
