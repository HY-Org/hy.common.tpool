package org.hy.common.thread;

import org.hy.common.Date;
import org.hy.common.StringHelp;
import org.hy.common.thread.ui.ThreadPoolWatch;
import org.hy.common.thread.ui.WatchTableColumnIndex;
import org.hy.common.xml.log.Logger;





/**
 * 通用线程类
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2011-06-08
 *           V1.1  2018-08-24  修复：防止在队列中长时间等待的线程，在濒临死亡的一瞬间，被当作空闲线程使用
 */
public class ThreadBase
{
    private static final Logger $Logger = Logger.getLogger(ThreadBase.class ,true);
    
    protected static long SERIALNO = 0;
    
    
    /** 线程编号 */
    protected String                   threadNo;
    
    /** 线程实例对象 */
    protected Thread                   thread;
    
    /** 内核线程对象 */
    protected CoreThread               coreThread;
    
    /** 任务对象 */
    protected Task<?>                  taskObject;
    
    /** 前一个任务对象的任务类型 */
    protected String                   oldTaskType;
    
    /** 线程是否在运行 */
    protected boolean                  isRun      = false;
    
    /** 线程是否暂停(挂起) */
    protected boolean                  isPause    = false;
    
    /** 是否有任务在身 */
    protected boolean                  isHaveTask = false;
    
    /** 线程运行状态 */
    protected ThreadRunStatus          threadRunStatus;
    
    /** 等待时间间隔(单位：毫秒) */
    protected long                     intervalTime;
    
    /** 空闲多少时间后线程自毁(单位：秒) */
    protected long                     idleTimeKill;
    
    /** 执行任务次数 */
    protected long                     executeTaskCount;
    
    /** 任务开始执行时间 */
    protected Date                     taskStartTime;
    
    /** 任务结束时间 */
    protected Date                     taskEndTime;
    
    /** 所有任务累计用时 */
    protected long                     totalTime;
    
    /** 线程在视窗化窗口的表格中的行位置(下标从零开始) */
    protected int                      watchTableRowIndex;
    
    /** 线程附加缓存。是预留给继承者使用的。继承者可以有选择的使用。 */
    protected Object                   cache;
    
    
    
    protected synchronized static long getSerialNo()
    {
        return ++SERIALNO;
    }
    
    
    
    public ThreadBase()
    {
        this(null);
    }
    
    
    public ThreadBase(Task<?> i_TaskObject)
    {
        this(i_TaskObject ,100 ,60);
    }
    
    
    /**
     * 构造器
     * 
     * @param i_TaskObject    任务对象
     * @param i_IntervalTime  等待时间间隔(单位：毫秒) 默认为：100毫秒
     * @param i_IdleTimeKill  空闲多少时间后线程自毁(单位：秒) 默认为：60秒
     */
    public ThreadBase(Task<?> i_TaskObject ,long i_IntervalTime ,long i_IdleTimeKill)
    {
        this.setIdleTimeKill(i_IdleTimeKill);
        this.setIntervalTime(i_IntervalTime);
        
        this.threadNo          = "线程号-" + getSerialNo();
        this.executeTaskCount  = 0;
        this.totalTime         = 0;
        this.threadRunStatus   = ThreadRunStatus.$Init;
        this.taskStartTime     = null;
        this.taskEndTime       = null;
        
        
        if ( ThreadPool.isWatch() )
        {
            this.watchTableRowIndex = ThreadPoolWatch.getInstance().addRow(this);
        }
        this.setTaskObject(i_TaskObject);
    }
    
    
    public String getThreadNo()
    {
        return this.threadNo;
    }
    
    
    public String getOldTaskType()
    {
        return this.oldTaskType;
    }
    
    
    public Object getCache()
    {
        return cache;
    }


    public void setCache(Object cache)
    {
        this.cache = cache;
    }


    public long getExecuteTaskCount()
    {
        return this.executeTaskCount;
    }
    
    
    public Date getTaskStartTime()
    {
        return this.taskStartTime;
    }
    
    
    public Date getTaskEndTime()
    {
        return this.taskEndTime;
    }
    
    
    public long getTotalTime()
    {
        return this.totalTime;
    }
    
    
    public String getTotalTimeSec()
    {
        return StringHelp.doubleParse(this.totalTime / 1000D, 2);
    }
    

    public long getIntervalTime()
    {
        return intervalTime;
    }


    public final void setIntervalTime(long i_IntervalTime)
    {
        if ( i_IntervalTime > 0 )
        {
            this.intervalTime = i_IntervalTime;
        }
    }
    

    public long getIdleTimeKill()
    {
        return idleTimeKill;
    }


    public final void setIdleTimeKill(long idleTimeKill)
    {
        if ( idleTimeKill > 0 )
        {
            this.idleTimeKill = idleTimeKill;
        }
    }
    
    
    public String getThreadRunStatusName()
    {
        return threadRunStatus.getName();
    }


    public synchronized ThreadRunStatus getThreadRunStatus()
    {
        return threadRunStatus;
    }
    
    
    /**
     * 内部使用
     * 
     * @param i_ThreadTaskRunStatus
     */
    protected synchronized void setThreadRunStatus(ThreadRunStatus i_ThreadTaskRunStatus)
    {
        setThreadRunStatus_NoSync(i_ThreadTaskRunStatus);
    }
    
    
    /**
     * 内部使用的，并且调用此方法的父方法有同步锁synchronized，所以此方法不再添加同步锁synchronized。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2018-08-23
     * @version     v1.0
     *
     * @param i_ThreadTaskRunStatus
     */
    private void setThreadRunStatus_NoSync(ThreadRunStatus i_ThreadTaskRunStatus)
    {
        this.threadRunStatus = i_ThreadTaskRunStatus;
        
        if ( this.threadRunStatus.equals(ThreadRunStatus.$Working) )
        {
            this.taskStartTime = new Date();
            this.taskEndTime   = null;
        }
        else if ( this.threadRunStatus.equals(ThreadRunStatus.$Finish) )
        {
            this.taskEndTime = new Date();
            this.totalTime   = this.totalTime + (this.taskEndTime.getTime() - this.taskStartTime.getTime());
        }
        
        if ( ThreadPool.isWatch() )
        {
            // 及时更新视窗化监视窗口的信息
            ThreadPoolWatch.getInstance().updateRow(this.watchTableRowIndex ,WatchTableColumnIndex.$RunStatus ,this.threadRunStatus.toString());
            
            if ( this.threadRunStatus.equals(ThreadRunStatus.$Finish) )
            {
                String v_LastTime = " ";
                if ( this.taskEndTime != null )
                {
                    v_LastTime = this.taskEndTime.getFullMilli();
                }
                else if ( this.taskStartTime != null )
                {
                    v_LastTime = this.taskStartTime.getFullMilli();
                }
                ThreadPoolWatch.getInstance().updateRow(this.watchTableRowIndex ,WatchTableColumnIndex.$ExecCount ,String.valueOf(this.executeTaskCount));
                ThreadPoolWatch.getInstance().updateRow(this.watchTableRowIndex ,WatchTableColumnIndex.$TotalTime ,this.getTotalTimeSec());
                ThreadPoolWatch.getInstance().updateRow(this.watchTableRowIndex ,WatchTableColumnIndex.$LastTime  ,v_LastTime);
                ThreadPoolWatch.getInstance().updateRow(this.watchTableRowIndex ,WatchTableColumnIndex.$TaskDesc  ,this.taskObject.getTaskDesc());
            }
        }
    }

    
    public Task<?> getTaskObject()
    {
        return taskObject;
    }


    public final void setTaskObject(Task<?> taskObject)
    {
        this.taskObject = taskObject;
        
        if ( ThreadPool.isWatch() )
        {
            // 及时更新视窗化监视窗口的信息
            if ( this.taskObject != null )
            {
                this.taskObject.setThread(this);
                
                String v_LastTime = " ";
                if ( this.taskEndTime != null )
                {
                    v_LastTime = this.taskEndTime.getFullMilli();
                }
                else if ( this.taskStartTime != null )
                {
                    v_LastTime = this.taskStartTime.getFullMilli();
                }
                
                ThreadPoolWatch.getInstance().updateRow(this.watchTableRowIndex ,WatchTableColumnIndex.$TaskName ,this.taskObject.getTaskName());
                ThreadPoolWatch.getInstance().updateRow(this.watchTableRowIndex ,WatchTableColumnIndex.$LastTime ,v_LastTime);
                ThreadPoolWatch.getInstance().updateRow(this.watchTableRowIndex ,WatchTableColumnIndex.$TaskDesc ,this.taskObject.getTaskDesc());
            }
            else
            {
                ThreadPoolWatch.getInstance().updateRow(this.watchTableRowIndex ,WatchTableColumnIndex.$TaskName ," ");
                ThreadPoolWatch.getInstance().updateRow(this.watchTableRowIndex ,WatchTableColumnIndex.$LastTime ," ");
                ThreadPoolWatch.getInstance().updateRow(this.watchTableRowIndex ,WatchTableColumnIndex.$TaskDesc ," ");
            }
        }
        else
        {
            if ( this.taskObject != null )
            {
                this.taskObject.setThread(this);
            }
        }
    }
    
    
    
    /**
     * 刷新一下监视窗口中的信息
     */
    public void refreshWatchInfo()
    {
        if ( ThreadPool.isWatch() )
        {
            String v_LastTime = " ";
            if ( this.taskEndTime != null )
            {
                v_LastTime = this.taskEndTime.getFullMilli();
            }
            else if ( this.taskStartTime != null )
            {
                v_LastTime = this.taskStartTime.getFullMilli();
            }
            
            ThreadPoolWatch.getInstance().updateRow(this.watchTableRowIndex ,WatchTableColumnIndex.$RunStatus ,this.threadRunStatus.toString());
            ThreadPoolWatch.getInstance().updateRow(this.watchTableRowIndex ,WatchTableColumnIndex.$ExecCount ,String.valueOf(this.executeTaskCount));
            ThreadPoolWatch.getInstance().updateRow(this.watchTableRowIndex ,WatchTableColumnIndex.$TotalTime ,this.getTotalTimeSec());
            ThreadPoolWatch.getInstance().updateRow(this.watchTableRowIndex ,WatchTableColumnIndex.$TaskName  ,this.taskObject.getTaskName());
            ThreadPoolWatch.getInstance().updateRow(this.watchTableRowIndex ,WatchTableColumnIndex.$LastTime  ,v_LastTime);
            ThreadPoolWatch.getInstance().updateRow(this.watchTableRowIndex ,WatchTableColumnIndex.$TaskDesc  ,this.taskObject.getTaskDesc());
        }
    }
    
    
    public boolean isRun()
    {
        return isRun;
    }
    
    
    public boolean isPause()
    {
        return isPause;
    }
    
    
    public boolean isHaveTask()
    {
        return isHaveTask;
    }
    
    
    // 待删除
    @Deprecated
    public void notPause()
    {
//      if ( !this.isPause )
//      {
//          this.isPause = true;
//      }
    }
    
    
    // 待删除
    @Deprecated
    public void newTask()
    {
//      this.isHaveTask = true;
    }
    
    
    // 待删除
    @Deprecated
    public void newTask(Task<?> i_TaskObject)
    {
//      this.isHaveTask = true;
//      this.setTaskObject(i_TaskObject);
    }
    
    
    // 待删除
    @Deprecated
    public void stopRun()
    {
//      this.isRun = false;
    }
    
    
    /**
     * 启动线程
     * 
     * 指线程已运行，但具体任务还没有执行，需要调用 executeTask() 方法执行。
     * 
     * @return
     */
    public boolean startup()
    {
        if ( this.isRun() )
        {
            return true;
        }
        else
        {
            return this.changeStatus(ThreadControlStatus.$Starting);
        }
    }
    
    
    /**
     * 启动线程并执行任务
     * 
     */
    public synchronized boolean startupAndExecuteTask()
    {
        boolean v_Ret = false;
        
        v_Ret = this.startup();
        
        if ( v_Ret )
        {
            v_Ret = this.executeTask();
        }
        
        return v_Ret;
    }
    
    
    /**
     * 执行任务
     * 
     * @return
     */
    public boolean executeTask()
    {
        return this.changeStatus(ThreadControlStatus.$Executing);
    }

    
    /**
     * 任务执行完成
     * 
     * @return
     */
    public boolean finishTask()
    {
        return this.changeStatus(ThreadControlStatus.$Finishing);
    }
    
    
    /**
     * 线程空闲
     * 
     * @return
     */
    public boolean rest()
    {
        return this.changeStatus(ThreadControlStatus.$Resting);
    }
    
    
    /**
     * 关闭线程
     * 
     * @return
     */
    public boolean shutdown()
    {
        return this.changeStatus(ThreadControlStatus.$Shutdowning);
    }
    
    
    /**
     * 设置线程状态的统一方法（对所有public、protected方法）。
     * 
     * 注意：只能用这个方法设置，不能直接访问具体的变量进行设置
     * 
     * 返回值表示：是否设置成功
     */
    protected synchronized boolean changeStatus(ThreadControlStatus i_ThreadStatus)
    {
        boolean v_Ret = false;
        
        if ( ThreadControlStatus.$Starting.equals(i_ThreadStatus) )
        {
            v_Ret = this.toStartupStatus();
            
            if ( v_Ret )
            {
                if ( this.thread == null )
                {
                    this.coreThread = new CoreThread(this);
                    
                    this.thread = new Thread(this.coreThread);
                    
                    this.thread.start();
                }
            }
        }
        else if ( ThreadControlStatus.$Executing.equals(i_ThreadStatus) )
        {
            v_Ret = this.toExecuteStatus();
        }
        else if ( ThreadControlStatus.$Finishing.equals(i_ThreadStatus) )
        {
            v_Ret = this.toFinishStatus();
            
            if ( v_Ret )
            {
                this.setThreadRunStatus_NoSync(ThreadRunStatus.$Finish);
                this.executeTaskCount++;
                ThreadPool.addIdleThread(this);
            }
        }
        else if ( ThreadControlStatus.$Resting.equals(i_ThreadStatus) )
        {
            v_Ret = this.toRestStatus();
            
            if ( v_Ret )
            {
                this.setThreadRunStatus_NoSync(ThreadRunStatus.$Rest);
                ThreadPool.addIdleThread(this);
            }
        }
        else if ( ThreadControlStatus.$Shutdowning.equals(i_ThreadStatus) )
        {
            int v_RetCode = ThreadPool.killMySelf(this);
            
            if ( v_RetCode == 0 )
            {
                v_Ret = this.toShutdownStatus();
            }
            else
            {
                return false;
            }
            
            
            if ( v_Ret )
            {
                this.setThreadRunStatus_NoSync(ThreadRunStatus.$Kill);
                
                if ( this.taskObject != null )
                {
                    this.taskObject = null;
                }
                
                if ( this.thread != null )
                {
                    try
                    {
                        if ( !this.thread.isInterrupted() )
                        {
                            this.thread.interrupt();
                        }
                    }
                    catch (Throwable exce)
                    {
                        $Logger.error(exce);
                    }

                    this.thread = null;
                }
                
                if ( this.coreThread != null )
                {
                    this.coreThread = null;
                }
            }
        }
        
        return v_Ret;
    }
    
    
    /**
     * 变为启动状态，空转不执行任务
     * 
     * 不用加 synchronized 同步锁，因为调用执行本的方法已经是同步的，并且只有一处调用执行此方法
     */
    private boolean toStartupStatus()
    {
        return this.setAllStatus(false ,false ,false
                                ,true  ,true  ,false);               // 【运行】线程，【无任务】，为【暂停】空转的状态
    }
    
    
    /**
     * 变为执行状态
     * 
     * 不用加 synchronized 同步锁，因为调用执行本的方法已经是同步的，并且只有一处调用执行此方法
     */
    private boolean toExecuteStatus()
    {
        return this.setAllStatus(true  ,true  ,false
                                ,true  ,false ,true);                // 【运行】线程，【有任务】，从【暂停】转【执行】的状态
    }
    
    
    /**
     * 变为执行完成状态
     * 
     * 不用加 synchronized 同步锁，因为调用执行本的方法已经是同步的，并且只有一处调用执行此方法
     */
    private boolean toFinishStatus()
    {
        return this.setAllStatus(true  ,false ,true
                                ,true  ,true  ,false);               // 【运行】线程，【无任务】，从【执行】转【暂停】的状态
    }
    
    
    /**
     * 变为空闲状态
     * 
     * 不用加 synchronized 同步锁，因为调用执行本的方法已经是同步的，并且只有一处调用执行此方法
     */
    private boolean toRestStatus()
    {
        return this.setAllStatus(true  ,false ,true
                                ,true  ,true  ,false);               // 【运行】线程，【无任务】，从【执行】转【暂停】的状态
    }
    
    
    /**
     * 变为停止运行状态
     * 
     * 不用加 synchronized 同步锁，因为调用执行本的方法已经是同步的，并且只有一处调用执行此方法
     */
    private boolean toShutdownStatus()
    {
        boolean v_Ret = this.setAllStatus(true  ,false ,true
                                         ,false ,true  ,false);      // 【执行中】转【停止】线程
        
        if ( v_Ret )
        {
            return true;
        }
        
        return this.setAllStatus(true  ,true  ,false
                                ,false ,true  ,false);               // 【运行】    转【停止】线程
                                                                     // 【执行完成】转【停止】线程
    }
    
    
    /**
     * 设置线程所有标记的统一方法（对所有private方法）。
     * 
     * 注意：只能用这个方法设置，不能直接访问具体的变量进行设置
     * 
     * 在设置前，须进行验证。验证通过对所有标记进行设置，只要有一个标记没有验证通过，侧所有标记都不进行设置
     * 
     * 返回值表示：是否设置成功
     * 
     * 不用加 synchronized 同步锁，因为调用执行本的方法已经是同步的，并且只有一处调用执行此方法
     */
    private boolean setAllStatus(boolean i_Old_IsRun
                                ,boolean i_Old_IsPause
                                ,boolean i_Old_IsHaveTask
                                ,boolean i_New_IsRun
                                ,boolean i_New_IsPause
                                ,boolean i_New_IsHaveTask)
    {
        if ( i_Old_IsRun == this.isRun )
        {
            if ( i_Old_IsPause == this.isPause )
            {
                if ( i_Old_IsHaveTask == this.isHaveTask )
                {
                    this.isRun      = i_New_IsRun;
                    this.isPause    = i_New_IsPause;
                    this.isHaveTask = i_New_IsHaveTask;
                    
                    return true;
                }
            }
        }
        
        return false;
    }
    
    
    
    /**
     * 设置内核线程，重新计算其空闲时间
     * 
     * @author      ZhengWei(HY)
     * @createDate  2018-08-24
     * @version     v1.0
     *
     * @return
     */
    public boolean coreIdleTimeRecalculation()
    {
        if ( this.coreThread != null )
        {
            this.coreThread.setIdleBeginTime(0);
            return true;
        }
        
        return false;
    }

    
    
    @Override
    protected void finalize() throws Throwable
    {
        super.finalize();
        this.shutdown();
    }





    class CoreThread implements Runnable
    {
        private ThreadBase myThreadBase;
        
        private long       idleBeginTime;
        
            
        public CoreThread(ThreadBase i_MyThreadBase)
        {
            this.myThreadBase  = i_MyThreadBase;
            this.idleBeginTime = 0;
        }
        
        
        @Override
        public void run()
        {
            this.idleBeginTime = 0;
            
            while ( this.myThreadBase.isRun() )
            {
                if ( this.myThreadBase.isPause() || !this.myThreadBase.isHaveTask() )
                {
                    try
                    {
                        // 记录空闲时间，并在空闲时间最大值后，线程自毁
                        if ( this.idleBeginTime == 0 )
                        {
                            this.idleBeginTime = System.currentTimeMillis();
                            this.myThreadBase.setThreadRunStatus(ThreadRunStatus.$Rest);
                        }
                        else
                        {
                            // 判断是否达到空闲时间后线程自毁
                            if ( (System.currentTimeMillis() - this.idleBeginTime) >= (this.myThreadBase.getIdleTimeKill() * 1000) )
                            {
                                if ( this.myThreadBase.shutdown() )
                                {
                                    return;
                                }
                                else
                                {
                                    // 如果没有销毁线程，就重新计算其空闲时间
                                    // 防止在队列中长时间等待的线程，在濒临死亡的一瞬间，被当作空闲线程使用  ZhengWei(HY) Add 2018-08-24
                                    this.idleBeginTime = 0;
                                }
                            }
                        }
                    }
                    catch (Throwable exce)
                    {
                        $Logger.error(exce);
                    }
                    
                    ThreadPool.sleep(this.myThreadBase.getIntervalTime());
                }
                else
                {
                    try
                    {
                        if ( this.myThreadBase.getTaskObject() != null )
                        {
                            this.idleBeginTime = 0;
                            this.myThreadBase.setThreadRunStatus(ThreadRunStatus.$Working);
                            this.myThreadBase.oldTaskType = new String(this.myThreadBase.getTaskObject().getTaskType());
                            
                            
                            // 执行任务
                            this.myThreadBase.taskObject.run();
                            
                            
                            this.myThreadBase.setThreadRunStatus(ThreadRunStatus.$Finish);
                        }
                        else
                        {
                            this.myThreadBase.rest();
                        }
                    }
                    catch (Exception exce)
                    {
                        this.myThreadBase.rest();
                        $Logger.error(exce);
                    }
                }
                
            }
            
        }

        
        public long getIdleBeginTime()
        {
            return idleBeginTime;
        }
        
        
        public void setIdleBeginTime(long idleBeginTime)
        {
            this.idleBeginTime = idleBeginTime;
        }
        
    }
    
}
