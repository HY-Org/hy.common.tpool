package org.hy.common.thread;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hy.common.Counter;
import org.hy.common.Date;
import org.hy.common.Help;
import org.hy.common.net.ClientSocket;
import org.hy.common.net.ClientSocketCluster;
import org.hy.common.net.data.CommunicationResponse;
import org.hy.common.xml.log.Logger;
import org.hy.common.xml.plugins.analyse.Cluster;






/**
 * 任务执行程序
 * 
 * @author      ZhengWei(HY)
 * @createDate  2013-12-16
 * @version     v1.0
 *              v2.0  2014-07-21：添加：融合XJava、任务池、线程池的功能
 *              v3.0  2015-11-03：添加：是否在初始时(即添加到Jobs时)，就执行一次任务
 *              v4.0  2016-07-08：添加：支持轮询间隔：秒
 *              v5.0  2018-05-22：添加：预防因主机系统时间不精确，时间同步机制异常（如来回调整时间、时间跳跃、时间波动等），
 *                                     造成定时任务重复执行的可能。
 *              v6.0  2018-11-29  添加：在条件判定为True时，才允许执行任务。并预定义了占位符的标准。
 *                                     可实现如下场景：某任务每天8~18点间周期执行。
 *              v7.0  2019-02-21  添加：定时任务的灾备机制。
 *                                      1. 建立定时任务服务的集群，并区分出集群的Master/Slave主从服务。
 *                                         只允许集群一台为Master主服务执行定时任务，其它Slave从服务作为备机并不执行定时任务。
 * 
 *                                      2. 所有Slave从服务将定时监控Master主服务是否存活。Master主服务不反向监控Slave从服务。
 * 
 *                                      3. 当Master主服务宕机后，从其它正常的Slave从服务中选出一台作为新的Master主服务。
 *                                         接管执行权限，确保定时任务的执行。
 * 
 *                                      4. 同时，新的Master主服务为了性能，也不再监控Slave从服务，再删除定时任务Job。
 * 
 *              v8.0  2020-08-12  添加：判定任务组是否运行中的标记。防止因重复开启Jobs运行造成的紊乱。发现人：张顺
 *              v9.0  2021-04-14  优化：使用专门的（统一的）lastTime来预防时间波动的问题。
 *                                     不再使用 v5.0 版本中，用Job.lastTime 判定的方法。
 */
public final class Jobs extends Job
{
 
    private static final Logger $Logger = Logger.getLogger(Jobs.class ,true);
    
    /** 定时任务服务的灾备机制的心跳任务的XJavaID */
    public static final String  $JOB_DisasterRecoverys_Check = "JOB_DisasterRecoverys_Check";
    
    /** 最后一次成功执行的时间。预防因主机系统时间不精确，时间同步机制异常（如来回调整时间、时间跳跃、时间波动等） */
    private Date                 lastTime;
    
    /** 启动时间。即 startup() 方法的执行时间 */
    private Date                 startTime;
    
    /** 是否启动中。防止重复执行startup()方法 */
    private boolean              isStarting;
    
    /**
     * 所有计划任务配置信息
     */
    private List<Job>            jobList;
    
    /**
     * 正在运行的任务池中的任务运行数量
     * 
     * Key: Job.getCode();
     */
    private Counter<String>      jobMonitor;
    
    /** 最小轮询间隔类型 */
    private int                  minIntervalType;
    
    /**
     * 定时任务的灾备机制：灾备服务器。
     * 灾备服务包括自己在内的所有服务器 。
     * 
     * 哪台服务的 Jobs.startTime 时间最早，即作为Master主服务，其它均为Slave从服务。
     * 只有Master主服务有权执行任务，其它Slave从服务作为灾备服务（仍然计算任务的计划时间，但不执行任务）。
     * 
     * 此属性为：可选项。集合元素个数大于等于2时，灾备机制才生效（其中包括本服务自己，所以只少是2台服务时才生效）。
     */
    private List<ClientSocket>   disasterRecoverys;
    
    /** 是否为Master主服务 */
    private boolean              isMaster;
    
    /** 定时任务服务的灾备机制的Job */
    private Job                  disasterRecoveryJob;
    
    /** 标记 disasterRecoveryJob 是否从 jobList 中移除  */
    private boolean              disasterRecoveryJobIsValid;
    
    /** 灾备检测的最大次数。当连续数次检测到原Master服务的异常时，再由Slave从服务接管Master执行权限。 */
    private int                  disasterCheckMax;
    
    /**
     * Slave从服务变成Master主服务前，Slave从服务每获得一次Master执行权限，此属性++。
     * 
     * 当 masterCount >= disasterCheckMax 时，Slave从服务才真正获得Master执行权限。
     * 在此期间，原Master服务心跳再次成功时，masterCount将变成 0，准备开始重新计值。
     * 
     * 预防某一次心跳检测的异常(如网络原因，原Master服务是正常的)，而造成多台Master服务并存的情况出现。
     */
    private int                  masterCount;
    
    /** 得到Master执行权限的时间点。当为Slave时，此属性为NULL */
    private Date                 masterTime;
    
    
    
    public Jobs()
    {
        this.jobList    = new ArrayList<Job>();
        this.jobMonitor = new Counter<String>();
        this.setDesc("Jobs Total scheduling");
        
        this.disasterCheckMax = 3;
        this.masterCount      = 0;
        this.masterTime       = null;
    }
    
    
    
    /**
     * 创建灾备机制的心跳任务
     * 
     * @author      ZhengWei(HY)
     * @createDate  2019-02-21
     * @version     v1.0
     *
     * @return
     */
    public synchronized Job createDisasterRecoveryJob()
    {
        if ( this.disasterRecoveryJob == null )
        {
            this.disasterRecoveryJob = new Job();
            
            this.disasterRecoveryJob.setXJavaID($JOB_DisasterRecoverys_Check);
            this.disasterRecoveryJob.setCode(this.disasterRecoveryJob.getXJavaID());
            this.disasterRecoveryJob.setName("定时任务服务的灾备机制的心跳任务");
            this.disasterRecoveryJob.setIntervalType(Job.$IntervalType_Minute);
            this.disasterRecoveryJob.setIntervalLen(1);
            this.disasterRecoveryJob.setStartTime("2000-01-01 00:00:00");
            this.disasterRecoveryJob.setXid(this.getXJavaID());
            this.disasterRecoveryJob.setMethodName("disasterRecoveryChecks");
        }
        
        return this.disasterRecoveryJob;
    }
    
    
    
    /**
     * 灾备机制的心跳及设定Master/Slave主从服务
     * 
     * @author      ZhengWei(HY)
     * @createDate  2019-02-21
     * @version     v1.0
     *
     */
    public List<JobDisasterRecoveryReport> disasterRecoveryChecks()
    {
        Map<ClientSocket ,CommunicationResponse> v_ResponseDatas   = ClientSocketCluster.sendCommands(this.disasterRecoverys ,Cluster.getClusterTimeout() ,this.getXJavaID() ,"getDisasterRecoveryReport" ,true ,"定时任务服务的灾备心跳");
        Date                                     v_MasterStartTime = null;
        ClientSocket                             v_Master          = null;
        List<ClientSocket>                       v_Slaves          = new ArrayList<ClientSocket>();
        int                                      v_Succeed         = 0;
        List<JobDisasterRecoveryReport>          v_Reports         = new ArrayList<JobDisasterRecoveryReport>();
        JobDisasterRecoveryReport                v_MasterReport    = null;
        
        
        for (Map.Entry<ClientSocket ,CommunicationResponse> v_Item : v_ResponseDatas.entrySet())
        {
            CommunicationResponse     v_ResponseData = v_Item.getValue();
            JobDisasterRecoveryReport v_Report       = new JobDisasterRecoveryReport();
            
            v_Report.setHostName(v_Item.getKey().getHostName());
            v_Report.setPort(    v_Item.getKey().getPort());
            v_Report.setOK(      false);
            
            if ( v_ResponseData.getResult() == 0 )
            {
                if ( v_ResponseData.getData() != null && v_ResponseData.getData() instanceof JobDisasterRecoveryReport )
                {
                    v_Succeed++;
                    JobDisasterRecoveryReport v_ReportTemp = (JobDisasterRecoveryReport)v_ResponseData.getData();
                    
                    v_Report.setOK(true);
                    v_Report.setStartTime( v_ReportTemp.getStartTime());
                    v_Report.setMasterTime(v_ReportTemp.getMasterTime());
                    
                    if ( v_MasterStartTime == null )
                    {
                        v_MasterStartTime = v_Report.getStartTime();
                        v_Master          = v_Item.getKey();
                        
                        v_Report.setMaster(true);
                        v_MasterReport = v_Report;
                    }
                    else if ( v_MasterReport != null && v_Master != null && v_MasterStartTime.differ(v_Report.getStartTime()) > 0 )
                    {
                        v_Slaves.add(v_Master);
                        v_MasterStartTime = v_Report.getStartTime();
                        v_Master          = v_Item.getKey();
                        
                        v_MasterReport.setMaster(false);
                        v_Report.setMaster(true);
                        v_MasterReport = v_Report;
                    }
                    else
                    {
                        v_Slaves.add(v_Item.getKey());
                    }
                }
            }
            
            v_Reports.add(v_Report);
        }
        
        if ( !Help.isNull(v_Slaves) )
        {
            ClientSocketCluster.sendCommands(v_Slaves ,Cluster.getClusterTimeout() ,this.getXJavaID() ,"setMaster" ,new Object[]{false ,v_Succeed==this.disasterRecoverys.size()} ,true ,"定时任务服务的灾备机制的Slave");
        }
        
        if ( v_Master != null )
        {
            StringBuilder v_Buffer = new StringBuilder();
            v_Buffer.append(Date.getNowTime().getFullMilli());
            v_Buffer.append(" 定时任务服务的灾备机制的Master：");
            v_Buffer.append(v_Master.getHostName());
            v_Buffer.append(":");
            v_Buffer.append(v_Master.getPort());
            $Logger.info(v_Buffer.toString());
            
            v_Master.sendCommand(this.getXJavaID() ,"setMaster" ,new Object[]{true ,v_Succeed==this.disasterRecoverys.size()});
        }
        
        return v_Reports;
    }
    
    
    
    /**
     * 运行
     */
    public synchronized void startup()
    {
        if ( this.isStarting )
        {
            $Logger.info("请误重复启动正在运行中的任务组Jobs。");
            return;
        }
        
        this.isStarting                 = true;
        this.isMaster                   = false;
        this.disasterRecoveryJobIsValid = false;
        this.masterTime                 = null;
        this.masterCount                = 0;
        
        if ( this.isDisasterRecovery() )
        {
            // Master主服务不监控Slave从服务，只有Slave从服务监控主服务的存活性。
            // 但，这里也必须先监控所有Slave从服务，只有监测到所有服务均正常时，才能移除Master主服务上的心跳监测。
            // 这是为了防止双Master服务现像出现。如，在B服务在启动时，A服务已启动在先，但因为网络等原因A服务暂时无法获取心跳监测的反馈。
            // B服务启动时，如果不添加上面断定，当A服务网络恢复后，就有可能出现双Master服务的问题。
            this.addJob(this.createDisasterRecoveryJob());
            this.disasterRecoveryJobIsValid = true;
        }
        
        Help.toSort(this.jobList ,"intervalType");
        
        // 遍历初始一次所有Job的下一次执行时间，防止首次执行时等待2倍的间隔时长
        if ( !Help.isNull(this.jobList) )
        {
            this.minIntervalType = this.jobList.get(0).getIntervalType();
            
            final Date v_Now = new Date();
            for (Job v_Job : this.jobList)
            {
                v_Job.getNextTime(v_Now);
            }
        }
        
        this.ready();
        TaskPool.putTask(this);
        this.startTime = new Date();
    }
    
    
    
    /**
     * 停止。但不会马上停止所有的线程，会等待已运行中的线程运行完成后，才停止。
     */
    public synchronized void shutdown()
    {
        this.isStarting    = false;
        this.startTime     = null;
        this.masterTime    = null;
        this.finishTask();
    }
    
    
    
    /**
     * 为了方便的XJava的配置文件使用
     * 
     * @param i_Job
     */
    public void setAddJob(Job i_Job)
    {
        this.addJob(i_Job);
    }
    
    
    
    public synchronized void addJob(Job i_Job)
    {
        if ( i_Job == null )
        {
            throw new NullPointerException("Job is null.");
        }
        
        if ( Help.isNull(i_Job.getCode()) )
        {
            throw new NullPointerException("Job.getCode() is null.");
        }
        
        i_Job.setMyJobs(this);
        this.jobList.add(i_Job);
        
        // 是否在初始时(即添加到Jobs时)，就执行一次任务
        if ( i_Job.isInitExecute() )
        {
            if ( i_Job.isAtOnceExecute() )
            {
                i_Job.execute();
            }
            else
            {
                this.executeJob(i_Job);
            }
        }
    }
    
    
    
    public synchronized void delJob(Job i_Job)
    {
        if ( i_Job == null )
        {
            throw new NullPointerException("Job is null.");
        }
        
        if ( Help.isNull(i_Job.getCode()) )
        {
            throw new NullPointerException("Job.getCode() is null.");
        }
        
        this.jobList.remove(i_Job);
        this.jobMonitor.remove(i_Job.getCode());
    }
    
    
    
    /**
     * 删除所有定时任务
     * 
     * @author      ZhengWei(HY)
     * @createDate  2018-05-22
     * @version     v1.0
     *
     */
    public synchronized void delJobs()
    {
        this.jobList.clear();
        this.jobMonitor.clear();
    }

    
    
    public Iterator<Job> getJobs()
    {
        return this.jobList.iterator();
    }
    
    
    
    /**
     * 定时触发执行动作的方法
     */
    @Override
    public void execute()
    {
        try
        {
            if ( this.minIntervalType == Job.$IntervalType_Second )
            {
                Thread.sleep(1000);
            }
            else
            {
                // 保证00秒运行
                Date v_Now = new Date();
                Thread.sleep(1000 * (59 - v_Now.getSeconds()) + (1000 - v_Now.getMilliSecond()));
                
                v_Now = new Date();
                while ( v_Now.getSeconds() >= 50 )
                {
                    // 时间同步机制异常（如时间停滞、时间回退、时间跳跃、时间波动等）时，重新sleep  Add 2018-12-05
                    // 也防止sleep(1000)并不是真正的睡眼了1秒。
                    Thread.sleep(1000 * (59 - v_Now.getSeconds()) + (1000 - v_Now.getMilliSecond()));
                    v_Now = new Date();
                }
            }
        }
        catch (Throwable exce)
        {
            $Logger.error(exce);
        }
        
        
        Date          v_Now  = new Date();
        Iterator<Job> v_Iter = this.jobList.iterator();
        
        if ( this.minIntervalType == Job.$IntervalType_Second )
        {
            while ( v_Iter.hasNext() )
            {
                try
                {
                    Job  v_Job      = v_Iter.next();
                    Date v_NextTime = v_Job.getNextTime(v_Now);
                    
                    if ( v_NextTime.equalsYMDHMS(v_Now) )
                    {
                        if ( v_Job.isAllow(v_Now) )
                        {
                            this.executeJob(v_Job);
                        }
                    }
                }
                catch (Throwable exce)
                {
                    $Logger.error(exce);
                }
            }
        }
        else
        {
            v_Now = v_Now.getFirstTimeOfMinute();
            
            // 预防因主机系统时间不精确，时间同步机制异常（如来回调整时间、时间跳跃、时间波动等），
            // 造成定时任务重复执行的可能。  ZhengWei(HY) Add 2018-05-22  优化于： 2021-04-14
            if ( this.lastTime != null )
            {
                long v_Differ = this.lastTime.differ(v_Now);
                if ( !this.lastTime.equalsYMDHM(v_Now) && v_Differ < 0L && v_Differ >= -2 * 60 * 1000L )
                {
                    this.lastTime = v_Now;
                }
                else
                {
                    $Logger.warn("出现时间波动。上次执行时间是：" + this.lastTime.getFull() + "，当前时间是：" + v_Now.getFull());
                    return;
                }
            }
            else
            {
                this.lastTime = v_Now;
            }
            
            
            while ( v_Iter.hasNext() )
            {
                try
                {
                    Job  v_Job      = v_Iter.next();
                    Date v_NextTime = v_Job.getNextTime(v_Now);
                    
                    if ( v_NextTime.equalsYMDHM(v_Now) )
                    {
                        if ( v_Job.isAllow(v_Now) )
                        {
                            this.executeJob(v_Job);
                        }
                    }
                }
                catch (Throwable exce)
                {
                    $Logger.error(exce);
                }
            }
        }
    }
    
    
    
    /**
     * 执行任务
     * 
     * @param i_Job
     */
    private void executeJob(Job i_Job)
    {
        i_Job.setMyJobs(this);
        if ( addMonitor(i_Job) )
        {
            i_Job.ready();
            TaskPool.putTask(i_Job);
        }
    }
    
    
    
    /**
     * 判定Job是否被监控
     * 
     * @author      ZhengWei(HY)
     * @createDate  2019-03-06
     * @version     v1.0
     *
     * @param i_Job
     * @return
     */
    public boolean isMonitor(Job i_Job)
    {
        return this.jobMonitor.containsKey(i_Job.getCode());
    }
    
    
    
    private boolean addMonitor(Job i_DBT)
    {
        return this.monitor(i_DBT ,1);
    }
    
    
    
    /**
     * 注意：delMonitor()方法及monitor()方法不要加同步锁。否则会出现线程阻塞
     */
    public void delMonitor(Job i_Job)
    {
        this.monitor(i_Job ,-1);
    }
    
    
    
    /**
     * 监控。
     * 
     * 控件任务同时运行的线程数
     * 
     * @param i_Job
     * @param i_Type  1:添加监控   -1:删除监控
     * @return
     */
    private boolean monitor(Job i_Job ,int i_Type)
    {
        if ( Help.isNull(i_Job.getCode()) )
        {
            return false;
        }
        
        if ( i_Type == 1 )
        {
            if ( this.jobMonitor.containsKey(i_Job.getCode()) )
            {
                Long v_Count = this.jobMonitor.get(i_Job.getCode());
                
                if ( v_Count.longValue() < i_Job.getTaskCount() )
                {
                    this.jobMonitor.put(i_Job.getCode());
                }
                else
                {
                    return false;
                }
            }
            else
            {
                this.jobMonitor.put(i_Job.getCode());
            }
        }
        else
        {
            if ( this.jobMonitor.containsKey(i_Job.getCode()) )
            {
                this.jobMonitor.putMinus(i_Job.getCode());
            }
            else
            {
                return false;
            }
        }
        
        return true;
    }
    
    
    
    /**
     * 获取：启动时间。即 startup() 方法的执行时间
     */
    public Date getStartTime()
    {
        return startTime;
    }
    
    
    
    /**
     * 获取：是否启动中。防止重复执行startup()方法
     */
    public boolean isStarting()
    {
        return isStarting;
    }



    /**
     * 是否启动的灾备机制
     * 
     * @author      ZhengWei(HY)
     * @createDate  2019-02-21
     * @version     v1.0
     *
     * @return
     */
    public boolean isDisasterRecovery()
    {
        if ( Help.isNull(this.disasterRecoverys) )
        {
            return false;
        }
        else if ( this.disasterRecoverys.size() >= 2 )
        {
            return true;
        }
        else
        {
            return false;
        }
    }
    
    
    
    /**
     * 获取：定时任务的灾备机制：灾备服务器。
     * 灾备服务包括自己在内的所有服务器 。
     * 
     * 哪台服务的 Jobs.startTime 时间最早，即作为Master主服务，其它均为Slave从服务。
     * 只有Master主服务有权执行任务，其它Slave从服务作为灾备服务（仍然计算任务的计划时间，但不执行任务）。
     * 
     * 此属性为：可选项。集合元素个数大于等于2时，灾备机制才生效（其中包括本服务自己，所以只少是2台服务时才生效）。
     */
    public List<ClientSocket> getDisasterRecoverys()
    {
        return disasterRecoverys;
    }
    

    
    /**
     * 设置：定时任务的灾备机制：灾备服务器。
     * 灾备服务包括自己在内的所有服务器 。
     * 
     * 哪台服务的 Jobs.startTime 时间最早，即作为Master主服务，其它均为Slave从服务。
     * 只有Master主服务有权执行任务，其它Slave从服务作为灾备服务（仍然计算任务的计划时间，但不执行任务）。
     * 
     * 此属性为：可选项。集合元素个数大于等于2时，灾备机制才生效（其中包括本服务自己，所以只少是2台服务时才生效）。
     * 
     * @param disasterRecoverys
     */
    public void setDisasterRecoverys(List<ClientSocket> disasterRecoverys)
    {
        this.disasterRecoverys = disasterRecoverys;
    }
    


    /**
     * 获取：是否为Master主服务
     */
    public boolean isMaster()
    {
        return isMaster;
    }
    
    
    
    /**
     * 设置：是否为Master主服务
     * 
     * @param i_IsMaster  本服务是否为Master主服务
     * @param i_AllOK     是否所有服务均正常
     */
    public synchronized void setMaster(boolean i_IsMaster ,boolean i_AllOK)
    {
        if ( i_IsMaster && i_AllOK )
        {
            // 当所有服务均正常时，判定出的Master主服务，才是真正的主服务，这时才能移除主服务对其它Slave从服务的监测
            if ( this.disasterRecoveryJobIsValid )
            {
                this.disasterRecoveryJobIsValid = false;
                if ( this.disasterRecoveryJob != null )
                {
                    // 这里没有直接调用 delJob(this.disasterRecoveryJob) 的原因是：delJob 方法也是同步的。
                    this.jobList.remove(this.disasterRecoveryJob);
                    this.jobMonitor.remove(this.disasterRecoveryJob.getCode());
                }
                $Logger.info("在所有服务的同意下，本服务接管定时任务的执行权限。");
            }
            this.masterCount = 0;
        }
        else if ( i_IsMaster && !this.isMaster )
        {
            this.masterCount++;
            if ( this.masterCount < this.disasterCheckMax )
            {
                $Logger.info("本服务第 " + this.masterCount +" 次准备接管定时任务的执行权限，共准备 " + this.disasterCheckMax + " 次。");
                return;
            }
            
            $Logger.info("本服务在第 " + this.masterCount + " 次正式接管定时任务的执行权限。");
            this.masterCount = 0;
        }
        else if ( !i_IsMaster )
        {
            this.masterCount = 0;
        }
        
        this.isMaster = i_IsMaster;
        if ( this.isMaster )
        {
            if ( this.masterTime == null )
            {
                this.masterTime = new Date();
            }
        }
        else
        {
            this.masterTime = null;
        }
    }


    
    /**
     * 获取：灾备检测的最大次数。当连续数次检测到原Master服务的异常时，再由Slave从服务接管Master执行权限。
     */
    public int getDisasterCheckMax()
    {
        return disasterCheckMax;
    }
    

    
    /**
     * 设置：灾备检测的最大次数。当连续数次检测到原Master服务的异常时，再由Slave从服务接管Master执行权限。
     * 
     * @param disasterCheckMax
     */
    public void setDisasterCheckMax(int disasterCheckMax)
    {
        this.disasterCheckMax = disasterCheckMax;
    }


    
    /**
     * 获取：得到Master执行权限的时间点。当为Slave时，此属性为NULL
     */
    public Date getMasterTime()
    {
        return masterTime;
    }
    
    
    
    /**
     * 获取本机的灾备报告
     * 
     * @author      ZhengWei(HY)
     * @createDate  2019-03-01
     * @version     v1.0
     *
     * @return
     */
    public JobDisasterRecoveryReport getDisasterRecoveryReport()
    {
        JobDisasterRecoveryReport v_Report = new JobDisasterRecoveryReport();
        
        v_Report.setOK(        true);
        v_Report.setMaster(    this.isMaster);
        v_Report.setMasterTime(this.masterTime);
        v_Report.setStartTime( this.startTime);
        
        return v_Report;
    }
    
}
