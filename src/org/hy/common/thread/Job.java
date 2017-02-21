package org.hy.common.thread;

import org.hy.common.xml.XJava;

import org.hy.common.Date;
import org.hy.common.Execute;
import org.hy.common.Help;





/**
 * 任务配置信息
 * 
 * @author      ZhengWei(HY)
 * @createDate  2013-12-16
 * @version     v1.0  
 *              v2.0  2014-07-21：融合XJava、任务池、线程池的功能
 *              v3.0  2015-11-03：是否在初始时(即添加到Jobs时)，就执行一次任务
 *              v4.0  2016-07-08: 支持轮询间隔：秒
 */
public class Job extends Task<Object> implements Comparable<Job>
{
    /** 间隔类型: 秒 */
    public  static final int $IntervalType_Second = -2;
    
    /** 间隔类型: 分钟 */
    public  static final int $IntervalType_Minute = 60;
    
    /** 间隔类型: 小时 */
    public  static final int $IntervalType_Hour   = 60 * $IntervalType_Minute;
    
    /** 间隔类型: 天 */
    public  static final int $IntervalType_Day    = 24 * $IntervalType_Hour;
    
    /** 间隔类型: 周 */
    public  static final int $IntervalType_Week   = 7  * $IntervalType_Day;
    
    /** 间隔类型: 月 */
    public  static final int $IntervalType_Month  = 1;
    
    /** 间隔类型: 手工执行 */
    public  static final int $IntervalType_Manual = -1;
    
    private static       int $SerialNo            = 0;
    
    
    
    /** 任务编号 */
    private String         code;
    
    /** 任务配置名称 */
    private String         name;
    
    /** 
     * 运行的线程数
     * taskCount=1表示单例，否则时间点一到，无论上次执行的任务是否完成，都将运行一个新的任务。
     * 此属性要与this.code配合使用，this.code做为惟一标记
     */
    private int            taskCount;
    
    /** 间隔类型 */
    private int            intervalType;
    
    /** 间隔长度 */
    private int            intervalLen;
    
    /** 开始时间 */
    private Date           startTime;
    
    /** 下次时间 */
    private Date           nextTime;
    
    /** XJava对象标识 */
    private String         xjavaID;
    
    /** 执行的方法名 */
    private String         methodName;
    
    /** 描述 */
    private String         desc;
    
    /** 是否在初始时(即添加到Jobs时)，就执行一次任务（默认：不执行） */
    private boolean        isInitExecute;
    
    /** 当 isInitExecute = true 时，这个任务是串行立刻执行? 还是多线程池执行（默认：延时执行） */
    private boolean        isAtOnceExecute;
    
    private Jobs           jobs;
    
    
    
    private synchronized int GetSerialNo()
    {
        return ++$SerialNo;
    }
    
    
    
    public Job()
    {
        super("$JOB$");
        
        this.startTime       = Date.getNowTime().getNextHour().getFirstTimeOfHour(); 
        this.intervalType    = $IntervalType_Manual;
        this.intervalLen     = 1; 
        this.taskCount       = 1;
        this.isInitExecute   = false;
        this.isAtOnceExecute = false;
    }
    
    
    
    /**
     * 获取任务描述
     * 
     * @return
     */
    public String getTaskDesc()
    {
        return Help.NVL(this.getDesc() ,Help.NVL(this.getName() ,this.getCode()));
    }
    
    
    
    /**
     * 执行任务的方法
     */
    public void execute()
    {
        if ( Help.isNull(this.xjavaID) )
        {
            throw new NullPointerException("Job.getXjavaID() is null.");
        }
        
        if ( Help.isNull(this.methodName) )
        {
            throw new NullPointerException("Job.getMethodName() is null."); 
        }
        
        Object v_Object = XJava.getObject(this.xjavaID.trim());
        if ( v_Object == null )
        {
            throw new NullPointerException("Job.getXjavaID() = " + this.xjavaID + " XJava.getObject(...) is null.");
        }
        
        try
        {
            (new Execute(v_Object ,this.methodName.trim())).start();
        }
        catch (Exception exce)
        {
            exce.printStackTrace();
        }
        
        this.jobs.delMonitor(this);
        this.finishTask();
    }
    
    
    
    /**
     * 获取任务编号。
     * 
     * 因为每个任务对象都应当有独立的编号顺序。
     * 即每个Task实现类，实例化的第一个类的编号应当都从0开始编号，所以这个工作就由实现者来完成。
     * 
     * @return
     */
    public int getSerialNo()
    {
        return GetSerialNo();
    }
    
    
    
    /**
     * 获取下一次运行时间
     * 
     * @return
     */
    public Date getNextTime()
    {
        return this.getNextTime(Date.getNowTime());
    }
    
    
    
    /**
     * 获取下一次运行时间
     * 
     * @return
     */
    public Date getNextTime(final Date i_Now)
    {
        if ( this.intervalType == $IntervalType_Manual )
        {
            return new Date("2100-12-31 23:59:59");
        }
        
        if ( this.nextTime == null )
        {
            this.nextTime = new Date(this.startTime);
        }
        
        if ( this.intervalType == $IntervalType_Second )
        {
            if ( i_Now.equalsYMDHMS(this.nextTime) )
            {
                // Nothing.
            }
            else if ( i_Now.getTime() > this.nextTime.getTime() )
            {
                // 为什么减1秒呢？ 原因是Jobs中已等待间隔是1秒。
                this.nextTime.setTime(i_Now.getTime() + ((this.intervalLen - 1) * 1000));
            }
        }
        else if ( i_Now.equalsYMDHM(this.nextTime) )
        {
            // Nothing.
        }
        else if ( i_Now.getTime() > this.nextTime.getTime() )
        {
            if ( this.intervalType >= $IntervalType_Minute )
            {
                long v_DiffSec = (i_Now.getTime() - this.nextTime.getTime()) / 1000;
                long v_PerC    = this.intervalType;
                long v_Value   = ((int)(v_DiffSec / v_PerC)) * v_PerC;
                
                if ( v_Value < v_PerC * this.intervalLen )
                {
                    v_Value = v_PerC * this.intervalLen;
                }
                else if ( v_Value == v_PerC * this.intervalLen )
                {
                    // Nothing.
                }
                else if ( v_Value % (v_PerC * this.intervalLen) == 0 )
                {
                    v_Value += v_PerC * this.intervalLen;
                }
                
                v_Value = ((int)(v_Value / (v_PerC * this.intervalLen))) * v_PerC * this.intervalLen * 1000;
                this.nextTime.setTime(this.nextTime.getTime() + v_Value);
            }
            else
            {
                while ( i_Now.getTime() >= this.nextTime.getTime() )
                {
                    this.nextTime = this.nextTime.getNextMonth();
                }
                
                // 计算间隔
                for (int i=1; i<this.intervalLen; i++)
                {
                    this.nextTime = this.nextTime.getNextMonth();
                }
            }
        }
        
        return this.nextTime;
    }
    
    
    
    public String getCode()
    {
        return code;
    }


    public void setCode(String code)
    {
        this.code = code;
    }


    public int getIntervalType()
    {
        return intervalType;
    }

    
    public void setIntervalType(int intervalType)
    {
        this.nextTime     = null;
        this.intervalType = intervalType;
    }
    
    
    public int getIntervalLen()
    {
        return intervalLen;
    }


    public void setIntervalLen(int i_IntervalLen)
    {
        if ( i_IntervalLen >= 1 )
        {
            this.intervalLen = i_IntervalLen;
        }
    }

    
    public int getTaskCount()
    {
        return taskCount;
    }

    
    public void setTaskCount(int taskCount)
    {
        this.taskCount = taskCount;
    }


    public String getName()
    {
        return name;
    }

    
    public void setName(String name)
    {
        this.name = name;
    }
    
    
    public Date getStartTime()
    {
        return startTime;
    }

    
    public void setStartTime(String i_StartTimeStr)
    {
        this.startTime = new Date(i_StartTimeStr);
    }

    
    public String getDesc()
    {
        return Help.NVL(this.desc);
    }

    
    public void setDesc(String i_Desc)
    {
        this.desc = i_Desc;
    }
    
    
    public String getXjavaID()
    {
        return xjavaID;
    }


    public void setXjavaID(String xjavaID)
    {
        this.xjavaID = xjavaID;
    }

    
    public String getMethodName()
    {
        return methodName;
    }

    
    public void setMethodName(String methodName)
    {
        this.methodName = methodName;
    }

    
    public void setMyJobs(Jobs jobs)
    {
        this.jobs = jobs;
    }

    
    /**
     * 获取：是否在初始时(即添加到Jobs时)，就执行一次任务（默认：不执行）
     */
    public boolean isInitExecute()
    {
        return isInitExecute;
    }

    
    /**
     * 设置：是否在初始时(即添加到Jobs时)，就执行一次任务（默认：不执行）
     * 
     * @param isInitExecute 
     */
    public void setInitExecute(boolean isInitExecute)
    {
        this.isInitExecute = isInitExecute;
    }

    
    /**
     * 获取：当 isInitExecute = true 时，这个任务是串行立刻执行? 还是多线程池执行（默认：延时执行）
     */
    public boolean isAtOnceExecute()
    {
        return isAtOnceExecute;
    }

    
    /**
     * 设置：当 isInitExecute = true 时，这个任务是串行立刻执行? 还是多线程池执行（默认：延时执行）
     * 
     * @param isAtOnceExecute 
     */
    public void setAtOnceExecute(boolean isAtOnceExecute)
    {
        this.isAtOnceExecute = isAtOnceExecute;
    }


    public int compareTo(Job i_Other)
    {
        if ( i_Other == null )
        {
            return 1;
        }
        else
        {
            int v_Ret = this.getNextTime().compareTo(i_Other.getNextTime());
            
            if ( v_Ret == 0 )
            {
                return this.getCode().compareTo(i_Other.getCode());
            }
            else
            {
                return v_Ret;
            }
        }
    }

}

