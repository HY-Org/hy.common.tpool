package org.hy.common.thread;

import org.hy.common.Queue;





/**
 * 线程任务 -- 队列型
 * 
 * 此类本身就是一个线程任务，作用就是扫描队列，触发最终要执行的任务(newQueueThreadTask)。也起到一个缓冲的作用。
 * 
 * <Que>指队列中存放元素的类型
 * <O>  指线程自有的缓存的对象类型
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2011-12-09
 */
public abstract class ThreadTaskQueue<Que ,O> extends Task<O>
{
	/** 队列 */
	protected Queue<Que>          queue;
	
	/** 每次执行队列元素个数 */
	protected long                perExecuteCount;
	
	
	
	/**
	 * 回调方法。获取最终要执行的任务
	 * 
	 * @param i_Object
	 * @return
	 */
	public abstract Task<?> callBack_NewQueueThreadTask(Que i_Object);
	
	
	
	/**
	 * 构造器
	 * 
	 * @param i_TaskType  任务类型
	 */
	public ThreadTaskQueue(String i_TaskType) 
	{
		super(i_TaskType);
		
		this.queue           = new Queue<Que>();
		this.perExecuteCount = 1;
	}
	
	
	/**
	 * 添加队列最终要执行的任务
	 * 
	 * @param i_Obj
	 */
	public void putQueue(Que i_Obj)
	{
		this.queue.put(i_Obj);
	}
	
	
	/**
	 * 获取队列大小
	 * 
	 * @return
	 */
	public long sizeQueue()
	{
		return this.queue.size();
	}
	
	
	/**
	 * 循环扫描队列。当队列中有元素时，开启一个线程执行任务。
	 */
	public void execute() 
	{
		long v_Index = this.queue.size();
		
		if ( v_Index > this.perExecuteCount )
		{
			v_Index = this.perExecuteCount;
		}
		
		while ( v_Index >= 1 )
		{
			Que v_Obj = this.queue.get();
			
			if ( v_Obj != null )
			{
				Task<?> v_QueueTask = this.callBack_NewQueueThreadTask(v_Obj);
				
				if ( v_QueueTask != null )
				{
					ThreadBase v_ThreadBase = ThreadPool.getThreadInstance(v_QueueTask);
					v_ThreadBase.startupAndExecuteTask();
				}
			}
			
			v_Index--;
		}
	}



	public long getPerExecuteCount() 
	{
		return perExecuteCount;
	}

	
	public void setPerExecuteCount(long perExecuteCount) 
	{
		if ( perExecuteCount <= 0 )
		{
			return;
		}
		
		this.perExecuteCount = perExecuteCount;
	}
	
}
