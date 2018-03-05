package org.hy.common.thread;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.hy.common.Queue;
import org.hy.common.StringHelp;
import org.hy.common.thread.ui.ThreadPoolWatch;





/**
 * 线程池。
 * 
 * 相对独立性高，主要对外提供获取空闲Thread的方法
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2011-06-08
 */
public class ThreadPool 
{
	/** 线程池(全部) */
	protected static List<ThreadBase>  THREADPOOL     = new Vector<ThreadBase>();
	
	/** 空闲线程池(为了高效) */
	protected static Queue<ThreadBase> IdleThreadPool = new Queue<ThreadBase>();
	
	/** 对线程池中的活动线程，分任务类型的统计其活动线程数 */
	protected static TaskTypeTotal     TaskTypeTotal  = new TaskTypeTotal();
	
	/** 线程池中最大线程数 */
	protected static int               MAXTHREAD      = 100;
	
	/** 线程池中最小线程数 */
	protected static int               MINTHREAD      = 10;
	
	/** 线程池中最小空闲线程数 */
	protected static int               MINIDLETHREAD  = 2;
	
	/** 线程池的大小。独立出大小是为性能与速度 */
	protected static int               ThreadPoolSize = 0;
	
	/** 等待时间间隔(单位：毫秒) */
	protected static long              IntervalTime   = 1000l;
	
	/** 空闲多少时间后线程自毁(单位：秒) */
	protected static long              IdleTimeKill   = 60l;
	
	/** 是否显示视窗化监视线程池窗口 */
	protected static boolean           IsWatch        = false;
	
	/** 当没有线程资源时的等待时长(单位：毫秒) */
	protected static long              WaitResource   = 100L;
	
	/** 停止线程池的线程 */
	protected static ThreadPoolStop    tPoolStop      = null;
	
	
	
	/**
	 * 获取线程对象
	 * 
	 * @param i_TaskObject      任务对象
	 * @return
	 */
	public static ThreadBase getThreadInstance(Task<?> i_TaskObject)
	{
		return getThreadInstance(i_TaskObject ,IntervalTime ,IdleTimeKill ,true);
	}
	
	
	/**
	 * 获取线程对象
	 * 
	 * @param i_TaskObject      任务对象
	 * @param i_IsWaitResource  是否等待空闲的线程资源
	 * @return
	 */
	public static ThreadBase getThreadInstance(Task<?> i_TaskObject ,boolean i_IsWaitResource)
	{
		return getThreadInstance(i_TaskObject ,IntervalTime ,IdleTimeKill ,i_IsWaitResource);
	}
	
	
	
	/**
	 * 创建一个新的线程，并关联任务。
	 * 
	 * 原本此方法是私有的，没有公开，现将其公开后，即可实现动态扩大线程池的最大数线程数。
	 * 同时，此方法将同步扩大 "最大线程数 MAXTHREAD"。
	 * 
	 * 
	 * @author      ZhengWei(HY)
	 * @createDate  2017-02-08
	 * @version     v1.0
	 *
	 * @param i_TaskObject      任务对象
     * @param i_IntervalTime    等待时间间隔(单位：毫秒) 默认为：100毫秒
     * @param i_IdleTimeKill    空闲多少时间后线程自毁(单位：秒) 默认为：60秒
	 * @return
	 */
	public synchronized static ThreadBase getNewThreadInstance(Task<?> i_TaskObject ,long i_IntervalTime ,long i_IdleTimeKill)
	{
	    ThreadBase v_ThreadBase = new ThreadBase(i_TaskObject ,i_IntervalTime ,i_IdleTimeKill);
	    THREADPOOL.add(v_ThreadBase);
        ThreadPoolSize++;
        MAXTHREAD = Math.max(MAXTHREAD ,ThreadPoolSize);
        TaskTypeTotal.active(i_TaskObject.getTaskType());
        
        if ( IsWatch )
        {
            ThreadPoolWatch.getInstance().updateTitle(showInfo());
        }
        
        return v_ThreadBase;
	}
	
	
	
	/**
	 * 获取线程任务的实例对象
	 * 
	 * @param i_TaskObject      任务对象
	 * @param i_IntervalTime    等待时间间隔(单位：毫秒) 默认为：100毫秒
	 * @param i_IdleTimeKill    空闲多少时间后线程自毁(单位：秒) 默认为：60秒
	 * @param i_IsWaitResource  是否等待空闲的线程资源
	 * @return
	 */
	public synchronized static ThreadBase getThreadInstance(Task<?> i_TaskObject ,long i_IntervalTime ,long i_IdleTimeKill ,boolean i_IsWaitResource)
	{
		if ( i_TaskObject == null )
		{
			throw new NullPointerException("Thread Task is null");
		}
		
		
		i_TaskObject.ready();
		
		
		ThreadBase v_ThreadBase = getIdleThread(i_TaskObject ,i_IntervalTime ,i_IdleTimeKill);
		
		
		if ( v_ThreadBase != null )
		{
			TaskTypeTotal.active(i_TaskObject.getTaskType());
			
			if ( IsWatch )
			{
				ThreadPoolWatch.getInstance().updateTitle(showInfo());
			}
			
			return v_ThreadBase;
		}
		else if ( ThreadPoolSize < MAXTHREAD )
		{
			return getNewThreadInstance(i_TaskObject ,i_IntervalTime ,i_IdleTimeKill);
		}
		else if ( !i_IsWaitResource && v_ThreadBase == null )
		{
			return null;
		}
		else
		{
			do 
			{
				sleep(WaitResource);
				
				v_ThreadBase = getIdleThread(i_TaskObject ,i_IntervalTime ,i_IdleTimeKill);
			} 
			while ( v_ThreadBase == null );
			
			if ( v_ThreadBase != null )
			{
				TaskTypeTotal.active(i_TaskObject.getTaskType());
				
				if ( IsWatch )
				{
					ThreadPoolWatch.getInstance().updateTitle(showInfo());
				}
			}
			
			return v_ThreadBase;
		}
	}
	
	
	/**
	 * 杀死线程(有限制的)
	 * 
	 * @param i_ThreadBase
	 * @return  返回 0 表示成功
	 */
	public synchronized static int killMySelf(ThreadBase i_ThreadBase) 
	{
		if ( ThreadPool.getIdleThreadCount() > ThreadPool.getMinIdleThread()
		  && ThreadPool.getThreadCount()     > ThreadPool.getMinThread() )
		{
			try
			{			
				killThread(i_ThreadBase);
				
				return 0;
			}
			catch (Exception exce)
			{
				exce.printStackTrace();
				return -1;
			}
		}
		else
		{
			return -1;
		}
	}
	
	
	/**
	 * 是否有条件(允许)线程自毁
	 * 
	 * @return
	 */
	public synchronized static boolean isAllowKillMySelf()
	{
		if ( ThreadPool.getIdleThreadCount() > ThreadPool.getMinIdleThread()
		  && ThreadPool.getThreadCount()     > ThreadPool.getMinThread() )
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	
	/**
	 * 杀死线程(无限制的)
	 * 
	 * @param i_ThreadBase
	 * @return  返回 0 表示成功
	 */
	public synchronized static void killThread(ThreadBase i_ThreadBase) 
	{
		ThreadPoolSize--;
		
		THREADPOOL.remove(i_ThreadBase);
		
		IdleThreadPool.remove(i_ThreadBase);
		
		
		if ( IsWatch )
		{
			ThreadPoolWatch.getInstance().updateTitle(showInfo());
		}
	}
	
	
	/**
	 * 对象是否存在在线程池中
	 * 
	 * @param i_ThreadBase
	 * @return
	 */
	public synchronized static boolean isExists(ThreadBase i_ThreadBase)
	{
		if ( i_ThreadBase == null )
		{
			return false;
		}
		return THREADPOOL.contains(i_ThreadBase);
	}
	
	
	/**
	 * 获取空闲的线程任务
	 * 
	 * @param i_TaskObject    任务对象
	 * @param i_IntervalTime  等待时间间隔(单位：毫秒) 默认为：100毫秒
	 * @param i_IdleTimeKill  空闲多少时间后线程自毁(单位：秒) 默认为：60秒
	 * @return
	 */
	protected synchronized static ThreadBase getIdleThread(Task<?> i_TaskObject ,long i_IntervalTime ,long i_IdleTimeKill)
	{
		// 其实在 IdleThreadPool.get() 内部也有 size() == 0 的判断
		// 但那是 synchronized 的，在此添加判断，可以提高性能与速度
		if ( IdleThreadPool.size() == 0 )
		{
			return null;
		}
		
		
		ThreadBase v_ThreadBase = IdleThreadPool.get();
		
		if ( v_ThreadBase != null )
		{
			v_ThreadBase.setTaskObject(i_TaskObject);
			v_ThreadBase.setIntervalTime(i_IntervalTime);
			v_ThreadBase.setIdleTimeKill(i_IdleTimeKill);
			
			return v_ThreadBase;
		}
		
		return null;
	}
	
	
	/**
	 * 添加空闲线程
	 * 
	 * 注意：不用加 synchronized 同步锁，因为调用执行本的方法已经是同步的，并且只有父方法调用执行此方法
	 *      如果加上 synchronized 同步锁，反而会造成死锁。
	 * 
	 * @param i_ThreadBase
	 */
	protected static void addIdleThread(ThreadBase i_ThreadBase)
	{
		if ( i_ThreadBase == null )
		{
			throw new NullPointerException("ThreadBase is null");
		}
		
		TaskTypeTotal.rest(i_ThreadBase.getOldTaskType());
		IdleThreadPool.put(i_ThreadBase);
		
		if ( IsWatch )
		{
			ThreadPoolWatch.getInstance().updateTitle(showInfo());
		}
	}
	
	
	/**
	 * 停止所有线程
	 */
	public static void shutdownAllThread()
	{
		Thread v_Thread = new Thread(getThreadPoolStop());
		v_Thread.start();
	}
	
	
	public static void setThreadPoolStop(ThreadPoolStop i_TPoolStop)
	{
		tPoolStop = i_TPoolStop;
	}
	
	
	public synchronized static ThreadPoolStop getThreadPoolStop()
	{
		if ( tPoolStop == null )
		{
			tPoolStop = new ThreadPoolStop();
		}
		
		return tPoolStop;
	}
	
	
	/**
	 * 获取线程池中，线程的总个数
	 * 
	 * @return
	 */
	public static int getThreadCount()
	{
		return ThreadPoolSize;
	}
	
	
	/**
	 * 获取线程池中，活动的线程数(有任务在身的)
	 * 
	 * @return
	 */
	public static int getActiveThreadCount()
	{
		return getThreadCount() - getIdleThreadCount();
	}
	
	
	/**
	 * 获取某种任务类型的在线程池中的活动线程数
	 * 
	 * @return
	 */
	public static int getActiveThreadCount(String i_TaskType)
	{
		return TaskTypeTotal.getActiveCount(i_TaskType);
	}
	
	
	/**
	 * 获取线程池中，空闲的线程数(无任务的)
	 * 
	 * @return
	 */
	public static int getIdleThreadCount()
	{
		return IdleThreadPool.size();
	}
	
	
	/**
	 * 公用的，方便线程睡眠的方法
	 * 
	 * @param i_Millis  
	 */
	public static void sleep(long i_Millis)
	{
		try
		{
			Thread.sleep(i_Millis);
		}
		catch (Exception exce)
		{
			
		}
	}
	
	
	public static long getIdleTimeKill() 
	{
		return IdleTimeKill;
	}


	public static void setIdleTimeKill(int idleTimeKill) 
	{
		if ( idleTimeKill > 0 )
		{
			IdleTimeKill = idleTimeKill;
		}
	}


	public static long getIntervalTime() 
	{
		return IntervalTime;
	}


	public static void setIntervalTime(long intervalTime) 
	{
		if ( intervalTime > 0 )
		{
			IntervalTime = intervalTime;
		}
	}


	public static void setMaxThread(int i_MaxThread)
	{
		if ( i_MaxThread > 0 )
		{
			MAXTHREAD = i_MaxThread;
		}
	}
	
	
	public static void setMinIdleThread(int i_MinIdleThread) 
	{
		if ( i_MinIdleThread >= 0 )
		{
			MINIDLETHREAD = i_MinIdleThread;
		}
	}
	
	
	public static void setMinThread(int i_MinThread) 
	{
		if ( i_MinThread >= 0 )
		{
			MINTHREAD = i_MinThread;
		}
	}
	
	
	public static void setWaitResource(long i_WaitResource)
	{
		if ( i_WaitResource >= 10 )
		{
			WaitResource = i_WaitResource;
		}
	}
	
	
	public static long getWaitResource()
	{
		return WaitResource;
	}
	
	
	public static int getMaxThread()
	{
		return MAXTHREAD;
	}
	
	
	public static int getMinIdleThread() 
	{
		return MINIDLETHREAD;
	}


	public static int getMinThread() 
	{
		return MINTHREAD;
	}
	
	
	public static ThreadBase getThreadBase(int i_Index)
	{
		if ( i_Index < 0 || i_Index > getThreadCount() - 1 )
		{
			return null;
		}
		
		try
		{
			return THREADPOOL.get(i_Index);
		}
		catch (Exception exce)
		{
			return null;
		}
	}
	


	public static boolean isWatch() 
	{
		return IsWatch;
	}


	public static void setWatch(boolean isWatch) 
	{
		IsWatch = isWatch;
		
		if ( IsWatch )
		{
            // 视窗化监视线程池
			ThreadPoolWatch v_WatchFrame = ThreadPoolWatch.getInstance();
			v_WatchFrame.watch();
		}
	}


	/**
	 * 线程池大体信息
	 */
	public static String showInfo()
	{
		return "Total: " + getThreadCount() + "  Idle: " + getIdleThreadCount() + "  Active: " + getActiveThreadCount();
	}
	
	
	
	/**
	 * 获取线程池监控信息
	 * 
	 * @author      ZhengWei(HY)
	 * @createDate  2018-02-26
	 * @version     v1.0
	 *
	 * @return
	 */
	public static List<ThreadReport> getThreadReports()
	{
	    List<ThreadReport> v_Datas = new ArrayList<ThreadReport>();
	    
	    for (ThreadBase v_ThreadBase : THREADPOOL)
	    {
	        v_Datas.add(new ThreadReport(v_ThreadBase));
	    }
	    
	    return v_Datas;
	}
	
	
	
	/**
	 * 线程池详细信息
	 */
	public static String showDetailInfo()
	{
	    StringBuilder v_Buffer = new StringBuilder();
		
		for (int i=0; i<THREADPOOL.size(); i++)
		{
			ThreadBase v_ThreadBase = THREADPOOL.get(i);
			v_Buffer.append(StringHelp.rpad(v_ThreadBase.getThreadNo()         ,32 ," "));
			v_Buffer.append(StringHelp.rpad(v_ThreadBase.getThreadRunStatus()  ,16 ," "));
			v_Buffer.append(                v_ThreadBase.getExecuteTaskCount()          );
			v_Buffer.append("\n");
		}
		
		return v_Buffer.toString();
	}
	
}
