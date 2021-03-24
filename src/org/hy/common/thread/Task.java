package org.hy.common.thread;

import org.hy.common.Help;





/**
 * 任务对象
 * 
 * <O>指线程自有的缓存的对象类型
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2011-06-08
 *           V2.0  2017-02-21  添加：isStop状态控制属性。任务是否停止执行。任务还未绑定线程执行前被停止。
 *                                  对于已绑定线程正在执行中的是不生效的。
 */
public abstract class Task<O> implements Runnable
{
    
    /** 执行任务的线程。为了安全与功能相关独立，不对实现类与子类开放此属性 */
    private   ThreadBase     thread;
    
    /** 任务组。可选属性，即任务可以在没有组的情况独立运行。为了安全与功能相关独立，不对实现类与子类开放此属性 */
    private   TaskGroup      taskGroup;
    
    /** 任务编号 */
    protected long           taskNo;
    
    /** 任务的名称 */
    protected String         taskName;
    
    /** 任务的类型 */
    protected String         taskType;
    
    /** 任务是否完成。为了安全与功能相关独立，不对实现类与子类开放此属性  */
    private   boolean        isFinish;
    
    /** 
     * 任务是否停止执行。任务还未绑定线程执行前被停止。
     * 对于已绑定线程正在执行中的是不生效的。
     * 默认为:false。
     */
    private   boolean        isStop;
    
    /** 窗口输出的缓存 */
    protected StringBuilder  printBuffer;
    
    /** SQL语句的缓存 */
    protected StringBuilder  sqlBuffer;
    
    
    
    /**
     * 获取任务描述
     * 
     * @return
     */
    public abstract String getTaskDesc();
    
    
    
    /**
     * 执行任务的方法
     * 
     * 常规情况下：一定要在 execute()执行后，调用 this.finishTask(); 方法。
     *           即使 execute() 异常，也应当调用  this.finishTask(); 方法。
     *           所以，建议如下方法写代码
     *           try 
     *           {
     *               ... ...
     *           } 
     *           finally 
     *           {
     *               this.finishTask();
     *           }
     */
    public abstract void execute();
    
    
    
    /**
     * 获取任务编号。
     * 
     * 因为每个任务对象都应当有独立的编号顺序。
     * 即每个Task实现类，实例化的第一个类的编号应当都从0开始编号，所以这个工作就由实现者来完成。
     * 
     * @return
     */
    public abstract long getSerialNo();
    
    
    
    /**
     * 构造器
     * 
     * @param i_TaskType  任务类型
     */
    public Task(String i_TaskType)
    {
        if ( Help.isNull(i_TaskType) )
        {
            throw new NullPointerException("Task Type is null");
        }
        
        this.taskType    = i_TaskType;
        this.taskNo      = this.getSerialNo();
        this.taskName    = this.taskType + "-" + this.taskNo; 
        this.isFinish    = false;
        this.isStop      = false;
        
        this.printBuffer = new StringBuilder();
        this.sqlBuffer   = new StringBuilder();
    }
    
    
    
    /**
     * 不写被继承或重写的方法。
     * 对于实现类，请使用 this.execute() 方法
     */
    public final void run()
    {
        if ( this.isStop )
        {
            return;
        }
        
        this.execute();
    }
    
    
    
    /**
     * 设置与任务对应的执行线程。
     * 
     * 为了安全与功能相关独立，不对外提供 getThread() 方法。
     * 
     * @param thread
     */
    public void setThread(ThreadBase thread) 
    {
        this.thread = thread;
    }
    
    
    
    /**
     * 设置任务组。
     * 
     * 当任务添加到任务组中时，任务组默认会调用 setTaskGroup() 方法，将任务组自己设置为任务的任务组属性。
     * 即，一般不用主动调用此方法。
     * 
     * @param i_TaskGroup
     */
    public void setTaskGroup(TaskGroup i_TaskGroup)
    {
        this.taskGroup = i_TaskGroup;
    }
    
    
    
    /**
     * 获取：任务组。可选属性，即任务可以在没有组的情况独立运行。为了安全与功能相关独立，不对实现类与子类开放此属性
     */
    public TaskGroup getTaskGroup()
    {
        return this.taskGroup;
    }

    

    /**
     * 获取任务的名称
     * 
     * @return
     */
    public String getTaskName() 
    {
        return taskName;
    }
    
    
    
    /**
     * 设置任务的名称。
     * 
     * 即使不由外围程序设置任务名称，它也有默认的名称。
     * 
     * @param taskName
     */
    public void setTaskName(String taskName) 
    {
        this.taskName = taskName;
    }
    
    
    
    /**
     * 获取任务的编号
     * 
     * @return
     */
    public long getTaskNo() 
    {
        return taskNo;
    }
    
    
    
    /**
     * 获取任务类型
     * 
     * @return
     */
    public String getTaskType()
    {
        return this.taskType;
    }
    
    
    /**
     * 获取任务的运行信息。
     * 主要用于窗口的显示。
     * 对于同一类型任务，会等同类任务都执行完成后，再统一顺序的显示其运行信息。
     * 
     * @return
     */
    public String getTaskRunInfo()
    {
        return this.printBuffer.toString();
    }
    
    
    
    /**
     * 获取任务的运行SQL语句。
     * 主要用于数据库记录。
     * 对于同一类型任务，会等同类任务都执行完成后，再统一顺序的执行其SQL。
     * 
     * @return
     */
    public String getTaskRunSQL()
    {
        return this.sqlBuffer.toString();
    }
    
    
    
    /**
     * 清空 printBuffer 和 sqlBuffer 中的缓存内容
     */
    public void clearBuffer()
    {
        this.printBuffer = new StringBuilder();
        this.sqlBuffer   = new StringBuilder();
    }
    
    
    
    /**
     * 设置线程自有的缓存
     * 
     * 建议在线程执行中使用，在线程执行完成后，不要在使用了。
     * 因为，某些瞬间，任务对应的线程已分配给其它任务。
     * 
     * @param i_Obj
     */
    public void setThreadCache(O i_Obj)
    {
        if ( this.thread != null )
        {
            this.thread.setCache(i_Obj);
        }
    }
    
    
    
    /**
     * 刷新一下监视窗口中的信息
     */
    public void refreshWatchInfo()
    {
        if ( this.thread != null )
        {
            this.thread.refreshWatchInfo();
        }
    }
    
    
    
    /**
     * 获取线程附加缓存。是预留给继承者使用的。继承者可以有选择的使用。
     * 
     * 建议在线程执行中使用，在线程执行完成后，不要在使用了。
     * 因为，某些瞬间，任务对应的线程已分配给其它任务。
     */
    @SuppressWarnings("unchecked")
    public O getThreadCache()
    {
        Object v_Ret = null;
        
        if ( this.thread != null )
        {
            v_Ret = this.thread.getCache();
        }
        else
        {
            return null;
        }
        
        if ( v_Ret != null )
        {
            return (O)v_Ret;
        }
        else
        {
            return null;
        }
    }
    
    
    
    /**
     * 线程序号
     * 
     * @return
     */
    public String getThreadNo()
    {
        if ( this.thread != null )
        {
            return this.thread.getThreadNo();
        }
        else
        {
            return "";
        }
    }
    
    
    
    /**
     * 获取线程运行状态。
     * 
     * 不建议使用。因为，某些瞬间，任务对应的线程已分配给其它任务。
     * 
     * 某些瞬间指：任务已完成，对应线程已空闲。但之后线程会再分配新的任务，此时的线程状态就不在准确了。
     * 
     * @return
     */
    @Deprecated
    public ThreadRunStatus getThreadRunStatus()
    {
        if ( this.thread != null )
        {
            return this.thread.getThreadRunStatus();
        }
        else
        {
            return ThreadRunStatus.$Exception;
        }
    }
    
    
    
    /**
     * 判断任务是否完成
     * 
     * @return
     */
    public boolean isFinishTask()
    {
        return this.isFinish;
    }
    
    
    
    /**
     * 准备 -- 任务在开始执行前的准备动作。
     * 
     * 它应由 TaskPool 或 ThreadPool 调用。
     * 
     * 对于用户来说，不用主动的调用。
     */
    public void ready()
    {
        this.isFinish = false;
        this.isStop   = false;
    }
    
    
    
    /**
     * 标记任务执行完成。
     * 
     * 此方法主要用于 this.execute() 方法中使用。是由实现任务的类型来调用的。
     * 如果实现任务的类不在 this.execute() 中调用 this.finishTask() ，则任务一直运行。
     * 
     * 此外，不提供如 startTask() 方法及功能。
     * 而有引入 TaskPool 池的概念，由它来实现线程Thread与任务Task的绑定和启动。
     */
    public void finishTask()
    {
        if ( this.taskGroup != null )
        {
            this.taskGroup.taskFinish(this);
        }
        
        if ( this.thread != null )
        {
            this.thread.finishTask();
        }
        
        this.isFinish = true;
    }
    
    
    
    /**
     * 任务是否停止执行。任务还未绑定线程执行前被停止。
     * 对于已绑定线程正在执行中的是不生效的。
     * 默认为:false。
     */
    public boolean isStop()
    {
        return isStop;
    }
    
    
    
    /**
     * 任务是否停止执行。任务还未绑定线程执行前被停止。
     * 对于已绑定线程正在执行中的是不生效的。
     * 默认为:false。
     * 
     * @param isStop 
     */
    public void stopTasksNoExecute()
    {
        this.isStop = true;
    }
    
    
    
    /**
     * 获取任务组中任务数量
     * 
     * @return
     */
    public int getTaskGroupSize()
    {
        if ( this.taskGroup != null )
        {
            return this.taskGroup.size();
        }
        else
        {
            return 1;
        }
    }


    
    protected void finalize() throws Throwable 
    {
        this.printBuffer = null;
        this.sqlBuffer   = null;
        
        // 在判断是否“完成”后，再对线程进行处理，就是怕任务已完成了，但线程已分配给其它任务来使用，造成错误“停止”线程的问题
        if ( !this.isFinish )
        {
            finishTask();
        }
        
        super.finalize();
    }
        
}
