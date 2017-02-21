package org.hy.common.thread.junit;

import org.hy.common.thread.Task;
import org.hy.common.thread.ThreadPool;





/**
 * 测试线程任务
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2011-11-23
 */
public class TestThreadTask extends Task
{
	private static int $SerialNo = 0;
	
	
	
	private synchronized int GetSerialNo()
	{
		return ++$SerialNo;
	}
	
	

	public TestThreadTask() 
	{
		super("TestThreadTask");
	}

	
	public void execute() 
	{
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
		return "测试";
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
