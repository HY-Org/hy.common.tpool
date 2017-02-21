package org.hy.common.thread.junit;

import static org.junit.Assert.*;

import org.hy.common.thread.ThreadBase;
import org.hy.common.thread.ThreadPool;
import org.junit.Test;





/**
 * 对自己编写的线程池功能，进行整体的测试
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2011-11-23
 */
public class ThreadPoolTest
{

	@Test
	public void test()
	{
		ThreadPool.setMaxThread(1000);
		ThreadPool.setMinThread(10);
		ThreadPool.setMinIdleThread(2);
		ThreadPool.setIntervalTime(100);
		ThreadPool.setIdleTimeKill(60);
		ThreadPool.setWatch(true);
		
		
		
		
		
		for (int i=1; i<=100; i++)
		{
			TestThreadTask v_Task = new TestThreadTask();
			
			ThreadBase v_ThreadBase = ThreadPool.getThreadInstance(v_Task);
			
			v_ThreadBase.startupAndExecuteTask();
			
			System.out.flush();
			System.out.println(i + " 秒"); 
			System.out.println(ThreadPool.showDetailInfo());
			ThreadPool.sleep(500);
		}
		
		
		while ( ThreadPool.getActiveThreadCount() >= 1 )
		{
			System.out.println("-- " + ThreadPool.showInfo());
			ThreadPool.sleep(1000);
		}
		System.out.println("-- " + ThreadPool.showInfo());
		
		ThreadPool.shutdownAllThread();
		
		assertTrue(true);
	}
	
}
