package org.hy.common.thread.event;

import org.hy.common.BaseEvent;





/**
 * 停止线程线中所有线程的事件
 * 
 * 此类是个只读类，即只有 getter 方法。
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2012-04-27
 */
public class ThreadPoolStopEvent extends BaseEvent 
{
	
	private static final long serialVersionUID = -8432874946980600911L;
	
	
	
	public ThreadPoolStopEvent(Object i_Source) 
	{
		super(i_Source);
	}
	
	
	
	public ThreadPoolStopEvent(Object i_Source ,long i_Size)
	{
		super(i_Source ,i_Size);
	}
	
}
