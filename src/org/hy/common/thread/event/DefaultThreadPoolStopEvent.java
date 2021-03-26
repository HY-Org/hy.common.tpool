package org.hy.common.thread.event;

import org.hy.common.Date;





/**
 * 停止线程线中所有线程的事件的默认实现
 *
 * 此类中可以有 setter 方法，主要用于内部。
 * 
 * @author   ZhengWei(HY)
 * @version  V1.0  2012-04-27
 */
public class DefaultThreadPoolStopEvent extends ThreadPoolStopEvent 
{
    private static final long serialVersionUID = -6891141322700482966L; 
    
    
    
    public DefaultThreadPoolStopEvent(Object i_Source) 
    {
        super(i_Source);
    }
    
    
    
    public DefaultThreadPoolStopEvent(Object i_Source ,long i_ThreadSize) 
    {
        super(i_Source ,i_ThreadSize);
    }

    
    
    public void setBeginTime(Date i_BeginTime)
    {
        this.beginTime = i_BeginTime;
    }
    
    
    
    public void setEndTime(Date i_EndTime) 
    {
        this.endTime = i_EndTime;
    }
    
    
    
    public void setSize(long i_Size)
    {
        this.size = i_Size;
    }
    
    
    
    public void setCompleteSize(long i_CompleteSize) 
    {
        this.completedSize = i_CompleteSize;
    }

}
