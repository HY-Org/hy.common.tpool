package org.hy.common.thread.ui;

import org.hy.common.ConstValue;





/**
 * 视窗化监视线程池的表格字段位置的列号常量类
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2011-11-24
 */
public class WatchTableColumnIndex extends ConstValue
{
	/** 此常量的常量类型 */
	private final static ConstValue $ConstType$               = ConstValue.newConstType("WatchTableColumnIndex");
	
	
	
	/** 线程编号 */
	public final static WatchTableColumnIndex $ThreadNo       = new WatchTableColumnIndex("ThreadNo");
	
	/** 任务编号 */
	public final static WatchTableColumnIndex $TaskName       = new WatchTableColumnIndex("TaskName");
	
	/** 任务开始时间 */
	public final static WatchTableColumnIndex $TaskStartTime  = new WatchTableColumnIndex("TaskStartTime");
	
	/** 所有任务累计用时 */
	public final static WatchTableColumnIndex $TotalTime      = new WatchTableColumnIndex("TotalTime");
	
	/** 任务描述 */
	public final static WatchTableColumnIndex $TaskDesc       = new WatchTableColumnIndex("TaskDesc");
	
	/** 线程运行状态 */
	public final static WatchTableColumnIndex $RunStatus      = new WatchTableColumnIndex("RunStatus");
	
	/** 已执行完成的任务次数 */
	public final static WatchTableColumnIndex $ExecCount      = new WatchTableColumnIndex("ExecCount");
	
	
	
	public WatchTableColumnIndex(String i_Name)
	{
		super($ConstType$ ,i_Name);
	}
	
	
	public WatchTableColumnIndex(int i_ID ,String i_Name)
	{
		super($ConstType$ ,i_ID ,i_Name);
	}
	
}
