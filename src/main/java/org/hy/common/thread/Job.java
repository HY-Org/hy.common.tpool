package org.hy.common.thread;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.hy.common.Busway;
import org.hy.common.Date;
import org.hy.common.Execute;
import org.hy.common.Help;
import org.hy.common.StaticReflect;
import org.hy.common.StringHelp;
import org.hy.common.XJavaID;
import org.hy.common.net.ClientSocket;
import org.hy.common.net.common.ClientCluster;
import org.hy.common.net.data.CommunicationResponse;
import org.hy.common.net.data.LoginRequest;
import org.hy.common.net.netty.rpc.ClientRPC;
import org.hy.common.xml.log.Logger;

import com.greenpineyu.fel.FelEngine;
import com.greenpineyu.fel.FelEngineImpl;
import com.greenpineyu.fel.context.FelContext;
import com.greenpineyu.fel.context.MapContext;





/**
 * 任务配置信息（支持云服务任务的执行）
 * 
 * @author      ZhengWei(HY)
 * @createDate  2013-12-16
 * @version     v1.0
 *              v2.0  2014-07-21：融合XJava、任务池、线程池的功能
 *              v3.0  2015-11-03：是否在初始时(即添加到Jobs时)，就执行一次任务
 *              v4.0  2016-07-08：支持轮询间隔：秒
 *              v5.0  2017-07-03：添加：1. 最后一次执行时间的记录。
 *                                     2. toString()方法中显示出下次的执行时间
 *              v5.1  2018-04-11  添加：执行次数的统计属性
 *              v5.2  2018-05-22  添加：执行历史日志
 *              v5.3  2018-08-21  修改：将xjavaID改为xid与XSQLNode统一，同时防止与接口 org.hy.common.XJavaID 中的方法冲突。
 *              v6.0  2018-11-29  添加1：开始时间组，即开始时间可以有多个。
 *                                      可实现一项任务在多个时间点上周期执行，并且只须配置一个Job，而非多个Job。
 *                                      注：此功能对 "间隔类型:秒、分" 是无效的（只取最小时间为开始时间）
 *                                添加2：在条件判定为True时，才允许执行任务。并预定义了占位符的标准。
 *                                      可实现如下场景：某任务每天8~18点间周期执行。
 *                                建议人：邹德福、张德宏
 *              v7.0  2019-02-17  添加：执行云服务上的任务。
 *                                     只须额外配置云服务的IP:端口即可。
 *                                     当然，云服务要开启通讯的，见 https://github.com/HY-Org/hy.common.net
 *              v8.0  2019-03-02  添加：按年份间隔的时间类型（都用到年周期执行的，哈哈）。
 *                                建议人：王力
 *              v9.0  2019-03-06  添加：执行日志输出到控制台及二次预防重复执行的可能。
 *              v10.0 2020-01-14  添加：条件表达式支持星期几的判定条件占位符
 *              v11.0 2021-12-14  优化：使用Net 3.0.0版本
 *                                删除：将desc与comment属性合并
 *                                添加：允许外界直接传入 ClientCluster 对象，方便好的控制通讯，如控制超时时长等
 *              v12.0 2022-06-15  添加：记录执行的消息流水和消息发送结果
 *                                添加：执行成功次数，是否有调度成功，如远程服务是否成功接收了调度消息，但并不关心远程服务是否真的执行了任务
 *                                添加：尝试执行次数和尝试时间间隔
 *              v13.0 2023-10-08  添加：通过参数，可允许备节点也执行定时任务
 */
public class Job extends Task<Object> implements Comparable<Job> ,XJavaID
{
    private static final Logger $Logger = Logger.getLogger(Jobs.class ,true);
    
    /** 间隔类型: 秒 */
    public  static final int       $IntervalType_Second = -2;
    
    /** 间隔类型: 分钟 */
    public  static final int       $IntervalType_Minute = 60;
    
    /** 间隔类型: 小时 */
    public  static final int       $IntervalType_Hour   = 60 * $IntervalType_Minute;
    
    /** 间隔类型: 天 */
    public  static final int       $IntervalType_Day    = 24 * $IntervalType_Hour;
    
    /** 间隔类型: 周 */
    public  static final int       $IntervalType_Week   = 7  * $IntervalType_Day;
    
    /** 间隔类型: 月 */
    public  static final int       $IntervalType_Month  = 1;
    
    /** 间隔类型: 年 */
    public  static final int       $IntervalType_Year   = 2;
    
    /** 间隔类型: 手工执行 */
    public  static final int       $IntervalType_Manual = -1;
    
    
    /** 是否允许执行的条件中的表达式中，预定义占位符有：年份 */
    public  static final String    $Condition_Y         = "Y";
    
    /** 是否允许执行的条件中的表达式中，预定义占位符有：月份 */
    public  static final String    $Condition_M         = "M";

    /** 是否允许执行的条件中的表达式中，预定义占位符有：天 */
    public  static final String    $Condition_D         = "D";
    
    /** 是否允许执行的条件中的表达式中，预定义占位符有：小时(24小时制) */
    public  static final String    $Condition_H         = "H";
    
    /** 是否允许执行的条件中的表达式中，预定义占位符有：分钟 */
    public  static final String    $Condition_MI        = "MI";
    
    /** 是否允许执行的条件中的表达式中，预定义占位符有：秒 */
    public  static final String    $Condition_S         = "S";
    
    /** 是否允许执行的条件中的表达式中，预定义占位符有：年月日，格式为YYYYMMDD 样式的整数类型。整数类型是为了方便比较 */
    public  static final String    $Condition_YMD       = "YMD";
    
    /** 是否允许执行的条件中的表达式中，预定义占位符有：星期几，星期一为1，星期二为2...星期六为6，星期天为7 */
    public  static final String    $Condition_Week      = "WEEK";
    
    /** 表达式引擎 */
    private static final FelEngine $FelEngine           = new FelEngineImpl();
    
    /** 执行未成功时，尝试执行的最大次数 */
    private static final int       $TryMaxCount         = 10;
    
    /** 执行未成功时，每次尝试间隔时长（秒） */
    private static final int       $TryIntervalLen      = 5;
    
    
    private static       long      $SerialNo            = 0;
    
    
    /** XJava池中对象的ID标识 */
    private String         xjavaID;
    
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
    
    /** 开始时间组。多个开始时间用分号分隔。多个开始时间对 "间隔类型:秒、分" 是无效的（只取最小时间为开始时间） */
    private List<Date>     startTimes;
    
    /** 下次时间 */
    private Date           nextTime;
    
    /** 下次时间组。 */
    private List<Date>     nextTimes;
    
    /** 最后一次的执行时间 */
    private Date           lastTime;
    
    /** 强行执行。即不受时间波动的监管也要执行。哪怕是时间回退了，也要执行 */
    private boolean        forceRun;
    
    /** 备节点是否执行。默认情况下：仅允许主节点执行 */
    private boolean        slaveRun;
    
    /**
     * 允许执行的条件。
     * 
     *  表达式中，预定义占位符有（占位符不区分大小写）：
     *    :Y    表示年份
     *    :M    表示月份
     *    :D    表示天
     *    :H    表示小时(24小时制)
     *    :MI   表示分钟
     *    :S    表示秒
     *    :YMD  表示年月日，格式为YYYYMMDD 样式的整数类型。整数类型是为了方便比较
     */
    private String         condition;
    
    /** 云计算服务器的地址端口。格式为：IP:Port */
    private String         cloudServer;
    
    /**
     * 云计算服务器的通讯版本
     *   Socket：表示2021-12-13之前，采用原生Socket实现的通讯技术
     *   RPC：   表示2021-12-13之后，采用Netty实现的通讯技术
     * 
     * 默认值：RPC。
     */
    private String         cloudVersion;
    
    /** 云计算服务器的对象 */
    private ClientCluster  clientCluster;
    
    /** XJava对象标识 */
    private String         xid;
    
    /** 执行的方法名 */
    private String         methodName;
    
    /** 是否在初始时(即添加到Jobs时)，就执行一次任务（默认：不执行） */
    private boolean        isInitExecute;
    
    /** 当 isInitExecute = true 时，这个任务是串行立刻执行? 还是多线程池执行（默认：延时执行） */
    private boolean        isAtOnceExecute;
    
    private Jobs           jobs;
    
    /** 执行未成功时，尝试执行的最大次数 */
    private int            tryMaxCount;
    
    /** 执行未成功时，每次尝试间隔时长（秒） */
    private int            tryIntervalLen;
    
    /** 执行次数 */
    private long           runCount;
    
    /**
     * 执行成功次数。
     * 
     * 是否有调度成功，如远程服务是否成功接收了调度消息，但并不关心远程服务是否真的执行了任务
     */
    private long           runOKCount;
    
    /** 执行日志。记录最后1440次内的执行时间 */
    private Busway<String> runLogs;
    
    /** 注释。可用于日志的输出等帮助性的信息 */
    private String         comment;
    
    
    
    /**
     * 注意：本方法可能在多个实例、多个线程中执行，所以要用 static synchronized
     * 
     * @return
     */
    private static synchronized long GetSerialNo()
    {
        return ++$SerialNo;
    }
    
    
    
    public Job()
    {
        super("$JOB$");
        
        this.startTimes      = new ArrayList<Date>();
        this.startTimes.add(Date.getNowTime().getNextHour().getFirstTimeOfHour());
        this.nextTime        = null;
        this.nextTimes       = null;
        this.intervalType    = $IntervalType_Manual;
        this.intervalLen     = 1;
        this.taskCount       = 1;
        this.isInitExecute   = false;
        this.isAtOnceExecute = false;
        this.lastTime        = null;
        this.tryMaxCount     = $TryMaxCount;
        this.tryIntervalLen  = $TryIntervalLen;
        this.runCount        = 0L;
        this.runOKCount      = 0L;
        this.runLogs         = new Busway<String>(1440);
        this.forceRun        = false;
        this.slaveRun        = false;
        this.cloudVersion    = "RPC";
    }
    
    
    
    /**
     * 设置XJava池中对象的ID标识。此方法不用用户调用设置值，是自动的。
     * 
     * @param i_XJavaID
     */
    @Override
    public void setXJavaID(String i_XJavaID)
    {
        this.xjavaID = i_XJavaID;
    }
    
    
    
    /**
     * 获取XJava池中对象的ID标识。
     * 
     * @return
     */
    @Override
    public String getXJavaID()
    {
        return this.xjavaID;
    }
    
    
    
    /**
     * 获取任务描述
     * 
     * @return
     */
    @Override
    public String getTaskDesc()
    {
        return Help.NVL(this.getDesc() ,Help.NVL(this.getName() ,this.getCode()));
    }
    
    
    
    /**
     * 执行任务的方法
     */
    @Override
    public void execute()
    {
        if ( Help.isNull(this.xid) )
        {
            throw new NullPointerException("Job.getXid() is null.");
        }
        
        if ( Help.isNull(this.methodName) )
        {
            throw new NullPointerException("Job.getMethodName() is null.");
        }
        
        try
        {
            Date    v_Now     = new Date();
            boolean v_IsAllow = false;
            
            if ( this.lastTime == null )
            {
                v_IsAllow     = true;
                this.lastTime = v_Now;
            }
            else if ( this.intervalType == Job.$IntervalType_Second
                   || this.intervalType == Job.$IntervalType_Manual
                   || this.jobs         == null)
            {
                v_IsAllow     = true;
                this.lastTime = v_Now;
            }
            // 第二个地方再次预防，第一个地方在Jobs.execute()中。
            // 又添加一次预防的原因是：这里离 this.lastTime = new Date(); 最近。
            // 预防因主机系统时间不精确，时间同步机制异常（如来回调整时间、时间跳跃、时间波动等），
            // 造成定时任务重复执行的可能。  ZhengWei(HY) Add 2019-03-06
            else if ( !this.getLastTime().equalsYMDHM(v_Now) && this.getLastTime().differ(v_Now) < 0 )
            {
                v_IsAllow     = true;
                this.lastTime = v_Now;
            }
            // 手工执行时
            else if ( this.jobs != null && !this.jobs.isMonitor(this) )
            {
                v_IsAllow     = true;
                this.lastTime = v_Now;
            }
            else if ( this.forceRun )
            {
                v_IsAllow     = true;
                this.lastTime = v_Now;
            }
            
            if ( v_IsAllow )
            {
                this.runCount++;
                
                // 本机执行：默认的
                if ( this.clientCluster == null )
                {
                    // 用反射的方式执行 XJava.getObject()
                    Method v_XJavaGetObjectMethod = Help.forName("org.hy.common.xml.XJava").getMethod("getObject" ,String.class);
                    Object v_Object               = StaticReflect.invoke(v_XJavaGetObjectMethod ,this.xid.trim());
                    if ( v_Object == null )
                    {
                        this.runLogs.put(this.lastTime.getFullMilli() + "  R=-3");
                        throw new NullPointerException("Job.getXid() = " + this.xid + " XJava.getObject(...) is null.");
                    }
                    
                    this.runLogs.put(this.lastTime.getFullMilli() + "  R=本机触发成功");
                    (new Execute(v_Object ,this.methodName.trim())).start();
                    this.runOKCount++;
                }
                // 云服务执行：当配置CloudServer时。
                else
                {
                    new Execute(this ,"executeCluster").start();
                }
                
                $Logger.info("执行定时任务 " + this.xid + "." + this.getMethodName() + "：" + this.getTaskDesc());
            }
        }
        catch (Exception exce)
        {
            $Logger.error(exce);
        }
        
        if ( this.jobs != null )
        {
            // 注意：delMonitor()方法不要加同步锁。否则会出现线程阻塞
            this.jobs.delMonitor(this);
            this.finishTask();
        }
        /*
        else
        {
            // 当 this.jobs 为空时，表示本方法是手工执行，并不是定时任务自动执行的。
        }
        */
    }
    
    
    
    /**
     * 远程调试执行。并记录执行的消息流水和消息发送结果
     * 
     * @author      ZhengWei(HY)
     * @createDate  2022-06-15
     * @version     v1.0
     */
    public void executeCluster()
    {
        int v_TryCount = 1;
        
        while ( v_TryCount <= this.tryMaxCount )
        {
            if ( againExecuteCluster(v_TryCount++) )
            {
                v_TryCount = this.tryMaxCount + 1;
                break;
            }
            
            try
            {
                Thread.sleep(this.tryIntervalLen * 1000);
            }
            catch (InterruptedException e)
            {
                // Nothing.
            }
        }
    }
    
    
    
    /**
     * 当首次执行未成功时，尝试再执行一次远程调试执行
     * 
     * @author      ZhengWei(HY)
     * @createDate  2022-06-15
     * @version     v1.0
     * 
     * @param  i_TryCount  尝试执行次数
     */
    private boolean againExecuteCluster(int i_TryCount)
    {
        boolean v_IsOK = false;
        
        try
        {
            if ( !this.clientCluster.operation().isStartServer() )
            {
                this.clientCluster.operation().startServer();
            }
            
            if ( !this.clientCluster.operation().isLogin() )
            {
                this.clientCluster.operation().login(new LoginRequest("Job" ,"").setSystemName("JobCloud"));
            }
            
            CommunicationResponse v_Response = this.clientCluster.operation().sendCommand(-1L ,this.xid ,this.methodName.trim() ,false ,true);
            
            if ( v_Response != null )
            {
                String v_Result = null;
                if ( CommunicationResponse.$Succeed == v_Response.getResult() )
                {
                    this.runOKCount++;
                    v_Result = "对方接收成功 " + i_TryCount;
                    v_IsOK = true;
                }
                else
                {
                    v_Result = "" + v_Response.getResult();
                }
                this.runLogs.put(Date.getNowTime().getFullMilli() + "  " + v_Response.getSerialNo() + "  R=" + v_Result);
            }
            else
            {
                this.runLogs.put(Date.getNowTime().getFullMilli() + "  R=-1");
            }
        }
        catch (Exception exce)
        {
            this.runLogs.put(Date.getNowTime().getFullMilli() + "  R=-2  " + exce.getMessage());
            $Logger.error(exce);
        }
        
        return v_IsOK;
    }
    
    
    
    /**
     * 获取任务编号。
     * 
     * 因为每个任务对象都应当有独立的编号顺序。
     * 即每个Task实现类，实例化的第一个类的编号应当都从0开始编号，所以这个工作就由实现者来完成。
     * 
     * @return
     */
    @Override
    public long getSerialNo()
    {
        return GetSerialNo();
    }
    
    
    
    /**
     * 获取：下次时间组。
     */
    public List<Date> getNextTimes()
    {
        return nextTimes;
    }



    /**
     * 获取下一次运行时间
     * 
     * @return
     */
    public Date getNextTime()
    {
        if ( this.intervalType == $IntervalType_Second )
        {
            return this.getNextTime(Date.getNowTime());
        }
        else if ( this.lastTime == null || this.nextTime == null )
        {
            return this.getNextTime(Date.getNowTime());
        }
        else if ( this.lastTime.equalsYMDHM(this.nextTime) )
        {
            return this.getNextTime(Date.getNowTime().getMinutes(1));
        }
        else
        {
            return this.getNextTime(Date.getNowTime());
        }
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
            return new Date("9999-12-31 23:59:59");
        }
        
        if ( this.nextTime == null )
        {
            // 重新创建时间对象，防止nextTime修改影响this.startTime
            // startTimes已按从小到大排序过，此处取最小时间
            this.nextTime  = new Date(this.startTimes.get(0));
            this.nextTimes = new ArrayList<Date>();
            for (Date v_STime : this.startTimes)
            {
                this.nextTimes.add(new Date(v_STime));
            }
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
            // 间隔类型: 分钟 小时 天 周
            if ( this.intervalType >= $IntervalType_Minute )
            {
                // 为了性能，所以在if分支语句中写for
                for (Date v_NextTime : this.nextTimes)
                {
                    if ( i_Now.getTime() <= v_NextTime.getTime() )
                    {
                        continue;
                    }
                    
                    long v_DiffSec = (i_Now.getTime() - v_NextTime.getTime()) / 1000;
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
                    v_NextTime.setTime(v_NextTime.getTime() + v_Value);
                }
            }
            // 间隔类型: 月
            else if ( this.intervalType == $IntervalType_Month )
            {
                // 为了性能，所以在if分支语句中写for
                for (Date v_NextTime : this.nextTimes)
                {
                    if ( i_Now.getTime() <= v_NextTime.getTime() )
                    {
                        continue;
                    }
                    
                    int v_AddCount = 0;
                    while ( i_Now.getTime() >= v_NextTime.getTime() )
                    {
                        v_NextTime.toDate(v_NextTime.getNextMonth());
                        v_AddCount++;
                    }
                    
                    // 计算间隔
                    int i = 0;
                    if ( this.intervalLen == 1 )
                    {
                        i = 1;
                    }
                    else
                    {
                        i = v_AddCount % this.intervalLen;
                    }
                    for (; i<this.intervalLen; i++)
                    {
                        v_NextTime.toDate(v_NextTime.getNextMonth());
                    }
                }
            }
            // 间隔类型: 年
            else if ( this.intervalType == $IntervalType_Year )
            {
                // 为了性能，所以在if分支语句中写for
                for (Date v_NextTime : this.nextTimes)
                {
                    if ( i_Now.getTime() <= v_NextTime.getTime() )
                    {
                        continue;
                    }
                    
                    int v_AddCount = 0;
                    while ( i_Now.getTime() >= v_NextTime.getTime() )
                    {
                        v_NextTime.toDate(v_NextTime.getNextYear());
                        v_AddCount++;
                    }
                    
                    // 计算间隔
                    int i = 0;
                    if ( this.intervalLen == 1 )
                    {
                        i = 1;
                    }
                    else
                    {
                        i = v_AddCount % this.intervalLen;
                    }
                    for (; i<this.intervalLen; i++)
                    {
                        v_NextTime.toDate(v_NextTime.getNextYear());
                    }
                }
            }
            
            Help.toSort(this.nextTimes);
            this.nextTime = this.nextTimes.get(0);
        }
        
        return this.nextTime;
    }
    
    
    /**
     * 获取：任务编号
     */
    public String getCode()
    {
        return code;
    }

    
    /**
     * 设置：任务编号
     * 
     * @param code
     */
    public void setCode(String code)
    {
        this.code = code;
    }



    /**
     * 获取：间隔类型
     */
    public int getIntervalType()
    {
        return intervalType;
    }
    
    
    /**
     * 设置：间隔类型
     * 
     * @param intervalType
     */
    public void setIntervalType(int intervalType)
    {
        this.nextTime     = null;
        this.nextTimes    = null;
        this.intervalType = intervalType;
    }
    
    
    /**
     * 间隔长度
     *
     * @return
     */
    public int getIntervalLen()
    {
        return intervalLen;
    }


    /**
     * 设置：间隔长度
     * 
     * @param i_IntervalLen
     */
    public void setIntervalLen(int i_IntervalLen)
    {
        if ( i_IntervalLen >= 1 )
        {
            this.intervalLen = i_IntervalLen;
        }
    }

    
    /**
     * 获取：运行的线程数
     * taskCount=1表示单例，否则时间点一到，无论上次执行的任务是否完成，都将运行一个新的任务。
     * 此属性要与this.code配合使用，this.code做为惟一标记
     */
    public int getTaskCount()
    {
        return taskCount;
    }

    
    /**
     * 设置：运行的线程数
     * taskCount=1表示单例，否则时间点一到，无论上次执行的任务是否完成，都将运行一个新的任务。
     * 此属性要与this.code配合使用，this.code做为惟一标记
     * 
     * @param taskCount
     */
    public void setTaskCount(int taskCount)
    {
        this.taskCount = taskCount;
    }


    /**
     * 获取：任务配置名称
     */
    public String getName()
    {
        return name;
    }

    
    /**
     * 设置：任务配置名称
     * 
     * @param name
     */
    public void setName(String name)
    {
        this.name = name;
    }


    /**
     * 获取：开始时间组。多个开始时间用分号分隔。多个开始时间对 "间隔类型:秒、分" 是无效的（只取最小时间为开始时间）
     * 
     * @return
     */
    public List<Date> getStartTimes()
    {
        return this.startTimes;
    }

    
    /**
     * 设置：开始时间组。多个开始时间用分号分隔。多个开始时间对 "间隔类型:秒、分" 是无效的（只取最小时间为开始时间）
     * 
     * @param startTimes
     */
    public void setStartTimes(List<Date> startTimes)
    {
        this.startTimes = startTimes;
        Help.toSort(this.startTimes);
    }


    /**
     * 设置：开始时间组。多个开始时间用分号分隔。多个开始时间对 "间隔类型:秒、分" 是无效的（只取最小时间为开始时间）
     * 
     * @param i_StartTimesStr
     */
    public void setStartTime(String i_StartTimesStr)
    {
        if ( Help.isNull(i_StartTimesStr) )
        {
            return;
        }
        
        this.startTimes = new ArrayList<Date>();
        String [] v_STimeArr = StringHelp.replaceAll(i_StartTimesStr ,new String[]{"\t" ,"\n" ,"\r"} ,new String[]{""}).split(",");
        for (String v_STime : v_STimeArr)
        {
            this.startTimes.add(new Date(v_STime.trim()));
        }
        
        Help.toSort(this.startTimes);
    }


    /**
     * 获取：描述
     */
    public String getDesc()
    {
        return this.getComment();
    }

    
    /**
     * 设置：描述
     * 
     * @param xid
     */
    public void setDesc(String i_Desc)
    {
        this.setComment(i_Desc);
    }
    
    
    /**
     * 获取：云计算服务器的地址端口。格式为：IP:Port
     */
    public String getCloudServer()
    {
        return cloudServer;
    }
    
    
    /**
     * 设置云计算服务器的对象
     * 
     * @author      ZhengWei(HY)
     * @createDate  2021-12-15
     * @version     v1.0
     * 
     * @param i_ClientCluster
     */
    public void setCloudServer(ClientCluster i_ClientCluster)
    {
        if ( i_ClientCluster != null )
        {
            if ( i_ClientCluster instanceof ClientRPC )
            {
                ClientRPC v_OldCRPC = (ClientRPC)i_ClientCluster;
                ClientRPC v_NewCRPC = new ClientRPC();
                
                v_NewCRPC.setHost(       v_OldCRPC.getHost());
                v_NewCRPC.setPort(       v_OldCRPC.getPort());
                v_NewCRPC.setTimeout(    v_OldCRPC.getTimeout());
                v_NewCRPC.setSessionTime(v_OldCRPC.getSessionTime());
                v_NewCRPC.setComment(    v_OldCRPC.getComment());
                
                this.clientCluster = v_NewCRPC;
            }
            else if ( this.clientCluster instanceof ClientSocket )
            {
                this.clientCluster = i_ClientCluster;
                ((ClientSocket) this.clientCluster).setComment(Help.NVL(this.getName() ,this.getComment()));
            }
            
            this.cloudServer = this.clientCluster.getHost() + ":" + this.clientCluster.getPort();
        }
        else
        {
            this.clientCluster = null;
            this.cloudServer   = null;
        }
    }

    
    /**
     * 设置：云计算服务器的地址端口。格式为：IP:Port。
     * 
     * 默认端口是：1721
     * 
     * @param i_CloudServer
     */
    public void setCloudServer(String i_CloudServer)
    {
        if ( Help.isNull(i_CloudServer) )
        {
            this.clientCluster = null;
            this.cloudServer   = null;
            return;
        }
        
        this.cloudServer = StringHelp.replaceAll(i_CloudServer ,new String[]{"，" ," " ,"\t" ,"\r" ,"\n"} ,new String[]{"," ,""});
        
        String [] v_HostPort = (this.cloudServer.trim() + ":1721").split(":");
        
        if ( Help.isNull(this.cloudVersion) || "RPC".equalsIgnoreCase(this.cloudVersion.trim()) )
        {
            this.clientCluster = new ClientRPC().setHost(v_HostPort[0]).setPort(Integer.parseInt(v_HostPort[1])).setComment(Help.NVL(this.getName() ,this.getComment()));
        }
        else if ( "Socket".equalsIgnoreCase(this.cloudVersion) )
        {
            this.clientCluster = new ClientSocket(v_HostPort[0] ,Integer.parseInt(v_HostPort[1])).setComment(Help.NVL(this.getName() ,this.getComment()));
        }
        else
        {
            throw new java.lang.VerifyError("CloudVersion[" + this.cloudVersion + "] is not valid.");
        }
    }


    /**
     * 获取：XJava对象标识
     */
    public String getXid()
    {
        return xid;
    }

    
    /**
     * 设置：XJava对象标识
     * 
     * @param xid
     */
    public void setXid(String xid)
    {
        this.xid = xid;
    }


    /**
     * 获取：执行的方法名
     */
    public String getMethodName()
    {
        return methodName;
    }

    
    /**
     * 设置：执行的方法名
     * 
     * @param methodName
     */
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

    
    /**
     * 获取：最后一次的执行时间
     */
    public Date getLastTime()
    {
        return lastTime;
    }

    
    /**
     * 设置：最后一次的执行时间
     * 
     * @param lastTime
     */
    public void setLastTime(Date lastTime)
    {
        this.lastTime = lastTime;
    }
    

    
    /**
     * 获取：执行次数
     */
    public long getRunCount()
    {
        return runCount;
    }
    

    
    /**
     * 设置：执行次数
     * 
     * @param runCount
     */
    public void setRunCount(long runCount)
    {
        this.runCount = runCount;
    }
    

    
    /**
     * 获取：执行日志。记录最后1440次内的执行时间
     */
    public Busway<String> getRunLogs()
    {
        return runLogs;
    }
    

    
    /**
     * 设置：执行日志。记录最后1440次内的执行时间
     * 
     * @param runLogs
     */
    public void setRunLogs(Busway<String> runLogs)
    {
        this.runLogs = runLogs;
    }


    
    /**
     * 获取：允许执行的条件。
     * 
     *  表达式中，预定义占位符有（占位符不区分大小写）：
     *    :Y    表示年份
     *    :M    表示月份
     *    :D    表示天
     *    :H    表示小时(24小时制)
     *    :MI   表示分钟
     *    :S    表示秒
     *    :YMD  表示年月日，格式为YYYYMMDD 样式的整数类型。整数类型是为了方便比较
     */
    public String getCondition()
    {
        return condition;
    }


    
    /**
     * 设置：允许执行的条件。
     * 
     *  表达式中，预定义占位符有（占位符不区分大小写）：
     *    :Y    表示年份
     *    :M    表示月份
     *    :D    表示天
     *    :H    表示小时(24小时制)
     *    :MI   表示分钟
     *    :S    表示秒
     *    :YMD  表示年月日，格式为YYYYMMDD 样式的整数类型。整数类型是为了方便比较
     * 
     * @param i_Condition
     */
    public void setCondition(String i_Condition)
    {
        this.condition = Help.NVL(i_Condition).toUpperCase();
        this.condition = StringHelp.replaceAll(this.condition
                                              ,new String[]{
                                                            ":" + $Condition_YMD
                                                           ,":" + $Condition_Week
                                                           ,":" + $Condition_S
                                                           ,":" + $Condition_MI
                                                           ,":" + $Condition_H
                                                           ,":" + $Condition_D
                                                           ,":" + $Condition_D
                                                           ,":" + $Condition_Y
                                                           }
                                              ,new String[]{
                                                            $Condition_YMD
                                                           ,$Condition_Week
                                                           ,$Condition_S
                                                           ,$Condition_MI
                                                           ,$Condition_H
                                                           ,$Condition_D
                                                           ,$Condition_M
                                                           ,$Condition_Y
                                                            });
    }
    
    
    
    /**
     * 是否允许执行
     * 
     * @author      ZhengWei(HY)
     * @createDate  2018-11-29
     * @version     v1.0
     *
     * @param i_Now  当前时间
     * @return
     */
    public boolean isAllow(final Date i_Now)
    {
        if ( this.jobs != null )
        {
            if ( this.jobs.isDisasterRecovery() )
            {
                if ( !this.jobs.isMaster() )
                {
                    if ( this.isSlaveRun() )
                    {
                        // 允许备节点也执行定时任务（须满足基本条件）
                    }
                    else
                    {
                        return Jobs.$JOB_DisasterRecoverys_Check.equals(this.xjavaID);
                    }
                }
            }
        }
        
        if ( Help.isNull(this.condition) )
        {
            return true;
        }
        
        try
        {
            FelContext v_FelContext = new MapContext();
            
            v_FelContext.set($Condition_Y    ,i_Now.getYear());
            v_FelContext.set($Condition_M    ,i_Now.getMonth());
            v_FelContext.set($Condition_D    ,i_Now.getDay());
            v_FelContext.set($Condition_H    ,i_Now.getHours());
            v_FelContext.set($Condition_MI   ,i_Now.getMinutes());
            v_FelContext.set($Condition_S    ,i_Now.getSeconds());
            v_FelContext.set($Condition_YMD  ,Integer.parseInt(i_Now.getYMD_ID()));
            v_FelContext.set($Condition_Week ,i_Now.getWeek());
            
            return (Boolean) $FelEngine.eval(this.condition ,v_FelContext);
        }
        catch (Exception exce)
        {
            throw new RuntimeException("Fel[" + this.condition + "] is error." + exce.getMessage());
        }
    }
    
    
    
    /**
     * 获取：强行执行。即不受时间波动的监管也要执行。哪怕是时间回退了，也要执行
     */
    public boolean isForceRun()
    {
        return forceRun;
    }


    
    /**
     * 设置：强行执行。即不受时间波动的监管也要执行。哪怕是时间回退了，也要执行
     * 
     * @param forceRun
     */
    public void setForceRun(boolean forceRun)
    {
        this.forceRun = forceRun;
    }


    
    /**
     * 获取：备节点是否执行。默认情况下：仅允许主节点执行
     */
    public boolean isSlaveRun()
    {
        return slaveRun;
    }

    

    /**
     * 设置：备节点是否执行。默认情况下：仅允许主节点执行
     * 
     * @param i_SlaveRun 备节点是否执行。默认情况下：仅允许主节点执行
     */
    public void setSlaveRun(boolean i_SlaveRun)
    {
        this.slaveRun = i_SlaveRun;
    }



    /**
     * 注释。可用于日志的输出等帮助性的信息
     * 
     * @param i_Comment
     */
    @Override
    public void setComment(String i_Comment)
    {
        this.comment = i_Comment;
    }
    
    
    
    /**
     * 注释。可用于日志的输出等帮助性的信息
     *
     * @return
     */
    @Override
    public String getComment()
    {
        return Help.NVL(this.comment);
    }


    
    /**
     * 云计算服务器的通讯版本
     *   Socket：表示2021-12-13之前，采用原生Socket实现的通讯技术
     *   RPC：   表示2021-12-13之后，采用Netty实现的通讯技术
     * 
     * 默认值：RPC。
     *
     * @return
     */
    public String getCloudVersion()
    {
        return cloudVersion;
    }

    

    /**
     * 云计算服务器的通讯版本
     *   Socket：表示2021-12-13之前，采用原生Socket实现的通讯技术
     *   RPC：   表示2021-12-13之后，采用Netty实现的通讯技术
     * 
     * 默认值：RPC。
     *
     * @return
     */
    public void setCloudVersion(String i_CloudVersion)
    {
        if ( Help.isNull(i_CloudVersion) || "RPC".equalsIgnoreCase(i_CloudVersion.trim()) )
        {
            this.cloudVersion = "RPC";
        }
        else if ( "Socket".equalsIgnoreCase(i_CloudVersion) )
        {
            this.cloudVersion = "Socket";
        }
        else
        {
            throw new java.lang.VerifyError("CloudVersion[" + this.cloudVersion + "] is not valid.");
        }
    }
    
    
    
    
    /**
     * 执行未成功时，尝试执行的最大次数
     * 
     * @return
     */
    public int getTryMaxCount()
    {
        return tryMaxCount;
    }



    /**
     * 执行未成功时，尝试执行的最大次数
     * 
     * @param tryMaxCount
     */
    public void setTryMaxCount(int tryMaxCount)
    {
        this.tryMaxCount = tryMaxCount;
    }



    /**
     * 执行未成功时，每次尝试间隔时长（秒）
     * 
     * @return
     */
    public int getTryIntervalLen()
    {
        return tryIntervalLen;
    }



    /**
     * 执行未成功时，每次尝试间隔时长（秒）
     * 
     * @param tryIntervalLen
     */
    public void setTryIntervalLen(int tryIntervalLen)
    {
        this.tryIntervalLen = tryIntervalLen;
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



    @Override
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
    
    
    @Override
    public String toString()
    {
        if ( this.nextTime == null )
        {
            return this.getTaskDesc();
        }
        else
        {
            return this.nextTime.getFull() + " " + this.getTaskDesc();
        }
    }

}

