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
 */
public final class Jobs extends Job
{
 
    /** 定时任务服务的灾备机制的心跳任务的XJavaID */
    private static final String  $JOB_DisasterRecoverys_Check = "JOB_DisasterRecoverys_Check";
    
    /** 启动时间。即 startup() 方法的执行时间 */
    private Date                 startTime;
    
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
    
    
    
    public Jobs()
    {
        this.jobList    = new ArrayList<Job>();
        this.jobMonitor = new Counter<String>();
        this.setDesc("Jobs Total scheduling");
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
    private Job createDisasterRecoveryJob()
    {
        Job v_Job = new Job();
        
        v_Job.setXJavaID($JOB_DisasterRecoverys_Check);
        v_Job.setCode(v_Job.getXJavaID());
        v_Job.setName("定时任务服务的灾备机制的心跳任务");
        v_Job.setIntervalType(Job.$IntervalType_Minute);
        v_Job.setIntervalLen(1);
        v_Job.setStartTime("2000-01-01 00:00:00");
        v_Job.setXid(this.getXJavaID());
        v_Job.setMethodName("disasterRecoveryChecks");
        
        return v_Job;
    }
    
    
    
    /**
     * 灾备机制的心跳及设定Master/Slave主从服务
     * 
     * @author      ZhengWei(HY)
     * @createDate  2019-02-21
     * @version     v1.0
     *
     */
    public void disasterRecoveryChecks()
    {
        Map<ClientSocket ,CommunicationResponse> v_ResponseDatas   = ClientSocketCluster.sendCommands(this.disasterRecoverys ,Cluster.getClusterTimeout() ,this.getXJavaID() ,"getStartTime" ,true ,"定时任务服务的灾备心跳");
        Date                                     v_MasterStartTime = null;
        ClientSocket                             v_Master          = null;
        List<ClientSocket>                       v_Slaves          = new ArrayList<ClientSocket>();
        
        for (Map.Entry<ClientSocket ,CommunicationResponse> v_Item : v_ResponseDatas.entrySet())
        {
            CommunicationResponse v_ResponseData = v_Item.getValue();
            
            if ( v_ResponseData.getResult() == 0 )
            {
                if ( v_ResponseData.getData() != null && v_ResponseData.getData() instanceof Date )
                {
                    Date v_StartTime = (Date)v_ResponseData.getData();
                    
                    if ( v_MasterStartTime == null )
                    {
                        v_MasterStartTime = v_StartTime;
                        v_Master          = v_Item.getKey();
                    }
                    else if ( v_MasterStartTime.differ(v_StartTime) > 0 )
                    {
                        v_Slaves.add(v_Master);
                        v_MasterStartTime = v_StartTime;
                        v_Master          = v_Item.getKey();
                    }
                    else
                    {
                        v_Slaves.add(v_Item.getKey());
                    }
                }
            }
        }
        
        if ( !Help.isNull(v_Slaves) )
        {
            ClientSocketCluster.sendCommands(v_Slaves ,Cluster.getClusterTimeout() ,this.getXJavaID() ,"setMaster" ,new Object[]{false} ,true ,"定时任务服务的灾备机制的Slave");
        }
        
        if ( v_Master != null )
        {
            v_Master.sendCommand(this.getXJavaID() ,"setMaster" ,new Object[]{true});
            
            StringBuilder v_Buffer = new StringBuilder();
            v_Buffer.append(Date.getNowTime().getFullMilli());
            v_Buffer.append("定时任务服务的灾备机制的Master：");
            v_Buffer.append(v_Master.getHostName());
            v_Buffer.append(":");
            v_Buffer.append(v_Master.getPort() );
            System.out.println(v_Buffer.toString());
        }
    }
    
    
    
    /**
     * 运行
     */
    public synchronized void startup()
    {
        if ( this.isDisasterRecovery() )
        {
            disasterRecoveryChecks();
            
            if ( !this.isMaster )
            {
                // Master主服务不监控Slave从服务，只有Slave从服务监控主服务的存活性。
                this.jobList.add(this.createDisasterRecoveryJob());
            }
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
        
        TaskPool.putTask(this);
        this.startTime = new Date();
    }
    
    
    
    /**
     * 停止。但不会马上停止所有的线程，会等待已运行中的线程运行完成后，才停止。
     */
    public synchronized void shutdown()
    {
        this.startTime = null;
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
        catch (Exception exce)
        {
            exce.printStackTrace();
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
                catch (Exception exce)
                {
                    System.out.println(exce.getMessage());
                }
            }
        }
        else
        {
            v_Now = v_Now.getFirstTimeOfMinute();
            
            while ( v_Iter.hasNext() )
            {
                try
                {
                    Job  v_Job      = v_Iter.next();
                    Date v_NextTime = v_Job.getNextTime(v_Now);
                    
                    if ( v_NextTime.equalsYMDHM(v_Now) )
                    {
                        if ( v_Job.getLastTime() == null )
                        {
                            if ( v_Job.isAllow(v_Now) )
                            {
                                this.executeJob(v_Job);
                            }
                        }
                        // 预防因主机系统时间不精确，时间同步机制异常（如来回调整时间、时间跳跃、时间波动等），
                        // 造成定时任务重复执行的可能。  ZhengWei(HY) Add 2018-05-22
                        else if ( !v_Job.getLastTime().equalsYMDHM(v_Now) && v_Job.getLastTime().differ(v_Now) < 0 )
                        {
                            if ( v_Job.isAllow(v_Now) )
                            {
                                this.executeJob(v_Job);
                            }
                        }
                    }
                }
                catch (Exception exce)
                {
                    System.out.println(exce.getMessage());
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
            TaskPool.putTask(i_Job);
        }
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
     * @param isMaster 
     */
    public void setMaster(boolean isMaster)
    {
        this.isMaster = isMaster;
        
        if ( this.isMaster )
        {
            for (int v_Index=this.jobList.size() - 1; v_Index>=0; v_Index--)
            {
                if ( $JOB_DisasterRecoverys_Check.equals(this.jobList.get(v_Index).getXJavaID()) )
                {
                    this.jobList.remove(v_Index);
                    return;
                }
            }
        }
    }
    
}
