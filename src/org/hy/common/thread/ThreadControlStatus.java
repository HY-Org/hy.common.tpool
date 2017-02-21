package org.hy.common.thread;

import org.hy.common.ConstValue;





/**
 * 通用线程的线程控制运行状态
 * 
 * 控制线程的命令集
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2011-11-17
 */
public class ThreadControlStatus extends ConstValue
{
	/** 此常量的常量类型 */
	private final static ConstValue $ConstType$          = ConstValue.newConstType("ThreadControlStatus");
	
	
	
	/** 启动状态 */
	public final static ThreadControlStatus $Starting    = new ThreadControlStatus("Starting");
	
	/** 执行状态 */
	public final static ThreadControlStatus $Executing   = new ThreadControlStatus("Executing");
	
	/** 完成状态 */
	public final static ThreadControlStatus $Finishing   = new ThreadControlStatus("Finishing");
	
	/** 空闲状态 */
	public final static ThreadControlStatus $Resting     = new ThreadControlStatus("Resting");
	
	/** 停止状态 */
	public final static ThreadControlStatus $Shutdowning = new ThreadControlStatus("Shutdowning");
	
	
	
	public ThreadControlStatus(String i_Name)
	{
		super($ConstType$ ,i_Name);
	}
	
	
	public ThreadControlStatus(int i_ID ,String i_Name)
	{
		super($ConstType$ ,i_ID ,i_Name);
	}
	
}
