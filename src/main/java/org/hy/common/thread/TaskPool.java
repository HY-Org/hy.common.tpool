package org.hy.common.thread;

import org.hy.common.Date;
import org.hy.common.Queue;





/**
 * 任务池。
 * 
 * 内含一个相对独立的任务，此任务用于将线程池中空闲的Thread与Task绑定，再执行Task。
 * 
 * 任务池采用先进先出原则。
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2012-05-16
 *           V2.0  2017-02-09  添加：空闲开始时间和扫描任务是否在运行中的状态，
 *                                  用其控制扫描任务在长时间空闲时，自动销毁的功能，防止长时间占用资源。
 *           V3.0  2017-02-21  添加：停止尚未绑定线程开始执行的任务。对于已绑定线程执行的任务不生效。
 */
public class TaskPool 
{

    private static long     $SerialNo = 0;
    
    private static TaskPool $TaskPool = new TaskPool();
    
    
    /** 任务队列 */
    private Queue<Task<?>>   taskQueue;
    
    /** 扫描任务池的任务，即：内含的独立任务 */
    private ScanTaskPoolTask scanTaskPoolTask; 
    
    
    
    /**
     * 单态模式获取唯一的任务池对象实例
     * 
     * @return
     */
    public static TaskPool getInstance()
    {
        return $TaskPool;
    }
    
    
    
    /**
     * 将任务放入池中
     * 
     * @param i_Task
     */
    public synchronized static void putTask(Task<?> i_Task)
    {
        if ( i_Task == null )
        {
            return;
        }
        
        // 如果任务池中无排队对象，并且线程池中有空闲线程，则任务不进任务池，而是直接与线程绑定并执行
        TaskPool v_TaskPool = getInstance();
        if ( v_TaskPool.getQueue().size() == 0 )
        {
            // 以不等待空闲的线程资源的方式
            ThreadBase v_ThreadBase = ThreadPool.getThreadInstance(i_Task ,false);
            
            if ( v_ThreadBase != null  )
            {
                v_ThreadBase.startupAndExecuteTask();
                return;
            }
        }
        
        v_TaskPool.getQueue().put(i_Task);
        v_TaskPool.startScan();
    }
    
    
    
    /**
     * 获取队列中的元素。当队列为空时，返回null
     * 
     * @return
     */
    public static Task<?> getTask()
    {
        return getInstance().getQueue().get();
    }
    
    
    
    /**
     * 获取池中任务数量
     * 
     * @return
     */
    public static long size()
    {
        return getInstance().getQueue().size();
    }
    
    
    
    /**
     * 获取曾经进入过队列的任务次数
     * 
     * @return
     */
    public static long getPutedCount()
    {
        return getInstance().getQueue().getPutedCount();
    }
    
    
    
    /**
     * 获取曾经出去过队列的任务次数
     * 
     * @return
     */
    public static long getOutedCount()
    {
        return getInstance().getQueue().getOutedCount();
    }
    
    
    
    private synchronized long GetSerialNo()
    {
        return ++$SerialNo;
    }
    
    
    
    /**
     * 私有构造器
     */
    private TaskPool()
    {
        this.taskQueue = new Queue<Task<?>>();
    }
    
    
    
    private Queue<Task<?>> getQueue()
    {
        return this.taskQueue;
    }
    
    
    
    /**
     * 启动扫描任务池任务
     * 
     * 当空闲超过5分钟后，退出 "永远循环"
     * 
     * 不用加 synchronized 同步锁，因为调用执行本的方法已经是同步的，并且只有一处调用执行此方法
     */
    private void startScan()
    {
        if ( this.scanTaskPoolTask == null )
        {
            this.scanTaskPoolTask = new ScanTaskPoolTask();
        }
        else
        {
            if ( this.scanTaskPoolTask.isScanIsRunning() )
            {
                this.scanTaskPoolTask.refreshWatchInfo();
                return;
            }
            else
            {
                this.scanTaskPoolTask.setScanIsRunning(true);
            }
        }

        // 当线程池中的线程以经不够用时，才会执行本方法，所以要再独立允许一个线程来执行扫描动作。
        ThreadBase v_ThreadBase = ThreadPool.getNewThreadInstance(this.scanTaskPoolTask ,10 ,10);
        v_ThreadBase.startupAndExecuteTask();
    }
    
    
    
    
    
    /**
     * 扫描任务池的任务
     * 
     * @author   ZhengWei(HY)
     * @version  V1.0  2012-05-16
     *           V2.0  2017-02-09  添加：空闲开始时间和扫描任务是否在运行中的状态，
     *                                  用其控制扫描任务在长时间空闲时，自动销毁的功能，防止长时间占用资源。
     */
    class ScanTaskPoolTask extends Task<Object>
    {   
        /** 任务类型常量 */
        public final static String $TaskType$ = "ScanTaskPool";
        
        /** 空闲开始时间 */
        private long          idleBeginTime;
        
        /** 扫描任务是否运行中 */
        private boolean       scanIsRunning;
        
//      private StringBuilder buffer;

        
        
        public ScanTaskPoolTask() 
        {
            super($TaskType$);
            
            this.idleBeginTime = Date.getNowTime().getTime();
            this.scanIsRunning = true;
//          this.buffer        = new StringBuilder();
        }
        
        
        
        @Override
        public void execute() 
        {
            if ( TaskPool.size() >= 1 )
            {
                Task<?> v_Task = TaskPool.getTask();
                if ( v_Task != null )
                {
                    ThreadBase v_ThreadBase = ThreadPool.getThreadInstance(v_Task ,true);
                    v_ThreadBase.startupAndExecuteTask();
//                  this.buffer.append(Date.getNowTime().getFullMilli()).append("\t")
//                             .append(v_ThreadBase.getThreadNo())
//                             .append("\t任务号:").append(v_Task.getTaskNo()).append("\n");
                }
                
                this.refreshWatchInfo();
                idleBeginTime = Date.getNowTime().getTime();
            }
            else
            {
//              if ( !Help.isNull(this.buffer.toString()) )
//              {
//                  System.out.println(this.buffer.toString());
//                  this.buffer = new StringBuilder();
//              }
                ThreadPool.sleep(100);
                
                // 当空闲超过5分钟后，退出 "永远循环"
                if ( Date.getNowTime().getTime() - idleBeginTime >= 5 * 60 * 1000 )
                {
                    if ( TaskPool.size() <= 0 )
                    {
                        this.scanIsRunning = false;
                        this.finishTask();
                    }
                }
            }
            
            // 永远循环
            // this.finishTask();
        }

        
        
        /**
         * 获取：扫描任务是否运行中
         */
        public boolean isScanIsRunning()
        {
            return scanIsRunning;
        }


        
        /**
         * 设置：扫描任务是否运行中
         * 
         * @param scanIsRunning 
         */
        public void setScanIsRunning(boolean scanIsRunning)
        {
            this.scanIsRunning = scanIsRunning;
        }



        @Override
        public long getSerialNo() 
        {
            return GetSerialNo();
        }
        
        
        
        @Override
        public String getTaskDesc() 
        {
            return "Scan TaskPool waiting task size = " + TaskPool.size();
        }
        
    }
    
}
