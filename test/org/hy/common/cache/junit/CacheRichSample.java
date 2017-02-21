package org.hy.common.cache.junit;

import org.hy.common.cache.CacheRich;





/**
 * 高速缓存 -- 财富型 的测试单元。缓存的实现类
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2011-11-29
 */
public class CacheRichSample extends CacheRich<CacheRichSampleMetadata>
{

	@Override
	protected CacheRichSampleMetadata callBack_NewCacheMetadata() 
	{
		return new CacheRichSampleMetadata();
	}

}
