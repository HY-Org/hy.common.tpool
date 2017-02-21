package org.hy.common.thread.junit;

import org.hy.common.thread.Task;
import org.hy.common.thread.ThreadBase;
import org.hy.common.thread.ThreadPool;





/**
 * 测试线程任务
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2011-11-25
 */
public class TestThreadTask01 extends Task<Object>
{
	private static int $SerialNo = 0;
	
	
	
	private synchronized int GetSerialNo()
	{
		return ++$SerialNo;
	}
	

	public TestThreadTask01() 
	{
		super("TestThreadTask01");
	}

	
	public void execute() 
	{
		ThreadPool.sleep(10000);
		
		if ( this.getTaskNo() <= 3 )
		{
			ThreadBase v_ThreadBase = ThreadPool.getThreadInstance(new TestThreadTask01());
			
			v_ThreadBase.startupAndExecuteTask();
		}
		
		ThreadPool.sleep(10000);
		this.finishTask();
	}
	
	
	/**
	 * 获取任务描述
	 * 
	 * @return
	 */
	public String getTaskDesc()
	{
		return "测试" + this.getTaskName();
	}
	
	
	@Override
	protected void finalize() throws Throwable 
	{
		super.finalize();
	}


	@Override
	public int getSerialNo()
	{
		return GetSerialNo();
	}

}
