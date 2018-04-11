package org.hy.common.thread;

import java.util.Iterator;
import java.util.Map;

import org.hy.common.xml.SerializableDef;
import org.hy.common.xml.XJava;





/**
 * 定时任务监控用到的信息
 *
 * @author      ZhengWei(HY)
 * @createDate  2018-02-28
 * @version     v1.0
 *              v2.0  2018-04-11  添加：执行次数的统计属性
 */
public class JobReport extends SerializableDef
{

    private static final long serialVersionUID = 467967958670829923L;

    /** Job ID */
    private String  jobID;
    
    /** 间隔类型 */
    private String  intervalType;
    
    /** 间隔长度 */
    private String  intervalLen;
    
    /** 最后执行时间 */
    private String  lastTime;
    
    /** 计划执行时间 */
    private String  nextTime;
    
    /** 执行次数 */
    private Integer runCount;
    
    /** 描述 */
    private String  jobDesc;
    
    
    
    public JobReport(String i_JobID ,Job i_Job)
    {
        this.jobID       = i_JobID;
        this.intervalLen = i_Job.getIntervalLen() + "";
        this.jobDesc     = i_Job.getTaskDesc();
        this.runCount    = i_Job.getRunCount();
        
        boolean             v_IsAddJobs = false;
        Map<String ,Object> v_JobsMap   = XJava.getObjects(Jobs.class ,false);
        for (Object v_Item : v_JobsMap.values())
        {
            Jobs          v_Jobs    = (Jobs)v_Item;
            Iterator<Job> v_JobIter = v_Jobs.getJobs();
            
            while (v_JobIter.hasNext())
            {
                if ( i_Job == v_JobIter.next() )
                {
                    v_IsAddJobs = true;
                    break;
                }
            }
            
            if ( v_IsAddJobs ) break;
        }
        
        if ( v_IsAddJobs )
        {
            this.lastTime = i_Job.getLastTime() == null ? "-" : i_Job.getLastTime().getFull();
            this.nextTime = i_Job.getNextTime() == null ? "-" : i_Job.getNextTime().getFull();
        }
        else
        {
            this.lastTime = "-";
            this.nextTime = "-";
        }
        
        switch ( i_Job.getIntervalType() )
        {
            case Job.$IntervalType_Second:
                this.intervalType = "秒";   break;
                
            case Job.$IntervalType_Minute:
                this.intervalType = "分钟"; break;
                
            case Job.$IntervalType_Hour:
                this.intervalType = "小时"; break;
                
            case Job.$IntervalType_Day:
                this.intervalType = "天";   break;
                
            case Job.$IntervalType_Week:
                this.intervalType = "周";   break;
                
            case Job.$IntervalType_Month:
                this.intervalType = "月";   break;
                
            default:
                this.intervalType = "手工"; 
                this.intervalLen  = "-";
                break;
        }
    }

    
    /**
     * 获取：Job ID
     */
    public String getJobID()
    {
        return jobID;
    }

    
    /**
     * 获取：间隔类型
     */
    public String getIntervalType()
    {
        return intervalType;
    }
    

    /**
     * 获取：间隔长度
     */
    public String getIntervalLen()
    {
        return intervalLen;
    }
    
    
    /**
     * 获取：最后执行时间
     */
    public String getLastTime()
    {
        return lastTime;
    }
    
    
    /**
     * 获取：计划执行时间
     */
    public String getNextTime()
    {
        return nextTime;
    }

    
    /**
     * 获取：描述
     */
    public String getJobDesc()
    {
        return jobDesc;
    }

    
    /**
     * 设置：Job ID
     * 
     * @param jobID 
     */
    public void setJobID(String jobID)
    {
        this.jobID = jobID;
    }


    /**
     * 设置：间隔类型
     * 
     * @param intervalType 
     */
    public void setIntervalType(String intervalType)
    {
        this.intervalType = intervalType;
    }

    
    /**
     * 设置：间隔长度
     * 
     * @param intervalLen 
     */
    public void setIntervalLen(String intervalLen)
    {
        this.intervalLen = intervalLen;
    }
    
    
    /**
     * 设置：最后执行时间
     * 
     * @param lastTime 
     */
    public void setLastTime(String lastTime)
    {
        this.lastTime = lastTime;
    }

    
    /**
     * 设置：计划执行时间
     * 
     * @param nextTime 
     */
    public void setNextTime(String nextTime)
    {
        this.nextTime = nextTime;
    }
    
    
    /**
     * 获取：执行次数
     */
    public Integer getRunCount()
    {
        return runCount;
    }

    
    /**
     * 设置：执行次数
     * 
     * @param runCount 
     */
    public void setRunCount(Integer runCount)
    {
        this.runCount = runCount;
    }


    /**
     * 设置：描述
     * 
     * @param jobDesc 
     */
    public void setJobDesc(String jobDesc)
    {
        this.jobDesc = jobDesc;
    }
    
}