package org.hy.common.cache;

import java.util.ArrayList;
import java.util.List;

import org.hy.common.thread.Task;
import org.hy.common.thread.ThreadBase;
import org.hy.common.thread.ThreadPool;





/**
 * 高速缓存
 * 
 * <C>指缓存的对象类型
 * <O>指缓存中元素的对象类型
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2011-11-29
 */
public abstract class Cache<C ,O>
{
	private static int $SerialNo = 0;
	
	
	/** 默认缓存初始化大小 */
	protected final static int         $DefaultCacheInitSize$    = 100;
	
	/** 默认缓存每次扩展大小 */
	protected final static int         $DefaultCacheNextSize$    = 100;
	
	/** 缓存自有线程等待时间间隔(单位：毫秒) */
	protected final static long        $CacheThreadIntervalTime$ = 100;
	
	
	
	/** 缓存 */
	protected C                        cache;
	
	/** 缓存初始化大小 */
	protected int                      cacheInitSize;
	
	/** 缓存每次扩展大小 */
	protected int                      cacheNextSize;
	
	/** 缓存自增长任务 */
	protected List<CacheTask<C ,O>>    cacheTaskList;
	
	
	
	/**
	 * 回调方法。new一个元数据对象实例。(保护类型)
	 * 
	 * @return
	 */
	protected abstract O callBack_NewCacheMetadata();
	
	
	
	/**
	 * 回调方法。将new好的元数对象实例存放在缓存中(保护类型)
	 * 
	 * @param i_CacheMetadata  元数据对象实例
	 */
	protected abstract void callBack_AddCacheMetadata(O i_CacheMetadata);
	
	
	
	/**
	 * 从缓存中获取一个 new 好的实例对象
	 * 
	 * 这是向外提供的最主要的方法
	 * 
	 * @return
	 */
	public abstract O newObject();
	
	
	
	/**
	 * 获取曾经创建过的对象实例的个数
	 * 
	 * @return
	 */
	public abstract int getCreatedCount();
	
	
	
	/**
	 * 获取被有效使用过的对象实例的个数
	 * 
	 * @return
	 */
	public abstract int getUsedCount();
	
	
	
	/**
	 * 获取缓存现有大小
	 * 
	 * @return
	 */
	public abstract int size();
	
	
	
	/**
	 * 清空缓存
	 */
	public abstract void clear();
	
	
	
	private synchronized int GetSerialNo()
	{
		return ++$SerialNo;
	}
	
	
	
	/**
	 * 构造器
	 * 
	 * @param i_Cache          缓存对象数据类型
	 */
	public Cache(C i_Cache)
	{
		this(i_Cache ,$DefaultCacheInitSize$ ,$DefaultCacheNextSize$);
	}
	

	/**
	 * 构造器
	 * 
	 * @param i_Cache          缓存对象数据类型
	 * @param i_CacheInitSize  缓存初始化大小
	 * @param i_CacheNextSize  缓存每次扩展大小
	 */
	public Cache(C i_Cache ,int i_CacheInitSize ,int i_CacheNextSize)
	{
		if ( i_Cache == null )
		{
			throw new NullPointerException("Cache is null");
		}
		
		this.cache = i_Cache;
		
		this.setCacheInitSize(i_CacheInitSize);
		this.setCacheNextSize(i_CacheNextSize);
		
		cacheTaskList = new ArrayList<CacheTask<C ,O>>();
		
		this.initCache();
	}
	
	
	
	/**
	 * 初始化缓存
	 */
	protected void initCache()
	{
		this.clear();
		
		if ( this.cacheTaskList.size() == 0 )
		{
			this.addCacheTask();
		}
		
		this.nextCacheAllCache(this.cacheInitSize);
	}
	
	
	/**
	 * 缓存按每次扩展大小进行扩展
	 */
	protected void nextCache()
	{
		if ( this.size() <= this.cacheNextSize / 2 )
		{
			this.addCacheTask();
		}
		
		this.nextCacheAllCache(this.cacheNextSize);
	}
	
	
	/**
	 * 添加一个内部任务，加快缓存的扩展
	 */
	protected synchronized void addCacheTask()
	{
		CacheTask<C ,O> v_CacheTask = new CacheTask<C ,O>(this);
		
		ThreadBase v_ThreadBase = ThreadPool.getThreadInstance(v_CacheTask);
		v_ThreadBase.startupAndExecuteTask();
		
		v_CacheTask.setCacheTaskIndex(this.cacheTaskList.size());
		this.cacheTaskList.add(v_CacheTask);
	}
	
	
	/**
	 * 启动所有(多)线程，对缓存扩展
	 * 
	 * @param i_Size
	 */
	protected void nextCacheAllCache(int i_Size)
	{
		for (int i=0; i<this.cacheTaskList.size(); i++)
		{
			this.cacheTaskList.get(i).nextCache(i_Size);
		}
	}
	
		
	public int getCacheInitSize() 
	{
		return cacheInitSize;
	}
	
	
	public void setCacheInitSize(int cacheInitSize) 
	{
		this.cacheInitSize = cacheInitSize;
	}
	
	
	public int getCacheNextSize() 
	{
		return cacheNextSize;
	}
	
	
	public void setCacheNextSize(int cacheNextSize) 
	{
		this.cacheNextSize = cacheNextSize;
	}
	
	
	/*
    ZhengWei(HY) Del 2016-07-30
    不能实现这个方法。首先JDK中的Hashtable、ArrayList中也没有实现此方法。
    它会在元素还有用，但集合对象本身没有用时，释放元素对象
    
    一些与finalize相关的方法，由于一些致命的缺陷，已经被废弃了
	protected void finalize() throws Throwable 
	{
		for (int i=0; i<this.cacheTaskList.size(); i++)
		{
			this.cacheTaskList.get(i).finishTask();
		}
		
		this.cacheTaskList = null;
		this.clear();
		this.cache = null;
	}
	*/
	
	
	
	/**
	 * 内部类。主要功能为：缓存的自增长
	 * 
	 * <C2>与Cache类的<C>一样
	 * <O2>与Cache类的<O>一样
	 */
	class CacheTask<C2 ,O2> extends Task<Object>
	{
		/** 任务类型常量 */
		public final static String $TaskType$ = "CACHE";
		
		
		/** 回指缓存实例对象 */
		private Cache<C2 ,O2>      cacheObject;
		
		/** 是否扩展缓存 */
		private boolean            isNextCache;
		
		/** 扩展的大小 */
		private int                nextSize;
		
		/** 实例在 Cache.cacheTaskList 中的索引位置(下标从零开始) */
		private int                cacheTaskIndex;
		
		
		
		public CacheTask(Cache<C2 ,O2> i_Cache) 
		{
			super($TaskType$);
			
			this.cacheObject    = i_Cache;
			this.nextSize       = 0;
			this.cacheTaskIndex = 0;
		}
		
		
		@Override
		public String getTaskDesc() 
		{
			return "CacheSize: "      + this.cacheObject.size()         + "    "
			     + "UsedSize: "       + this.cacheObject.getUsedCount() + "    "
			     + "CacheTaskIndex: " + this.cacheTaskIndex;
		}
		
		
		public void setCacheTaskIndex(int i_CacheTaskIndex)
		{
			this.cacheTaskIndex = i_CacheTaskIndex;
		}
		
		
		public int getCacheTaskIndex()
		{
			return this.cacheTaskIndex;
		}
		
		
		public boolean isNextCache() 
		{
			return isNextCache;
		}


		public void nextCache(int i_Size) 
		{
			this.isNextCache = true;
			this.nextSize    = i_Size;
		}
		
		
		public void execute() 
		{
			if ( this.isNextCache )
			{
				while ( this.nextSize > 0 )
				{
					try
					{
						callBack_AddCacheMetadata(callBack_NewCacheMetadata());
					}
					catch (Exception exce)
					{
						exce.printStackTrace();
					}
					
					this.nextSize--;
				}
				
				this.isNextCache = false;
			}
			
			
			// 线程退出机制
			// 当缓存大小大于"缓存每次扩展大小"时，并且线程为最后一个线程时，可以退出。
			if ( this.cacheTaskIndex == this.cacheObject.cacheTaskList.size() - 1 )
			{
				if ( this.cacheObject.size() > this.cacheObject.getCacheNextSize() )
				{
					this.cacheObject.cacheTaskList.remove(this.cacheTaskIndex);
					this.finishTask();
				}
			}
			
			ThreadPool.sleep($CacheThreadIntervalTime$);
		}


		@Override
		public int getSerialNo() 
		{
			return GetSerialNo();
		}
		
	}
		
}
