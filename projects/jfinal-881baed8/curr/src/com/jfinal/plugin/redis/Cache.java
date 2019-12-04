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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import com.jfinal.plugin.redis.serializer.ISerializer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * Cache.
 * Cache api 添加了中文注释，便于工程师更方便使用，�?�外还原样�?�?了
 * Jedis api 的方法�??称�?�使用方法，以便于仅仅通过查看 Redis 文档
 * �?��?�快速掌�?�使用方法
 * Redis 命令�?�考: http://redisdoc.com/
 */
public class Cache {
	
	final String name;
	final JedisPool jedisPool;
	final ISerializer serializer;
	final IKeyNamingPolicy keyNamingPolicy;
	
	private final ThreadLocal<Jedis> threadLocalJedis = new ThreadLocal<Jedis>();
	
	Cache(String name, JedisPool jedisPool, ISerializer serializer, IKeyNamingPolicy keyNamingPolicy) {
		this.name = name;
		this.jedisPool = jedisPool;
		this.serializer = serializer;
		this.keyNamingPolicy = keyNamingPolicy;
	}
	
	/**
	 * 存放 key value 对到 redis
	 * 如果 key 已�?�?有其他值， SET 就覆写旧值，无视类型。
	 * 对于�?个原本带有生存时间（TTL）的键�?�说， 当 SET 命令�?功在这个键上执行时， 这个键原有的 TTL 将被清除。
	 */
	public String set(Object key, Object value) {
		Jedis jedis = getJedis();
		try {
			return jedis.set(keyToBytes(key), valueToBytes(value));
		}
		finally {close(jedis);}
	}
	
	/**
	 * 存放 key value 对到 redis，并将 key 的生存时间设为 seconds (以秒为�?��?)。
	 * 如果 key 已�?存在， SETEX 命令将覆写旧值。
	 */
	public String setex(Object key, int seconds, Object value) {
		Jedis jedis = getJedis();
		try {
			return jedis.setex(keyToBytes(key), seconds, valueToBytes(value));
		}
		finally {close(jedis);}
	}
	
	/**
	 * 返回 key 所关�?�的 value 值
	 * 如果 key �?存在那么返回特殊值 nil 。
	 */
	@SuppressWarnings("unchecked")
	public <T> T get(Object key) {
		Jedis jedis = getJedis();
		try {
			return (T)valueFromBytes(jedis.get(keyToBytes(key)));
		}
		finally {close(jedis);}
	}
	
	/**
	 * 删除给定的一个 key
	 * �?存在的 key 会被忽略。
	 */
	public Long del(Object key) {
		Jedis jedis = getJedis();
		try {
			return jedis.del(keyToBytes(key));
		}
		finally {close(jedis);}
	}
	
	/**
	 * 删除给定的多个 key
	 * �?存在的 key 会被忽略。
	 */
	public Long del(Object... keys) {
		Jedis jedis = getJedis();
		try {
			return jedis.del(keysToBytesArray(keys));
		}
		finally {close(jedis);}
	}
	
	/**
	 * 查找所有符�?�给定模�? pattern 的 key 。
	 * KEYS * 匹�?数�?�库中所有 key 。
	 * KEYS h?llo 匹�? hello ， hallo 和 hxllo 等。
	 * KEYS h*llo 匹�? hllo 和 heeeeello 等。
	 * KEYS h[ae]llo 匹�? hello 和 hallo ，但�?匹�? hillo 。
	 * 特殊符�?�用 \ 隔开
	 */
	public Set<String> keys(String pattern) {
		Jedis jedis = getJedis();
		try {
			return jedis.keys(pattern);
		}
		finally {close(jedis);}
	}
	
	/**
	 * �?�时设置一个或多个 key-value 对。
	 * 如果�?个给定 key 已�?存在，那么 MSET 会用新值覆盖原�?�的旧值，如果这�?是你所希望的效果，请考虑使用 MSETNX 命令：它�?�会在所有给定 key 都�?存在的情况下进行设置�?作。
	 * MSET 是一个原�?性(atomic)�?作，所有给定 key 都会在�?�一时间内被设置，�?些给定 key 被更新而�?�一些给定 key 没有改�?�的情况，�?�?�能�?�生。
	 * <pre>
	 * 例�?：
	 * Cache cache = RedisKit.use();			// 使用 Redis 的 cache
	 * cache.mset("k1", "v1", "k2", "v2");		// 放入多个 key value 键值对
	 * List list = cache.mget("k1", "k2");		// 利用多个键值得到上�?�代�?放入的值
	 * </pre>
	 */
	public String mset(Object... keysValues) {
		if (keysValues.length % 2 != 0)
			throw new IllegalArgumentException("wrong number of arguments for met, keysValues length can not be odd");
		Jedis jedis = getJedis();
		try {
			byte[][] kv = new byte[keysValues.length][];
			for (int i=0; i<keysValues.length; i++) {
				if (i % 2 == 0)
					kv[i] = keyToBytes(keysValues[i]);
				else
					kv[i] = valueToBytes(keysValues[i]);
			}
			return jedis.mset(kv);
		}
		finally {close(jedis);}
	}
	
	/**
	 * 返回所有(一个或多个)给定 key 的值。
	 * 如果给定的 key 里�?�，有�?个 key �?存在，那么这个 key 返回特殊值 nil 。因此，该命令永�?失败。
	 */
	@SuppressWarnings("rawtypes")
	public List mget(Object... keys) {
		Jedis jedis = getJedis();
		try {
			byte[][] keysBytesArray = keysToBytesArray(keys);
			List<byte[]> data = jedis.mget(keysBytesArray);
			return valueListFromBytesList(data);
		}
		finally {close(jedis);}
	}
	
	/**
	 * 将 key 中储存的数字值�?一。
	 * 如果 key �?存在，那么 key 的值会先被�?始化为 0 ，然�?��?执行 DECR �?作。
	 * 如果值包�?�错误的类型，或字符串类型的值�?能表示为数字，那么返回一个错误。
	 * 本�?作的值�?制在 64 �?(bit)有符�?�数字表示之内。
	 * 关于递增(increment) / 递�?(decrement)�?作的更多信�?�，请�?��? INCR 命令。
	 */
	public Long decr(Object key) {
		Jedis jedis = getJedis();
		try {
			return jedis.decr(keyToBytes(key));
		}
		finally {close(jedis);}
	}
	
	/**
	 * 将 key 所储存的值�?去�?�? decrement 。
	 * 如果 key �?存在，那么 key 的值会先被�?始化为 0 ，然�?��?执行 DECRBY �?作。
	 * 如果值包�?�错误的类型，或字符串类型的值�?能表示为数字，那么返回一个错误。
	 * 本�?作的值�?制在 64 �?(bit)有符�?�数字表示之内。
	 * 关于更多递增(increment) / 递�?(decrement)�?作的更多信�?�，请�?��? INCR 命令。
	 */
	public Long decrBy(Object key, long longValue) {
		Jedis jedis = getJedis();
		try {
			return jedis.decrBy(keyToBytes(key), longValue);
		}
		finally {close(jedis);}
	}
	
	/**
	 * 将 key 中储存的数字值增一。
	 * 如果 key �?存在，那么 key 的值会先被�?始化为 0 ，然�?��?执行 INCR �?作。
	 * 如果值包�?�错误的类型，或字符串类型的值�?能表示为数字，那么返回一个错误。
	 * 本�?作的值�?制在 64 �?(bit)有符�?�数字表示之内。
	 */
	public Long incr(Object key) {
		Jedis jedis = getJedis();
		try {
			return jedis.incr(keyToBytes(key));
		}
		finally {close(jedis);}
	}
	
	/**
	 * 将 key 所储存的值加上增�? increment 。
	 * 如果 key �?存在，那么 key 的值会先被�?始化为 0 ，然�?��?执行 INCRBY 命令。
	 * 如果值包�?�错误的类型，或字符串类型的值�?能表示为数字，那么返回一个错误。
	 * 本�?作的值�?制在 64 �?(bit)有符�?�数字表示之内。
	 * 关于递增(increment) / 递�?(decrement)�?作的更多信�?�，�?��? INCR 命令。 
	 */
	public Long incrBy(Object key, long longValue) {
		Jedis jedis = getJedis();
		try {
			return jedis.incrBy(keyToBytes(key), longValue);
		}
		finally {close(jedis);}
	}
	
	/**
	 * 检查给定 key 是�?�存在。
	 */
	public boolean exists(Object key) {
		Jedis jedis = getJedis();
		try {
			return jedis.exists(keyToBytes(key));
		}
		finally {close(jedis);}
	}
	
	/**
	 * 从当�?数�?�库中�?机返回(�?删除)一个 key 。
	 */
	public String randomKey() {
		Jedis jedis = getJedis();
		try {
			return jedis.randomKey();
		}
		finally {close(jedis);}
	}
	
	/**
	 * 将 key 改�??为 newkey 。
	 * 当 key 和 newkey 相�?�，或者 key �?存在时，返回一个错误。
	 * 当 newkey 已�?存在时， RENAME 命令将覆盖旧值。
	 */
	public String rename(Object oldkey, Object newkey) {
		Jedis jedis = getJedis();
		try {
			return jedis.rename(keyToBytes(oldkey), keyToBytes(newkey));
		}
		finally {close(jedis);}
	}
	
	/**
	 * 将当�?数�?�库的 key 移动到给定的数�?�库 db 当中。
	 * 如果当�?数�?�库(�?数�?�库)和给定数�?�库(目标数�?�库)有相�?��??字的给定 key ，或者 key �?存在于当�?数�?�库，那么 MOVE 没有任何效果。
	 * 因此，也�?�以利用这一特性，将 MOVE 当作�?(locking)原语(primitive)。
	 */
	public Long move(Object key, int dbIndex) {
		Jedis jedis = getJedis();
		try {
			return jedis.move(keyToBytes(key), dbIndex);
		}
		finally {close(jedis);}
	}
	
	/**
	 * 将 key 原�?性地从当�?实例传�?到目标实例的指定数�?�库上，一旦传�?�?功， key �?�?会出现在目标实例上，而当�?实例上的 key 会被删除。
	 */
	public String migrate(String host, int port, Object key, int destinationDb, int timeout) {
		Jedis jedis = getJedis();
		try {
			return jedis.migrate(valueToBytes(host), port, keyToBytes(key), destinationDb, timeout);
		}
		finally {close(jedis);}
	}
	
	/**
	 * 切�?�到指定的数�?�库，数�?�库索引�?� index 用数字值指定，以 0 作为起始索引值。
	 * 默认使用 0 �?�数�?�库。
	 * 注�?：在 Jedis 对象被关闭时，数�?�库�?�会�?新被设置为 1，所以本方法 select(...)
	 * 正常工作需�?使用如下方�?之一：
	 * 1：使用 RedisInterceptor，在本线程内共享�?�一个 Jedis 对象
	 * 2：使用 Redis.call(ICallback) 进行�?作
	 * 2：自行获�?� Jedis 对象进行�?作
	 */
	public String select(int databaseIndex) {
		Jedis jedis = getJedis();
		try {
			return jedis.select(databaseIndex);
		}
		finally {close(jedis);}
	}
	
	/**
	 * 为给定 key 设置生存时间，当 key 过期时(生存时间为 0 )，它会被自动删除。
	 * 在 Redis 中，带有生存时间的 key 被称为『易失的�?(volatile)。
	 */
	public Long expire(Object key, int seconds) {
		Jedis jedis = getJedis();
		try {
			return jedis.expire(keyToBytes(key), seconds);
		}
		finally {close(jedis);}
	}
	
	/**
	 * 这个命令和 EXPIREAT 命令类似，但它以毫秒为�?��?设置 key 的过期 unix 时间戳，而�?是�? EXPIREAT 那样，以秒为�?��?。
	 */
	public Long expireAt(Object key, long unixTime) {
		Jedis jedis = getJedis();
		try {
			return jedis.expireAt(keyToBytes(key), unixTime);
		}
		finally {close(jedis);}
	}
	
	/**
	 * 这个命令和 EXPIRE 命令的作用类似，但是它以毫秒为�?��?设置 key 的生存时间，而�?�? EXPIRE 命令那样，以秒为�?��?。
	 */
	public Long pexpire(Object key, long milliseconds) {
		Jedis jedis = getJedis();
		try {
			return jedis.pexpire(keyToBytes(key), milliseconds);
		}
		finally {close(jedis);}
	}
	
	/**
	 * 这个命令和 EXPIREAT 命令类似，但它以毫秒为�?��?设置 key 的过期 unix 时间戳，而�?是�? EXPIREAT 那样，以秒为�?��?。
	 */
	public Long pexpireAt(Object key, long millisecondsTimestamp) {
		Jedis jedis = getJedis();
		try {
			return jedis.pexpireAt(keyToBytes(key), millisecondsTimestamp);
		}
		finally {close(jedis);}
	}
	
	/**
	 * 将给定 key 的值设为 value ，并返回 key 的旧值(old value)。
	 * 当 key 存在但�?是字符串类型时，返回一个错误。
	 */
	@SuppressWarnings("unchecked")
	public <T> T getSet(Object key, Object value) {
		Jedis jedis = getJedis();
		try {
			return (T)valueFromBytes(jedis.getSet(keyToBytes(key), valueToBytes(value)));
		}
		finally {close(jedis);}
	}
	
	/**
	 * 移除给定 key 的生存时间，将这个 key 从『易失的�?(带生存时间 key )转�?��?『�?久的�?(一个�?带生存时间�?永�?过期的 key )。
	 */
	public Long persist(Object key) {
		Jedis jedis = getJedis();
		try {
			return jedis.persist(keyToBytes(key));
		}
		finally {close(jedis);}
	}
	
	/**
	 * 返回 key 所储存的值的类型。
	 */
	public String type(Object key) {
		Jedis jedis = getJedis();
		try {
			return jedis.type(keyToBytes(key));
		}
		finally {close(jedis);}
	}
	
	/**
	 * 以秒为�?��?，返回给定 key 的剩余生存时间(TTL, time to live)。
	 */
	public Long ttl(Object key) {
		Jedis jedis = getJedis();
		try {
			return jedis.ttl(keyToBytes(key));
		}
		finally {close(jedis);}
	}
	
	/**
	 * 这个命令类似于 TTL 命令，但它以毫秒为�?��?返回 key 的剩余生存时间，而�?是�? TTL 命令那样，以秒为�?��?。
	 */
	public Long pttl(Object key) {
		Jedis jedis = getJedis();
		try {
			return jedis.pttl(keyToBytes(key));
		}
		finally {close(jedis);}
	}
	
	/**
	 * 对象被引用的数�?
	 */
	public Long objectRefcount(Object key) {
		Jedis jedis = getJedis();
		try {
			return jedis.objectRefcount(keyToBytes(key));
		}
		finally {close(jedis);}
	}
	
	/**
	 * 对象没有被访问的空闲时间
	 */
	public Long objectIdletime(Object key) {
		Jedis jedis = getJedis();
		try {
			return jedis.objectIdletime(keyToBytes(key));
		}
		finally {close(jedis);}
	}
	
	/**
	 * 将哈希表 key 中的域 field 的值设为 value 。
	 * 如果 key �?存在，一个新的哈希表被创建并进行 HSET �?作。
	 * 如果域 field 已�?存在于哈希表中，旧值将被覆盖。
	 */
	public Long hset(Object key, Object field, Object value) {
		Jedis jedis = getJedis();
		try {
			return jedis.hset(keyToBytes(key), keyToBytes(field), valueToBytes(value));
		}
		finally {close(jedis);}
	}
	
	/**
	 * �?�时将多个 field-value (域-值)对设置到哈希表 key 中。
	 * 此命令会覆盖哈希表中已存在的域。
	 * 如果 key �?存在，一个空哈希表被创建并执行 HMSET �?作。
	 */
	public String hmset(Object key, Map<Object, Object> hash) {
		Jedis jedis = getJedis();
		try {
			Map<byte[], byte[]> para = new HashMap<byte[], byte[]>();
			for (Entry<Object, Object> e : hash.entrySet())
				para.put(keyToBytes(e.getKey()), valueToBytes(e.getValue()));
			return jedis.hmset(keyToBytes(key), para);
		}
		finally {close(jedis);}
	}
	
	/**
	 * 返回哈希表 key 中给定域 field 的值。
	 */
	@SuppressWarnings("unchecked")
	public <T> T hget(Object key, Object field) {
		Jedis jedis = getJedis();
		try {
			return (T)valueFromBytes(jedis.hget(keyToBytes(key), keyToBytes(field)));
		}
		finally {close(jedis);}
	}
	
	/**
	 * 返回哈希表 key 中，一个或多个给定域的值。
	 * 如果给定的域�?存在于哈希表，那么返回一个 nil 值。
	 * 因为�?存在的 key 被当作一个空哈希表�?�处�?�，所以对一个�?存在的 key 进行 HMGET �?作将返回一个�?�带有 nil 值的表。
	 */
	@SuppressWarnings("rawtypes")
	public List hmget(Object key, Object... fields) {
		Jedis jedis = getJedis();
		try {
			List<byte[]> data = jedis.hmget(keyToBytes(key), keysToBytesArray(fields));
			return valueListFromBytesList(data);
		}
		finally {close(jedis);}
	}
	
	/**
	 * 删除哈希表 key 中的一个或多个指定域，�?存在的域将被忽略。
	 */
	public Long hdel(Object key, Object... fields) {
		Jedis jedis = getJedis();
		try {
			return jedis.hdel(keyToBytes(key), keysToBytesArray(fields));
		}
		finally {close(jedis);}
	}
	
	/**
	 * 查看哈希表 key 中，给定域 field 是�?�存在。
	 */
	public boolean hexists(Object key, Object field) {
		Jedis jedis = getJedis();
		try {
			return jedis.hexists(keyToBytes(key), keyToBytes(field));
		}
		finally {close(jedis);}
	}
	
	/**
	 * 返回哈希表 key 中，所有的域和值。
	 * 在返回值里，紧跟�?个域�??(field name)之�?�是域的值(value)，所以返回值的长度是哈希表大�?的两�?。
	 */
	@SuppressWarnings("rawtypes")
	public Map hgetAll(Object key) {
		Jedis jedis = getJedis();
		try {
			Map<byte[], byte[]> data = jedis.hgetAll(keyToBytes(key));
			Map<Object, Object> result = new HashMap<Object, Object>();
			for (Entry<byte[], byte[]> e : data.entrySet())
				result.put(keyFromBytes(e.getKey()), valueFromBytes(e.getValue()));
			return result;
		}
		finally {close(jedis);}
	}
	
	/**
	 * 返回哈希表 key 中所有域的值。
	 */
	@SuppressWarnings("rawtypes")
	public List hvals(Object key) {
		Jedis jedis = getJedis();
		try {
			List<byte[]> data = jedis.hvals(keyToBytes(key));
			return valueListFromBytesList(data);
		}
		finally {close(jedis);}
	}
	
	/**
	 * 返回哈希表 key 中的所有域。
	 */
	public Set<String> hkeys(Object key) {
		Jedis jedis = getJedis();
		try {
			Set<byte[]> keySet = jedis.hkeys(keyToBytes(key));
			return keySetFromBytesSet(keySet);	// 返回 key 的方法�?能使用 valueSetFromBytesSet(...)
		}
		finally {close(jedis);}
	}
	
	/**
	 * 返回哈希表 key 中域的数�?。 
	 */
	public Long hlen(Object key) {
		Jedis jedis = getJedis();
		try {
			return jedis.hlen(keyToBytes(key));
		}
		finally {close(jedis);}
	}
	
	/**
	 * 返回列表 key 中，下标为 index 的元素。
	 * 下标(index)�?�数 start 和 stop 都以 0 为底，也就是说，以 0 表示列表的第一个元素，以 1 表示列表的第二个元素，以此类推。
	 * 你也�?�以使用负数下标，以 -1 表示列表的最�?�一个元素， -2 表示列表的倒数第二个元素，以此类推。
	 * 如果 key �?是列表类型，返回一个错误。
	 */
	@SuppressWarnings("unchecked")
	
	/**
	 * 返回列表 key 中，下标为 index 的元素。
	 * 下标(index)�?�数 start 和 stop 都以 0 为底，也就是说，以 0 表示列表的第一个元素，
	 * 以 1 表示列表的第二个元素，以此类推。
	 * 你也�?�以使用负数下标，以 -1 表示列表的最�?�一个元素， -2 表示列表的倒数第二个元素，以此类推。
	 * 如果 key �?是列表类型，返回一个错误。
	 */
	public <T> T lindex(Object key, long index) {
		Jedis jedis = getJedis();
		try {
			return (T)valueFromBytes(jedis.lindex(keyToBytes(key), index));
		}
		finally {close(jedis);}
	}
	
	/**
	 * 获�?�记数器的值
	 */
	public Long getCounter(Object key) {
		Jedis jedis = getJedis();
		try {
			return Long.parseLong((String)jedis.get(keyNamingPolicy.getKeyName(key)));
		}
		finally {close(jedis);}
	}
	
	/**
	 * 返回列表 key 的长度。
	 * 如果 key �?存在，则 key 被解释为一个空列表，返回 0 .
	 * 如果 key �?是列表类型，返回一个错误。
	 */
	public Long llen(Object key) {
		Jedis jedis = getJedis();
		try {
			return jedis.llen(keyToBytes(key));
		}
		finally {close(jedis);}
	}
	
	/**
	 * 移除并返回列表 key 的头元素。
	 */
	@SuppressWarnings("unchecked")
	public <T> T lpop(Object key) {
		Jedis jedis = getJedis();
		try {
			return (T)valueFromBytes(jedis.lpop(keyToBytes(key)));
		}
		finally {close(jedis);}
	}
	
	/**
	 * 将一个或多个值 value �?�入到列表 key 的表头
	 * 如果有多个 value 值，那么�?�个 value 值按从左到�?�的顺�?�?次�?�入到表头： 比如说，
	 * 对空列表 mylist 执行命令 LPUSH mylist a b c ，列表的值将是 c b a ，
	 * 这等�?�于原�?性地执行 LPUSH mylist a �? LPUSH mylist b 和 LPUSH mylist c 三个命令。
	 * 如果 key �?存在，一个空列表会被创建并执行 LPUSH �?作。
	 * 当 key 存在但�?是列表类型时，返回一个错误。
	 */
	public Long lpush(Object key, Object... values) {
		Jedis jedis = getJedis();
		try {
			return jedis.lpush(keyToBytes(key), valuesToBytesArray(values));
		}
		finally {close(jedis);}
	}
	
	/**
	 * 将列表 key 下标为 index 的元素的值设置为 value 。
	 * 当 index �?�数超出范围，或对一个空列表( key �?存在)进行 LSET 时，返回一个错误。
	 * 关于列表下标的更多信�?�，请�?�考 LINDEX 命令。
	 */
	public String lset(Object key, long index, Object value) {
		Jedis jedis = getJedis();
		try {
			return jedis.lset(keyToBytes(key), index, valueToBytes(value));
		}
		finally {close(jedis);}
	}
	
	/**
	 * 根�?��?�数 count 的值，移除列表中与�?�数 value 相等的元素。
	 * count 的值�?�以是以下几�?：
	 * count > 0 : 从表头开始�?�表尾�?�索，移除与 value 相等的元素，数�?为 count 。
	 * count < 0 : 从表尾开始�?�表头�?�索，移除与 value 相等的元素，数�?为 count 的�?对值。
	 * count = 0 : 移除表中所有与 value 相等的值。
	 */
	public Long lrem(Object key, long count, Object value) {
		Jedis jedis = getJedis();
		try {
			return jedis.lrem(keyToBytes(key), count, valueToBytes(value));
		}
		finally {close(jedis);}
	}
	
	/**
	 * 返回列表 key 中指定区间内的元素，区间以�??移�? start 和 stop 指定。
	 * 下标(index)�?�数 start 和 stop 都以 0 为底，也就是说，以 0 表示列表的第一个元素，以 1 表示列表的第二个元素，以此类推。
	 * 你也�?�以使用负数下标，以 -1 表示列表的最�?�一个元素， -2 表示列表的倒数第二个元素，以此类推。
	 * <pre>
	 * 例�?：
	 * 获�?� list 中所有数�?�：cache.lrange(listKey, 0, -1);
	 * 获�?� list 中下标 1 到 3 的数�?�： cache.lrange(listKey, 1, 3);
	 * </pre>
	 */
	@SuppressWarnings("rawtypes")
	public List lrange(Object key, long start, long end) {
		Jedis jedis = getJedis();
		try {
			List<byte[]> data = jedis.lrange(keyToBytes(key), start, end);
			return valueListFromBytesList(data);
		}
		finally {close(jedis);}
	}
	
	/**
	 * 对一个列表进行修剪(trim)，就是说，让列表�?��?留指定区间内的元素，�?在指定区间之内的元素都将被删除。
	 * 举个例�?，执行命令 LTRIM list 0 2 ，表示�?��?留列表 list 的�?三个元素，其余元素全部删除。
	 * 下标(index)�?�数 start 和 stop 都以 0 为底，也就是说，以 0 表示列表的第一个元素，以 1 表示列表的第二个元素，以此类推。
	 * 你也�?�以使用负数下标，以 -1 表示列表的最�?�一个元素， -2 表示列表的倒数第二个元素，以此类推。
	 * 当 key �?是列表类型时，返回一个错误。
	 */
	public String ltrim(Object key, long start, long end) {
		Jedis jedis = getJedis();
		try {
			return jedis.ltrim(keyToBytes(key), start, end);
		}
		finally {close(jedis);}
	}
	
	/**
	 * 移除并返回列表 key 的尾元素。
	 */
	@SuppressWarnings("unchecked")
	public <T> T rpop(Object key) {
		Jedis jedis = getJedis();
		try {
			return (T)valueFromBytes(jedis.rpop(keyToBytes(key)));
		}
		finally {close(jedis);}
	}
	
	/**
	 * 命令 RPOPLPUSH 在一个原�?时间内，执行以下两个动作：
	 * 将列表 source 中的最�?�一个元素(尾元素)弹出，并返回给客户端。
	 * 将 source 弹出的元素�?�入到列表 destination ，作为 destination 列表的的头元素。
	 */
	@SuppressWarnings("unchecked")
	public <T> T rpoplpush(Object srcKey, Object dstKey) {
		Jedis jedis = getJedis();
		try {
			return (T)valueFromBytes(jedis.rpoplpush(keyToBytes(srcKey), keyToBytes(dstKey)));
		}
		finally {close(jedis);}
	}
	
	/**
	 * 将一个或多个值 value �?�入到列表 key 的表尾(最�?�边)。
	 * 如果有多个 value 值，那么�?�个 value 值按从左到�?�的顺�?�?次�?�入到表尾：比如
	 * 对一个空列表 mylist 执行 RPUSH mylist a b c ，得出的结果列表为 a b c ，
	 * 等�?�于执行命令 RPUSH mylist a �? RPUSH mylist b �? RPUSH mylist c 。
	 * 如果 key �?存在，一个空列表会被创建并执行 RPUSH �?作。
	 * 当 key 存在但�?是列表类型时，返回一个错误。
	 */
	public Long rpush(Object key, Object... values) {
		Jedis jedis = getJedis();
		try {
			return jedis.rpush(keyToBytes(key), valuesToBytesArray(values));
		}
		finally {close(jedis);}
	}
	
	/**
	 * BLPOP 是列表的阻塞�?(blocking)弹出原语。
	 * 它是 LPOP 命令的阻塞版本，当给定列表内没有任何元素�?�供弹出的时候，连接将被 BLPOP 命令阻塞，直到等待超时或�?�现�?�弹出元素为止。
	 * 当给定多个 key �?�数时，按�?�数 key 的先�?�顺�?�?次检查�?�个列表，弹出第一个�?�空列表的头元素。
	 */
	@SuppressWarnings("rawtypes")
	public List blpop(Object... keys) {
		Jedis jedis = getJedis();
		try {
			List<byte[]> data = jedis.blpop(keysToBytesArray(keys));
			return valueListFromBytesList(data);
		}
		finally {close(jedis);}
	}
	
	/**
	 * BLPOP 是列表的阻塞�?(blocking)弹出原语。
	 * 它是 LPOP 命令的阻塞版本，当给定列表内没有任何元素�?�供弹出的时候，连接将被 BLPOP 命令阻塞，直到等待超时或�?�现�?�弹出元素为止。
	 * 当给定多个 key �?�数时，按�?�数 key 的先�?�顺�?�?次检查�?�个列表，弹出第一个�?�空列表的头元素。
	 */
	@SuppressWarnings("rawtypes")
	public List blpop(int timeout, Object... keys) {
		Jedis jedis = getJedis();
		try {
			List<byte[]> data = jedis.blpop(timeout, keysToBytesArray(keys));
			return valueListFromBytesList(data);
		}
		finally {close(jedis);}
	}
	
	/**
	 * BRPOP 是列表的阻塞�?(blocking)弹出原语。
	 * 它是 RPOP 命令的阻塞版本，当给定列表内没有任何元素�?�供弹出的时候，连接将被 BRPOP 命令阻塞，直到等待超时或�?�现�?�弹出元素为止。
	 * 当给定多个 key �?�数时，按�?�数 key 的先�?�顺�?�?次检查�?�个列表，弹出第一个�?�空列表的尾部元素。
	 * 关于阻塞�?作的更多信�?�，请查看 BLPOP 命令， BRPOP 除了弹出元素的�?置和 BLPOP �?�?�之外，其他表现一致。
	 */
	@SuppressWarnings("rawtypes")
	public List brpop(Object... keys) {
		Jedis jedis = getJedis();
		try {
			List<byte[]> data = jedis.brpop(keysToBytesArray(keys));
			return valueListFromBytesList(data);
		}
		finally {close(jedis);}
	}
	
	/**
	 * BRPOP 是列表的阻塞�?(blocking)弹出原语。
	 * 它是 RPOP 命令的阻塞版本，当给定列表内没有任何元素�?�供弹出的时候，连接将被 BRPOP 命令阻塞，直到等待超时或�?�现�?�弹出元素为止。
	 * 当给定多个 key �?�数时，按�?�数 key 的先�?�顺�?�?次检查�?�个列表，弹出第一个�?�空列表的尾部元素。
	 * 关于阻塞�?作的更多信�?�，请查看 BLPOP 命令， BRPOP 除了弹出元素的�?置和 BLPOP �?�?�之外，其他表现一致。
	 */
	@SuppressWarnings("rawtypes")
	public List brpop(int timeout, Object... keys) {
		Jedis jedis = getJedis();
		try {
			List<byte[]> data = jedis.brpop(timeout, keysToBytesArray(keys));
			return valueListFromBytesList(data);
		}
		finally {close(jedis);}
	}
	
	/**
	 * 使用客户端�?� Redis �?务器�?��?一个 PING ，如果�?务器�?作正常的�?，会返回一个 PONG 。
	 * 通常用于测试与�?务器的连接是�?��?然生效，或者用于测�?延迟值。
	 */
	public String ping() {
		Jedis jedis = getJedis();
		try {
			return jedis.ping();
		}
		finally {close(jedis);}
	}
	
	/**
	 * 将一个或多个 member 元素加入到集�?� key 当中，已�?存在于集�?�的 member 元素将被忽略。
	 * �?�如 key �?存在，则创建一个�?�包�?� member 元素作�?员的集�?�。
	 * 当 key �?是集�?�类型时，返回一个错误。
	 */
	public Long sadd(Object key, Object... members) {
		Jedis jedis = getJedis();
		try {
			return jedis.sadd(keyToBytes(key), valuesToBytesArray(members));
		}
		finally {close(jedis);}
	}
	
	/**
	 * 返回集�?� key 的基数(集�?�中元素的数�?)。
	 */
	public Long scard(Object key) {
		Jedis jedis = getJedis();
		try {
			return jedis.scard(keyToBytes(key));
		}
		finally {close(jedis);}
	}
	
	/**
	 * 移除并返回集�?�中的一个�?机元素。
	 * 如果�?�想获�?�一个�?机元素，但�?想该元素从集�?�中被移除的�?，�?�以使用 SRANDMEMBER 命令。
	 */
	@SuppressWarnings("unchecked")
	public <T> T spop(Object key) {
		Jedis jedis = getJedis();
		try {
			return (T)valueFromBytes(jedis.spop(keyToBytes(key)));
		}
		finally {close(jedis);}
	}
	
	/**
	 * 返回集�?� key 中的所有�?员。
	 * �?存在的 key 被视为空集�?�。
	 */
	@SuppressWarnings("rawtypes")
	public Set smembers(Object key) {
		Jedis jedis = getJedis();
		try {
			Set<byte[]> data = jedis.smembers(keyToBytes(key));
			Set<Object> result = new HashSet<Object>();
			valueSetFromBytesSet(data, result);
			return result;
		}
		finally {close(jedis);}
	}
	
	/**
	 * 判断 member 元素是�?�集�?� key 的�?员。
	 */
	public boolean sismember(Object key, Object member) {
		Jedis jedis = getJedis();
		try {
			return jedis.sismember(keyToBytes(key), valueToBytes(member));
		}
		finally {close(jedis);}
	}
	
	/**
	 * 返回多个集�?�的交集，多个集�?�由 keys 指定
	 */
	@SuppressWarnings("rawtypes")
	public Set sinter(Object... keys) {
		Jedis jedis = getJedis();
		try {
			Set<byte[]> data = jedis.sinter(keysToBytesArray(keys));
			Set<Object> result = new HashSet<Object>();
			valueSetFromBytesSet(data, result);
			return result;
		}
		finally {close(jedis);}
	}
	
	/**
	 * 返回集�?�中的一个�?机元素。
	 */
	@SuppressWarnings("unchecked")
	public <T> T srandmember(Object key) {
		Jedis jedis = getJedis();
		try {
			return (T)valueFromBytes(jedis.srandmember(keyToBytes(key)));
		}
		finally {close(jedis);}
	}
	
	/**
	 * 返回集�?�中的 count 个�?机元素。
	 * 从 Redis 2.6 版本开始， SRANDMEMBER 命令接�?��?�选的 count �?�数：
	 * 如果 count 为正数，且�?于集�?�基数，那么命令返回一个包�?� count 个元素的数组，数组中的元素�?��?相�?�。
	 * 如果 count 大于等于集�?�基数，那么返回整个集�?�。
	 * 如果 count 为负数，那么命令返回一个数组，数组中的元素�?�能会�?�?出现多次，而数组的长度为 count 的�?对值。
	 * 该�?作和 SPOP 相似，但 SPOP 将�?机元素从集�?�中移除并返回，而 SRANDMEMBER 则仅仅返回�?机元素，而�?对集�?�进行任何改动。
	 */
	@SuppressWarnings("rawtypes")
	public List srandmember(Object key, int count) {
		Jedis jedis = getJedis();
		try {
			List<byte[]> data = jedis.srandmember(keyToBytes(key), count);
			return valueListFromBytesList(data);
		}
		finally {close(jedis);}
	}
	
	/**
	 * 移除集�?� key 中的一个或多个 member 元素，�?存在的 member 元素会被忽略。
	 */
	public Long srem(Object key, Object... members) {
		Jedis jedis = getJedis();
		try {
			return jedis.srem(keyToBytes(key), valuesToBytesArray(members));
		}
		finally {close(jedis);}
	}
	
	/**
	 * 返回多个集�?�的并集，多个集�?�由 keys 指定
	 * �?存在的 key 被视为空集。
	 */
	@SuppressWarnings("rawtypes")
	public Set sunion(Object... keys) {
		Jedis jedis = getJedis();
		try {
			Set<byte[]> data = jedis.sunion(keysToBytesArray(keys));
			Set<Object> result = new HashSet<Object>();
			valueSetFromBytesSet(data, result);
			return result;
		}
		finally {close(jedis);}
	}
	
	/**
	 * 返回一个集�?�的全部�?员，该集�?�是所有给定集�?�之间的差集。
	 * �?存在的 key 被视为空集。
	 */
	@SuppressWarnings("rawtypes")
	public Set sdiff(Object... keys) {
		Jedis jedis = getJedis();
		try {
			Set<byte[]> data = jedis.sdiff(keysToBytesArray(keys));
			Set<Object> result = new HashSet<Object>();
			valueSetFromBytesSet(data, result);
			return result;
		}
		finally {close(jedis);}
	}
	
	/**
	 * 将一个或多个 member 元素�?�其 score 值加入到有�?集 key 当中。
	 * 如果�?个 member 已�?是有�?集的�?员，那么更新这个 member 的 score 值，
	 * 并通过�?新�?�入这个 member 元素，�?��?�?该 member 在正确的�?置上。
	 */
	public Long zadd(Object key, double score, Object member) {
		Jedis jedis = getJedis();
		try {
			return jedis.zadd(keyToBytes(key), score, valueToBytes(member));
		}
		finally {close(jedis);}
	}
	
	public Long zadd(Object key, Map<Object, Double> scoreMembers) {
		Jedis jedis = getJedis();
		try {
			Map<byte[], Double> para = new HashMap<byte[], Double>();
			for (Entry<Object, Double> e : scoreMembers.entrySet())
				para.put(valueToBytes(e.getKey()), e.getValue());	// valueToBytes is important
			return jedis.zadd(keyToBytes(key), para);
		}
		finally {close(jedis);}
	}
	
	/**
	 * 返回有�?集 key 的基数。
	 */
	public Long zcard(Object key) {
		Jedis jedis = getJedis();
		try {
			return jedis.zcard(keyToBytes(key));
		}
		finally {close(jedis);}
	}
	
	/**
	 * 返回有�?集 key 中， score 值在 min 和 max 之间(默认包括 score 值等于 min 或 max )的�?员的数�?。
	 * 关于�?�数 min 和 max 的详细使用方法，请�?�考 ZRANGEBYSCORE 命令。
	 */
	public Long zcount(Object key, double min, double max) {
		Jedis jedis = getJedis();
		try {
			return jedis.zcount(keyToBytes(key), min, max);
		}
		finally {close(jedis);}
	}
	
	/**
	 * 为有�?集 key 的�?员 member 的 score 值加上增�? increment 。
	 */
	public Double zincrby(Object key, double score, Object member) {
		Jedis jedis = getJedis();
		try {
			return jedis.zincrby(keyToBytes(key), score, valueToBytes(member));
		}
		finally {close(jedis);}
	}
	
	/**
	 * 返回有�?集 key 中，指定区间内的�?员。
	 * 其中�?员的�?置按 score 值递增(从�?到大)�?�排�?。
	 * 具有相�?� score 值的�?员按字典�?(lexicographical order )�?�排列。
	 * 如果你需�?�?员按 score 值递�?(从大到�?)�?�排列，请使用 ZREVRANGE 命令。
	 */
	@SuppressWarnings("rawtypes")
	public Set zrange(Object key, long start, long end) {
		Jedis jedis = getJedis();
		try {
			Set<byte[]> data = jedis.zrange(keyToBytes(key), start, end);
			Set<Object> result = new LinkedHashSet<Object>();	// 有�?集�?�必须 LinkedHashSet
			valueSetFromBytesSet(data, result);
			return result;
		}
		finally {close(jedis);}
	}
	
	/**
	 * 返回有�?集 key 中，指定区间内的�?员。
	 * 其中�?员的�?置按 score 值递�?(从大到�?)�?�排列。
	 * 具有相�?� score 值的�?员按字典�?的逆�?(reverse lexicographical order)排列。
	 * 除了�?员按 score 值递�?的次�?排列这一点外， ZREVRANGE 命令的其他方�?�和 ZRANGE 命令一样。
	 */
	@SuppressWarnings("rawtypes")
	public Set zrevrange(Object key, long start, long end) {
		Jedis jedis = getJedis();
		try {
			Set<byte[]> data = jedis.zrevrange(keyToBytes(key), start, end);
			Set<Object> result = new LinkedHashSet<Object>();	// 有�?集�?�必须 LinkedHashSet
			valueSetFromBytesSet(data, result);
			return result;
		}
		finally {close(jedis);}
	}
	
	/**
	 * 返回有�?集 key 中，所有 score 值介于 min 和 max 之间(包括等于 min 或 max )的�?员。
	 * 有�?集�?员按 score 值递增(从�?到大)次�?排列。
	 */
	@SuppressWarnings("rawtypes")
	public Set zrangeByScore(Object key, double min, double max) {
		Jedis jedis = getJedis();
		try {
			Set<byte[]> data = jedis.zrangeByScore(keyToBytes(key), min, max);
			Set<Object> result = new LinkedHashSet<Object>();	// 有�?集�?�必须 LinkedHashSet
			valueSetFromBytesSet(data, result);
			return result;
		}
		finally {close(jedis);}
	}
	
	/**
	 * 返回有�?集 key 中�?员 member 的排�??。其中有�?集�?员按 score 值递增(从�?到大)顺�?排列。
	 * 排�??以 0 为底，也就是说， score 值最�?的�?员排�??为 0 。
	 * 使用 ZREVRANK 命令�?�以获得�?员按 score 值递�?(从大到�?)排列的排�??。
	 */
	public Long zrank(Object key, Object member) {
		Jedis jedis = getJedis();
		try {
			return jedis.zrank(keyToBytes(key), valueToBytes(member));
		}
		finally {close(jedis);}
	}
	
	/**
	 * 返回有�?集 key 中�?员 member 的排�??。其中有�?集�?员按 score 值递�?(从大到�?)排�?。
	 * 排�??以 0 为底，也就是说， score 值最大的�?员排�??为 0 。
	 * 使用 ZRANK 命令�?�以获得�?员按 score 值递增(从�?到大)排列的排�??。
	 */
	public Long zrevrank(Object key, Object member) {
		Jedis jedis = getJedis();
		try {
			return jedis.zrevrank(keyToBytes(key), valueToBytes(member));
		}
		finally {close(jedis);}
	}
	
	/**
	 * 移除有�?集 key 中的一个或多个�?员，�?存在的�?员将被忽略。
	 * 当 key 存在但�?是有�?集类型时，返回一个错误。
	 */
	public Long zrem(Object key, Object... members) {
		Jedis jedis = getJedis();
		try {
			return jedis.zrem(keyToBytes(key), valuesToBytesArray(members));
		}
		finally {close(jedis);}
	}
	
	/**
	 * 返回有�?集 key 中，�?员 member 的 score 值。
	 * 如果 member 元素�?是有�?集 key 的�?员，或 key �?存在，返回 nil 。
	 */
	public Double zscore(Object key, Object member) {
		Jedis jedis = getJedis();
		try {
			return jedis.zscore(keyToBytes(key), valueToBytes(member));
		}
		finally {close(jedis);}
	}
	
	// ---------
	
	private byte[] keyToBytes(Object key) {
		String keyStr = keyNamingPolicy.getKeyName(key);
		return serializer.keyToBytes(keyStr);
	}
	
	private String keyFromBytes(byte[] bytes) {
		return serializer.keyFromBytes(bytes);
	}
	
	private byte[][] keysToBytesArray(Object... keys) {
		byte[][] result = new byte[keys.length][];
		for (int i=0; i<result.length; i++)
			result[i] = keyToBytes(keys[i]);
		return result;
	}
	
	private Set<String> keySetFromBytesSet(Set<byte[]> data) {
		Set<String> result = new HashSet<String>();
		for (byte[] keyBytes : data)
			result.add(keyFromBytes(keyBytes));
		return result;
	}
	
	private byte[] valueToBytes(Object object) {
		return serializer.valueToBytes(object);
	}
	
	private Object valueFromBytes(byte[] bytes) {
		return serializer.valueFromBytes(bytes);
	}
	
	private byte[][] valuesToBytesArray(Object... objectArray) {
		byte[][] data = new byte[objectArray.length][];
		for (int i=0; i<data.length; i++)
			data[i] = valueToBytes(objectArray[i]);
		return data;
	}
	
	private void valueSetFromBytesSet(Set<byte[]> data, Set<Object> result) {
		for (byte[] d : data)
			result.add(valueFromBytes(d));
	}
	
	@SuppressWarnings("rawtypes")
	private List valueListFromBytesList(List<byte[]> data) {
		List<Object> result = new ArrayList<Object>();
		for (byte[] d : data)
			result.add(valueFromBytes(d));
		return result;
	}
	
	// ---------
	
	public String getName() {
		return name;
	}
	
	public ISerializer getSerializer() {
		return serializer;
	}
	
	public IKeyNamingPolicy getKeyNamingPolicy() {
		return keyNamingPolicy;
	}
	
	// ---------
	
	public Jedis getJedis() {
		Jedis jedis = threadLocalJedis.get();
		return jedis != null ? jedis : jedisPool.getResource();
	}
	
	public void close(Jedis jedis) {
		if (threadLocalJedis.get() == null && jedis != null)
			jedis.close();
	}
	
	public Jedis getThreadLocalJedis() {
		return threadLocalJedis.get();
	}
	
	public void setThreadLocalJedis(Jedis jedis) {
		threadLocalJedis.set(jedis);
	}
	
	public void removeThreadLocalJedis() {
		threadLocalJedis.remove();
	}
}






