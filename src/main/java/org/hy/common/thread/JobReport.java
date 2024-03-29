package org.hy.common.thread;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;

import org.hy.common.Help;
import org.hy.common.StaticReflect;
import org.hy.common.xml.SerializableDef;





/**
 * 定时任务监控用到的信息
 *
 * @author      ZhengWei(HY)
 * @createDate  2018-02-28
 * @version     v1.0
 *              v2.0  2018-04-11  添加：执行次数的统计属性
 *              v3.0  2018-05-22  添加：执行历史日志
 *              v4.0  2020-04-17  添加：云计算服务器的地址端口
 *              v5.0  2022-06-15  添加：是否调度执行，true仅表示，是否有调度成功，如远程服务是否成功接收了调度消息，但并不关心远程服务是否真的执行了任务
 */
public class JobReport extends SerializableDef
{

    private static final long serialVersionUID = 467967958670829923L;

    /** Job ID */
    private String     jobID;
    
    /** 间隔类型 */
    private String     intervalType;
    
    /** 间隔长度 */
    private String     intervalLen;
    
    /** 最后执行时间 */
    private String     lastTime;
    
    /** 计划执行时间 */
    private String     nextTime;
    
    /** 执行次数 */
    private Long       runCount;
    
    /**
     * 执行成功次数。
     * 
     * 是否有调度成功，如远程服务是否成功接收了调度消息，但并不关心远程服务是否真的执行了任务
     */
    private long       runOKCount;
    
    /** 执行日志。记录最后32次内的执行时间 */
    private Object []  runLogs;
    
    /** 云计算服务器的地址端口。格式为：IP:Port */
    private String     cloudServer;
    
    /** 描述 */
    private String     jobDesc;
    
    
    
    public JobReport(String i_JobID ,Job i_Job)
    {
        this(i_JobID ,i_Job ,null);
    }
    
    
    
    /**
     * 任务汇报信息
     *
     * @author      ZhengWei(HY)
     * @createDate  2024-01-17
     * @version     v1.0
     *
     * @param i_JobID      任务XID
     * @param i_Job        任务
     * @param i_IsAddJobs  任务是否添加到任务池中
     */
    @SuppressWarnings("unchecked")
    public JobReport(String i_JobID ,Job i_Job ,Boolean i_IsAddJobs)
    {
        this.jobID       = i_JobID;
        this.intervalLen = i_Job.getIntervalLen() + "";
        this.jobDesc     = i_Job.getTaskDesc();
        this.cloudServer = i_Job.getCloudServer();
        this.runCount    = i_Job.getRunCount();
        this.runOKCount  = i_Job.getRunOKCount();
        this.runLogs     = i_Job.getRunLogs().toArray();
        
        boolean v_IsAddJobs = false;
        if ( i_IsAddJobs != null )
        {
            v_IsAddJobs = i_IsAddJobs;
        }
        else
        {
            try
            {
                // 用反射的方式执行 XJava.getObjects()
                Method v_XJavaGetObjectMethod = Help.forName("org.hy.common.xml.XJava").getMethod("getObjects" ,Class.class ,boolean.class);
                Map<String ,Object> v_JobsMap = (Map<String ,Object>) StaticReflect.invoke(v_XJavaGetObjectMethod ,Jobs.class ,false);
                if ( !Help.isNull(v_JobsMap) )
                {
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
                }
            }
            catch (Exception exce)
            {
                exce.printStackTrace();
            }
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
            
            case Job.$IntervalType_Year:
                this.intervalType = "年";   break;
                
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
    public Long getRunCount()
    {
        return runCount;
    }

    
    /**
     * 设置：执行次数
     * 
     * @param runCount
     */
    public void setRunCount(Long runCount)
    {
        this.runCount = runCount;
    }

    
    /**
     * 获取：执行日志。记录最后32次内的执行时间
     */
    public Object [] getRunLogs()
    {
        return runLogs;
    }

    
    /**
     * 设置：执行日志。记录最后32次内的执行时间
     * 
     * @param runLogs
     */
    public void setRunLogs(Object [] runLogs)
    {
        this.runLogs = runLogs;
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


    /**
     * 获取：云计算服务器的地址端口。格式为：IP:Port
     */
    public String getCloudServer()
    {
        return cloudServer;
    }

    
    /**
     * 设置：云计算服务器的地址端口。格式为：IP:Port
     * 
     * @param cloudServer
     */
    public void setCloudServer(String cloudServer)
    {
        this.cloudServer = cloudServer;
    }
    
    
    /**
     * 是否调度执行。
     * 
     * 是否有调度成功，如远程服务是否成功接收了调度消息，但并不关心远程服务是否真的执行了任务
     * 
     * @return
     */
    public long getRunOKCount()
    {
        return this.runOKCount;
    }
    
}