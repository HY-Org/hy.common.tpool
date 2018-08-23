package org.hy.common.thread;

import org.hy.common.xml.SerializableDef;





/**
 * 线程池监控用到的信息
 *
 * @author      ZhengWei(HY)
 * @createDate  2018-02-26
 * @version     v1.0
 */
public class ThreadReport extends SerializableDef
{
    
    private static final long serialVersionUID = 2246388152740563211L;
    
    
    /** 线程编号 */
    private String threadNo;
    
    /** 任务编号 */
    private String taskName;
    
    /** 累计用时 */
    private long   totalTime;
    
    /** 最后更新时间 */
    private String lastTime;
    
    /** 运行状态 */
    private String runStatus;
    
    /** 执行次数 */
    private long   execCount;
    
    /** 任务描述 */
    private String taskDesc;
    
    
    
    public ThreadReport(ThreadBase i_ThreadBase)
    {
        Task<?> v_Task = i_ThreadBase.getTaskObject();
        if ( v_Task != null )
        {
            this.taskName = v_Task.getTaskName();
            this.taskDesc = v_Task.getTaskDesc();
        }
        else
        {
            this.taskName = "";
            this.taskDesc = "";
        }
        
        if ( null == i_ThreadBase.getTaskStartTime()
          && null == i_ThreadBase.getTaskEndTime() )
        {
            this.lastTime = "";
        }
        else if ( null == i_ThreadBase.getTaskStartTime() )
        {
            this.lastTime = i_ThreadBase.getTaskEndTime().getFull();
        }
        else if ( null == i_ThreadBase.getTaskEndTime() )
        {
            this.lastTime = i_ThreadBase.getTaskStartTime().getFull();
        }
        else if ( i_ThreadBase.getTaskStartTime().getTime() >= i_ThreadBase.getTaskEndTime().getTime() )
        {
            this.lastTime = i_ThreadBase.getTaskStartTime().getFull();
        }
        else
        {
            this.lastTime = i_ThreadBase.getTaskEndTime().getFull();
        }
        
        this.threadNo  = i_ThreadBase.getThreadNo();
        this.totalTime = i_ThreadBase.getTotalTime();
        this.runStatus = i_ThreadBase.getThreadRunStatusName();
        this.execCount = i_ThreadBase.getExecuteTaskCount();
    }
    
    
    
    /**
     * 获取：线程编号
     */
    public String getThreadNo()
    {
        return threadNo;
    }
    

    
    /**
     * 获取：任务编号
     */
    public String getTaskName()
    {
        return taskName;
    }
    

    
    /**
     * 获取：累计用时
     */
    public long getTotalTime()
    {
        return totalTime;
    }
    
    
    
    /**
     * 获取：最后更新时间
     */
    public String getLastTime()
    {
        return lastTime;
    }
    

    
    /**
     * 设置：最后更新时间
     * 
     * @param lastTime 
     */
    public void setLastTime(String lastTime)
    {
        this.lastTime = lastTime;
    }
    


    /**
     * 获取：运行状态
     */
    public String getRunStatus()
    {
        return runStatus;
    }
    

    
    /**
     * 获取：执行次数
     */
    public long getExecCount()
    {
        return execCount;
    }
    

    
    /**
     * 获取：任务描述
     */
    public String getTaskDesc()
    {
        return taskDesc;
    }
    

    
    /**
     * 设置：线程编号
     * 
     * @param threadNo 
     */
    public void setThreadNo(String threadNo)
    {
        this.threadNo = threadNo;
    }
    

    
    /**
     * 设置：任务编号
     * 
     * @param taskName 
     */
    public void setTaskName(String taskName)
    {
        this.taskName = taskName;
    }
    

    
    /**
     * 设置：累计用时
     * 
     * @param totalTime 
     */
    public void setTotalTime(long totalTime)
    {
        this.totalTime = totalTime;
    }
    

    
    /**
     * 设置：运行状态
     * 
     * @param runStatus 
     */
    public void setRunStatus(String runStatus)
    {
        this.runStatus = runStatus;
    }
    

    
    /**
     * 设置：执行次数
     * 
     * @param execCount 
     */
    public void setExecCount(long execCount)
    {
        this.execCount = execCount;
    }
    

    
    /**
     * 设置：任务描述
     * 
     * @param taskDesc 
     */
    public void setTaskDesc(String taskDesc)
    {
        this.taskDesc = taskDesc;
    }
    
}
