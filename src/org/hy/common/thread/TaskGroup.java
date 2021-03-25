package org.hy.common.thread;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.hy.common.Date;
import org.hy.common.Help;
import org.hy.common.thread.event.TaskGroupEvent;
import org.hy.common.thread.event.TaskGroupListener;

import org.hy.common.thread.event.DefaultTaskGroupEvent;





/**
 * 任务组
 * 
 * <O>指线程自有的缓存的对象类型
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2012-04-28
 *           V2.0  2017-02-21  添加：停止尚未绑定线程开始执行的任务。对于已绑定线程执行的任务不生效。
 *           V3.0  2021-01-13  添加：组中累计的任务数量。防止动态向组内添加任务时，误判组全部完成的问题。
 *                             添加：组级停止状态。用于组内某一任务发起“停止”后，
 *                                   任务池中的其它任务及马上将要执行的任务均能不抛异常的停止。
 *           V4.0  2021-03-24  添加：准备添加的任务数量。
 *                                   在执行 addTaskAndStart() 方法的前，预前判定准备执行的任务总数。
 *                                   来辅助预防高并发时，“添加任务”的动作慢于“任务执行”动作的情况，
 *                                   造成任务组误判任务组整体完成的问题。
 */
public class TaskGroup 
{
    /** 最后一个创建的任务组 */
    private static TaskGroup                     $LastTaskGroup = null;
    
    
    
    /** 任务组名称 */
    private String                               taskGroupName;
    
    /** 任务组中的任务列表 */
    private List<Task<?>>                        taskList;
    
    /** 准备添加的任务数量 */
    private long                                 readyTotalSize;
    
    /** 组中累计的任务数量 */
    private long                                 totalSize;
    
    /** 组中完成任务的数量 */
    private long                                 finishSize;
    
    /** 自定义事件的监听器集合 */
    private Collection<TaskGroupListener>        taskGroupListeners;
    
    /** 事件对象 */
    private DefaultTaskGroupEvent                taskGroupEvent;
    
    /** 组内所有任务全部停止 */
    private boolean                              isAllStop;
    
    /** 
     * 任务组和任务组中所有任务是否都完成了
     * 即，注册监听者都执行完 finishAllTask(...) 方法了，返回true。
     **/
    private boolean                              taskGroupIsFinish;
    
    /** 
     * 任务组中所有任务是否都完成了
     * 即，注册监听者都执行 finishAllTask(...) 方法之前，就返回true了。
     **/
    private boolean                              tasksIsFinish;
    
    /** 上一个任务组对象 */
    private TaskGroup                            upperTaskGroup;
    
    
    
    /**
     * 设置上一个任务组对象
     * 
     * @param i_TaskGroup
     */
    private synchronized static void setUpperTaskGroup(TaskGroup i_TaskGroup)
    {
        i_TaskGroup.upperTaskGroup = $LastTaskGroup;
        $LastTaskGroup             = i_TaskGroup;
    }
    
    
    
    /**
     * 构造器
     * 
     * @param i_TaskGroupName  任务组名称
     */
    public TaskGroup(String i_TaskGroupName)
    {
        if ( Help.isNull(i_TaskGroupName) )
        {
            throw new NullPointerException("Task group name is null.");
        }
        
        this.taskGroupName     = i_TaskGroupName;
        this.taskList          = new ArrayList<Task<?>>();
        this.readyTotalSize    = 0L;
        this.totalSize         = 0L;
        this.finishSize        = 0L;
        this.isAllStop         = false;
        this.taskGroupIsFinish = false;
        this.tasksIsFinish     = false;
        
        setUpperTaskGroup(this);
    }
    
    
    
    /**
     * 启动所有任务。
     * 
     * 说是启动不如说它是"装载"，将每个任务 put 到任务池中，排队顺次执行。
     */
    public void startupAllTask()
    {
        int v_Size = this.taskList.size();
        
        if ( v_Size <= 0 )
        {
            return;
        }
        
        this.isAllStop         = false;
        this.taskGroupIsFinish = false;
        this.tasksIsFinish     = false;
        this.readyTotalSize    = v_Size;
        this.totalSize         = v_Size;
        this.finishSize        = 0L;
        this.taskGroupEvent = new DefaultTaskGroupEvent(this);
        this.taskGroupEvent.setSize(v_Size);
        this.taskGroupEvent.setTasks(this.taskList.iterator());
        
        this.fireStartupAllTaskListener(this.taskGroupEvent);
        
        
        for (int v_Index=0; v_Index<v_Size; v_Index++)
        {
            Task<?> v_Task = this.taskList.get(v_Index);
            TaskPool.putTask(v_Task);
        }
    }
    
    
    
    /**
     * 停止尚未绑定线程开始执行的任务。对于已绑定线程执行的任务不生效。
     * 
     *      注意：此方法只是通知所有任务应当停止，并不是立刻将任务停止。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2017-02-21
     * @version     v1.0
     *
     */
    public void stopTasksNoExecute()
    {
        synchronized ( this )
        {
            if ( this.isAllStop )
            {
                return;
            }
            
            this.isAllStop = true;
        }
        
        Iterator<Task<?>> v_Iter = this.taskList.iterator();
        if ( v_Iter == null )
        {
            return;
        }
        
        while ( v_Iter.hasNext() )
        {
            Task<?> v_Task = v_Iter.next();
            
            if ( v_Task != null )
            {
                v_Task.stopTasksNoExecute();
                // v_Task.finishTask();       此处不要额外的帮助任务完成。应叫队列等待的任务自行判定后，自己停止并改成完成状态。
            }
        }
    }
    
    
    
    /**
     * 获取：组内所有任务全部停止
     */
    public boolean isAllStop()
    {
        return isAllStop;
    }



    /**
     * 报告每个任务完成。
     * 
     * 当每个任务执行完成时，即在 Task.finishTask() 方法中，会自动调用此方法。
     * 
     * @param i_Task
     */
    public void taskFinish(Task<?> i_Task)
    {
        // 任务完成数量++，并判定任务组是否整体完成。
        synchronized (this)
        {
            if ( this.tasksIsFinish )
            {
                // 任务组已标记完成，就不在接收每个任务的报告了。
                return;
            }
            
            this.finishSize++;
            
            if ( this.finishSize > 0 
              && this.finishSize >= this.size() 
              && this.finishSize >= this.totalSize 
              && this.finishSize >= this.readyTotalSize )
            {
                this.tasksIsFinish = true;
            }
            else
            {
                return;
            }
        }
        
        if ( this.tasksIsFinish )
        {
            if ( this.taskGroupEvent != null )
            {
                this.taskGroupEvent.setCompleteSize(this.finishSize);
                this.taskGroupEvent.setTasks(this.taskList.iterator());
                this.taskGroupEvent.setEndTime(new Date());
                
                try
                {
                    this.fireFinishAllTaskListener(this.taskGroupEvent);
                }
                catch (Exception exce)
                {
                    exce.printStackTrace();
                }
            }
            
            this.taskGroupIsFinish = true;
        }
    }
    
    
    
    /**
     * 获取完成任务数。
     * 
     * 注意：在组完成时，此值会变为0值，它只是一个过程变量。
     *      当判定组完成时，建议用this.isTasksFinish()方法。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2018-08-20
     * @version     v1.0
     *
     * @return
     */
    public long getFinishSize()
    {
        return this.finishSize;
    }
    
    
    
    /**
     * 添加任务
     * 
     * @param i_Task
     */
    public synchronized void addTask(Task<?> i_Task)
    {
        if ( i_Task == null )
        {
            return;
        }
        
        i_Task.setTaskGroup(this);
        i_Task.ready();
        this.taskList.add(i_Task);
        this.totalSize++;
    }
    
    
    
    /**
     * 添加任务，并执行任务
     * 
     * 注意：
     *        在执行 addTaskAndStart() 方法的前，预前判定准备执行的任务总数（ addReadyTotalSize(...) ）。
     *        来辅助预防高并发时，“添加任务”的动作慢于“任务执行”动作的情况，
     *        造成任务组误判任务组整体完成的问题
     * 
     * @author      ZhengWei(HY)
     * @createDate  2017-02-22
     * @version     v1.0
     *
     * @param i_Task
     */
    public void addTaskAndStart(Task<?> i_Task)
    {
        this.addTask(i_Task);
        TaskPool.putTask(i_Task);
    }
    
    
    
    /**
     * 获取任务
     * 
     * @param i_Index  小标从零开始
     * @return
     */
    public Task<?> getTask(int i_Index)
    {
        return this.taskList.get(i_Index);
    }
    
    
    
    /**
     * 获取任务组中任务数量
     * 
     * @return
     */
    public int size()
    {
        return this.taskList.size();
    }
    
    
    
    /**
     * 任务组和任务组中所有任务是否都完成了
     * 
     * 即，注册监听者都执行完 finishAllTask(...) 方法了，返回true。
     * 
     * @return
     */
    public boolean isTaskGroupFinish()
    {
        return this.taskGroupIsFinish;
    }
    
    
    
    /** 
     * 任务组中所有任务是否都完成了
     * 即，注册监听者都执行 finishAllTask(...) 方法之前，就返回true了。
     **/
    public boolean isTasksFinish()
    {
        return this.tasksIsFinish;
    }
    
    
    
    /**
     * 获取上一个任务组对象
     * @return
     */
    public TaskGroup getUpperTaskGroup()
    {
        return this.upperTaskGroup;
    }
    
    
    
    /**
     * 清空任务组中所有的任务
     */
    public synchronized void clear()
    {
        for (int v_Index=this.taskList.size()-1; v_Index>=0; v_Index--)
        {
            Task<?> v_Task = null;
            
            try
            {
                v_Task= this.taskList.remove(v_Index);
                
                // 2012-07-23 不能有此句，它会引发 this.taskFinish 的调用，造成异常 
                // v_Task.finishTask();
                
                v_Task.finalize();
                
                v_Task = null;
            }
            catch (Exception exce)
            {
                exce.printStackTrace();
            }
            catch (Throwable t)
            {
                t.printStackTrace();
            }
            finally
            {
                v_Task = null;
            }
        }
    }
    
    
    
    /**
     * 注册事件
     * 
     * @param e
     */
    public void addTaskGroupListener(TaskGroupListener e)
    {
        if ( this.taskGroupListeners == null )
        {
            this.taskGroupListeners = new HashSet<TaskGroupListener>();
        }
        
        this.taskGroupListeners.add(e);
    }
    
    
    
    /**
     * 移除事件
     * 
     * @param e
     */
    public void removeTaskGroupListener(TaskGroupListener e)
    {
        if ( this.taskGroupListeners == null )
        {
            return;
        }
        
        this.taskGroupListeners.remove(e);
    }
    
    
    
    /**
     * 触发启用任务组所有任务的事件
     * 
     * @param i_Event
     */
    protected void fireStartupAllTaskListener(TaskGroupEvent i_Event)
    {
        if ( this.taskGroupListeners == null )
        {
            return;
        }
        
        notifyStartupAllTaskListeners(i_Event);
    }
    
    
    
    /**
     * 触发任务组中任务都完成后的事件
     * 
     * @param i_Event
     */
    protected void fireFinishAllTaskListener(TaskGroupEvent i_Event)
    {
        if ( this.taskGroupListeners == null )
        {
            return;
        }
        
        notifyFinishAllTaskListeners(i_Event);
    }
    
    
    
    /**
     * 通知所有注册启用任务组所有任务事件监听的对象
     * 
     * @param i_Event
     */
    private void notifyStartupAllTaskListeners(TaskGroupEvent i_Event)
    {
        Iterator<TaskGroupListener> v_Iter = this.taskGroupListeners.iterator(); 

        while ( v_Iter.hasNext() ) 
        {
            TaskGroupListener v_Listener = v_Iter.next();

            v_Listener.startupAllTask(i_Event);
        }
    }
    
    
    
    /**
     * 通知所有注册任务组中任务都完成后事件监听的对象
     * 
     * @param i_Event
     */
    private void notifyFinishAllTaskListeners(TaskGroupEvent i_Event)
    {
        Iterator<TaskGroupListener> v_Iter = this.taskGroupListeners.iterator(); 

        while ( v_Iter.hasNext() ) 
        {
            TaskGroupListener v_Listener = v_Iter.next();

            v_Listener.finishAllTask(i_Event);
        }
    }
    
    
    
    public String getTaskGroupName() 
    {
        return taskGroupName;
    }



    public void setTaskGroupName(String taskGroupName) 
    {
        this.taskGroupName = taskGroupName;
    }


    
    /**
     * 获取：准备添加的任务数量
     */
    public long getReadyTotalSize()
    {
        return readyTotalSize;
    }

    
    
    /**
     * 设置：准备添加的任务数量
     * 
     * @param readyTotalSize 
     */
    public synchronized void addReadyTotalSize(long readyTotalSize)
    {
        this.readyTotalSize = +readyTotalSize;
    }
    
}
