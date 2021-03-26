package org.hy.common.thread.event;

import java.util.Iterator;

import org.hy.common.thread.Task;

import org.hy.common.Date;





/**
 * 任务组的事件的默认实现
 *
 * 此类中可以有 setter 方法，主要用于内部。
 * 
 * @author   ZhengWei(HY)
 * @version  V1.0  2012-04-28
 */
public class DefaultTaskGroupEvent extends TaskGroupEvent 
{
    private static final long serialVersionUID = -6891141322700482967L; 
    
    
    
    public DefaultTaskGroupEvent(Object i_Source) 
    {
        super(i_Source);
    }
    
    
    
    /**
     * 任务组中所有的任务
     */
    public void setTasks(Iterator<Task<?>> i_Iterator)
    {
        this.iterator = i_Iterator;
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
