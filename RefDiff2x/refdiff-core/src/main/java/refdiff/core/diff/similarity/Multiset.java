package refdiff.core.diff.similarity;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

public class Multiset<E> implements Collection<E> {
	
	private HashMap<E, Integer> map;
	private int count;
	
	public Multiset() {
		this.map = new HashMap<E, Integer>();
		this.count = 0;
	}
	
	public E getFirst() {
		return map.keySet().iterator().next();
	}
	
	public Multiset<E> minus(Multiset<E> other) {
		if (other.isEmpty()) {
			return this;
		}
		Multiset<E> result = new Multiset<E>();
		for (Entry<E, Integer> e : map.entrySet()) {
			Integer thisCount = e.getValue();
			Integer otherCount = other.map.get(e.getKey());
			Integer diff;
			if (otherCount == null) {
				diff = thisCount;
			} else {
				diff = thisCount - otherCount;
			}
			if (diff > 0) {
				result.add(e.getKey(), diff);
			}
		}
		return result;
	}
	
	public Multiset<E> minusElements(Collection<E> other) {
		Multiset<E> result = new Multiset<E>();
		Set<E> keysToIgnore = new HashSet<>(other);
		for (Entry<E, Integer> e : map.entrySet()) {
			if (!keysToIgnore.contains(e.getKey())) {
				result.add(e.getKey(), e.getValue());
			}
		}
		return result;
	}
	
	public Multiset<E> plus(Multiset<E> other) {
		Multiset<E> result = new Multiset<E>();
		for (Entry<E, Integer> e : map.entrySet()) {
			result.add(e.getKey(), e.getValue());
		}
		for (Entry<E, Integer> e : other.map.entrySet()) {
			result.add(e.getKey(), e.getValue());
		}
		return result;
	}
	
	public Multiset<E> minus(E entity) {
		if (map.containsKey(entity)) {
			Multiset<E> result = new Multiset<E>();
			for (Entry<E, Integer> e : map.entrySet()) {
				if (!e.getKey().equals(entity)) {
					result.add(e.getKey(), e.getValue());
				}
			}
			return result;
		}
		return this;
	}
	
	public boolean add(E e, int cardinality) {
		Integer value = map.get(e);
		this.count += cardinality;
		if (value != null) {
			map.put(e, value + cardinality);
			return true;
		} else {
			map.put(e, cardinality);
			return false;
		}
	}
	
	public int getMultiplicity(E entity) {
		Integer value = map.get(entity);
		return value == null ? 0 : value.intValue();
	}
	
	public int size() {
		return count;
	}
	
	public boolean isEmpty() {
		return count == 0;
	}
	
	public boolean contains(Object o) {
		return map.containsKey(o);
	}
	
	public Iterator<E> iterator() {
		return map.keySet().iterator();
	}
	
	public Object[] toArray() {
		return map.keySet().toArray();
	}
	
	public <T> T[] toArray(T[] a) {
		return map.keySet().toArray(a);
	}
	
	public boolean add(E e) {
		return this.add(e, 1);
	}
	
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}
	
	public boolean containsAll(Collection<?> c) {
		return map.keySet().containsAll(c);
	}
	
	public boolean addAll(Collection<? extends E> c) {
		for (E e : c) {
			this.add(e);
		}
		return true;
	}
	
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}
	
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}
	
	public void clear() {
		map.clear();
		count = 0;
	}
	
	public String toString() {
		return map.toString();
	}
	
	public Set<E> asSet() {
		return map.keySet();
	}
	
}
