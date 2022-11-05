package org.hy.common.cache.junit;

import org.hy.common.Date;
import org.hy.common.thread.ThreadPool;





/**
 * 高速缓存 -- 财富型 的测试单元。缓存元数据，即缓存中存放的数据类型
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2011-11-29
 */
public class CacheRichSampleMetadata
{
    private Date createDate;
    
    

    public CacheRichSampleMetadata()
    {
        this.createDate = new Date();
        ThreadPool.sleep(100);
    }
    
    
    @Override
    public String toString()
    {
        return this.createDate.getFull();
    }
    
}
