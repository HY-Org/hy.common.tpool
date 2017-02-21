package org.hy.common.thread;

import org.hy.common.ConstValue;





/**
 * 通用线程的运行状态
 * 
 * 反映当前线程状态
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2011-11-20
 */
public class ThreadRunStatus extends ConstValue
{
	/** 此常量的常量类型 */
	private final static ConstValue $ConstType$         = ConstValue.newConstType("ThreadRunStatus");
	
	
	
	/** 初始化 */
	public final static ThreadRunStatus $Init       = new ThreadRunStatus("Init");
	
	/** 休息中 */
	public final static ThreadRunStatus $Rest       = new ThreadRunStatus("Rest");
	
	/** 执行任务中、工作中 */
	public final static ThreadRunStatus $Working    = new ThreadRunStatus("Working");
	
	/** 任务完成 */
	public final static ThreadRunStatus $Finish     = new ThreadRunStatus("Finish");
	
	/** 执行异常 */
	public final static ThreadRunStatus $Exception  = new ThreadRunStatus("Exception");
	
	/** 线程自毁 */
	public final static ThreadRunStatus $Kill       = new ThreadRunStatus("Kill");
	
	

	public ThreadRunStatus(String i_Name)
	{
		super($ConstType$ ,i_Name);
	}
	
	
	public ThreadRunStatus(int i_ID ,String i_Name)
	{
		super($ConstType$ ,i_ID ,i_Name);
	}

}
