package org.hy.common.thread.junit;

import static org.junit.Assert.*;

import org.hy.common.thread.Task;
import org.hy.common.thread.TaskGroup;
import org.hy.common.thread.ThreadPool;
import org.junit.Test;





/**
 * 对自己编写的任务池、线程池功能，进行整体的测试
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2012-05-22
 */
public class JU20120522_TaskPool 
{
	private static final String $TaskType   = "JU20120522_TaskPool";
	
	private static       int    $SerialNo   = 0;
	
	
	
	/**
	 * 获取任务序列号
	 * 
	 * @return
	 */
	private synchronized int GetSerialNo()
	{
		return ++$SerialNo;
	}
	

	
	class MyTask extends Task<Object>
	{

		public MyTask() 
		{
			super($TaskType);
		}



		@Override
		public void execute() 
		{
			ThreadPool.sleep(1000);
			
			this.finishTask();
		}

		
		
		@Override
		public int getSerialNo() 
		{
			return GetSerialNo();
		}

		
		
		@Override
		public String getTaskDesc() 
		{
			return "" + this.getTaskNo();
		}
		
	}
	
	
	
	@Test
	public void test()
	{
		ThreadPool.setMaxThread(3);
		ThreadPool.setMinThread(0);
		ThreadPool.setMinIdleThread(0);
		ThreadPool.setIntervalTime(100);
		ThreadPool.setIdleTimeKill(600);
		ThreadPool.setWatch(true);
		
		
		
		TaskGroup v_TaskGroup = new TaskGroup($TaskType);
		
		for (int v_Index=0; v_Index<100; v_Index++)
		{
			v_TaskGroup.addTask(new MyTask());
		}
		v_TaskGroup.startupAllTask();

		
		// 所有任务再次执行一次
		ThreadPool.sleep(1 * 60 * 1000);
		v_TaskGroup.startupAllTask();
		
		
		ThreadPool.sleep(5 * 60 * 1000);
		assertTrue(true);
	}
	
}
