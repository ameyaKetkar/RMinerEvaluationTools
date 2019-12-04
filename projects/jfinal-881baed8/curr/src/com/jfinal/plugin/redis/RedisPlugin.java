/**
 * Copyright (c) 2011-2015, James Zhan 詹波 (jfinal@126.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jfinal.plugin.redis;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;
import com.jfinal.kit.StrKit;
import com.jfinal.plugin.IPlugin;
import com.jfinal.plugin.redis.serializer.FstSerializer;
import com.jfinal.plugin.redis.serializer.ISerializer;

/**
 * RedisPlugin.
 * RedisPlugin 支�?多个 Redis �?务端，�?�需�?创建多个 RedisPlugin 对象
 * 对应这多个�?�?�的 Redis �?务端�?��?�。也支�?多个 RedisPlugin 对象对应�?�一
 * Redis �?务的�?�?� database，具体例�?�? jfinal 手册
 */
public class RedisPlugin implements IPlugin {
	
	private String cacheName;
	
	private String host;
	private Integer port = null;
	private Integer timeout = null;
	private String password = null;
	private Integer database = null;
	private String clientName = null;
	
	private ISerializer serializer = null;
	private IKeyNamingPolicy keyNamingPolicy = null;
	private JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
	
	public RedisPlugin(String cacheName, String host) {
		if (StrKit.isBlank(cacheName))
			throw new IllegalArgumentException("cacheName can not be blank.");
		if (StrKit.isBlank(host))
			throw new IllegalArgumentException("host can not be blank.");
		this.cacheName = cacheName.trim();
		this.host = host;
	}
	
	public RedisPlugin(String cacheName, String host, int port) {
		this(cacheName, host);
		this.port = port;
	}
	
	public RedisPlugin(String cacheName, String host, int port, int timeout) {
		this(cacheName, host, port);
		this.timeout = timeout;
	}
	
	public RedisPlugin(String cacheName, String host, int port, int timeout, String password) {
		this(cacheName, host, port, timeout);
		if (StrKit.isBlank(password))
			throw new IllegalArgumentException("password can not be blank.");
		this.password = password;
	}
	
	public RedisPlugin(String cacheName, String host, int port, int timeout, String password, int database) {
		this(cacheName, host, port, timeout, password);
		this.database = database;
	}
	
	public RedisPlugin(String cacheName, String host, int port, int timeout, String password, int database, String clientName) {
		this(cacheName, host, port, timeout, password, database);
		if (StrKit.isBlank(clientName))
			throw new IllegalArgumentException("clientName can not be blank.");
		this.clientName = clientName;
	}
	
	public RedisPlugin(String cacheName, String host, int port, String password) {
		this(cacheName, host, port, Protocol.DEFAULT_TIMEOUT, password);
	}
	
	public RedisPlugin(String cacheName, String host, String password) {
		this(cacheName, host, Protocol.DEFAULT_PORT, Protocol.DEFAULT_TIMEOUT, password);
	}
	
	public boolean start() {
		JedisPool jedisPool;
		if      (port != null && timeout != null && password != null && database != null && clientName != null)
			jedisPool = new JedisPool(jedisPoolConfig, host, port, timeout, password, database, clientName);
		else if (port != null && timeout != null && password != null && database != null)
			jedisPool = new JedisPool(jedisPoolConfig, host, port, timeout, password, database);
		else if (port != null && timeout != null && password != null)
			jedisPool = new JedisPool(jedisPoolConfig, host, port, timeout, password);
		else if (port != null && timeout != null)
			jedisPool = new JedisPool(jedisPoolConfig, host, port, timeout);
		else if (port != null)
			jedisPool = new JedisPool(jedisPoolConfig, host, port);
		else
			jedisPool = new JedisPool(jedisPoolConfig, host);
		
		if (serializer == null)
			serializer = FstSerializer.me;
		if (keyNamingPolicy == null)
			keyNamingPolicy = IKeyNamingPolicy.defaultKeyNamingPolicy;
		
		Cache cache = new Cache(cacheName, jedisPool, serializer, keyNamingPolicy);
		Redis.addCache(cache);
		return true;
	}
	
	public boolean stop() {
		Cache cache = Redis.removeCache(cacheName);
		if (cache == Redis.mainCache)
			Redis.mainCache = null;
		cache.jedisPool.destroy();
		return true;
	}
	
	/**
	 * 当RedisPlugin �??供的设置属性�?然无法满足需求时，通过此方法获�?�到
	 * JedisPoolConfig 对象，�?�对 redis 进行更加细致的�?置
	 * <pre>
	 * 例如：
	 * redisPlugin.getJedisPoolConfig().setMaxTotal(100);
	 * </pre>
	 */
	public JedisPoolConfig getJedisPoolConfig() {
		return jedisPoolConfig;
	}
	
	// ---------
	
	public void setSerializer(ISerializer serializer) {
		this.serializer = serializer;
	}
	
	public void setKeyNamingPolicy(IKeyNamingPolicy keyNamingPolicy) {
		this.keyNamingPolicy = keyNamingPolicy;
	}
	
	// ---------
	
	public void setTestWhileIdle(boolean testWhileIdle) {
		jedisPoolConfig.setTestWhileIdle(testWhileIdle);
	}
	
	public void setMinEvictableIdleTimeMillis(int minEvictableIdleTimeMillis) {
		jedisPoolConfig.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
	}
	
	public void setTimeBetweenEvictionRunsMillis(int timeBetweenEvictionRunsMillis) {
		jedisPoolConfig.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
	}
	
	public void setNumTestsPerEvictionRun(int numTestsPerEvictionRun) {
		jedisPoolConfig.setNumTestsPerEvictionRun(numTestsPerEvictionRun);
	}
}


